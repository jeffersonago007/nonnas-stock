package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.usuario.AtivarUsuarioUseCase;
import com.nonnas.identity.application.usuario.AtualizarUsuarioUseCase;
import com.nonnas.identity.application.usuario.BuscarUsuarioUseCase;
import com.nonnas.identity.application.usuario.CriarUsuarioUseCase;
import com.nonnas.identity.application.usuario.DesativarUsuarioUseCase;
import com.nonnas.identity.application.usuario.ListarUsuariosUseCase;
import com.nonnas.identity.domain.FilialId;
import com.nonnas.identity.interfaces.rest.dto.UsuarioDto;
import com.nonnas.web.security.SecurityScope;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final CriarUsuarioUseCase criar;
    private final ListarUsuariosUseCase listar;
    private final BuscarUsuarioUseCase buscar;
    private final AtualizarUsuarioUseCase atualizar;
    private final DesativarUsuarioUseCase desativar;
    private final AtivarUsuarioUseCase ativar;

    public UsuarioController(CriarUsuarioUseCase criar,
                             ListarUsuariosUseCase listar,
                             BuscarUsuarioUseCase buscar,
                             AtualizarUsuarioUseCase atualizar,
                             DesativarUsuarioUseCase desativar,
                             AtivarUsuarioUseCase ativar) {
        this.criar = criar;
        this.listar = listar;
        this.buscar = buscar;
        this.atualizar = atualizar;
        this.desativar = desativar;
        this.ativar = ativar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public UsuarioDto.Response create(@Valid @RequestBody UsuarioDto.CreateRequest req) {
        return UsuarioDto.Response.from(
                criar.execute(req.filialId(), req.nome(), req.email(), req.senha(), req.perfil()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<UsuarioDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        var todos = listar.execute(page, size).stream();
        if (!SecurityScope.isAdmin()) {
            FilialId minha = FilialId.of(SecurityScope.currentFilialId().orElseThrow());
            todos = todos.filter(u -> u.filialId().map(minha::equals).orElse(false));
        }
        return todos.map(UsuarioDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public UsuarioDto.Response getById(@PathVariable UUID id) {
        var usuario = buscar.execute(id);
        usuario.filialId().ifPresent(f -> SecurityScope.assertCanAccess(f.value()));
        return UsuarioDto.Response.from(usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public UsuarioDto.Response update(@PathVariable UUID id,
                                      @Valid @RequestBody UsuarioDto.UpdateRequest req) {
        return UsuarioDto.Response.from(atualizar.execute(id, req.nome()));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public UsuarioDto.Response deactivate(@PathVariable UUID id) {
        return UsuarioDto.Response.from(desativar.execute(id));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public UsuarioDto.Response activate(@PathVariable UUID id) {
        return UsuarioDto.Response.from(ativar.execute(id));
    }
}
