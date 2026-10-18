package com.chd.rental.service.impl;

import com.chd.rental.dto.UsuarioRequest;
import com.chd.rental.dto.UsuarioResponse;
import com.chd.rental.entity.Usuario;
import com.chd.rental.entity.enums.EstadoRegistro;
import com.chd.rental.exception.RecursoNoEncontradoException;
import com.chd.rental.exception.ReglaNegocioException;
import com.chd.rental.repository.ContratoRepository;
import com.chd.rental.repository.UsuarioRepository;
import com.chd.rental.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ContratoRepository contratoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll(Sort.by("nombreCompleto")).stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new ReglaNegocioException("La contrasena es obligatoria al crear un usuario",
                    HttpStatus.BAD_REQUEST);
        }
        String email = req.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + email);
        }
        Usuario usuario = Usuario.builder()
                .nombreCompleto(req.getNombreCompleto().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .rol(req.getRol())
                .telefono(req.getTelefono())
                .estado(req.getEstado() != null ? req.getEstado() : EstadoRegistro.ACTIVO)
                .build();
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req) {
        Usuario usuario = buscar(id);
        String email = req.getEmail().trim().toLowerCase();
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("Ya existe un usuario con el email " + email);
                });
        usuario.setNombreCompleto(req.getNombreCompleto().trim());
        usuario.setEmail(email);
        usuario.setRol(req.getRol());
        usuario.setTelefono(req.getTelefono());
        if (req.getEstado() != null) {
            usuario.setEstado(req.getEstado());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = buscar(id);
        if (contratoRepository.existsByUsuarioId(id)) {
            throw new ReglaNegocioException(
                    "El usuario tiene contratos registrados; cambie su estado a INACTIVO en lugar de eliminarlo");
        }
        usuarioRepository.delete(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }
}
