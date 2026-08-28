---
description: Abrir PR de release (promoção dev → main)
---

# /pr-release — Promoção `dev → main`

Corta uma branch de promoção da `dev`, monta o changelog e abre o PR pra `main`.
O merge é **manual, pelo dono do repositório**.

> **Idioma:** título, changelog e corpo do PR **em inglês** (CLAUDE.md, seção 6).

---

## 1. Pré-checagem

```bash
git fetch origin main dev
git log origin/main..origin/dev --oneline        # o que vai subir
git diff origin/main...origin/dev --stat
gh pr list --base main --json number,title,url   # já existe promoção aberta?
```

- Se não há diferença entre `main` e `dev`, **pare**: não há o que promover.
- Se já existe PR de promoção aberto, informe e **pare**.
- Se há PR aberto na `dev` que o dev esperava incluir, avise antes de seguir.

---

## 2. Cortar a branch de promoção

```bash
git switch -c chore/release-dev-to-main-$(date +%Y%m%d-%H%M%S) origin/dev
git push -u origin HEAD
```

---

## 3. Montar o changelog

A partir de `git log origin/main..origin/dev`, agrupe por tipo (feature / bugfix / refactor /
chore) e escreva **em inglês, orientado a impacto** — o que muda para quem usa ou lê o
projeto, não a mensagem crua do commit. Cite o número do PR quando existir.

Destaque em bloco próprio:
- **Database migrations** incluídas.
- **Environment variables / Secrets** novas ou alteradas — e o que precisa ser ajustado no
  `.env.example` e nos manifestos do K8s.
- **Image tags** que precisam ser reconstruídas ou republicadas.
- **Breaking changes** e o que quebra.

---

## 4. Abrir o PR

```bash
gh pr create --base main --label pending --label ready-to-review \
  --title "[chore][release] promote dev to main $(date +%Y-%m-%d)" \
  --body "<changelog em inglês>"
```

---

## 5. Regras absolutas

- **NUNCA** mergear a promoção — é decisão do dono.
- **NUNCA** cherry-pickar "só uma parte" da `dev` — promoção leva a `dev` como está.
  Se algo não pode subir, o caminho é reverter na `dev` antes.
- **NUNCA** incluir `Co-Authored-By`.
