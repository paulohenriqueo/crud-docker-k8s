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

O objetivo do projeto é um **POC de Kubernetes**: a aplicação é o meio, não o fim. O critério
de avaliação tem duas metades de peso igual — **construir** o ambiente e **explicar o papel de
cada componente**. Por isso a documentação é escrita ao longo do caminho, não empilhada no
final.

Prazo: ~2 a 3 semanas a partir de 02/09/2026. O que estiver marcado como **corta primeiro**
é o que sai se o tempo apertar.

### Bloco 0 — Aplicação ✅

- [x] Backend com controller REST (GET, POST, PUT, DELETE) e conexão MySQL
- [x] Frontend com formulário de cadastro e tabela de listagem
- [x] Integração validada em ambiente local

### Bloco 1 — Dockerização

Pré-requisito do Kubernetes: sem imagem não há o que orquestrar.

- [ ] Dockerfile do backend (multi-stage, imagem final só com JRE)
- [ ] Dockerfile do frontend (build + Nginx) e a decisão de config em runtime
      (a URL da API é resolvida em build pelo Vite — ver `frontend/README.md`)
- [ ] `.dockerignore` nos dois contextos
- [ ] `docker-compose.yml` com os 3 serviços conversando
- [ ] `docs/01-containers.md` — por que multi-stage, o que entra e o que fica de fora da
      imagem final, e o que muda de Compose para Kubernetes

### Bloco 2 — Kubernetes

O núcleo do POC.

- [ ] Cluster local (Minikube ou Kind)
- [ ] Manifests do MySQL: Deployment, Service, PersistentVolumeClaim
- [ ] Manifests do backend e frontend: Deployment e Service
- [ ] ConfigMap para configuração, Secret para senha
- [ ] `readinessProbe` e `livenessProbe` em todo Deployment
- [ ] `resources.requests` e `resources.limits` em todo container
- [ ] `docs/02-componentes-kubernetes.md` — **o entregável mais importante**: o que faz cada
      componente do cluster (api-server, etcd, scheduler, controller-manager, kubelet,
      kube-proxy, CoreDNS) e o que acontece, passo a passo, quando se roda `kubectl apply`
- [ ] `docs/03-comunicacao.md` — como o frontend chega no backend e o backend no MySQL:
      Service, DNS interno, ClusterIP e por que nunca se usa IP de pod

### Bloco 3 — Resiliência: simular falha e observar

Requisito explícito do POC. Cada experimento registra **hipótese, comando, o que foi observado
e a explicação**.

- [ ] Matar um pod e observar o ReplicaSet recriar
- [ ] `kubectl drain` no nó e observar o reagendamento
- [ ] Derrubar o MySQL e observar a readiness tirar o backend do Service
- [ ] Escalar réplicas e observar a distribuição de tráfego
- [ ] Remover o PVC do caminho e comprovar que o dado sobrevive ao pod, não ao container
- [ ] `docs/04-resiliencia.md` — a tabela de experimentos com o comportamento observado

### Bloco 4 — Segurança e custo

Os dois itens do plano de desenvolvimento que o roadmap anterior não cobria. Segurança pesa
mais, por ser a direção de carreira declarada.

- [ ] `docs/05-seguranca.md` — RBAC e ServiceAccount, NetworkPolicy, Pod Security Standards,
      container não-root, e por que `Secret` é **base64, não criptografia** (e o que se faz a
      respeito)
- [ ] `docs/06-custo.md` — o que custa dinheiro num cluster, papel de requests/limits no
      dimensionamento, HPA e autoscaler, e a comparação cluster local x gerenciado
- [ ] Aplicar no repositório o que for viável em cluster local (não-root, NetworkPolicy)

### Bloco 5 — Publicação — *corta primeiro*

- [ ] Imagens publicadas no Docker Hub
- [ ] Diagrama de arquitetura
- [ ] README final com instruções de execução ponta a ponta

## Como executar

*A ser preenchido ao final da Semana 2 (Docker Compose) e da Semana 3 (Kubernetes).*

```bash
# em breve
```

## Licença

MIT
