# CLAUDE.md

Guia para o Claude Code (claude.ai/code) neste repositório.

---

## 1. Preferências de interação

- **Contexto:** projeto de **estudo e portfólio** sobre containerização e orquestração.
  A aplicação (CRUD de `usuario`) é deliberadamente simples — o objeto de estudo é o
  caminho `desenvolvimento local → Docker → Kubernetes`, não a complexidade do domínio.
  **Não há ambiente de produção nem usuário real.** Nenhuma regra aqui deve ser justificada
  por "impacto em produção"; o que está em jogo é o aprendizado e a qualidade do repositório
  como peça de portfólio.
- Sempre responder em **português brasileiro**. Direto, sem introdução, sem conclusão óbvia,
  sem analogia forçada.
- **Priorizar o que é padrão de mercado.** Quando houver duas formas de fazer, preferir a que
  o dev encontraria numa empresa — o valor do projeto está em treinar a prática real.
  Sinalizar explicitamente quando algo for simplificação didática.
- **Nunca especular sem base concreta.** Se não souber, dizer "não sei" ou "preciso de mais
  contexto". Não inventar caminho de arquivo, nome de função, flag ou comportamento.
- **Nunca concordar por educação (sycophancy).** Ideia com falha recebe contestação técnica
  direta: "não vai funcionar porque X" — não "ótima ideia, mas...".
- **Medir antes de concluir.** Diagnóstico sem evidência é chute. Aqui a fonte viva é o
  container/cluster **rodando** — não o Dockerfile, não o manifesto YAML, não a memória da
  conversa. `docker ps`, `docker logs`, `kubectl describe` e `kubectl logs` são a prova.
- Para código: clean code, SOLID, testável e resiliente (timeout, fallback, observabilidade).
- **Explicar o porquê da infra.** Toda escolha de Docker/K8s (base image, probe, limite de
  recurso, tipo de Service) vem com uma linha dizendo o motivo. Um portfólio que não explica
  a decisão não demonstra domínio.

---

## 2. Autonomia — o que fazer sem perguntar

Executar sem pedir confirmação: ler código, buscar, rodar teste, criar branch, commitar na
própria branch, abrir PR, escrever arquivo novo, rodar build/lint, subir e derrubar ambiente
local (`docker compose up/down`, `kubectl apply` no cluster local).

**Pedir confirmação sempre em:** merge de PR, qualquer escrita em `main`, operação destrutiva
(drop de banco, `rm -rf`, `docker volume rm`, `kubectl delete pvc`, reset de dados), push de
imagem para o Docker Hub, e envio de conteúdo para qualquer serviço externo.

**Nunca `--force` / `push --force`.** Reescrever um PR se faz com commit novo pra frente ou
branch nova — nunca reescrevendo histórico publicado.

---

## 3. Git Flow (OBRIGATÓRIO)

**Push direto em `main` e `dev` é BLOQUEADO. Tudo passa por Pull Request.**

```
# Fluxo normal — toda mudança passa pela dev
dev (base) → sua branch → PR → merge dev → validação → promoção dev→main

# Hotfix — leva SÓ o fix, isolado da dev
main → hotfix/x → PR → merge main → back-merge main→dev
```

`main` é a branch estável (o que uma pessoa vê ao abrir o repositório). `dev` é a branch de
integração, onde o trabalho é validado antes de virar "versão apresentável".

### Regras absolutas

- **NUNCA** commitar ou pushar direto em `main` ou `dev`. Criar branch e abrir PR.
- **NUNCA** commitar `.env` ou qualquer secret. Em Kubernetes, valor sensível vai em `Secret`,
  nunca em `ConfigMap` nem chumbado no manifesto. O repositório versiona apenas
  `secret.example.yaml` com valores fake.
- **NUNCA** criar branch fora do padrão. Formato **obrigatório**: `tipo/descricao-em-kebab-case`,
  **em inglês**. Tipos válidos: `feature`, `bugfix`, `hotfix`, `docs`, `style`, `refactor`,
  `test`, `chore`. Nomes como `paulo/algo`, `minha-branch`, `fix-bug` são **proibidos**.
- **SEMPRE** cortar branch de `dev` — **exceto hotfix**, que sai de `main`.
- **SEMPRE** validar em `dev` antes de promover para `main`.
- **NUNCA** mergear. Merge é decisão do dono — o Claude só cria o PR e aguarda.
- **Commits sem `Co-Authored-By`.**

