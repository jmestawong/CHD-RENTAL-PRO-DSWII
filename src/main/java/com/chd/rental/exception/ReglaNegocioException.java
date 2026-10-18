package com.chd.rental.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Violacion de una regla de negocio (duplicados, cruce de fechas, estados no permitidos).
 * Por defecto responde HTTP 409 Conflict.
 */
@Getter
public class ReglaNegocioException extends RuntimeException {

    private final HttpStatus status;

    public ReglaNegocioException(String mensaje) {
        this(mensaje, HttpStatus.CONFLICT);
    }

    public ReglaNegocioException(String mensaje, HttpStatus status) {
        super(mensaje);
        this.status = status;
    }
}
