package com.nonnas.identity.infrastructure.persistence;

import com.nonnas.identity.domain.Cnpj;
import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.Empresa;
import com.nonnas.identity.domain.EmpresaId;
import com.nonnas.identity.domain.Filial;
import com.nonnas.identity.domain.FilialId;
import com.nonnas.identity.domain.RazaoSocial;
import com.nonnas.identity.domain.SenhaHash;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;

/**
 * Mappers manuais Domain ↔ JPA Entity. Mantidos como utilitários estáticos
 * para evitar magia de codegen no MVP — quando T03+ trouxer o número de
 * mappers para algo significativo, migra para MapStruct.
 */
final class IdentityMappers {

    private IdentityMappers() {}

    static EmpresaEntity toEntity(Empresa source) {
        EmpresaEntity e = new EmpresaEntity();
        e.setId(source.id().value());
        e.setRazaoSocial(source.razaoSocial().value());
        e.setCnpj(source.cnpj().value());
        e.setAtivo(source.ativa());
        e.setCreatedAt(source.createdAt());
        e.setUpdatedAt(source.updatedAt());
        return e;
    }

    static Empresa toDomain(EmpresaEntity source) {
        return new Empresa(
                EmpresaId.of(source.getId()),
                RazaoSocial.of(source.getRazaoSocial()),
                Cnpj.of(source.getCnpj()),
                source.isAtivo(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }

    static FilialEntity toEntity(Filial source) {
        FilialEntity e = new FilialEntity();
        e.setId(source.id().value());
        e.setEmpresaId(source.empresaId().value());
        e.setNome(source.nome());
        e.setCnpj(source.cnpj().value());
        e.setEndereco(source.endereco().orElse(null));
        e.setAtiva(source.ativa());
        e.setCreatedAt(source.createdAt());
        e.setUpdatedAt(source.updatedAt());
        return e;
    }

    static Filial toDomain(FilialEntity source) {
        return new Filial(
                FilialId.of(source.getId()),
                EmpresaId.of(source.getEmpresaId()),
                source.getNome(),
                Cnpj.of(source.getCnpj()),
                source.getEndereco(),
                source.isAtiva(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }

    static UsuarioEntity toEntity(Usuario source) {
        UsuarioEntity e = new UsuarioEntity();
        e.setId(source.id().value());
        e.setFilialId(source.filialId().map(FilialId::value).orElse(null));
        e.setNome(source.nome());
        e.setEmail(source.email().value());
        e.setSenhaHash(source.senhaHash().value());
        e.setPerfil(source.perfil());
        e.setAtivo(source.ativo());
        e.setTentativasFalhas(source.tentativasFalhas());
        e.setBloqueadoAte(source.bloqueadoAte().orElse(null));
        e.setTravada(source.travada());
        e.setCreatedAt(source.createdAt());
        e.setUpdatedAt(source.updatedAt());
        return e;
    }

    static Usuario toDomain(UsuarioEntity source) {
        return new Usuario(
                UsuarioId.of(source.getId()),
                source.getFilialId() != null ? FilialId.of(source.getFilialId()) : null,
                source.getNome(),
                Email.of(source.getEmail()),
                SenhaHash.of(source.getSenhaHash()),
                source.getPerfil(),
                source.isAtivo(),
                source.getTentativasFalhas(),
                source.getBloqueadoAte(),
                source.isTravada(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}
