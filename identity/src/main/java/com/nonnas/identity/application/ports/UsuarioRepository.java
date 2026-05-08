package com.nonnas.identity.application.ports;

import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(UsuarioId id);

    Optional<Usuario> findByEmail(Email email);

    boolean existsByEmail(Email email);

    List<Usuario> findAll(int page, int size);

    long count();
}
