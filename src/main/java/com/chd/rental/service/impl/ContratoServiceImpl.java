package com.chd.rental.service.impl;

import com.chd.rental.dto.ContratoRequest;
import com.chd.rental.dto.ContratoResponse;
import com.chd.rental.entity.Cliente;
import com.chd.rental.entity.Contrato;
import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.EstadoContrato;
import com.chd.rental.entity.enums.EstadoRegistro;
import com.chd.rental.entity.enums.EstadoVehiculo;
import com.chd.rental.exception.RecursoNoEncontradoException;
import com.chd.rental.exception.ReglaNegocioException;
import com.chd.rental.repository.ClienteRepository;
import com.chd.rental.repository.ContratoRepository;
import com.chd.rental.repository.PagoRepository;
import com.chd.rental.repository.VehiculoRepository;
import com.chd.rental.service.AuthService;
import com.chd.rental.service.CalculadoraContrato;
import com.chd.rental.service.ContratoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// Logica principal del negocio. Cada metodo es transaccional para que el contrato
// y el estado del vehiculo se guarden juntos; si algo falla no se guarda nada.
@Service
@RequiredArgsConstructor
public class ContratoServiceImpl implements ContratoService {

    private static final Set<EstadoContrato> ESTADOS_CERRADOS =
            EnumSet.of(EstadoContrato.FINALIZADO, EstadoContrato.CANCELADO);

    private final ContratoRepository contratoRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PagoRepository pagoRepository;
    private final AuthService authService;

