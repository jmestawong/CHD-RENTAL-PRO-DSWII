package com.chd.rental.service.impl;

import com.chd.rental.dto.VehiculoRequest;
import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.EstadoVehiculo;
import com.chd.rental.exception.RecursoNoEncontradoException;
import com.chd.rental.exception.ReglaNegocioException;
import com.chd.rental.repository.ContratoRepository;
import com.chd.rental.repository.MantenimientoRepository;
import com.chd.rental.repository.VehiculoRepository;
import com.chd.rental.service.VehiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final ContratoRepository contratoRepository;
    private final MantenimientoRepository mantenimientoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Vehiculo> listar(EstadoVehiculo estado) {
        if (estado != null) {
            return vehiculoRepository.findByEstadoOrderByMarcaAscModeloAsc(estado);
        }
        return vehiculoRepository.findAll(Sort.by("marca", "modelo"));
    }

    @Override
    @Transactional(readOnly = true)
    public Vehiculo obtener(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehiculo", id));
    }

    @Override
    @Transactional
    public Vehiculo crear(VehiculoRequest req) {
        String placa = req.getPlaca().toUpperCase();
        if (vehiculoRepository.existsByPlacaIgnoreCase(placa)) {
            throw new ReglaNegocioException("Ya existe un vehiculo con la placa " + placa);
        }
        Vehiculo v = new Vehiculo();
        v.setPlaca(placa);
        v.setMarca(req.getMarca().trim());
        v.setModelo(req.getModelo().trim());
        v.setEstado(req.getEstado() != null ? req.getEstado() : EstadoVehiculo.DISPONIBLE);
        copiarDatosOperativos(req, v);
        return vehiculoRepository.save(v);
    }

    @Override
    @Transactional
    public Vehiculo actualizar(Long id, VehiculoRequest req) {
        Vehiculo v = obtener(id);
        // placa, marca y modelo identifican la unidad, por eso no se pueden cambiar
        // despues del registro (observacion del profesor en el ciclo anterior)
        if (!v.getPlaca().equalsIgnoreCase(req.getPlaca())
                || !v.getMarca().equalsIgnoreCase(req.getMarca().trim())
                || !v.getModelo().equalsIgnoreCase(req.getModelo().trim())) {
            throw new ReglaNegocioException(
                    "La placa, marca y modelo no se pueden modificar despues del registro del vehiculo");
        }
        if (req.getEstado() != null && req.getEstado() != v.getEstado()) {
            validarCambioEstado(v, req.getEstado());
            v.setEstado(req.getEstado());
        }
        copiarDatosOperativos(req, v);
        return vehiculoRepository.save(v);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Vehiculo v = obtener(id);
        if (contratoRepository.existsByVehiculoId(id) || mantenimientoRepository.existsByVehiculoId(id)) {
            throw new ReglaNegocioException(
                    "El vehiculo tiene contratos o mantenimientos; cambie su estado a INACTIVO en lugar de eliminarlo");
        }
        vehiculoRepository.delete(v);
    }

    // El estado ALQUILADO lo maneja el modulo de contratos, no se cambia a mano desde aqui
    private void validarCambioEstado(Vehiculo v, EstadoVehiculo nuevo) {
        if (v.getEstado() == EstadoVehiculo.ALQUILADO && nuevo != EstadoVehiculo.ALQUILADO) {
            throw new ReglaNegocioException(
                    "El vehiculo esta ALQUILADO; finalice o cancele el contrato para liberarlo");
        }
        if (nuevo == EstadoVehiculo.ALQUILADO) {
            throw new ReglaNegocioException(
                    "El estado ALQUILADO se asigna automaticamente al registrar un contrato activo");
        }
    }

    private void copiarDatosOperativos(VehiculoRequest req, Vehiculo v) {
        if (req.getKilometraje() != null && v.getKilometraje() != null
                && v.getId() != null && req.getKilometraje() < v.getKilometraje()) {
            throw new ReglaNegocioException("El kilometraje no puede ser menor al registrado ("
                    + v.getKilometraje() + " km)");
        }
        v.setAnio(req.getAnio());
        v.setTipo(req.getTipo());
        v.setColor(req.getColor());
        v.setNumMotor(req.getNumMotor());
        v.setNumChasis(req.getNumChasis());
        v.setCapacidadPasajeros(req.getCapacidadPasajeros());
        v.setCapacidadCarga(req.getCapacidadCarga());
        v.setKilometraje(req.getKilometraje() != null ? req.getKilometraje() : (v.getKilometraje() != null ? v.getKilometraje() : 0));
        v.setTarifaDiaria(req.getTarifaDiaria());
        v.setFechaSoat(req.getFechaSoat());
        v.setFechaRevisionTecnica(req.getFechaRevisionTecnica());
        v.setObservaciones(req.getObservaciones());
    }
}
