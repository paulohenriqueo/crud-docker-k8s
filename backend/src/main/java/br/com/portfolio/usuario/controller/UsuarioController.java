package br.com.portfolio.usuario.controller;

import br.com.portfolio.usuario.dto.UsuarioRequest;
import br.com.portfolio.usuario.dto.UsuarioResponse;
import br.com.portfolio.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    /**
     * Sempre paginado. O tamanho máximo é limitado por configuração
     * (spring.data.web.pageable.max-page-size), então nenhum cliente
     * consegue pedir a tabela inteira numa página só.
     *
     * Devolve PagedModel, e não Page: o Spring Data não garante estabilidade
     * do JSON de PageImpl entre versões. Envolver aqui fixa o contrato que o
     * frontend consome — content + um objeto "page" com os metadados.
     */
    @GetMapping
    public PagedModel<UsuarioResponse> listar(
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return new PagedModel<>(service.listar(pageable));
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    /** 201 com header Location apontando para o recurso criado. */
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(
            @Valid @RequestBody UsuarioRequest request, UriComponentsBuilder uriBuilder) {

        UsuarioResponse criado = service.criar(request);
        URI location = uriBuilder.path("/api/usuarios/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return service.atualizar(id, request);
    }

    /** 204: a resposta não tem corpo, então não é 200. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
