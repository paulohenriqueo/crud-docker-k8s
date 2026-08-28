#!/usr/bin/env python3
"""PreToolUse guard da skill /analise — libera leitura, barra escrita.

Escopo: só roda enquanto a skill /analise está ativa (declarado no frontmatter da
SKILL.md, não em settings.json) — o trabalho normal de commit/PR fica intocado.

Contrato (docs oficiais de hooks):
  stdin  : JSON com tool_name, tool_input, cwd, session_id, ...
  stdout : {"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                   "permissionDecision": "allow"|"deny",
                                   "permissionDecisionReason": "..."}}
  exit 0 : decisão respeitada. Sem JSON = cai no fluxo normal de permissão.

Política:
  * Bash        — nega por PADRÃO DE CONTEÚDO (escrita/mutação); libera o resto.
  * Edit/Write  — só no scratchpad, em tests/ (arquivo NOVO) ou em arquivo que o
                  mutation-guard já tenha em backup. Fora disso, nega.
  * git/checkout de restauração e mutação de fonte — só com manifesto ativo.

Por que inspecionar CONTEÚDO e não só prefixo: `Bash(docker exec *)` costuma estar no
allow do settings.json, e `docker exec ... -c 'UPDATE ...'` casa nesse prefixo. Regra
por prefixo não enxerga dentro do comando; este hook enxerga.
"""
import json
import os
import re
import sys

REPO = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
GUARD_DIR = os.path.expanduser(
    os.environ.get("MUTATION_GUARD_DIR", "~/.claude/mutation-guard")
)
GUARD_MANIFEST = os.path.join(GUARD_DIR, "active.json")

# Ciclo de vida: o harness mantém hooks de skill até o fim da SESSÃO, mas a
# cerca só vale ENQUANTO a análise está ativa. O marcador é criado pelo
# preflight da skill (mutation-guard.sh check --analise) e removido no
# SessionStart da sessão seguinte (mutation-guard.sh check). TTL defensivo de
# 12h cobre marcador órfão de sessão que morreu antes do próximo SessionStart.
ANALISE_FLAG = os.path.join(GUARD_DIR, "analise-active.flag")

# Zonas onde escrever é sempre permitido (nada de código do projeto vive aqui).
# A memória persistente do Claude entra na lista: é estado do assistente (RCAs,
# aprendizados), não código — e TODA análise termina gravando memória; bloquear
# forçava salvar só depois de desligar a cerca (ou perder o registro se a sessão
# morresse antes).
WRITE_ZONES = (
    "/tmp/claude-",
    GUARD_DIR,
    os.path.expanduser("~/.claude/projects"),
)


def analysis_active():
    try:
        import time
        return (time.time() - os.path.getmtime(ANALISE_FLAG)) < 12 * 3600
    except OSError:
        return False


def decide(action, reason):
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": action,
            "permissionDecisionReason": reason,
        }
    }))
    sys.exit(0)


def defer():
    """Sem JSON = fluxo normal de permissão (vai perguntar)."""
    sys.exit(0)


def guarded_files():
    """Arquivos com backup ativo no mutation-guard (mutação autorizada)."""
    try:
        with open(GUARD_MANIFEST) as fh:
            return {os.path.realpath(i["path"]) for i in json.load(fh).get("files", [])}
    except Exception:
        return set()


# ---------------------------------------------------------------- Bash
# Cada par: (regex, motivo). Case-insensitive, avaliado no comando inteiro —
# inclusive no que vai dentro de `docker exec ... sh -c '...'` e `ssh host '...'`.
BASH_DENY = [
    # --- SQL de escrita. Só dispara em contexto de execução de query, pra não
    # barrar um `grep -rn "UPDATE" arquivo` legítimo durante a análise.
    (r"(?is)(mysql|psql|sqlite3|mongo|redis-cli|pdo|->query|->exec|->prepare|tinker"
     r"|prisma|knex|flyway|liquibase|rails\s+(db|console))"
     r".{0,400}?\b(insert\s+into|update\s+\w+\s+set|delete\s+from|drop\s+(table|database|index)"
     r"|alter\s+table|truncate|replace\s+into|grant\s|revoke\s|create\s+(table|database|user))\b",
     "SQL de escrita em contexto de execução — a análise é somente SELECT"),

    # --- git que altera estado do repositório
    (r"(?i)\bgit\s+(commit|push|reset|revert|rebase|merge|cherry-pick|am\b|apply\b"
     r"|branch|tag|clean|stash|worktree|remote|submodule|filter-branch|gc\b|prune)",
     "git de escrita bloqueado na análise (nada de commit/push/branch)"),

    # --- gh que altera estado remoto
    (r"(?i)\bgh\s+(pr\s+(create|merge|close|reopen|edit|comment|review|ready|checkout)"
     r"|issue\s+(create|close|comment|edit)|release\b|workflow\s+run|run\s+rerun"
     r"|repo\s+(create|delete|edit)|api\b[^|;]*-X\s*(POST|PUT|PATCH|DELETE))",
     "gh de escrita bloqueado — a análise não abre, comenta nem mergeia nada"),

    # --- kubernetes que altera cluster
    (r"(?i)\bkubectl\s+[^|;]*\b(apply|delete|edit|scale|patch|rollout|create|replace"
     r"|cordon|drain|taint|annotate|label|set|expose|autoscale)\b",
     "kubectl de escrita bloqueado — na análise só get/logs/describe"),

    # --- docker que altera containers/imagens (exec é liberado; o conteúdo dele é inspecionado)
    (r"(?i)\bdocker\s+(rm\b|rmi\b|stop|start|restart|kill|pause|unpause|cp\b|build"
     r"|run\b|create|commit|push|network|volume|prune|system\s+prune)",
     "docker de mutação bloqueado (exec de leitura/teste continua liberado)"),
    (r"(?i)\bdocker(\s+compose|-compose)\s+(up|down|restart|stop|rm|build)",
     "docker compose de mutação bloqueado"),

    # --- destruição de arquivo fora das zonas de escrita
    (r"(?i)(^|[;&|]\s*)(rm|rmdir|shred|truncate|dd|mkfs)\b",
     "remoção/truncamento de arquivo bloqueado fora do scratchpad"),
    (r"(?i)(^|[;&|]\s*)(mv|chmod|chown|chgrp|ln)\b",
     "alteração de arquivo/permissão bloqueada na análise"),

    # --- estado do sistema / agendamento
    (r"(?i)\b(systemctl|service\s+\w+\s+(start|stop|restart)|reboot|shutdown|halt|crontab)\b",
     "alteração de serviço/sistema bloqueada"),

    # --- dependências e schema da aplicação
    (r"(?i)\b(composer\s+(install|update|require|remove)|npm\s+(install|i\b|ci\b|update)"
     r"|yarn\s+(add|install)|pnpm\s+(add|install|i\b)|pip\s+(install|uninstall)"
     r"|cargo\s+(add|install)|go\s+(get|install)"
     r"|mvn[^|;]*flyway:(migrate|clean)|flyway\s+(migrate|clean)"
     r"|prisma\s+(migrate|db\s+push)|drizzle-kit\s+(push|migrate))",
     "mutação de dependências/schema bloqueada na análise"),
]

