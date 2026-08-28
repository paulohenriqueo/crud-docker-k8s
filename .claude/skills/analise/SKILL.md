---
name: analise
description: Roda análise de causa raiz e simulação de bug com autonomia total e SEM pedir confirmação, em modo somente-leitura — lê código, mede o ambiente em execução (containers, cluster, banco só SELECT) e prova a causa com teste automatizado e teste por mutação protegido por rollback à prova de crash. Nunca commita, nunca abre PR, nunca altera estado do ambiente, nunca altera código do projeto fora do que a simulação exige. Use quando o dev pedir "analisa o bug X", "investiga isso", "reproduz o problema", "por que o pod não sobe", "faz a varredura", ou /analise.
argument-hint: [a pergunta a investigar]
disable-model-invocation: true
allowed-tools: Read Bash Write Edit WebFetch WebSearch TodoWrite Agent Skill ToolSearch
hooks:
  PreToolUse:
    - matcher: "Bash|Edit|Write|NotebookEdit"
      hooks:
        - type: command
          command: "${CLAUDE_PROJECT_DIR}/.claude/hooks/analise-readonly-guard.py"
          timeout: 10
---

# /analise — investigação autônoma, somente leitura

Investiga e **prova** a causa de um problema sem parar pra pedir permissão a cada passo, e sem
poder estragar nada. Autonomia total dentro de uma cerca que o harness impõe — não a minha
boa vontade.

**A cerca é mecânica.** O hook `PreToolUse` declarado no frontmatter inspeciona o
**conteúdo** de cada comando antes dele rodar. Ele existe porque `settings.json` libera
regras por prefixo (`Bash(docker exec *)`), e `docker exec ... -c 'UPDATE ...'` casa nesse
prefixo — regra por prefixo não enxerga dentro do comando; o hook enxerga. O escopo do hook
é a skill: o trabalho normal de commit/PR fora do `/analise` continua intocado.

## O que roda sem perguntar

| Categoria | Exemplos |
|---|---|
| Código | `grep`/`rg`/`find`, `git log`/`show`/`blame`/`diff`/`status`, leitura de qualquer arquivo |
| Containers | `docker ps`, `docker logs`, `docker inspect`, `docker compose config`, `docker exec` de leitura |
| Kubernetes | `kubectl get`/`describe`/`logs`/`top`/`explain`, `kubectl rollout status` |
| Banco | cliente MySQL **só SELECT**, `SHOW`, `DESCRIBE`, `EXPLAIN` |
| Rede | `curl` na API local, `kubectl port-forward` para leitura |
| Simulação | rodar a suíte de testes, criar teste **novo** nos diretórios de teste |
| GitHub | `gh search prs`, `gh pr view`, `gh api` (GET) |

## O que o hook bloqueia, mesmo se eu tentar

`INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE` em contexto de execução · `git commit/push/reset/
branch/stash/rebase/merge` · `gh pr create/merge/close/comment` · `kubectl apply/delete/scale/
patch` · `docker rm/stop/restart/build/run` · `rm`/`mv`/`chmod` · redirecionamento `>` fora do
scratchpad · migration · `npm install` · `crontab` · `Write`/`Edit` em qualquer arquivo do
projeto que não seja teste novo ou mutação com backup registrado.

Bloqueio devolve o motivo. Se um comando legítimo for barrado, **não contorne** — avise, e a
regra é ajustada em `.claude/hooks/analise-readonly-guard.py`.

---

## Protocolo

### 0. Regra do dado atual (vale para todos os passos)

**Toda conclusão descreve o sistema como ele está HOJE, medido nesta sessão.** Nenhum número
entra no relatório sem ter sido colhido agora, na fonte viva.

- **Análise anterior é hipótese, não evidência.** Memória, `*.md` do repo e histórico da
  conversa dizem *onde medir* e *o que já foi visto* — nunca *como está*. Antes de repetir
  qualquer número de lá, re-medir. Se o valor mudou, o veredito antigo pode ter invertido.
- **Carimbar tudo.** Cada medição sai com data/hora e fonte.
- **Não preencher buraco com valor velho.** Fonte inacessível vira "não verificado", com o
  que faltou medir — nunca o número da vez passada.

**Armadilha central deste projeto: o arquivo no repositório ≠ o que está rodando.**

- **Dockerfile e manifesto YAML descrevem intenção, não estado.** Um `kubectl apply` que
  falhou deixa o cluster na versão anterior enquanto o arquivo já mostra a nova. Confirmar com
  `kubectl rollout status` e `kubectl describe`, nunca lendo só o YAML.
- **Imagem não é o código do checkout.** A imagem em execução foi buildada num momento
  específico; se o build não rodou depois da última edição, o container está com código velho.
  Conferir com `docker inspect --format '{{.Created}}'` e comparando o conteúdo servido.
- **Container reiniciado não é container recriado.** `restart` mantém a imagem antiga.
- **O banco pode ter schema defasado.** Se `ddl-auto` não é `update` e a migration não rodou,
  a tabela real diverge da entidade. `DESCRIBE usuario` decide, não a classe Java.
- **Pod `Running` não significa pronto.** Ler `READY` (`1/1` vs `0/1`) e as probes em
  `kubectl describe pod` — pod sem readiness passando não recebe tráfego do Service.

### 1. Preflight (sempre, antes de qualquer coisa)

```bash
.claude/skills/analise/scripts/mutation-guard.sh check --analise
```