> **Versionamento e deploy:** ainda não existe esteira de CI/CD neste projeto (previsto para a
> Semana 4). Enquanto não existir, não há arquivo de versão para bumpar e o build de imagem é
> manual. Quando a esteira entrar, a regra passa a ser a de mercado: **versão é escrita
> exclusivamente pela esteira** e deploy nunca é manual — atualizar esta seção junto.

### Hotfix

Hotfix isola uma correção urgente na branch estável, **não** serve para pular review por pressa.
Correção que aguenta o ciclo normal vai pra `dev`.

- A branch sai de **`main`** (`git switch -c hotfix/x main`). Cortada da `dev`, arrastaria
  todo o trabalho não-liberado que está lá — o oposto de um hotfix isolado.
- A branch **DEVE** ser `hotfix/*`.
- Passa por review normal.
- Back-merge `main → dev` é obrigatório logo após o merge — senão a `dev` regride e a
  próxima promoção **reverte o hotfix**.

### Padrão de PR

- **Título:** `[tipo][contexto] short description` — **em inglês**, ≤ 70 caracteres.
  Ex.: `[feature][backend] add usuario REST controller`.
- **Corpo:** seguir `.github/PULL_REQUEST_TEMPLATE.md`, **em inglês**. Bugfix preenche também
  **Root cause / What changed / Why it fixes it for everyone**.
- **Labels:** `pending` + `ready-to-review` em todo PR, sempre, sem pedir confirmação.
- **A cada commit novo**, a aprovação anterior é invalidada e as labels voltam.

### Commits

- **Em inglês**, formato `tipo: descrição no imperativo` (`feature`, `bugfix`, `docs`,
  `style`, `refactor`, `test`, `chore`).
- Corpo explicando o **porquê** quando a mudança não for óbvia pelo diff.
- Um commit por motivo. Mudança de infra e mudança de código de aplicação não entram juntas.

---

## 4. Obrigatoriedades de desenvolvimento

Valem para **todo código escrito**, não só em review. Violar item de "Segurança", "Backend"
ou "Docker/Kubernetes" é bloqueante.

### 4.1 Segurança

- **Validar ownership/existência antes de mutação.** DELETE/PUT/PATCH devem confirmar que o
  recurso existe antes de agir, e responder 404 quando não existe — nunca confiar cegamente
  no ID recebido no request.
  > Isolamento multi-tenant não se aplica aqui: `usuario` é uma entidade única, sem hierarquia
  > de cliente. **Se o projeto ganhar autenticação**, esta regra vira obrigatória: toda query
  > filtra por ownership. Anotar aqui a coluna escolhida quando acontecer.
- **Nunca fazer mass assignment** com o body cru direto no `save`/`update`. Sempre DTO com
  validação ou whitelist explícita de campos — nunca expor a entidade JPA direto no controller.
- **Nunca commitar secret** — senha de banco, token, `.env`. Ler de variável de ambiente e
  manter `.env.example` atualizado.
- **Senha de banco nunca no `application.properties` versionado.** Sempre
  `${MYSQL_PASSWORD}` resolvido por env var, vindo de Secret no K8s.
- **Nunca usar header do cliente (`origin`, `referer`, `host`) para montar URL** sem allowlist.
- **CORS explícito**, nunca `*` com credenciais. A origem do frontend vem de env var.
- **Ferramenta de debug vai em dependência de desenvolvimento**, nunca na imagem final.

### 4.2 Backend — API e banco

- **Toda rota que recebe input DEVE validar** (Bean Validation: `@Valid`, `@NotBlank`,
  `@Email`). Sem validação = aceitando qualquer payload.
- **Listagem sem `limit`/paginação é proibida.** `findAll()` sem `Pageable` pode retornar a
  tabela inteira. Mesmo com 3 registros de teste — o hábito é o que está sendo treinado.
- **Selecionar só as colunas necessárias** em vez de carregar o registro inteiro.
- **Cuidado com N+1** — eager loading sempre que iterar coleção acessando relação.
- **Operação multi-step DEVE ser transacional** (`@Transactional`) quando duas ou mais
  escritas precisam ser atômicas.
- **Chamada a serviço externo SEMPRE com timeout configurado.**
- **Status HTTP correto:** 201 + `Location` no POST, 204 no DELETE, 404 em recurso ausente,
  400 em validação, 409 em conflito (email duplicado). Nunca 200 para tudo.