# Redirecionamento que grava fora das zonas de escrita.
# O lookbehind exclui `->` e `=>`, `>=`, `<>` e `2>` — senão um
# `$pdo->query(...)` legítimo é lido como redirecionamento e a análise trava.
REDIR = re.compile(r"(?<![-=<>0-9!+])>{1,2}\s*(?P<path>[^\s;&|]+)")


def check_bash(cmd):
    for pattern, reason in BASH_DENY:
        if re.search(pattern, cmd):
            # `git checkout -- <arq>` e afins são liberados SE houver mutação registrada.
            if "git" in reason and guarded_files() and re.search(r"(?i)\bgit\s+(checkout|restore)\b", cmd):
                continue
            decide("deny", f"[/analise read-only] {reason}.\nComando: {cmd[:200]}")

    for m in REDIR.finditer(cmd):
        path = m.group("path").strip("'\"")
        if path.startswith("/dev/"):
            continue
        full = path if path.startswith("/") else os.path.join(REPO, path)
        if not any(full.startswith(z) for z in WRITE_ZONES):
            decide("deny",
                   f"[/analise read-only] redirecionamento grava fora do scratchpad: {path}")

    if re.search(r"(?i)\btee\b", cmd) and "/dev/null" not in cmd:
        decide("deny", "[/analise read-only] `tee` grava arquivo — use o scratchpad")

    decide("allow", "leitura/análise liberada pela skill /analise")


# ---------------------------------------------------------------- Edit/Write
def check_write(tool, tool_input):
    path = tool_input.get("file_path") or tool_input.get("notebook_path") or ""
    if not path:
        defer()
    full = os.path.realpath(path)

    if any(full.startswith(z) for z in WRITE_ZONES):
        decide("allow", "escrita no scratchpad")

    # Arquivo NOVO em diretório de teste — é o artefato da simulação (autorizado pelo dono).
    # Monorepo: cada camada tem a convenção da sua própria stack.
    #   backend  → Maven, backend/src/test/java
    #   frontend → Vitest, frontend/src (arquivo *.test.* / *.spec.*)
    TEST_DIRS = ("backend/src/test", "frontend/src", "tests")
    test_roots = [os.path.realpath(os.path.join(REPO, d)) for d in TEST_DIRS]
    in_test_dir = any(full.startswith(root + os.sep) for root in test_roots)

    # Em frontend/src convivem código e teste: só arquivo de teste conta.
    if in_test_dir and full.startswith(os.path.realpath(os.path.join(REPO, "frontend/src")) + os.sep):
        in_test_dir = bool(re.search(r"\.(test|spec)\.[jt]sx?$", full))

    if in_test_dir and (tool == "Write" and not os.path.exists(full)):
        decide("allow", "arquivo de teste NOVO — artefato da simulação")
    if in_test_dir and os.path.exists(full):
        # Editar teste que já existe só vale se ele for untracked (criado por nós).
        import subprocess
        r = subprocess.run(["git", "-C", REPO, "ls-files", "--error-unmatch", full],
                           capture_output=True)
        if r.returncode != 0:
            decide("allow", "teste untracked criado pela própria simulação")

    # Mutação de fonte: só com backup registrado no mutation-guard.
    if full in guarded_files():
        decide("allow", "mutação autorizada — arquivo tem backup no mutation-guard")

    decide("deny",
           f"[/analise read-only] escrita bloqueada em {path}.\n"
           "Para mutar código, rode antes:\n"
           "  .claude/skills/analise/scripts/mutation-guard.sh begin \"<motivo>\" <arquivo>")


def main():
    try:
        payload = json.load(sys.stdin)
    except Exception:
        defer()

    tool = payload.get("tool_name", "")
    tool_input = payload.get("tool_input", {}) or {}

    # Fora de análise ativa, o hook é inerte (fluxo normal de permissão) — o
    # registro dele sobrevive à skill, a política não deve sobreviver.
    if not analysis_active():
        defer()

    if tool == "Bash":
        cmd = tool_input.get("command", "") or ""
        if not cmd:
            defer()
        check_bash(cmd)
    elif tool in ("Edit", "Write", "NotebookEdit"):
        check_write(tool, tool_input)
    else:
        defer()


if __name__ == "__main__":
    main()
