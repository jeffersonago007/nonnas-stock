package com.nonnas.identity.application.usuario;

import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarUsuariosUseCase {

    private final UsuarioRepository repository;

    public ListarUsuariosUseCase(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Usuario> execute(int page, int size) {
        return repository.findAll(page, size);
    }
}