- **Não remover log de erro** sem justificativa. **Nunca logar** senha, token ou connection
  string completa.
- **Migration já aplicada nunca é editada.** Criar migration nova.
  `ddl-auto` fica em `validate` fora do ambiente local — nunca `update` ou `create-drop`.

### 4.3 Frontend — UI e UX

O foco do projeto é infraestrutura; a UI é intencionalmente simples. Ainda assim:

- **URL da API SEMPRE por variável de ambiente** (`VITE_API_URL`), nunca `localhost` chumbado.
  Isto não é detalhe: é o que permite a mesma imagem rodar em Compose e em Kubernetes.
- **Feedback de erro obrigatório.** Toda ação (salvar, deletar) tem feedback de sucesso **e**
  de erro. Catch silencioso e rollback mudo são proibidos.
- **Estado de carregamento** visível em toda chamada de rede. Em Kubernetes o backend pode
  demorar a ficar pronto — a UI não pode parecer travada.
- **Ação destrutiva exige dialog de confirmação.**
- **Acessibilidade mínima:** fonte ≥ 12px, contraste suficiente, `label` associado a todo
  input, `aria-label` em botão só com ícone.
- **Performance:** lista grande com paginação; limpar timer/listener ao desmontar.

> i18n **não se aplica** a este projeto: interface em português apenas, sem biblioteca de
> tradução. Se um segundo idioma entrar, esta seção volta a exigir chave em todos os locales.

### 4.4 Docker e Kubernetes

Esta é a seção que o projeto existe para treinar. Violação aqui é bloqueante.

- **Dockerfile SEMPRE multi-stage.** Estágio de build com o SDK, estágio final só com o
  runtime. Mandar JDK completo ou `node_modules` para a imagem final é o erro nº 1.
- **Tag de imagem base sempre fixada e específica.** `eclipse-temurin:21-jre-alpine`, nunca
  `:latest` nem `:21`. Build não reprodutível é build quebrado no futuro.
- **`.dockerignore` obrigatório** em todo contexto de build — sem ele, `.git/`, `target/` e
  `node_modules/` vão para o daemon e o build fica lento e a imagem gorda.
- **Container não roda como root.** `USER` não-privilegiado no estágio final.
- **Uma responsabilidade por container.** Nada de backend e banco na mesma imagem.
- **Nada de estado dentro do container.** Dado do MySQL vive em volume nomeado (Compose) ou
  `PersistentVolumeClaim` (K8s). Container é descartável por definição.
- **Todo Deployment DEVE ter `readinessProbe` e `livenessProbe`.** Sem readiness, o Service
  manda tráfego para pod que ainda não subiu — é a causa nº 1 de "funciona no Compose e
  quebra no K8s".
- **Todo container DEVE ter `resources.requests` e `resources.limits`.** Sem requests o
  scheduler não tem como decidir; sem limits um pod derruba o nó.
- **Configuração em `ConfigMap`, segredo em `Secret`.** Senha em ConfigMap é violação.
- **Nunca depender da ordem de inicialização.** Backend deve tolerar MySQL ainda indisponível
  (retry/backoff) — `depends_on` do Compose não espera o banco estar *pronto*, só *iniciado*.
- **Comunicação entre serviços por nome de Service do K8s** (`mysql:3306`), nunca por IP.
- **`kubectl apply` só no cluster local.** Deploy manual em cluster remoto é proibido.
- **Manifesto YAML é código:** versionado, revisado em PR, nunca editado direto com
  `kubectl edit` (a mudança se perde no próximo apply e ninguém consegue reproduzir).

### 4.5 Qualidade de código

- **DRY** — extrair componente/service quando houver duplicação.
- **Arquivo com 500+ linhas** deve ser quebrado. Controller gigante é red flag — extrair para
  service.
- **Sem cast desnecessário.** Tipo específico > tipo genérico.
- **Sem magic number.** Timeout, limite e tamanho viram constante nomeada.
- **Self-review antes de commitar:** aplicar ao próprio código as mesmas regras que seriam
  cobradas em review. Não commitar o que seria flaggeado.

---

## 5. Code review

Todo PR passa por review antes do merge. Severidade:

