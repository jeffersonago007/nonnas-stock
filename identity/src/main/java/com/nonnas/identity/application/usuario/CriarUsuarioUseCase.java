package com.nonnas.identity.application.usuario;

import com.nonnas.identity.application.password.HistoricoSenhaService;
import com.nonnas.identity.application.password.SenhaValidaValidator;
import com.nonnas.identity.application.ports.FilialRepository;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.FilialId;
import com.nonnas.identity.domain.Perfil;
import com.nonnas.identity.domain.SenhaHash;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class CriarUsuarioUseCase {

    private final UsuarioRepository usuarioRepo;
    private final FilialRepository filialRepo;
    private final PasswordEncoder encoder;
    private final HistoricoSenhaService historico;
    private final SenhaValidaValidator senhaValidator;
    private final Clock clock;

    public CriarUsuarioUseCase(UsuarioRepository usuarioRepo,
                               FilialRepository filialRepo,
                               PasswordEncoder encoder,
                               HistoricoSenhaService historico,
                               Clock clock) {
        this.usuarioRepo = usuarioRepo;
        this.filialRepo = filialRepo;
        this.encoder = encoder;
        this.historico = historico;
        this.senhaValidator = new SenhaValidaValidator();
        this.clock = clock;
    }

    @Transactional
    public Usuario execute(UUID filialId, String nome, String email, String senhaPlaintext, Perfil perfil) {
        Email emailVo = Email.of(email);
        if (usuarioRepo.existsByEmail(emailVo)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Já existe usuário com email " + emailVo.value());
        }

        if (!senhaValidator.isValid(senhaPlaintext, null)) {
            throw new ValidationException(
                    "Senha não atende à política: mínimo 10 caracteres, ao menos 1 letra, 1 número e 1 caractere especial");
        }

        FilialId filialVo = null;
        if (filialId != null) {
            filialVo = FilialId.of(filialId);
            if (filialRepo.findById(filialVo).isEmpty()) {
                throw new NotFoundException("Filial", filialId);
            }
        } else if (perfil != Perfil.ADMIN) {
            throw new ValidationException(
                    "Usuários não-ADMIN devem estar vinculados a uma filial");
        }

        SenhaHash hash = SenhaHash.of(encoder.encode(senhaPlaintext));
        Usuario usuario = Usuario.novo(filialVo, nome, emailVo, hash, perfil, clock.instant());
        Usuario saved = usuarioRepo.save(usuario);
        historico.registrarSenha(saved.id(), hash.value());
        return saved;
    }
}
