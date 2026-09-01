/**
 * Mensagem de sucesso ou erro. Existe como componente próprio para garantir
 * que toda ação tenha retorno visível — falha silenciosa é proibida.
 *
 * role="alert" faz o leitor de tela anunciar a mensagem assim que ela aparece.
 */
export default function Feedback({ tipo, mensagem, aoFechar }) {
  if (!mensagem) return null;

  return (
    <div className={`feedback feedback--${tipo}`} role="alert">
      <span>{mensagem}</span>
      <button type="button" className="feedback__fechar" onClick={aoFechar} aria-label="Fechar mensagem">
        ×
      </button>
    </div>
  );
}
