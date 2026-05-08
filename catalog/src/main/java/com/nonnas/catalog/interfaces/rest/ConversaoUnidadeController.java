package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.application.conversao.ConverterUnidadeUseCase;
import com.nonnas.catalog.application.conversao.CriarConversaoUnidadeUseCase;
import com.nonnas.catalog.interfaces.rest.dto.ConversaoUnidadeDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/conversoes-unidade")
public class ConversaoUnidadeController {

    private final CriarConversaoUnidadeUseCase criar;
    private final ConverterUnidadeUseCase converter;

    public ConversaoUnidadeController(CriarConversaoUnidadeUseCase criar,
                                      ConverterUnidadeUseCase converter) {
        this.criar = criar;
        this.converter = converter;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversaoUnidadeDto.Response create(@Valid @RequestBody ConversaoUnidadeDto.CreateRequest req) {
        return ConversaoUnidadeDto.Response.from(
                criar.execute(req.origemId(), req.destinoId(), req.fator(), req.insumoId()));
    }

    @PostMapping("/converter")
    public ConversaoUnidadeDto.ConverterResponse converter(
            @Valid @RequestBody ConversaoUnidadeDto.ConverterRequest req) {
        BigDecimal resultado = converter.execute(req.valor(), req.origemId(), req.destinoId(), req.insumoId());
        return new ConversaoUnidadeDto.ConverterResponse(
                req.valor(), req.origemId(), resultado, req.destinoId(), req.insumoId());
    }
}
