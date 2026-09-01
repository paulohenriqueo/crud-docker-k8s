# frontend

SPA de cadastro de usuários: formulário e tabela paginada.

**Stack:** React 19 · Vite 6 · sem biblioteca de UI.

> **Por que Vite 6 e não 7:** o Vite 7 exige Node 20+, e a máquina de desenvolvimento tem
> Node 18.19 (ver CLAUDE.md, seção 7). O Vite 6 declara `^18.0.0 || ^20.0.0 || >=22.0.0`,
> então roda aqui sendo a versão mais recente compatível. Isso não limita a imagem Docker da
> Semana 2, que define a própria versão de Node no estágio de build.

## Configuração

| Variável | Obrigatória | Para quê |
|---|---|---|
| `VITE_API_URL` | sim | URL base da API |

Copie `.env.example` para `.env` antes de rodar. A aplicação **falha explicitamente** se a
variável não existir, em vez de cair num `localhost` chumbado — erro de configuração deve
aparecer, não se disfarçar de bug.

## Armadilha conhecida: a URL da API é resolvida em BUILD, não em runtime

O Vite substitui `import.meta.env.VITE_API_URL` por literal durante o `vite build`. Verificado
neste projeto: `grep "localhost:8080" dist/assets/*.js` encontra a string dentro do bundle.

A consequência é direta para a Semana 3: **uma imagem buildada com uma URL não pode ser
reconfigurada por variável de ambiente no Kubernetes.** Passar `VITE_API_URL` no Deployment
não tem efeito nenhum — o valor já está compilado.

Isso precisa ser resolvido no Dockerfile. As duas saídas usuais:

1. **Nginx faz proxy de `/api` para o backend.** O frontend passa a usar caminho relativo e a
   variável some. Costuma ser a opção mais limpa em Kubernetes, e ainda elimina o CORS.
2. **Entrypoint gera um `config.js` na subida do container**, lendo variáveis de ambiente, e o
   `index.html` carrega esse arquivo antes do bundle.

Decidir ao escrever o Dockerfile, na Semana 2.

## Rodar

```bash
cd frontend
cp .env.example .env
npm install
npm run dev     # http://localhost:5173
```

O backend precisa estar de pé: `./scripts/dev-backend.sh up` na raiz do repositório.

A origem `http://localhost:5173` já está autorizada no CORS do backend por padrão.

## Estrutura

```
src/
├── api/usuarioApi.js        cliente HTTP: timeout, tradução de erro da API
├── components/
│   ├── UsuarioForm.jsx      formulário; serve a cadastro e edição
│   ├── UsuarioTable.jsx     tabela + paginação (do servidor, não do cliente)
│   ├── ConfirmDialog.jsx    confirmação de exclusão, com <dialog> nativo
│   └── Feedback.jsx         mensagem de sucesso/erro
└── App.jsx                  estado e orquestração
```

## Decisões

- **Paginação no servidor.** A tabela consome `content` + `page` do `PagedModel` da API e
  nunca carrega a base inteira.
- **Timeout de 10s em toda chamada.** Sem ele, rede lenta deixa a UI carregando para sempre.
  Implementado com `AbortController`.
- **Erro de validação vem da API, não é reinventado no front.** O `400` traz `erros` com campo
  e mensagem; o formulário só posiciona isso abaixo do input certo. A validação do navegador é
  conveniência, não a fonte da verdade.
- **`<dialog>` nativo na exclusão.** Traz travamento de foco e fechamento no Esc sem código.
- **Estado de carregamento visível.** Em Kubernetes o backend pode demorar a ficar pronto — a
  UI não pode parecer travada.

## Pendências

- [ ] Dockerfile multi-stage servindo o build pelo Nginx, e a decisão de runtime config acima
      (Semana 2)
- [ ] Testes automatizados (Vitest + Testing Library). Hoje a validação é lint, build e teste
      manual no navegador.
