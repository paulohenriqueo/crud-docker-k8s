import { useEffect, useRef } from 'react';

/**
 * Confirmação para ação destrutiva. Usa <dialog> nativo pelo modal real que
 * ele oferece de graça: trava o foco dentro do diálogo e fecha no Esc.
 */
export default function ConfirmDialog({ aberto, titulo, mensagem, aoConfirmar, aoCancelar }) {
  const referencia = useRef(null);

  useEffect(() => {
    const dialogo = referencia.current;
    if (!dialogo) return;

    if (aberto && !dialogo.open) dialogo.showModal();
    if (!aberto && dialogo.open) dialogo.close();
  }, [aberto]);

  useEffect(() => {
    const dialogo = referencia.current;
    if (!dialogo) return;

    // Esc fecha o <dialog> sozinho; sem isto o estado do React ficaria
    // dizendo "aberto" com o diálogo já fechado na tela.
    const aoFechar = () => aoCancelar();
    dialogo.addEventListener('close', aoFechar);
    return () => dialogo.removeEventListener('close', aoFechar);
  }, [aoCancelar]);

  return (
    <dialog ref={referencia} className="dialogo" aria-labelledby="dialogo-titulo">
      <h2 id="dialogo-titulo">{titulo}</h2>
      <p>{mensagem}</p>
      <div className="dialogo__acoes">
        <button type="button" className="botao" onClick={aoCancelar}>
          Cancelar
        </button>
        <button type="button" className="botao botao--perigo" onClick={aoConfirmar}>
          Excluir
        </button>
      </div>
    </dialog>
  );
}
