import { useEffect, useState } from 'react';

const VAZIO = { nome: '', email: '' };

/**
 * Formulário de cadastro e edição. O mesmo componente serve aos dois casos:
 * receber `usuarioEmEdicao` alterna o modo.
 *
 * A validação aqui é conveniência de UX, não segurança — quem valida de
 * verdade é o backend. Por isso os erros vindos da API (campo `erros` do
 * ProblemDetail) também são exibidos abaixo dos campos.
 */
export default function UsuarioForm({ usuarioEmEdicao, salvando, errosDaApi, aoSalvar, aoCancelar }) {
  const [valores, setValores] = useState(VAZIO);

  useEffect(() => {
    setValores(usuarioEmEdicao ? { nome: usuarioEmEdicao.nome, email: usuarioEmEdicao.email } : VAZIO);
  }, [usuarioEmEdicao]);

  const editando = Boolean(usuarioEmEdicao);

  function alterar(evento) {
    const { name, value } = evento.target;
    setValores((atual) => ({ ...atual, [name]: value }));
  }

  function enviar(evento) {
    evento.preventDefault();
    aoSalvar({ nome: valores.nome.trim(), email: valores.email.trim() });
  }

  return (
    <form className="cartao" onSubmit={enviar} noValidate>
      <h2>{editando ? `Editando ${usuarioEmEdicao.nome}` : 'Novo usuário'}</h2>

      <div className="campo">
        <label htmlFor="nome">Nome</label>
        <input
          id="nome"
          name="nome"
          value={valores.nome}
          onChange={alterar}
          maxLength={120}
          required
          aria-invalid={Boolean(errosDaApi?.nome)}
          aria-describedby={errosDaApi?.nome ? 'erro-nome' : undefined}
        />
        {errosDaApi?.nome && (
          <span className="campo__erro" id="erro-nome">
            {errosDaApi.nome}
          </span>
        )}
      </div>

      <div className="campo">
        <label htmlFor="email">E-mail</label>
        <input
          id="email"
          name="email"
          type="email"
          value={valores.email}
          onChange={alterar}
          maxLength={180}
          required
          aria-invalid={Boolean(errosDaApi?.email)}
          aria-describedby={errosDaApi?.email ? 'erro-email' : undefined}
        />
        {errosDaApi?.email && (
          <span className="campo__erro" id="erro-email">
            {errosDaApi.email}
          </span>
        )}
      </div>

      <div className="cartao__acoes">
        <button type="submit" className="botao botao--primario" disabled={salvando}>
          {salvando ? 'Salvando...' : editando ? 'Salvar alterações' : 'Cadastrar'}
        </button>
        {editando && (
          <button type="button" className="botao" onClick={aoCancelar} disabled={salvando}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}
