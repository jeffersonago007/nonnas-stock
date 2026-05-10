package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.empresa.AtivarEmpresaUseCase;
import com.nonnas.identity.application.empresa.AtualizarEmpresaUseCase;
import com.nonnas.identity.application.empresa.BuscarEmpresaUseCase;
import com.nonnas.identity.application.empresa.CriarEmpresaUseCase;
import com.nonnas.identity.application.empresa.DesativarEmpresaUseCase;
import com.nonnas.identity.application.empresa.ListarEmpresasUseCase;
import com.nonnas.identity.interfaces.rest.dto.EmpresaDto;
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
@RequestMapping("/api/v1/empresas")
public class EmpresaController {

    private final CriarEmpresaUseCase criar;
    private final ListarEmpresasUseCase listar;
    private final BuscarEmpresaUseCase buscar;
    private final AtualizarEmpresaUseCase atualizar;
    private final DesativarEmpresaUseCase desativar;
    private final AtivarEmpresaUseCase ativar;

    public EmpresaController(CriarEmpresaUseCase criar,
                             ListarEmpresasUseCase listar,
                             BuscarEmpresaUseCase buscar,
                             AtualizarEmpresaUseCase atualizar,
                             DesativarEmpresaUseCase desativar,
                             AtivarEmpresaUseCase ativar) {
        this.criar = criar;
        this.listar = listar;
        this.buscar = buscar;
        this.atualizar = atualizar;
        this.desativar = desativar;
        this.ativar = ativar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EmpresaDto.Response create(@Valid @RequestBody EmpresaDto.CreateRequest req) {
        return EmpresaDto.Response.from(criar.execute(req.razaoSocial(), req.cnpj()));
    }

    @GetMapping
    public List<EmpresaDto.Response> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return listar.execute(page, size).stream().map(EmpresaDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    public EmpresaDto.Response getById(@PathVariable UUID id) {
        return EmpresaDto.Response.from(buscar.execute(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmpresaDto.Response update(@PathVariable UUID id,
                                      @Valid @RequestBody EmpresaDto.UpdateRequest req) {
        return EmpresaDto.Response.from(atualizar.execute(id, req.razaoSocial()));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public EmpresaDto.Response deactivate(@PathVariable UUID id) {
        return EmpresaDto.Response.from(desativar.execute(id));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    public EmpresaDto.Response activate(@PathVariable UUID id) {
        return EmpresaDto.Response.from(ativar.execute(id));
    }
}
