package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.filial.AtivarFilialUseCase;
import com.nonnas.identity.application.filial.AtualizarFilialUseCase;
import com.nonnas.identity.application.filial.BuscarFilialUseCase;
import com.nonnas.identity.application.filial.CriarFilialUseCase;
import com.nonnas.identity.application.filial.DesativarFilialUseCase;
import com.nonnas.identity.application.filial.ListarFiliaisUseCase;
import com.nonnas.identity.interfaces.rest.dto.FilialDto;
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
@RequestMapping("/api/v1/filiais")
public class FilialController {

    private final CriarFilialUseCase criar;
    private final ListarFiliaisUseCase listar;
    private final BuscarFilialUseCase buscar;
    private final AtualizarFilialUseCase atualizar;
    private final DesativarFilialUseCase desativar;
    private final AtivarFilialUseCase ativar;

    public FilialController(CriarFilialUseCase criar,
                            ListarFiliaisUseCase listar,
                            BuscarFilialUseCase buscar,
                            AtualizarFilialUseCase atualizar,
                            DesativarFilialUseCase desativar,
                            AtivarFilialUseCase ativar) {
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
    public FilialDto.Response create(@Valid @RequestBody FilialDto.CreateRequest req) {
        return FilialDto.Response.from(criar.execute(req.empresaId(), req.nome(), req.cnpj(), req.endereco()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<FilialDto.Response> list(@RequestParam(required = false) UUID empresaId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "100") int size) {
        var todas = listar.execute(empresaId, page, size).stream();
        if (!SecurityScope.isAdmin()) {
            UUID minha = SecurityScope.currentFilialId().orElseThrow();
            todas = todas.filter(f -> f.id().value().equals(minha));
        }
        return todas.map(FilialDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public FilialDto.Response getById(@PathVariable UUID id) {
        SecurityScope.assertCanAccess(id);
        return FilialDto.Response.from(buscar.execute(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public FilialDto.Response update(@PathVariable UUID id,
                                     @Valid @RequestBody FilialDto.UpdateRequest req) {
        return FilialDto.Response.from(atualizar.execute(id, req.nome(), req.endereco()));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public FilialDto.Response deactivate(@PathVariable UUID id) {
        return FilialDto.Response.from(desativar.execute(id));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public FilialDto.Response activate(@PathVariable UUID id) {
        return FilialDto.Response.from(ativar.execute(id));
    }
}
