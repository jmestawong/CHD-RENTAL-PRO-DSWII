package com.chd.rental.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// Se usa cuando no se cumple una regla del negocio (datos duplicados, cruce de fechas, etc.).
// Por defecto devuelve 409 Conflict.
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
