/**
 * Cliente HTTP da API de usuários.
 *
 * A URL base vem de variável de ambiente, nunca chumbada: é o que permite
 * apontar para o backend local, para o serviço do Compose ou para o Service
 * do Kubernetes sem mexer no código.
 *
 * ATENÇÃO para a Semana 2: o Vite injeta import.meta.env em tempo de BUILD,
 * não de execução. Ou seja, esta variável fica compilada dentro do bundle e a
 * imagem resultante é específica de um ambiente. Para a mesma imagem rodar em
 * Compose e em Kubernetes, o caminho é o Nginx fazer proxy de /api para o
 * backend (aí a URL vira relativa e some daqui) ou um entrypoint gerar um
 * config.js em runtime. Decidir isso ao escrever o Dockerfile.
 */

const BASE_URL = import.meta.env.VITE_API_URL;

if (!BASE_URL) {
  throw new Error(
    'VITE_API_URL não está definida. Copie frontend/.env.example para frontend/.env.',
  );
}

/** Sem timeout, uma rede lenta deixa a UI carregando para sempre. */
const TIMEOUT_MS = 10000;
const RECURSO = '/api/usuarios';

/** Erro com o status HTTP preservado, para a UI decidir o que dizer. */
export class ApiError extends Error {
  constructor(mensagem, status, camposInvalidos) {
    super(mensagem);
    this.name = 'ApiError';
    this.status = status;
    this.camposInvalidos = camposInvalidos ?? null;
  }
}

async function requisitar(caminho, opcoes = {}) {
  const controlador = new AbortController();
  const timeout = setTimeout(() => controlador.abort(), TIMEOUT_MS);

  let resposta;
  try {
    resposta = await fetch(`${BASE_URL}${caminho}`, {
      ...opcoes,
      signal: controlador.signal,
      headers: opcoes.body ? { 'Content-Type': 'application/json' } : undefined,
    });
  } catch (erro) {
    if (erro.name === 'AbortError') {
      throw new ApiError('O servidor demorou demais para responder.', 0);
    }
    throw new ApiError('Não foi possível falar com o servidor.', 0);
  } finally {
    clearTimeout(timeout);
  }

  if (resposta.status === 204) return null;

  const corpo = await resposta.json().catch(() => null);

  if (!resposta.ok) {
    // A API responde RFC 7807: "detail" traz a mensagem e "erros" os campos.
    throw new ApiError(
      corpo?.detail ?? 'Erro inesperado ao falar com o servidor.',
      resposta.status,
      corpo?.erros,
    );
  }

  return corpo;
}

export function listarUsuarios({ pagina = 0, tamanho = 10 } = {}) {
  return requisitar(`${RECURSO}?page=${pagina}&size=${tamanho}`);
}

export function criarUsuario(usuario) {
  return requisitar(RECURSO, { method: 'POST', body: JSON.stringify(usuario) });
}

export function atualizarUsuario(id, usuario) {
  return requisitar(`${RECURSO}/${id}`, { method: 'PUT', body: JSON.stringify(usuario) });
}

export function removerUsuario(id) {
  return requisitar(`${RECURSO}/${id}`, { method: 'DELETE' });
}