Restaura arquivo que uma análise anterior tenha deixado mutado por morte de sessão
(idempotente) e **liga a cerca read-only** desta sessão (marcador `analise-active.flag`,
TTL 12h). O hook só aplica a política com o marcador presente — sem ele, fica inerte. O
`SessionStart` de toda sessão roda `check` puro, que apaga marcador órfão da anterior.

### 2. Coleta do relato

Reúna **tudo** que o relato tem antes de opinar: descrição, mensagem de erro completa (não o
resumo dela), comandos que o dev rodou, prints e logs. **Leia cada imagem e cada frame** —
evidência não lida é evidência que não existe na análise.

Para falha de infra, o relato mínimo é: o comando exato, a saída completa, e o estado do
ambiente (`docker compose ps` / `kubectl get pods -o wide`).

### 3. Levantamento no código

Varra o código e cruze com `git log -S`/`-L` pra separar **regressão** de **bug de origem**.
Em mudança de infra, `git log -p` no Dockerfile/manifesto costuma responder sozinho.

### 4. Medição no ambiente em execução (só leitura, sempre no estado de hoje)

Sem número medido, é palpite. Roteiro por camada:

```bash
docker compose ps                       # o que está de pé
docker compose logs --tail=200 <svc>    # o erro real, não o sintoma
kubectl get pods -o wide                # READY, RESTARTS, NODE
kubectl describe pod <pod>              # Events — é onde a causa aparece
kubectl logs <pod> --previous           # log do container que morreu
```

`RESTARTS` alto com `CrashLoopBackOff` → a causa está no log **anterior**, não no atual.
Credenciais nunca são ecoadas.

### 5. Simulação

Teste **novo**, isolado e descartável, no diretório de teste da camada afetada
(`backend/src/test/java/...` ou `frontend/src/**/*.test.*`). Schema/fixture próprios,
transação com rollback. Não asserir sobre artefato gerado no build.

### 6. Prova por mutação (o passo que separa diagnóstico de prova)

Teste verde só mostra que o problema existe. Mutação mostra **o que exatamente o causa** — e
se corrigir um ponto só resolve. Sempre com o guard:

```bash
G=.claude/skills/analise/scripts/mutation-guard.sh
$G begin "cenário A" caminho/Arquivo.ext outro/Arquivo.ext   # backup + manifesto
#   ... aplicar a mutação, rodar o teste, anotar quais casos flipam ...
$G end                                                        # restaura e confere md5
```

`begin` recusa se já houver mutação ativa. `end` restaura **in-place** (`cat backup >
arquivo`), nunca `mv`/`cp` — bind mount de container não propaga arquivo trocado por rename,
e o teste passaria a rodar contra versão defasada. Sem `begin`, o hook **nega** a escrita.

Rode um cenário por ponto suspeito e um com todos. A tabela é o entregável:

| cenário | destrava | casos que flipam |
|---|---|---|
| A | só o ponto 1 | … |
| B | só o ponto 2 | … |
| AB | os dois | … |

### 7. Relatório

Causa raiz ancorada em `arquivo:linha` (ou `manifesto:campo`), medições com data/hora, o que
ficou **não verificado**, e o que seria preciso medir pra fechar. Nada de conclusão sem âncora.

Quando a causa for de infra, o relatório diz também **em que camada** ela está: imagem,
configuração do container, manifesto, rede do cluster ou aplicação. Confundir camada é o que
faz o fix ir para o arquivo errado.

---

## Regras duras

- **Não reaproveita dado velho.** Todo número é medido nesta sessão, na fonte viva.
- **Não corrige.** `/analise` diagnostica e prova. Fix é decisão do dono, em sessão separada.
- **Não decide produto.** Onde houver escolha de semântica, apresentar opções com número e
  recomendação — não escolher sozinho.
- **Não confia em subagente.** O hook pode não alcançar subagentes: todo prompt de agente
  desta skill precisa repetir "somente leitura, nada de escrita, SELECT apenas".
- **Não contorna bloqueio.** Barrou, reporta.

## Limites conhecidos (leia antes de confiar)

1. **`allowed-tools` vale pela vez.** A pré-aprovação do frontmatter cobre o turno que
   invocou a skill e expira na próxima mensagem. Em análise longa, quem sustenta o "sem
   confirmação" são as regras do `.claude/settings.json`. Se voltar a perguntar, reinvoque.
2. **Subagentes podem não herdar o hook.** Trate como leak: instrua explicitamente.
3. **O hook lê texto de comando.** Ofuscação (base64, variável montada em runtime) escapa.
   Ele protege contra acidente, não contra intenção.
4. **Rollback cobre morte de sessão**, não `kill -9` no meio do `cat`. Janela de ms.
5. **`settings.json` tem regras amplas herdadas.** Fora da skill elas seguem valendo; o hook
   só cobre `/analise`.

## Arquivos

| Caminho | Papel |
|---|---|
| `.claude/skills/analise/SKILL.md` | este arquivo |
| `.claude/skills/analise/scripts/mutation-guard.sh` | backup/restauração à prova de crash |
| `.claude/hooks/analise-readonly-guard.py` | cerca de somente-leitura (PreToolUse) |
| `~/.claude/mutation-guard/` | manifesto + backups (fora do repo e do scratchpad, de propósito) |

Credenciais ficam **fora do repositório** (`~/.claude/*.env` ou `.env` local ignorado pelo
git). Nunca imprimir, nunca copiar para o repo.
