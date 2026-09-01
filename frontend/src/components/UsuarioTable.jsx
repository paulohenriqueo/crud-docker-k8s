/**
 * Tabela de usuários com paginação. Os metadados vêm do objeto `page` que a
 * API devolve (PagedModel) — a paginação é do servidor, não do cliente, então
 * a tabela nunca carrega a base inteira.
 */
export default function UsuarioTable({ usuarios, pagina, carregando, aoEditar, aoRemover, aoMudarPagina }) {
  if (carregando) {
    return (
      <div className="cartao estado" role="status">
        Carregando usuários...
      </div>
    );
  }

  if (usuarios.length === 0) {
    return (
      <div className="cartao estado">
        Nenhum usuário cadastrado ainda. Use o formulário acima para criar o primeiro.
      </div>
    );
  }

  const primeiraPagina = pagina.number === 0;
  const ultimaPagina = pagina.number >= pagina.totalPages - 1;

  return (
    <div className="cartao">
      <table className="tabela">
        <caption className="tabela__legenda">
          {pagina.totalElements} usuário(s) cadastrado(s)
        </caption>
        <thead>
          <tr>
            <th scope="col">Nome</th>
            <th scope="col">E-mail</th>
            <th scope="col">Ações</th>
          </tr>
        </thead>
        <tbody>
          {usuarios.map((usuario) => (
            <tr key={usuario.id}>
              <td>{usuario.nome}</td>
              <td>{usuario.email}</td>
              <td className="tabela__acoes">
                <button
                  type="button"
                  className="botao botao--pequeno"
                  onClick={() => aoEditar(usuario)}
                  aria-label={`Editar ${usuario.nome}`}
                >
                  Editar
                </button>
                <button
                  type="button"
                  className="botao botao--pequeno botao--perigo"
                  onClick={() => aoRemover(usuario)}
                  aria-label={`Excluir ${usuario.nome}`}
                >
                  Excluir
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <nav className="paginacao" aria-label="Paginação">
        <button
          type="button"
          className="botao botao--pequeno"
          onClick={() => aoMudarPagina(pagina.number - 1)}
          disabled={primeiraPagina}
        >
          Anterior
        </button>
        <span aria-live="polite">
          Página {pagina.number + 1} de {Math.max(pagina.totalPages, 1)}
        </span>
        <button
          type="button"
          className="botao botao--pequeno"
          onClick={() => aoMudarPagina(pagina.number + 1)}
          disabled={ultimaPagina}
        >
          Próxima
        </button>
      </nav>
    </div>
  );
}
