package br.com.portfolio.usuario.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.portfolio.usuario.dto.UsuarioRequest;
import br.com.portfolio.usuario.dto.UsuarioResponse;
import br.com.portfolio.usuario.exception.EmailJaCadastradoException;
import br.com.portfolio.usuario.exception.UsuarioNaoEncontradoException;
import br.com.portfolio.usuario.service.UsuarioService;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica o contrato HTTP: status, header Location e corpo de erro.
 * Não sobe banco — o service é mockado, então o teste roda em qualquer
 * máquina sem MySQL disponível.
 */
@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService service;

    @Test
    @DisplayName("GET da listagem devolve content e o objeto page de metadados")
    void getListagemDevolveContratoPaginado() throws Exception {
        when(service.listar(any()))
                .thenReturn(new PageImpl<>(
                        List.of(new UsuarioResponse(1L, "Ana", "ana@exemplo.com")),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("POST válido responde 201 com header Location")
    void postValidoRetorna201ComLocation() throws Exception {
        when(service.criar(any(UsuarioRequest.class)))
                .thenReturn(new UsuarioResponse(1L, "Ana", "ana@exemplo.com"));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/usuarios/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST com e-mail inválido responde 400 e aponta o campo")
    void postComEmailInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"nao-e-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists());
    }

    @Test
    @DisplayName("POST com nome em branco responde 400")
    void postComNomeEmBrancoRetorna400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  \",\"email\":\"ana@exemplo.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());
    }

    @Test
    @DisplayName("POST com e-mail duplicado responde 409")
    void postComEmailDuplicadoRetorna409() throws Exception {
        when(service.criar(any(UsuarioRequest.class)))
                .thenThrow(new EmailJaCadastradoException("ana@exemplo.com"));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET de id inexistente responde 404")
    void getInexistenteRetorna404() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new UsuarioNaoEncontradoException(99L));

        mockMvc.perform(get("/api/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT de id inexistente responde 404")
    void putInexistenteRetorna404() throws Exception {
        when(service.atualizar(eq(99L), any(UsuarioRequest.class)))
                .thenThrow(new UsuarioNaoEncontradoException(99L));

        mockMvc.perform(put("/api/usuarios/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE responde 204 sem corpo")
    void deleteRetorna204() throws Exception {
        doNothing().when(service).deletar(1L);

        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("método HTTP não suportado responde 405, não 500")
    void metodoNaoSuportadoRetorna405() throws Exception {
        mockMvc.perform(patch("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Content-Type não suportado responde 415, não 500")
    void contentTypeNaoSuportadoRetorna415() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("JSON malformado responde 400, não 500")
    void jsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("id em formato inválido responde 400, não 500")
    void idInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/usuarios/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE de id inexistente responde 404, não 204")
    void deleteInexistenteRetorna404() throws Exception {
        doThrow(new UsuarioNaoEncontradoException(99L)).when(service).deletar(99L);

        mockMvc.perform(delete("/api/usuarios/99"))
                .andExpect(status().isNotFound());
    }
}
