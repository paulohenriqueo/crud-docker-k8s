# backend

API REST do CRUD de `usuario` (id, nome, email).

Stack: **Spring Boot (Java 21)** + Spring Web + Spring Data JPA + MySQL Connector.

## Endpoints previstos

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/usuarios` | Lista todos os usuários |
| GET | `/api/usuarios/{id}` | Busca um usuário |
| POST | `/api/usuarios` | Cria um usuário |
| PUT | `/api/usuarios/{id}` | Atualiza um usuário |
| DELETE | `/api/usuarios/{id}` | Remove um usuário |

## Pendências

- [ ] Inicializar o projeto (Spring Initializr)
- [ ] Configurar conexão com MySQL via variáveis de ambiente
- [ ] Dockerfile multi-stage (Semana 2)
