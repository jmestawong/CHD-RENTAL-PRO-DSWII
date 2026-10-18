package com.chd.rental.service.impl;

import com.chd.rental.dto.ClienteRequest;
import com.chd.rental.entity.Cliente;
import com.chd.rental.entity.enums.EstadoRegistro;
import com.chd.rental.exception.RecursoNoEncontradoException;
import com.chd.rental.exception.ReglaNegocioException;
import com.chd.rental.repository.ClienteRepository;
import com.chd.rental.repository.ContratoRepository;
import com.chd.rental.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ContratoRepository contratoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listar(EstadoRegistro estado, String buscar) {
        if (buscar != null && !buscar.isBlank()) {
            return clienteRepository.findByRazonSocialContainingIgnoreCaseOrderByRazonSocialAsc(buscar.trim());
        }
        if (estado != null) {
            return clienteRepository.findByEstadoOrderByRazonSocialAsc(estado);
        }
        return clienteRepository.findAll(Sort.by("razonSocial"));
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    @Override
    @Transactional
    public Cliente crear(ClienteRequest req) {
        if (clienteRepository.existsByRuc(req.getRuc())) {
            throw new ReglaNegocioException("Ya existe un cliente con el RUC " + req.getRuc());
        }
        Cliente cliente = new Cliente();
        copiar(req, cliente);
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public Cliente actualizar(Long id, ClienteRequest req) {
        Cliente cliente = obtener(id);
        clienteRepository.findByRuc(req.getRuc())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("Ya existe un cliente con el RUC " + req.getRuc());
                });
        copiar(req, cliente);
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = obtener(id);
        if (contratoRepository.existsByClienteId(id)) {
            throw new ReglaNegocioException(
                    "El cliente tiene contratos registrados; cambie su estado a INACTIVO en lugar de eliminarlo");
        }
        clienteRepository.delete(cliente);
    }

    private void copiar(ClienteRequest req, Cliente c) {
        c.setRazonSocial(req.getRazonSocial().trim());
        c.setRuc(req.getRuc());
        c.setDireccion(req.getDireccion());
        c.setSector(req.getSector());
        c.setTelefono(req.getTelefono());
        c.setEmail(req.getEmail());
        c.setContactoNombre(req.getContactoNombre());
        c.setContactoCargo(req.getContactoCargo());
        c.setContactoTelefono(req.getContactoTelefono());
        c.setEstado(req.getEstado() != null ? req.getEstado() : EstadoRegistro.ACTIVO);
        c.setObservaciones(req.getObservaciones());
    }
}
