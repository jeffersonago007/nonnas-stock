package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.application.fornecedor.AtivarFornecedorUseCase;
import com.nonnas.catalog.application.fornecedor.AtualizarFornecedorUseCase;
import com.nonnas.catalog.application.fornecedor.BuscarFornecedorUseCase;
import com.nonnas.catalog.application.fornecedor.CriarFornecedorUseCase;
import com.nonnas.catalog.application.fornecedor.DesativarFornecedorUseCase;
import com.nonnas.catalog.application.fornecedor.ListarFornecedoresUseCase;
import com.nonnas.catalog.interfaces.rest.dto.FornecedorDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/fornecedores")
public class FornecedorController {

    private final CriarFornecedorUseCase criar;
    private final ListarFornecedoresUseCase listar;
    private final BuscarFornecedorUseCase buscar;
    private final AtualizarFornecedorUseCase atualizar;
    private final DesativarFornecedorUseCase desativar;
    private final AtivarFornecedorUseCase ativar;

    public FornecedorController(CriarFornecedorUseCase criar,
                                ListarFornecedoresUseCase listar,
                                BuscarFornecedorUseCase buscar,
                                AtualizarFornecedorUseCase atualizar,
                                DesativarFornecedorUseCase desativar,
                                AtivarFornecedorUseCase ativar) {
        this.criar = criar;
        this.listar = listar;
        this.buscar = buscar;
        this.atualizar = atualizar;
        this.desativar = desativar;
        this.ativar = ativar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FornecedorDto.Response create(@Valid @RequestBody FornecedorDto.CreateRequest req) {
        var contatos = req.contatos() == null ? java.util.List.<com.nonnas.catalog.domain.ContatoFornecedor>of()
                : req.contatos().stream().map(FornecedorDto.ContatoRequest::toDomain).toList();
        return FornecedorDto.Response.from(criar.execute(req.razaoSocial(), req.cnpj(), contatos));
    }

    @GetMapping
    public List<FornecedorDto.Response> list(@RequestParam(required = false) Boolean ativo,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return listar.execute(ativo, q, page, size).stream().map(FornecedorDto.Response::from).toList();
    }

    @GetMapping("/{id}")
    public FornecedorDto.Response getById(@PathVariable UUID id) {
        return FornecedorDto.Response.from(buscar.execute(id));
    }

    @PutMapping("/{id}")
    public FornecedorDto.Response update(@PathVariable UUID id,
                                         @Valid @RequestBody FornecedorDto.UpdateRequest req) {
        var contatos = req.contatos() == null ? null
                : req.contatos().stream().map(FornecedorDto.ContatoRequest::toDomain).toList();
        return FornecedorDto.Response.from(atualizar.execute(id, req.razaoSocial(), contatos));
    }

    @PatchMapping("/{id}/desativar")
    public FornecedorDto.Response deactivate(@PathVariable UUID id) {
        return FornecedorDto.Response.from(desativar.execute(id));
    }

    @PatchMapping("/{id}/ativar")
    public FornecedorDto.Response activate(@PathVariable UUID id) {
        return FornecedorDto.Response.from(ativar.execute(id));
    }
}
