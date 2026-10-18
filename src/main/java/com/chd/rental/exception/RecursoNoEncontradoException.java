package com.chd.rental.exception;

/** Se lanza cuando el recurso solicitado no existe (HTTP 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " con id " + id + " no existe");
    }
}