| Emoji | Severidade | Bloqueia merge? | Significado |
|---|---|---|---|
| 🔴 | CRÍTICO | **SIM** | Segurança, secret exposto, perda de dados, container inutilizável |
| 🟡 | IMPORTANTE | **SIM** | Bug, regressão, violação de regra deste documento |
| ⚠️ | MENOR | NÃO | Tech debt, melhoria, sugestão |
| ✅ | OK | — | Item de review anterior corrigido |

Regras do reviewer:

- **Contexto do PR é obrigatório.** Código pré-existente que o PR não alterou **não é
  flaggeado**. Cada issue precisa fazer sentido dado o que o PR está fazendo.
- **Pular arquivo gerado:** lock files (`package-lock.json`, `pnpm-lock.yaml`), bundle
  buildado, snapshot. Conferir só que acompanham o source.
- **Nunca elogiar por educação.** Código normal não merece comentário.
- **Só 🔴 e 🟡 bloqueiam.** PR com apenas ⚠️ é aprovado.
- **Calibrar pelo objetivo do projeto.** É estudo: falta de teste de borda em CRUD trivial é
  ⚠️, não 🟡. Já Dockerfile sem multi-stage, Secret vazado ou Deployment sem probe são 🔴/🟡
  mesmo aqui — são exatamente o que o projeto se propõe a demonstrar.

---

## 6. Convenções

- **Stack:** Spring Boot (Java 21) + Spring Data JPA · React + Vite · MySQL 8 · Docker /
  Docker Compose · Kubernetes (Minikube ou Kind) · Nginx servindo o build do frontend.
- **Idioma:** **inglês** em commits, nomes de branch e PRs (título e corpo).
  **Português (BR)** em UI, comentários de código, documentação, README e nas respostas.
- **Tipos de commit/PR/branch:** feature, bugfix, hotfix, docs, style, refactor, test, chore.
- **Branch default:** `main` (estável). **Integração:** `dev`.
- **Commits sem `Co-Authored-By`.**

---

## 7. Arquitetura e armadilhas conhecidas

> Registrar aqui, **com data absoluta** (nunca "recentemente"/"semana passada"), tudo que já
> custou um diagnóstico errado ou uma hora perdida.

### Ambiente da máquina de desenvolvimento (verificado em 28/08/2026)

| Ferramenta | Estado |
|---|---|
| Docker | 29.7.2 — disponível |
| Node / npm | 18.19.1 / 9.2.0 |
| JDK / Maven | **não instalados** |
| Minikube / Kind | **não instalados** |
| `kubectl` | presente |

Consequências práticas:

- **Sem JDK local, o build do backend só roda dentro de container** (`maven:3-eclipse-temurin-21`).
  Antes de sugerir `mvn` direto no terminal, confirmar que o JDK foi instalado.
- **Node 18 não roda Vite 7** (exige Node 20+). Ou o projeto fixa Vite 5, ou o Node sobe.
  A imagem de build do Dockerfile define a própria versão, então isso só afeta o dev local.
- **Não há cluster local ainda** — nada de `kubectl apply` até Minikube/Kind serem instalados
  (Semana 3). `kubectl` respondendo não significa cluster no ar: checar `kubectl cluster-info`.

### Fonte da verdade

- **Estado da aplicação:** o container/pod **em execução**. Dockerfile e manifesto YAML
  descrevem a intenção, não o que está no ar. Um `kubectl apply` que falhou deixa o cluster
  na versão anterior enquanto o arquivo já mostra a nova — confirmar com
  `kubectl rollout status` / `kubectl describe`.
- **Dados:** o volume do MySQL. Recriar o container **não** limpa o volume; `docker compose
  down -v` limpa. Confundir os dois já é a origem clássica de "o dado sumiu" e de "a mudança
  de schema não pegou".

### Armadilhas a preencher conforme aparecerem

<!-- Formato: **DD/MM/AAAA — título.** O que parecia, o que era, como verificar. -->

---

## 8. Skills e comandos deste repositório

| Comando | O que faz |
|---|---|
| `/pr-abrir` | Abre PR da branch atual para `dev`, com título padrão, template e labels |
| `/pr-hotfix` | Abre PR de `hotfix/*` (cortada de `main`) direto pra `main` |
| `/pr-release` | Promoção `dev → main` com changelog |
| `/review-pr N` | Code review completo do PR N, saída só no terminal |
| `/fix-pr N` | Aplica as correções apontadas no review do PR N |
| `/analise` | Investigação de causa raiz **somente leitura**, com cerca mecânica via hook |

Detalhes em `.claude/commands/` e `.claude/skills/`.
