package com.nttdata.bank_ms_credit.domain.exception;

public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
