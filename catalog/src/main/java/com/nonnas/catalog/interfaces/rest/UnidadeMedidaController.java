package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.application.unidademedida.AtivarUnidadeMedidaUseCase;
import com.nonnas.catalog.application.unidademedida.AtualizarUnidadeMedidaUseCase;
import com.nonnas.catalog.application.unidademedida.BuscarUnidadeMedidaUseCase;
import com.nonnas.catalog.application.unidademedida.CriarUnidadeMedidaUseCase;
import com.nonnas.catalog.application.unidademedida.DesativarUnidadeMedidaUseCase;
import com.nonnas.catalog.application.unidademedida.ListarUnidadesMedidaUseCase;
import com.nonnas.catalog.interfaces.rest.dto.UnidadeMedidaDto;
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
@RequestMapping("/api/v1/unidades-medida")
public class UnidadeMedidaController {

    private final CriarUnidadeMedidaUseCase criar;
    private final ListarUnidadesMedidaUseCase listar;
    private final BuscarUnidadeMedidaUseCase buscar;
    private final AtualizarUnidadeMedidaUseCase atualizar;
    private final DesativarUnidadeMedidaUseCase desativar;
    private final AtivarUnidadeMedidaUseCase ativar;

    public UnidadeMedidaController(CriarUnidadeMedidaUseCase criar,
                                   ListarUnidadesMedidaUseCase listar,
                                   BuscarUnidadeMedidaUseCase buscar,
                                   AtualizarUnidadeMedidaUseCase atualizar,
                                   DesativarUnidadeMedidaUseCase desativar,
                                   AtivarUnidadeMedidaUseCase ativar) {
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
    public UnidadeMedidaDto.Response create(@Valid @RequestBody UnidadeMedidaDto.CreateRequest req) {
        return UnidadeMedidaDto.Response.from(criar.execute(req.codigo(), req.nome(), req.tipo()));
    }

    @GetMapping
    public List<UnidadeMedidaDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return listar.execute(page, size).stream().map(UnidadeMedidaDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    public UnidadeMedidaDto.Response getById(@PathVariable UUID id) {
        return UnidadeMedidaDto.Response.from(buscar.execute(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public UnidadeMedidaDto.Response update(@PathVariable UUID id,
                                            @Valid @RequestBody UnidadeMedidaDto.UpdateRequest req) {
        return UnidadeMedidaDto.Response.from(atualizar.execute(id, req.nome()));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public UnidadeMedidaDto.Response deactivate(@PathVariable UUID id) {
        return UnidadeMedidaDto.Response.from(desativar.execute(id));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public UnidadeMedidaDto.Response activate(@PathVariable UUID id) {
        return UnidadeMedidaDto.Response.from(ativar.execute(id));
    }
}
