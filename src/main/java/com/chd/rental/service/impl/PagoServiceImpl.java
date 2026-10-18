package com.chd.rental.service.impl;

import com.chd.rental.dto.PagoRequest;
import com.chd.rental.dto.PagoResponse;
import com.chd.rental.entity.Contrato;
import com.chd.rental.entity.Pago;
import com.chd.rental.entity.enums.EstadoContrato;
import com.chd.rental.entity.enums.EstadoPago;
import com.chd.rental.exception.RecursoNoEncontradoException;
import com.chd.rental.exception.ReglaNegocioException;
import com.chd.rental.repository.ContratoRepository;
import com.chd.rental.repository.PagoRepository;
import com.chd.rental.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final ContratoRepository contratoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listar(Long idContrato) {
        List<Pago> pagos = idContrato != null
                ? pagoRepository.findByContratoIdOrderByFechaPagoDesc(idContrato)
                : pagoRepository.findAllByOrderByFechaPagoDesc();
        return pagos.stream().map(PagoResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtener(Long id) {
        return PagoResponse.from(buscar(id));
    }

    @Override
    @Transactional
    public PagoResponse crear(PagoRequest req) {
        if (pagoRepository.existsByNumeroComprobante(req.getNumeroComprobante())) {
            throw new ReglaNegocioException("Ya existe un pago con el comprobante " + req.getNumeroComprobante());
        }
        Pago pago = new Pago();
        pago.setNumeroComprobante(req.getNumeroComprobante());
        copiar(req, pago);
        return PagoResponse.from(pagoRepository.save(pago));
    }

    @Override
    @Transactional
    public PagoResponse actualizar(Long id, PagoRequest req) {
        Pago pago = buscar(id);
        if (!pago.getNumeroComprobante().equals(req.getNumeroComprobante())
                && pagoRepository.existsByNumeroComprobante(req.getNumeroComprobante())) {
            throw new ReglaNegocioException("Ya existe un pago con el comprobante " + req.getNumeroComprobante());
        }
        if (pago.getEstado() == EstadoPago.ANULADO) {
            throw new ReglaNegocioException("El pago esta ANULADO y no puede modificarse");
        }
        pago.setNumeroComprobante(req.getNumeroComprobante());
        copiar(req, pago);
        return PagoResponse.from(pagoRepository.save(pago));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        pagoRepository.delete(buscar(id));
    }

    private void copiar(PagoRequest req, Pago pago) {
        Contrato contrato = contratoRepository.findWithRelacionesById(req.getIdContrato())
                .orElseThrow(() -> new RecursoNoEncontradoException("Contrato", req.getIdContrato()));
        if (contrato.getEstado() == EstadoContrato.CANCELADO) {
            throw new ReglaNegocioException("No se pueden registrar pagos en un contrato CANCELADO");
        }
        if (req.getMonto().compareTo(contrato.getTotal().add(contrato.getGarantia())) > 0) {
            throw new ReglaNegocioException("El monto supera el total del contrato mas la garantia ("
                    + contrato.getTotal().add(contrato.getGarantia()) + ")");
        }
        pago.setContrato(contrato);
        pago.setTipoComprobante(req.getTipoComprobante());
        pago.setFechaPago(req.getFechaPago());
        pago.setMonto(req.getMonto());
        pago.setMetodoPago(req.getMetodoPago());
        pago.setBanco(req.getBanco());
        pago.setNumeroOperacion(req.getNumeroOperacion());
        pago.setEstado(req.getEstado() != null ? req.getEstado() : EstadoPago.CONFIRMADO);
        pago.setObservaciones(req.getObservaciones());
    }

    private Pago buscar(Long id) {
        return pagoRepository.findWithContratoById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago", id));
    }
}
