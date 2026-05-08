package com.nonnas.identity.infrastructure.persistence;

import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class UsuarioRepositoryImpl implements UsuarioRepository {

    private final SpringDataUsuarioRepository jpa;

    UsuarioRepositoryImpl(SpringDataUsuarioRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioEntity saved = jpa.save(IdentityMappers.toEntity(usuario));
        return IdentityMappers.toDomain(saved);
    }

    @Override
    public Optional<Usuario> findById(UsuarioId id) {
        return jpa.findById(id.value()).map(IdentityMappers::toDomain);
    }

    @Override
    public Optional<Usuario> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(IdentityMappers::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public List<Usuario> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size))
                .map(IdentityMappers::toDomain)
                .getContent();
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
