package br.com.portfolio.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Whitelist explícita do que a API aceita. O cliente não consegue escrever
 * em nenhum campo fora destes dois — em especial, não consegue definir o id.
 */
public record UsuarioRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        @Size(max = 180, message = "O e-mail deve ter no máximo 180 caracteres")
        String email) {
}
