package br.com.portfolio.usuario.service;

import br.com.portfolio.usuario.domain.Usuario;
import br.com.portfolio.usuario.dto.UsuarioRequest;
import br.com.portfolio.usuario.dto.UsuarioResponse;
import br.com.portfolio.usuario.exception.EmailJaCadastradoException;
import br.com.portfolio.usuario.exception.UsuarioNaoEncontradoException;
import br.com.portfolio.usuario.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concentra a regra de negócio. O controller só traduz HTTP; quem decide
 * o que é conflito, o que é inexistente e o que é válido é esta camada.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(UsuarioResponse::de);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    /**
     * Checagem prévia + tratamento da violação de constraint. A checagem dá a
     * mensagem boa; a constraint do banco é quem realmente garante a unicidade
     * se duas requisições chegarem ao mesmo tempo.
     */
    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoException(request.email());
        }
        Usuario usuario = new Usuario(request.nome(), request.email());
        return UsuarioResponse.de(salvar(usuario, request.email()));
    }

    /**
     * Buscar e alterar precisam ser atômicos: sem a transação, outra requisição
     * poderia remover o registro entre o find e o save.
     */
    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));

        if (repository.existsByEmailAndIdNot(request.email(), id)) {
            throw new EmailJaCadastradoException(request.email());
        }

        usuario.atualizar(request.nome(), request.email());
        return UsuarioResponse.de(salvar(usuario, request.email()));
    }

    /**
     * Confirma a existência antes de remover, para responder 404 em vez de 204
     * silencioso. Deletar um id inexistente não pode parecer sucesso.
     */
    @Transactional
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new UsuarioNaoEncontradoException(id);
        }
        repository.deleteById(id);
    }

    private Usuario salvar(Usuario usuario, String email) {
        try {
            return repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailJaCadastradoException(email);
        }
    }
}