    @Override
    @Transactional(readOnly = true)
    public List<ContratoResponse> listar(EstadoContrato estado) {
        List<Contrato> contratos = estado != null
                ? contratoRepository.findByEstadoOrderByFechaInicioDesc(estado)
                : contratoRepository.findAllByOrderByFechaCreacionDesc();
        return contratos.stream().map(ContratoResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResponse obtener(Long id) {
        return ContratoResponse.from(buscar(id));
    }

    // Al crear se valida el cliente, el vehiculo y que no haya cruce de fechas.
    // Los importes los calcula el servidor con la tarifa del vehiculo.
    @Override
    @Transactional
    public ContratoResponse crear(ContratoRequest req) {
        EstadoContrato estado = req.getEstado() != null ? req.getEstado() : EstadoContrato.ACTIVO;
        if (ESTADOS_CERRADOS.contains(estado)) {
            throw new ReglaNegocioException("Un contrato nuevo solo puede registrarse como PENDIENTE o ACTIVO",
                    HttpStatus.BAD_REQUEST);
        }
        Cliente cliente = buscarClienteActivo(req.getIdCliente());
        Vehiculo vehiculo = buscarVehiculoElegible(req.getIdVehiculo());
        validarFechasYCruce(req, vehiculo.getId(), null);

        Contrato c = new Contrato();
        c.setCliente(cliente);
        c.setVehiculo(vehiculo);
        c.setUsuario(authService.usuarioActual());
        c.setEstado(estado);
        c.setKilometrajeInicial(req.getKilometrajeInicial() != null
                ? req.getKilometrajeInicial() : vehiculo.getKilometraje());
        copiarYCalcular(req, c, vehiculo.getTarifaDiaria());

        c = contratoRepository.save(c);
        // el numero se arma con el id para que no se repita (CTR-000001, CTR-000002...)
        c.setNumeroContrato(String.format("CTR-%06d", c.getId()));

        if (estado == EstadoContrato.ACTIVO) {
            vehiculo.setEstado(EstadoVehiculo.ALQUILADO);
        }
        return ContratoResponse.from(c);
    }

    // Un contrato FINALIZADO o CANCELADO ya no se modifica. Al finalizar se pide el
    // kilometraje final y el vehiculo vuelve a quedar disponible.
    @Override
    @Transactional
    public ContratoResponse actualizar(Long id, ContratoRequest req) {
        Contrato c = buscar(id);
        if (ESTADOS_CERRADOS.contains(c.getEstado())) {
            throw new ReglaNegocioException("El contrato " + c.getNumeroContrato() + " esta "
                    + c.getEstado() + " y ya no puede modificarse");
        }
        EstadoContrato nuevoEstado = req.getEstado() != null ? req.getEstado() : c.getEstado();
        Vehiculo vehiculoAnterior = c.getVehiculo();

        Cliente cliente = c.getCliente().getId().equals(req.getIdCliente())
                ? c.getCliente() : buscarClienteActivo(req.getIdCliente());
        Vehiculo vehiculo = vehiculoAnterior.getId().equals(req.getIdVehiculo())
                ? vehiculoAnterior : buscarVehiculoElegible(req.getIdVehiculo());

        if (!ESTADOS_CERRADOS.contains(nuevoEstado)) {
            validarFechasYCruce(req, vehiculo.getId(), c.getId());
        }

        c.setCliente(cliente);
        c.setVehiculo(vehiculo);
        c.setEstado(nuevoEstado);
        if (req.getKilometrajeInicial() != null) {
            c.setKilometrajeInicial(req.getKilometrajeInicial());
        }
        // la tarifa pactada se conserva salvo que cambie el vehiculo
        BigDecimal tarifa = vehiculo == vehiculoAnterior ? c.getTarifaDiaria() : vehiculo.getTarifaDiaria();
        copiarYCalcular(req, c, tarifa);

        if (nuevoEstado == EstadoContrato.FINALIZADO) {
            cerrarComoFinalizado(c, req);
        }

        contratoRepository.saveAndFlush(c);
        sincronizarVehiculo(vehiculo, nuevoEstado);
        if (vehiculo != vehiculoAnterior) {
            liberarSiCorresponde(vehiculoAnterior);
        }
        return ContratoResponse.from(c);
    }

    // Si el contrato ya tiene pagos no se elimina, en ese caso se debe cancelar
    @Override
    @Transactional
    public void eliminar(Long id) {
        Contrato c = buscar(id);
        if (pagoRepository.existsByContratoId(id)) {
            throw new ReglaNegocioException(
                    "El contrato tiene pagos registrados; cambie su estado a CANCELADO en lugar de eliminarlo");
        }
        Vehiculo vehiculo = c.getVehiculo();
        contratoRepository.delete(c);
        contratoRepository.flush();
        liberarSiCorresponde(vehiculo);
    }

    private void validarFechasYCruce(ContratoRequest req, Long idVehiculo, Long excluirId) {
        if (req.getFechaFin().isBefore(req.getFechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la fecha de inicio",
                    HttpStatus.BAD_REQUEST);
        }
        long cruces = contratoRepository.contarCruces(idVehiculo, req.getFechaInicio(), req.getFechaFin(), excluirId);
        if (cruces > 0) {
            throw new ReglaNegocioException("El vehiculo ya tiene un contrato vigente que se cruza con el periodo "
                    + req.getFechaInicio() + " - " + req.getFechaFin());
        }
    }

    private void cerrarComoFinalizado(Contrato c, ContratoRequest req) {
        if (req.getKilometrajeFinal() == null) {
            throw new ReglaNegocioException("Para finalizar el contrato debe indicar el kilometraje final",
                    HttpStatus.BAD_REQUEST);
        }
        if (c.getKilometrajeInicial() != null && req.getKilometrajeFinal() < c.getKilometrajeInicial()) {
            throw new ReglaNegocioException("El kilometraje final no puede ser menor al inicial",
                    HttpStatus.BAD_REQUEST);
        }
        Vehiculo v = c.getVehiculo();
        if (req.getKilometrajeFinal() > v.getKilometraje()) {
            v.setKilometraje(req.getKilometrajeFinal());
        }
    }

    private void sincronizarVehiculo(Vehiculo vehiculo, EstadoContrato estadoContrato) {
        if (estadoContrato == EstadoContrato.ACTIVO) {
            vehiculo.setEstado(EstadoVehiculo.ALQUILADO);
        } else {
            liberarSiCorresponde(vehiculo);
        }
    }

    // El vehiculo vuelve a DISPONIBLE solo si ya no tiene otro contrato activo
    private void liberarSiCorresponde(Vehiculo vehiculo) {
        if (vehiculo.getEstado() == EstadoVehiculo.ALQUILADO
                && !contratoRepository.existsByVehiculoIdAndEstado(vehiculo.getId(), EstadoContrato.ACTIVO)) {
            vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        }
    }

    private void copiarYCalcular(ContratoRequest req, Contrato c, BigDecimal tarifa) {
        int dias = CalculadoraContrato.calcularDias(req.getFechaInicio(), req.getFechaFin());
        CalculadoraContrato.Importes imp = CalculadoraContrato.calcular(tarifa, dias);
        c.setFechaInicio(req.getFechaInicio());
        c.setFechaFin(req.getFechaFin());
        c.setDiasAlquiler(dias);
        c.setTarifaDiaria(tarifa);
        c.setSubtotal(imp.subtotal());
        c.setIgv(imp.igv());
        c.setTotal(imp.total());
        c.setGarantia(req.getGarantia() != null ? req.getGarantia() : BigDecimal.ZERO);
        c.setKilometrajeFinal(req.getKilometrajeFinal());
        c.setCombustibleInicial(req.getCombustibleInicial());
        c.setCombustibleFinal(req.getCombustibleFinal());
        c.setLugarEntrega(req.getLugarEntrega());
        c.setLugarDevolucion(req.getLugarDevolucion());
        c.setObservaciones(req.getObservaciones());
    }

    private Cliente buscarClienteActivo(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
        if (cliente.getEstado() != EstadoRegistro.ACTIVO) {
            throw new ReglaNegocioException("El cliente " + cliente.getRazonSocial() + " esta INACTIVO");
        }
        return cliente;
    }

    private Vehiculo buscarVehiculoElegible(Long id) {
        Vehiculo v = vehiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehiculo", id));
        if (v.getEstado() == EstadoVehiculo.MANTENIMIENTO || v.getEstado() == EstadoVehiculo.INACTIVO) {
            throw new ReglaNegocioException("El vehiculo " + v.getPlaca() + " no esta disponible (estado "
                    + v.getEstado() + ")");
        }
        return v;
    }

    private Contrato buscar(Long id) {
        return contratoRepository.findWithRelacionesById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Contrato", id));
    }
}
