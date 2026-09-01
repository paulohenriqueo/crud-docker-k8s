import { useCallback, useEffect, useState } from 'react';
import { ApiError, atualizarUsuario, criarUsuario, listarUsuarios, removerUsuario } from './api/usuarioApi';
import ConfirmDialog from './components/ConfirmDialog';
import Feedback from './components/Feedback';
import UsuarioForm from './components/UsuarioForm';
import UsuarioTable from './components/UsuarioTable';

const TAMANHO_PAGINA = 10;
const PAGINA_VAZIA = { number: 0, size: TAMANHO_PAGINA, totalElements: 0, totalPages: 0 };

export default function App() {
  const [usuarios, setUsuarios] = useState([]);
  const [pagina, setPagina] = useState(PAGINA_VAZIA);
  const [paginaAtual, setPaginaAtual] = useState(0);

  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);

  const [feedback, setFeedback] = useState(null);
  const [errosDaApi, setErrosDaApi] = useState(null);

  const [usuarioEmEdicao, setUsuarioEmEdicao] = useState(null);
  const [usuarioParaRemover, setUsuarioParaRemover] = useState(null);

  const avisar = useCallback((tipo, mensagem) => setFeedback({ tipo, mensagem }), []);

  const carregar = useCallback(
    async (numeroDaPagina) => {
      setCarregando(true);
      try {
        const resposta = await listarUsuarios({ pagina: numeroDaPagina, tamanho: TAMANHO_PAGINA });
        setUsuarios(resposta.content);
        setPagina(resposta.page);

        // A última página pode deixar de existir depois de uma exclusão.
        if (resposta.content.length === 0 && numeroDaPagina > 0) {
          setPaginaAtual(numeroDaPagina - 1);
        }
      } catch (erro) {
        setUsuarios([]);
        setPagina(PAGINA_VAZIA);
        avisar('erro', `Não foi possível carregar a lista. ${erro.message}`);
      } finally {
        setCarregando(false);
      }
    },
    [avisar],
  );

  useEffect(() => {
    carregar(paginaAtual);
  }, [carregar, paginaAtual]);

  function tratarErroDeEscrita(erro, acao) {
    if (erro instanceof ApiError && erro.status === 400) {
      setErrosDaApi(erro.camposInvalidos);
      avisar('erro', 'Verifique os campos destacados.');
      return;
    }
    // 409 (e-mail duplicado) e 404 (registro removido por outra aba) já vêm
    // com mensagem pronta e específica da API — repassar é melhor do que
    // inventar uma genérica.
    avisar('erro', `Não foi possível ${acao}. ${erro.message}`);
  }

  async function salvar(dados) {
    setSalvando(true);
    setErrosDaApi(null);
    try {
      if (usuarioEmEdicao) {
        await atualizarUsuario(usuarioEmEdicao.id, dados);
        avisar('sucesso', 'Usuário atualizado.');
        setUsuarioEmEdicao(null);
      } else {
        await criarUsuario(dados);
        avisar('sucesso', 'Usuário cadastrado.');
      }
      await carregar(paginaAtual);
    } catch (erro) {
      tratarErroDeEscrita(erro, usuarioEmEdicao ? 'salvar as alterações' : 'cadastrar o usuário');
    } finally {
      setSalvando(false);
    }
  }

  async function confirmarRemocao() {
    const alvo = usuarioParaRemover;
    setUsuarioParaRemover(null);
    if (!alvo) return;

    try {
      await removerUsuario(alvo.id);
      avisar('sucesso', `${alvo.nome} foi excluído.`);
      if (usuarioEmEdicao?.id === alvo.id) setUsuarioEmEdicao(null);
      await carregar(paginaAtual);
    } catch (erro) {
      tratarErroDeEscrita(erro, 'excluir o usuário');
    }
  }

  return (
    <main className="pagina">
      <header className="cabecalho">
        <h1>Usuários</h1>
        <p>CRUD de estudo para Docker e Kubernetes.</p>
      </header>

      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} aoFechar={() => setFeedback(null)} />

      <UsuarioForm
        usuarioEmEdicao={usuarioEmEdicao}
        salvando={salvando}
        errosDaApi={errosDaApi}
        aoSalvar={salvar}
        aoCancelar={() => {
          setUsuarioEmEdicao(null);
          setErrosDaApi(null);
        }}
      />

      <UsuarioTable
        usuarios={usuarios}
        pagina={pagina}
        carregando={carregando}
        aoEditar={(usuario) => {
          setUsuarioEmEdicao(usuario);
          setErrosDaApi(null);
        }}
        aoRemover={setUsuarioParaRemover}
        aoMudarPagina={setPaginaAtual}
      />

      <ConfirmDialog
        aberto={Boolean(usuarioParaRemover)}
        titulo="Excluir usuário"
        mensagem={
          usuarioParaRemover
            ? `Excluir ${usuarioParaRemover.nome} (${usuarioParaRemover.email})? Esta ação não pode ser desfeita.`
            : ''
        }
        aoConfirmar={confirmarRemocao}
        aoCancelar={() => setUsuarioParaRemover(null)}
      />
    </main>
  );
}
