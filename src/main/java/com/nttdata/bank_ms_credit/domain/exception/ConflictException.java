package com.nttdata.bank_ms_credit.domain.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
