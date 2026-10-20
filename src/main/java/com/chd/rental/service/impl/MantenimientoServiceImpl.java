package com.chd.rental.service.impl;

import com.chd.rental.dto.MantenimientoRequest;
import com.chd.rental.dto.MantenimientoResponse;
import com.chd.rental.entity.Mantenimiento;
import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.EstadoContrato;
import com.chd.rental.entity.enums.EstadoMantenimiento;
import com.chd.rental.entity.enums.EstadoVehiculo;
import com.chd.rental.exception.RecursoNoEncontradoException;
import com.chd.rental.exception.ReglaNegocioException;
import com.chd.rental.repository.ContratoRepository;
import com.chd.rental.repository.MantenimientoRepository;
import com.chd.rental.repository.VehiculoRepository;
import com.chd.rental.service.MantenimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MantenimientoServiceImpl implements MantenimientoService {

    private static final EnumSet<EstadoMantenimiento> ABIERTOS =
            EnumSet.of(EstadoMantenimiento.PROGRAMADO, EstadoMantenimiento.EN_PROCESO);

    private final MantenimientoRepository mantenimientoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ContratoRepository contratoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MantenimientoResponse> listar(Long idVehiculo) {
        List<Mantenimiento> lista = idVehiculo != null
                ? mantenimientoRepository.findByVehiculoIdOrderByFechaInicioDesc(idVehiculo)
                : mantenimientoRepository.findAllByOrderByFechaInicioDesc();
        return lista.stream().map(MantenimientoResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MantenimientoResponse obtener(Long id) {
        return MantenimientoResponse.from(buscar(id));
    }

    @Override
    @Transactional
    public MantenimientoResponse crear(MantenimientoRequest req) {
        Mantenimiento m = new Mantenimiento();
        copiar(req, m);
        m = mantenimientoRepository.save(m);
        sincronizarVehiculo(m);
        return MantenimientoResponse.from(m);
    }

    @Override
    @Transactional
    public MantenimientoResponse actualizar(Long id, MantenimientoRequest req) {
        Mantenimiento m = buscar(id);
        if (m.getEstado() == EstadoMantenimiento.COMPLETADO || m.getEstado() == EstadoMantenimiento.CANCELADO) {
            throw new ReglaNegocioException("El mantenimiento esta " + m.getEstado() + " y ya no puede modificarse");
        }
        Vehiculo anterior = m.getVehiculo();
        copiar(req, m);
        mantenimientoRepository.saveAndFlush(m);
        sincronizarVehiculo(m);
        if (anterior != m.getVehiculo()) {
            liberarSiCorresponde(anterior, m.getId());
        }
        return MantenimientoResponse.from(m);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Mantenimiento m = buscar(id);
        Vehiculo v = m.getVehiculo();
        mantenimientoRepository.delete(m);
        mantenimientoRepository.flush();
        liberarSiCorresponde(v, id);
    }

    // Mientras el mantenimiento esta PROGRAMADO o EN_PROCESO el vehiculo pasa a MANTENIMIENTO.
    // Cuando se completa o cancela se libera y, si se completo, se actualiza el kilometraje.
    private void sincronizarVehiculo(Mantenimiento m) {
        Vehiculo v = m.getVehiculo();
        if (ABIERTOS.contains(m.getEstado())) {
            if (v.getEstado() == EstadoVehiculo.ALQUILADO) {
                throw new ReglaNegocioException("El vehiculo " + v.getPlaca()
                        + " esta ALQUILADO; finalice el contrato antes de enviarlo a mantenimiento");
            }
            v.setEstado(EstadoVehiculo.MANTENIMIENTO);
            return;
        }
        if (m.getEstado() == EstadoMantenimiento.COMPLETADO) {
            if (m.getFechaFin() == null) {
                m.setFechaFin(LocalDate.now());
            }
            if (m.getKilometraje() != null && m.getKilometraje() > v.getKilometraje()) {
                v.setKilometraje(m.getKilometraje());
            }
        }
        liberarSiCorresponde(v, m.getId());
    }

    private void liberarSiCorresponde(Vehiculo v, Long idExcluir) {
        if (v.getEstado() != EstadoVehiculo.MANTENIMIENTO) {
            return;
        }
        boolean otroAbierto = mantenimientoRepository.existsByVehiculoIdAndEstadoInAndIdNot(
                v.getId(), ABIERTOS, idExcluir != null ? idExcluir : -1L);
        if (!otroAbierto) {
            v.setEstado(contratoRepository.existsByVehiculoIdAndEstado(v.getId(), EstadoContrato.ACTIVO)
                    ? EstadoVehiculo.ALQUILADO : EstadoVehiculo.DISPONIBLE);
        }
    }

    private void copiar(MantenimientoRequest req, Mantenimiento m) {
        if (req.getFechaFin() != null && req.getFechaFin().isBefore(req.getFechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la fecha de inicio",
                    HttpStatus.BAD_REQUEST);
        }
        Vehiculo v = m.getVehiculo() != null && m.getVehiculo().getId().equals(req.getIdVehiculo())
                ? m.getVehiculo()
                : vehiculoRepository.findById(req.getIdVehiculo())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Vehiculo", req.getIdVehiculo()));
        m.setVehiculo(v);
        m.setTipo(req.getTipo());
        m.setFechaInicio(req.getFechaInicio());
        m.setFechaFin(req.getFechaFin());
        m.setKilometraje(req.getKilometraje() != null ? req.getKilometraje() : v.getKilometraje());
        m.setDescripcion(req.getDescripcion().trim());
        m.setTaller(req.getTaller());
        m.setCosto(req.getCosto());
        m.setEstado(req.getEstado() != null ? req.getEstado() : EstadoMantenimiento.PROGRAMADO);
        m.setProximoMantenimiento(req.getProximoMantenimiento());
        m.setObservaciones(req.getObservaciones());
    }

    private Mantenimiento buscar(Long id) {
        return mantenimientoRepository.findWithVehiculoById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mantenimiento", id));
    }
}
