---
description: Code Review de Pull Request
---

# /review-pr — Code Review de Pull Request

Revise o PR #$ARGUMENTS com rigor de Staff Engineer. É um projeto de estudo e portfólio: o
padrão de exigência é o de mercado, mas calibrado pelo objetivo — o que o projeto se propõe a
demonstrar é **containerização e orquestração**.

**FORMATO OBRIGATÓRIO:** a resposta DEVE começar com `## PR #N — Título` e seguir o template
da seção 3. Sem raciocínio, rascunho ou comentário de progresso antes do review.

---

## 1. Coleta

Em paralelo:
- `gh pr view $ARGUMENTS --json title,body,state,baseRefName,headRefName,additions,deletions,author,url,commits`
- `gh pr diff $ARGUMENTS`
- `gh pr checks $ARGUMENTS`
- `gh pr diff $ARGUMENTS --name-only`

Diff muito grande: salve em arquivo e leia em partes. **Nunca pule arquivo.**
Acima de ~2000 linhas, paralelize a leitura com subagentes.

**Pule arquivo gerado:** lock files, bundle buildado, snapshot. Confira só que acompanham
o source — não analise o conteúdo.

---

## 2. Checklist — avalie cada item aplicável, arquivo a arquivo

### 2.1 Segurança
- [ ] **Injection** — SQL injection, query crua sem parametrização, concatenação em JPQL
- [ ] **Secret exposto** — senha, token ou connection string em código, manifesto, log ou
      `application.properties` versionado (**bloqueante**)
- [ ] **Ownership** — DELETE/PUT/PATCH confirma que o recurso existe antes de agir
- [ ] **Mass assignment** — entidade JPA exposta direto no controller, body cru no save/update
- [ ] **Validação de input** — `@Valid` + Bean Validation em todo body recebido
- [ ] **CORS** — `*` combinado com credenciais; origem chumbada em vez de env var
- [ ] **Open redirect** — header do cliente (`origin`/`referer`/`host`) montando URL
- [ ] **XSS** — input do usuário renderizado sem escape
- [ ] **Ferramenta de debug** em dependência de produção ou na imagem final

### 2.2 Lógica e resiliência
- [ ] **Error handling** — catch silencioso, fallback que esconde falha
- [ ] **Null safety** — acesso a propriedade possivelmente nula sem guard; `Optional` ignorado
- [ ] **Estado inconsistente** — update otimista no front sem ressincronizar
- [ ] **Timeout** em toda chamada a serviço externo
- [ ] **Transação** — operação multi-step sem `@Transactional`
- [ ] **Dependência de ordem de inicialização** — backend que morre se o MySQL ainda não
      respondeu, em vez de retry com backoff

### 2.3 Qualidade
- [ ] **DRY** — duplicação entre componentes, utilitário definido em dois arquivos
- [ ] **SOLID** — arquivo/componente com 500+ linhas, acoplamento alto, lógica no controller
- [ ] **Type safety** — cast genérico onde existe tipo específico
- [ ] **Dead code** — import não usado, variável não lida, código comentado
- [ ] **Naming** — nome genérico (`data`, `res`, `tmp`), negação dupla, mistura pt/en
- [ ] **Hardcoded** — magic number, URL, timeout sem constante

### 2.4 Docker e Kubernetes (o núcleo do projeto)
- [ ] **Dockerfile multi-stage** — imagem final sem SDK, sem Maven, sem `node_modules`
- [ ] **Tag da imagem base fixada** e específica; `:latest` é **bloqueante**
- [ ] **`.dockerignore`** presente e cobrindo `.git/`, `target/`, `node_modules/`
- [ ] **Container não roda como root** — `USER` no estágio final
- [ ] **Uma responsabilidade por container**
- [ ] **Estado fora do container** — volume nomeado (Compose) ou PVC (K8s)
- [ ] **`readinessProbe` e `livenessProbe`** em todo Deployment
- [ ] **`resources.requests` e `resources.limits`** em todo container
- [ ] **Secret vs ConfigMap** — valor sensível em ConfigMap é **bloqueante**
- [ ] **Service discovery por nome**, nunca IP chumbado
- [ ] **Manifesto versionado** e coerente com o que o PR diz que faz
- [ ] **Env var nova** documentada no `.env.example` e refletida no Compose e no K8s

### 2.5 Frontend
- [ ] **URL da API por env var** (`VITE_API_URL`) — `localhost` chumbado é **bloqueante**,
      quebra a imagem em qualquer ambiente que não seja a máquina do dev
