#!/usr/bin/env bash
# mutation-guard — rollback à prova de morte de sessão para teste por mutação.
#
# O problema: a prova por mutação (alterar arquivo rastreado, rodar o teste, restaurar) é a
# técnica mais forte da análise — é ela que prova que a causa apontada é a causa real:
# desfaz o fix e o bug volta; refaz e some. Mas se a sessão morrer entre o "alterar" e o "restaurar", o arquivo fica
# modificado sem ninguém saber.
#
# A defesa: ANTES de qualquer mutação, o manifesto e os backups vão para um diretório ESTÁVEL
# (~/.claude/mutation-guard), fora do repo e fora do scratchpad da sessão (que é descartado).
# Qualquer sessão futura — e o hook SessionStart — encontra o manifesto órfão e restaura sozinha.
#
# Uso:
#   mutation-guard.sh check                          # restaura manifesto órfão, se houver (idempotente)
#   mutation-guard.sh begin <motivo> <arq> [arq...]  # backup + manifesto, então pode mutar
#   mutation-guard.sh end                            # restaura tudo e apaga o manifesto
#   mutation-guard.sh status                         # mostra o estado atual
#
# Restauração é sempre IN-PLACE (`cat backup > arquivo`), nunca `mv`/`cp` — bind mount de
# container não propaga arquivo substituído por rename (inode novo), e o teste
# passaria a rodar contra uma versão defasada.
set -uo pipefail

GUARD_DIR="${MUTATION_GUARD_DIR:-$HOME/.claude/mutation-guard}"
MANIFEST="$GUARD_DIR/active.json"
REPO="${MUTATION_GUARD_REPO:-${CLAUDE_PROJECT_DIR:-$PWD}}"

die() { printf '\033[31mERRO:\033[0m %s\n' "$*" >&2; exit 1; }
info() { printf '%s\n' "$*"; }
ok() { printf '\033[32m%s\033[0m\n' "$*"; }
warn() { printf '\033[33m%s\033[0m\n' "$*"; }

need_python() { command -v python3 >/dev/null 2>&1 || die "python3 é obrigatório"; }

