package br.com.portfolio.usuario.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Centraliza a tradução de exceção para status HTTP. Sem isto, erro de
 * validação e recurso ausente voltariam como 500 — o que esconde a causa
 * de quem consome a API.
 *
 * Estende ResponseEntityExceptionHandler de propósito: ele já trata as
 * exceções do próprio Spring MVC com o status certo (404 em rota
 * inexistente, 405 em método errado, 415 em Content-Type errado, 400 em
 * JSON malformado). Sem essa herança, o handler genérico de Exception
 * abaixo capturaria todas elas e devolveria 500 para tudo, transformando
 * "essa rota não existe" em "erro interno" — um erro de diagnóstico caro,
 * porque manda quem chama procurar no lugar errado.
 *
 * O corpo segue RFC 7807 (ProblemDetail), formato padrão de erro em HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ProblemDetail handleNaoEncontrado(UsuarioNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso não encontrado");
        return problem;
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ProblemDetail handleEmailDuplicado(EmailJaCadastradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflito de dados");
        return problem;
    }

    /**
     * Sobrescreve o tratamento padrão para acrescentar o mapa "erros", que diz
     * qual campo falhou e por quê. A mensagem genérica sozinha obriga quem
     * consome a API a adivinhar o campo.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
        problem.setTitle("Erro de validação");
        problem.setProperty("erros", erros);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Rede de segurança: só chega aqui o que NÃO é exceção conhecida do Spring
     * nem do domínio — ou seja, falha realmente inesperada. A causa vai para o
     * log; a resposta fica genérica para não expor detalhe interno (nome de
     * tabela, stack, connection string).
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleInesperado(Exception ex) {
        log.error("Erro não tratado ao processar a requisição", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição");
        problem.setTitle("Erro interno");
        return problem;
    }
}
