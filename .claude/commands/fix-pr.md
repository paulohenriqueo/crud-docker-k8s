---
description: Corrigir os issues apontados no code review de um PR
---

# /fix-pr — Corrigir issues de Code Review

## 0. Resolver o PR

`$ARGUMENTS` com número → usa esse. Vazio → `gh pr view --json number --jq .number`
(PR da branch atual). Sem PR aberto → informe e pare. Guarde em `PR_NUMBER`.

## 1. Coleta (paralelo)

- `gh pr view <PR_NUMBER> --json title,body,state,baseRefName,headRefName,url,number`
- `gh pr diff <PR_NUMBER> --name-only`
- `gh api repos/{owner}/{repo}/pulls/<PR_NUMBER>/comments --jq '.[] | {path, line, body, user: .user.login}'`
- `gh pr view <PR_NUMBER> --comments`

Objetivo: o review **mais recente** que contém issues (🔴, 🟡, ⚠️).

## 2. Identificar e priorizar

1. 🔴 Críticos — corrigir TODOS, sem exceção
2. 🟡 Importantes — corrigir TODOS, sem exceção
3. ⚠️ Menores — corrigir TODOS, salvo instrução contrária do dev

Sem issue → informe e pare.

## 3. Preparação

`gh pr checkout <PR_NUMBER>`, confirme a branch, e **pare** se houver mudança não commitada.

## 4. Corrigir

Para cada issue: leia o arquivo inteiro (contexto, não só a linha apontada), aplique a
correção e **valide** (teste/lint/build).

Se um issue for **falso positivo**, não "corrija por corrigir": registre a contestação
técnica com evidência e siga. Dismiss é pra falso positivo, não pra atalho.

## 5. Commit e push

Um commit por grupo coerente de correções, mensagem **em inglês** no formato
`tipo: descrição no imperativo`, descrevendo o que mudou — não "fix review".
Depois `git push`.

**Atenção:** rode `git pull --rebase` antes de pushar. Não há bot de autofix neste
repositório, mas o dev pode ter commitado na branch pela IDE.

## 6. Fechamento

- Peça re-review.
- Reporte: o que foi corrigido, o que foi contestado e por quê.

**NUNCA** mergear. **NUNCA** `push --force`. **NUNCA** incluir `Co-Authored-By`.
