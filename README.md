# crud-docker-k8s

CRUD monorepo: REST API, SPA frontend and MySQL, from docker-compose to Kubernetes manifests

> 🚧 **Em desenvolvimento.** Projeto de estudo focado em containerização e orquestração. Este README será atualizado conforme as etapas forem concluídas.

---

## Sobre

Aplicação CRUD full-stack usada como laboratório prático para Docker e Kubernetes. O objetivo não é a complexidade da aplicação, e sim o caminho completo: desenvolvimento local → containers → orquestração em cluster.

A entidade de domínio é intencionalmente simples (`usuario`: id, nome, email) para manter o foco na infraestrutura.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Spring Boot (Java 21) + Spring Data JPA |
| Frontend | React + Vite |
| Banco de dados | MySQL 8 |
| Container | Docker / Docker Compose |
| Orquestração | Kubernetes (Minikube ou Kind) |
| Servidor web | Nginx (serve o build do frontend) |

## Estrutura

```
crud-docker-k8s/
├── backend/              # API REST (Spring Boot) + Dockerfile
├── frontend/             # SPA (React + Vite) + Dockerfile + config Nginx
├── database/             # scripts de inicialização
├── k8s/
│   ├── mysql/            # Deployment, Service, PVC
│   ├── backend/          # Deployment, Service, ConfigMap, Secret
│   └── frontend/         # Deployment, Service, Ingress
├── docs/                 # diagrama de arquitetura
├── docker-compose.yml
└── README.md
```

## Roadmap

- [ ] **Semana 1 — Aplicação**
  - [ ] Backend com controller REST (GET, POST, PUT, DELETE) e conexão MySQL
  - [ ] Frontend com formulário de cadastro e tabela de listagem
  - [ ] Integração validada em ambiente local
- [ ] **Semana 2 — Dockerização**
  - [ ] Dockerfile do backend (multi-stage build)
  - [ ] Dockerfile do frontend (build + Nginx)
  - [ ] `docker-compose.yml` com os 3 serviços comunicando entre si
- [ ] **Semana 3 — Kubernetes**
  - [ ] Cluster local (Minikube/Kind)
  - [ ] Manifests do MySQL: Deployment, Service, PersistentVolumeClaim
  - [ ] Manifests do backend e frontend: Deployment e Service
  - [ ] Probes de liveness/readiness e resolução da ordem de inicialização
- [ ] **Semana 4 — Publicação**
  - [ ] Imagens publicadas no Docker Hub
  - [ ] Variáveis de ambiente e Secrets ajustados
  - [ ] Diagrama de arquitetura e documentação final

## Como executar

*A ser preenchido ao final da Semana 2 (Docker Compose) e da Semana 3 (Kubernetes).*

```bash
# em breve
```

## Licença

MIT