- [ ] **Feedback de erro** em toda ação; rollback silencioso é proibido
- [ ] **Estado de carregamento** visível em chamada de rede
- [ ] **Ação destrutiva** sem dialog de confirmação
- [ ] **Acessibilidade** — fonte ≥ 12px, `label` associado ao input, contraste, `aria-label`
      em botão só com ícone
- [ ] **Performance** — lista sem paginação, efeito sem cleanup

### 2.6 Backend
- [ ] **Listagem sem `Pageable`** — `findAll()` sem limite
- [ ] **N+1** em loop acessando relação; seleção de colunas desnecessárias
- [ ] **Status HTTP** — 201 + `Location` no POST, 204 no DELETE, 404 em ausente, 400 em
      validação, 409 em conflito
- [ ] **Migration** correspondente a toda mudança de schema; migration antiga não editada;
      `ddl-auto` diferente de `validate` fora do local
- [ ] **Índice** em campo usado no WHERE (ex.: `email` único)
- [ ] **Log** — dado sensível logado; remoção de log sem justificativa

### 2.7 Infra e repositório
- [ ] **Dependência de dev** listada como produção
- [ ] **Arquivo de IDE** versionado (`.vscode/`, `.idea/`)
- [ ] **`.env` ou secret commitado** (**bloqueante**)
- [ ] **Higiene de git** — merge commit desnecessário, arquivo não relacionado ao PR,
      `Co-Authored-By` presente (proibido neste repo)
- [ ] **Idioma** — commit, branch e PR em inglês; documentação e UI em português

### 2.8 Escopo
- [ ] PR faz o que o título promete? Mudança não relacionada misturada?
- [ ] Mudança de infra e de código de aplicação no mesmo PR sem motivo?
- [ ] Test plan cobre happy path **e** edge case?
- [ ] Breaking change em contrato de API?

**Contexto do PR é obrigatório:** código pré-existente que o PR não alterou **não é
flaggeado**. Cada issue precisa fazer sentido dado o que o PR está fazendo.

**Calibragem:** é projeto de estudo. Falta de teste de borda em CRUD trivial é ⚠️, não 🟡.
Já Dockerfile sem multi-stage, Secret vazado, `:latest` ou Deployment sem probe são 🔴/🟡
mesmo aqui — são exatamente o que o projeto existe para demonstrar.

---

## 3. Output

| Emoji | Severidade | Bloqueia? | Significado |
|---|---|---|---|
| 🔴 | CRÍTICO | **SIM** | Segurança, secret exposto, perda de dados, container inutilizável |
| 🟡 | IMPORTANTE | **SIM** | Bug, regressão, violação de regra do CLAUDE.md |
| ⚠️ | MENOR | NÃO | Tech debt, melhoria, sugestão |
| ✅ | OK | — | Item de review anterior corrigido |

**Veredicto:** 🔴 ou 🟡 presentes → `request-changes`. Só ⚠️ ou zero issues → `approve`.

```
## PR #N — Título

**Autor:** X | **Base:** dev | **+A/-D** | N arquivos

📋 (1-2 frases: o que o PR faz e por quê. Sem repetir título/body.)

### 🔴 Críticos
(lista numerada — só se houver)

### 🟡 Importantes
(lista numerada — só se houver)

### ⚠️ Menores
(lista numerada — só se houver)

### ✅ Corrigidos (se re-review)

---

### 💬 Feedback
(OPCIONAL — só quando o trabalho for genuinamente acima da média: implementação completa
e organizada, iniciativa própria em teste/infra/DX, refator que simplificou sem quebrar,
atenção a edge case/observabilidade sem ninguém pedir. Tom de tech lead, 2-3 frases.
Código normal NÃO recebe esta seção.)

🤖 Review automatizado via Claude Code
```

---

## 4. Regras absolutas

- **NUNCA aprovar** PR com secret exposto ou `.env` commitado.
- **NUNCA aprovar** PR com Dockerfile ou manifesto que deixe o container inutilizável.
- **NUNCA ignorar** arquivo do diff (exceto gerados, da seção 1).
- **NUNCA elogiar por educação** — omita a seção 💬 se não houver destaque real.
- **SEMPRE verificar** se o fix de um review anterior realmente resolveu o problema.
- **Só 🔴 e 🟡 bloqueiam.** PR só com ⚠️ é aprovado.
- **NUNCA executar `gh pr review` / `gh pr comment`** — a saída é só no terminal.
