package com.nttdata.bank_ms_credit.domain.exception;

public class CustomerServiceUnavailableException extends RuntimeException {

    public CustomerServiceUnavailableException(String message) {
        super(message);
    }
}
