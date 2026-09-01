package br.com.portfolio.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.portfolio.usuario.domain.Usuario;
import br.com.portfolio.usuario.dto.UsuarioRequest;
import br.com.portfolio.usuario.dto.UsuarioResponse;
import br.com.portfolio.usuario.exception.EmailJaCadastradoException;
import br.com.portfolio.usuario.exception.UsuarioNaoEncontradoException;
import br.com.portfolio.usuario.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private UsuarioService service;

    @Test
    @DisplayName("cria o usuário quando o e-mail ainda não existe")
    void criaUsuarioComEmailInedito() {
        UsuarioRequest request = new UsuarioRequest("Ana", "ana@exemplo.com");
        when(repository.existsByEmail("ana@exemplo.com")).thenReturn(false);
        when(repository.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponse resposta = service.criar(request);

        assertThat(resposta.nome()).isEqualTo("Ana");
        assertThat(resposta.email()).isEqualTo("ana@exemplo.com");
    }

    @Test
    @DisplayName("recusa criação com e-mail já cadastrado, sem tocar no banco")
    void recusaEmailDuplicadoNaCriacao() {
        when(repository.existsByEmail("ana@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new UsuarioRequest("Ana", "ana@exemplo.com")))
                .isInstanceOf(EmailJaCadastradoException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("permite atualizar mantendo o próprio e-mail")
    void permiteManterOProprioEmailNaAtualizacao() {
        Usuario existente = new Usuario("Ana", "ana@exemplo.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailAndIdNot("ana@exemplo.com", 1L)).thenReturn(false);
        when(repository.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponse resposta = service.atualizar(1L, new UsuarioRequest("Ana Paula", "ana@exemplo.com"));

        assertThat(resposta.nome()).isEqualTo("Ana Paula");
    }

    @Test
    @DisplayName("recusa atualização para e-mail que pertence a outro usuário")
    void recusaEmailDeOutroUsuarioNaAtualizacao() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Usuario("Ana", "ana@exemplo.com")));
        when(repository.existsByEmailAndIdNot("bia@exemplo.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, new UsuarioRequest("Ana", "bia@exemplo.com")))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    @DisplayName("falha ao atualizar usuário inexistente")
    void falhaAoAtualizarInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, new UsuarioRequest("Ana", "ana@exemplo.com")))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    @DisplayName("não chama delete quando o usuário não existe")
    void naoDeletaInexistente() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(UsuarioNaoEncontradoException.class);

        verify(repository, never()).deleteById(any());
    }
}
