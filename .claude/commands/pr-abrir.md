---
description: Abrir PR da branch atual para dev (nunca main)
---

# /pr-abrir — Abrir Pull Request para `dev`

Cria um Pull Request da branch atual **sempre para `dev`** (nunca `main`), seguindo o
CLAUDE.md deste repositório.

> **Idioma:** título e corpo do PR são escritos **em inglês** (CLAUDE.md, seção 6). Esta
> descrição e a conversa com o dev seguem em português.

---

## 1. Validações (pare e avise se falhar)

- **Branch atual NÃO pode ser `main` nem `dev`** — PR sai de branch de trabalho.
- **Branch segue `tipo/descricao-kebab-case`**, em inglês, com `tipo` ∈
  `feature|bugfix|hotfix|docs|style|refactor|test|chore`.
  Nomes tipo `paulo/x`, `minha-branch`, `fix-bug` são **proibidos**.
- **Sem mudança não commitada** — se houver, pare e peça pra commitar primeiro.

---

## 2. Coleta de contexto

`git fetch origin dev`. Depois, em paralelo:
- `git branch --show-current`
- `git log origin/dev...HEAD --oneline`
- `git diff origin/dev...HEAD --stat`
- `gh pr list --head "$(git branch --show-current)" --json number,url` — se já existe PR
  pra essa branch, informe número/URL e **pare**.

---

## 3. Análise dos commits

Leia TODOS os commits entre `origin/dev` e `HEAD`: o que foi feito, por quê, e quais
domínios foram impactados. **Não invente** nada que não esteja nos commits/diff.

---

## 4. Criar o PR

```bash
git push -u origin <branch>

gh pr create --base dev --label pending --label ready-to-review \
  --title "[<tipo>][<contexto>] <short description>" \
  --body "$(cat <<'BODY'
<corpo seguindo .github/PULL_REQUEST_TEMPLATE.md, em inglês>
BODY
)"
```

**Título:** `[tipo][contexto] short description` — **inglês**, ≤ 70 caracteres.
Contextos usuais neste repo: `backend`, `frontend`, `database`, `docker`, `k8s`, `ci`, `docs`.

**Corpo:** preencher o template do repositório — Description, Type of PR (marcar os
checkboxes aplicáveis e remover os demais), How did you test this, Checklist. Se for
**bugfix**, preencher também **Root cause / What changed / Why it fixes it for everyone**.
Se o PR toca Dockerfile, Compose ou manifesto K8s, preencher também o
**Infrastructure checklist** — ele é a razão de o projeto existir.

---

## 5. Após criar

Exibir a URL. Conferir que o PR tem `pending` e `ready-to-review`; adicionar a label que
faltar imediatamente. Se a label não existir no repositório, criar com
`gh label create <nome>` e seguir.

---

## 6. Regras absolutas

- **SEMPRE** base `dev`. **NUNCA** `main` por aqui (hotfix tem comando próprio).
- **SEMPRE** com `pending` + `ready-to-review`.
- **NUNCA** mergear — só criar.
- **NUNCA** inventar mudança fora dos commits.
- **NUNCA** incluir `Co-Authored-By` em commit ou corpo de PR.
