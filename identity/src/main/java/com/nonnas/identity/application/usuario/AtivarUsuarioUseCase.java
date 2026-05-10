package com.nonnas.identity.application.usuario;

import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AtivarUsuarioUseCase {

    private final UsuarioRepository repository;
    private final Clock clock;

    public AtivarUsuarioUseCase(UsuarioRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Usuario execute(UUID id) {
        Usuario usuario = repository.findById(UsuarioId.of(id))
                .orElseThrow(() -> new NotFoundException("Usuario", id));
        usuario.ativar(clock.instant());
        return repository.save(usuario);
    }
}
