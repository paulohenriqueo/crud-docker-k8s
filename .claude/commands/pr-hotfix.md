---
description: Abrir PR de hotfix direto na main (correção isolada da branch estável)
---

# /pr-hotfix — Hotfix isolado para `main`

Abre PR de uma branch `hotfix/*` **cortada de `main`** direto pra `main`. Usar só quando algo
está quebrado na branch estável e a correção **não pode** esperar o ciclo normal pela `dev`.

> **Não é hotfix se pode esperar.** O lane existe pra isolar uma correção — não pra pular
> review por pressa. Se aguenta o ciclo, use `/pr-abrir`.

> **Idioma:** título e corpo do PR **em inglês** (CLAUDE.md, seção 6).

---

## 1. Validações (bloqueantes)

1. **Branch é `hotfix/descricao-kebab-case`**, em inglês. Outro prefixo → pare.
2. **Branch NÃO contém a `dev` inteira.** Este é o teste que define hotfix:

   ```bash
   git fetch origin main dev
   git rev-list --count origin/main..HEAD        # deve ser pequeno (os commits do fix)
   git merge-base --is-ancestor origin/dev HEAD  # se retornar 0, a dev está dentro → RECUSE
   ```

   Se a `dev` está contida na branch, o "hotfix" arrasta todo o trabalho não-liberado.
   **Recuse e explique** — a branch precisa ser recortada de `main`.
3. **Sem mudança não commitada.**
4. **Diff enxuto e coerente com a falha.** Se o diff toca domínio sem relação com o problema,
   aponte antes de abrir.

---

## 2. Criar o PR

```bash
git push -u origin <branch>

gh pr create --base main \
  --title "[hotfix][<contexto>] <short description>" \
  --body "$(cat <<'BODY'
## What broke
<what is broken on the stable branch, since when>

## Root cause
<the root cause, with the evidence that proves it>

## What changed
<the change, file by file>

## Why it fixes it for everyone
<why this is not a workaround for a single case>

## How did you test this
<the actual validation performed>

## Why it does not go through dev
<what requires the isolated lane>
BODY
)"
```

---

## 3. Depois do merge (responsabilidade do dev)

1. Confirmar que a `main` está sã.
2. **Garantir o back-merge `main → dev`.** Sem ele a `dev` regride e a próxima promoção
   **reverte o hotfix**. Se conflitar, resolver na hora.

---

## 4. Regras absolutas

- **NUNCA** mergear — merge na branch estável é decisão do dono.
- **NUNCA** abrir hotfix de branch cortada da `dev`.
- Hotfix **passa por review** como qualquer PR.
- **NUNCA** incluir `Co-Authored-By`.
