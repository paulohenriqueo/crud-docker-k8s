# backend

API REST do CRUD de `usuario` (id, nome, email).

**Stack:** Spring Boot 4.1.1 (Java 21) · Spring Data JPA · Flyway · MySQL 8 · Bean Validation.

## Endpoints

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| GET | `/api/usuarios` | 200 (paginado) | — |
| GET | `/api/usuarios/{id}` | 200 | 404 |
| POST | `/api/usuarios` | 201 + `Location` | 400, 409 |
| PUT | `/api/usuarios/{id}` | 200 | 400, 404, 409 |
| DELETE | `/api/usuarios/{id}` | 204 | 404 |

Erros seguem RFC 7807 (`ProblemDetail`). Em 400, o corpo traz `erros` com o campo e o motivo.

A listagem é **sempre** paginada e devolve `PagedModel` (`content` + `page`), não `PageImpl` —
o Spring Data não garante estabilidade do JSON de `PageImpl` entre versões, e o frontend
depende desse formato. O tamanho de página é limitado a 100 por configuração.

## Configuração

Tudo por variável de ambiente — ver `.env.example` na raiz. Nenhum valor sensível fica neste
repositório: `spring.datasource.password` não tem default, então a aplicação **não sobe** sem
a senha vir do ambiente (Secret, no Kubernetes).

| Variável | Default | Para quê |
|---|---|---|
| `MYSQL_HOST` | `localhost` | nome do serviço no Compose / do Service no K8s |
| `MYSQL_PORT` | `3306` | |
| `MYSQL_DATABASE` | `crud` | |
| `MYSQL_USER` | — | |
| `MYSQL_PASSWORD` | — | sem default de propósito |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | origem do frontend; nunca `*` |

## Decisões

- **Flyway com `ddl-auto=validate`.** O schema é do Flyway; o Hibernate só confere se a
  entidade bate com a tabela e falha no boot se divergir. `update` mascara migration esquecida.
- **`spring.flyway.connect-retries=10`.** O backend tolera o MySQL ainda não estar aceitando
  conexão. O `depends_on` do Compose garante que o container **iniciou**, não que o banco está
  **pronto** — sem retry, o primeiro `compose up` limpo derruba o backend.
- **Actuator com probes.** `/actuator/health/liveness` e `/actuator/health/readiness` já
  existem, prontos para as probes do Deployment na Semana 3.
- **DTO em vez da entidade no controller.** Impede mass assignment e desacopla o contrato
  HTTP do schema.
- **Unicidade de e-mail no banco**, não só na aplicação: a checagem prévia dá a mensagem boa,
  a constraint é o que segura duas requisições concorrentes.

## Rodar localmente

Não há JDK nem Maven instalados nesta máquina (ver CLAUDE.md, seção 7), então tudo roda
dentro de container. O script sobe MySQL + API:

```bash
./scripts/dev-backend.sh up      # sobe (API em http://localhost:8080)
./scripts/dev-backend.sh logs    # acompanha o log da API
./scripts/dev-backend.sh status  # o que está de pé
./scripts/dev-backend.sh down    # derruba, PRESERVANDO o volume do banco
./scripts/dev-backend.sh reset   # apaga os dados do MySQL (pede confirmação)
```

`down` e `reset` são coisas diferentes de propósito: derrubar o ambiente não pode apagar
dado. Essa confusão é a origem clássica de "o dado sumiu".

O script é provisório — some na Semana 2, quando `docker compose up` assumir o papel.

## Testar as rotas no Postman

Importe `backend/postman/usuario-api.postman_collection.json`. São 15 requests em 4 pastas:

- **Health** — os três endpoints que viram probes do Deployment na Semana 3.
- **CRUD** — fluxo feliz na ordem. O request de criação gera um e-mail único e guarda o `id`
  numa variável, então os seguintes já vêm encadeados.
- **Erros esperados** — cada request comprova um status de erro correto (400, 404, 409).
  Passar aqui significa que a API **rejeitou** como devia.
- **Limites** — prova que `?size=500` é servido como 100.

Todos os requests têm asserção de status, então dá para rodar a collection inteira pelo
Runner do Postman: o esperado é 15/15 verdes com a API no ar.

A variável `baseUrl` aponta para `http://localhost:8080`. Em Kubernetes, troque só ela.

## Build e testes

Para rodar só a suíte de testes, sem subir nada:

```bash
# testes
docker run --rm -u "$(id -u):$(id -g)" -e HOME=$HOME \
  -v "$HOME/.m2":"$HOME/.m2" -v "$PWD/backend":/app -w /app \
  maven:3.9-eclipse-temurin-21 \
  mvn -B -Dmaven.repo.local="$HOME/.m2/repository" test
```

Com JDK instalado, `./mvnw test` na pasta `backend/` faz o mesmo.

A partir da Semana 2 isto vira `docker compose up`.

## Pendências

- [ ] Dockerfile multi-stage e `.dockerignore` (Semana 2)
- [ ] Testes de integração contra MySQL real (Testcontainers) — hoje a suíte cobre a camada
      HTTP com o service mockado e a regra de negócio com o repository mockado
