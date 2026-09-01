package br.com.portfolio.usuario.repository;

import br.com.portfolio.usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    /** Usado na atualização: detecta e-mail já usado por OUTRO registro. */
    boolean existsByEmailAndIdNot(String email, Long id);
}
