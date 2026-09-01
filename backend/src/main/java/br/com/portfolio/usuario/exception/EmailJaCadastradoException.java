package br.com.portfolio.usuario.exception;

public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário com o e-mail " + email);
    }
}