# ---------------------------------------------------------------- restore
# Restaura a partir do manifesto. Idempotente: sem manifesto, não faz nada e sai 0.
do_restore() {
  local quiet="${1:-}"
  if [ ! -f "$MANIFEST" ]; then
    [ -n "$quiet" ] || info "mutation-guard: nada pendente."
    return 0
  fi

  need_python
  warn "mutation-guard: manifesto encontrado — restaurando arquivos mutados."
  python3 - "$MANIFEST" <<'PY'
import hashlib, json, os, sys

manifest_path = sys.argv[1]
with open(manifest_path) as fh:
    m = json.load(fh)

print(f"  motivo   : {m.get('reason','?')}")
print(f"  iniciado : {m.get('started_at','?')}  (sessao {m.get('session_id','?')})")

def md5(p):
    h = hashlib.md5()
    with open(p, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()

falhas = []
for item in m.get('files', []):
    path, backup, orig = item['path'], item['backup'], item['md5_original']
    if not os.path.exists(backup):
        falhas.append(f"backup sumiu: {backup} (arquivo {path} NAO restaurado)")
        continue
    if os.path.exists(path) and md5(path) == orig:
        print(f"  = intacto  {path}")
        continue
    # IN-PLACE: preserva o inode para bind mount de container enxergar a mudanca.
    with open(backup, 'rb') as src, open(path, 'wb') as dst:
        dst.write(src.read())
    novo = md5(path)
    if novo == orig:
        print(f"  ✓ restaurado {path}")
    else:
        falhas.append(f"md5 divergente apos restaurar {path}: {novo} != {orig}")

if falhas:
    print("\nFALHAS:")
    for f in falhas:
        print("  ! " + f)
    sys.exit(1)
PY
  local rc=$?
  if [ $rc -ne 0 ]; then
    die "restauração incompleta — o manifesto foi PRESERVADO em $MANIFEST. Resolva à mão antes de seguir."
  fi

  # Confirma com o git que os arquivos rastreados voltaram ao estado limpo.
  if [ -d "$REPO/.git" ]; then
    local sujos
    sujos=$(cd "$REPO" && python3 - "$MANIFEST" <<'PY'
import json, subprocess, sys
m = json.load(open(sys.argv[1]))
paths = [i['path'] for i in m.get('files', [])]
if paths:
    r = subprocess.run(['git', 'status', '--porcelain', '--'] + paths,
                       capture_output=True, text=True)
    sys.stdout.write(r.stdout)
PY
)
    if [ -n "$sujos" ]; then
      warn "git ainda vê modificação nos arquivos restaurados:"
      printf '%s\n' "$sujos"
      warn "(esperado se algum deles era untracked — confira antes de prosseguir)"
    else
      ok "git limpo nos arquivos restaurados."
    fi
  fi

  mv "$MANIFEST" "$GUARD_DIR/restored-$(date -u +%Y%m%dT%H%M%SZ).json"
  ok "mutation-guard: rollback concluído."
}

# ---------------------------------------------------------------- begin
do_begin() {
  [ $# -ge 2 ] || die "uso: mutation-guard.sh begin <motivo> <arquivo> [arquivo...]"
  local reason="$1"; shift

  if [ -f "$MANIFEST" ]; then
    die "já existe mutação ativa ($MANIFEST). Rode 'mutation-guard.sh check' primeiro."
  fi

  need_python
  local stamp; stamp=$(date -u +%Y%m%dT%H%M%SZ)
  local backup_dir="$GUARD_DIR/backups/$stamp"
  mkdir -p "$backup_dir" || die "não consegui criar $backup_dir"

  MUTATION_GUARD_REASON="$reason" \
  MUTATION_GUARD_STAMP="$stamp" \
  MUTATION_GUARD_BACKUPDIR="$backup_dir" \
  MUTATION_GUARD_MANIFEST="$MANIFEST" \
  MUTATION_GUARD_REPO="$REPO" \
  python3 - "$@" <<'PY'
import hashlib, json, os, shutil, sys, datetime

reason  = os.environ['MUTATION_GUARD_REASON']
stamp   = os.environ['MUTATION_GUARD_STAMP']
bdir    = os.environ['MUTATION_GUARD_BACKUPDIR']
mpath   = os.environ['MUTATION_GUARD_MANIFEST']
repo    = os.environ['MUTATION_GUARD_REPO']

def md5(p):
    h = hashlib.md5()
    with open(p, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()

files = []
for i, arg in enumerate(sys.argv[1:]):
    path = arg if os.path.isabs(arg) else os.path.join(repo, arg)
    if not os.path.isfile(path):
        print(f"ERRO: não é arquivo: {path}", file=sys.stderr)
        sys.exit(1)
    backup = os.path.join(bdir, f"{i:02d}_{os.path.basename(path)}")
    shutil.copy2(path, backup)
    files.append({'path': path, 'backup': backup, 'md5_original': md5(path)})
    print(f"  backup {path}")

os.makedirs(os.path.dirname(mpath), exist_ok=True)
with open(mpath, 'w') as fh:
    json.dump({
        'session_id': os.environ.get('CLAUDE_SESSION_ID', 'desconhecida'),
        'repo': repo,
        'reason': reason,
        'started_at': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'stamp': stamp,
        'files': files,
    }, fh, indent=1, ensure_ascii=False)
print(f"  manifesto: {mpath}")
PY
  [ $? -eq 0 ] || die "falha ao preparar o backup — NÃO mute nada."
  ok "mutation-guard: backup pronto ($# arquivo(s)). Pode mutar. Rode 'end' ao terminar."
}

ANALISE_FLAG="$GUARD_DIR/analise-active.flag"

case "${1:-}" in
  check)
    do_restore ""
    # `check --analise` = preflight da skill: liga a cerca read-only desta
    # sessão (o hook só aplica a política com o marcador presente). `check`
    # puro (SessionStart de toda sessão) apaga marcador órfão da anterior.
    if [ "${2:-}" = "--analise" ]; then
      mkdir -p "$GUARD_DIR" && touch "$ANALISE_FLAG"
      ok "mutation-guard: cerca read-only ATIVADA para esta análise."
    else
      rm -f "$ANALISE_FLAG"
    fi ;;
  begin)  shift; do_begin "$@" ;;
  end)    [ -f "$MANIFEST" ] || die "nenhuma mutação ativa."; do_restore "" ;;
  status)
    if [ -f "$MANIFEST" ]; then
      warn "MUTAÇÃO ATIVA:"; cat "$MANIFEST"
    else
      ok "nenhuma mutação ativa."
    fi ;;
  *) die "uso: mutation-guard.sh {check|begin <motivo> <arq>...|end|status}" ;;
esac
