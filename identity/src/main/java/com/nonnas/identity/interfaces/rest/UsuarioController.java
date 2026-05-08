package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.usuario.CriarUsuarioUseCase;
import com.nonnas.identity.application.usuario.ListarUsuariosUseCase;
import com.nonnas.identity.interfaces.rest.dto.UsuarioDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final CriarUsuarioUseCase criar;
    private final ListarUsuariosUseCase listar;

    public UsuarioController(CriarUsuarioUseCase criar, ListarUsuariosUseCase listar) {
        this.criar = criar;
        this.listar = listar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public UsuarioDto.Response create(@Valid @RequestBody UsuarioDto.CreateRequest req) {
        return UsuarioDto.Response.from(
                criar.execute(req.filialId(), req.nome(), req.email(), req.senha(), req.perfil()));
    }

    @GetMapping
    public List<UsuarioDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return listar.execute(page, size).stream().map(UsuarioDto.Response::from).toList();
    }
}
