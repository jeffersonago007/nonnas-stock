package com.nonnas.identity.application.usuario;

import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarUsuarioUseCase {

    private final UsuarioRepository repository;

    public BuscarUsuarioUseCase(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Usuario execute(UUID id) {
        return repository.findById(UsuarioId.of(id))
                .orElseThrow(() -> new NotFoundException("Usuario", id));
    }
}
