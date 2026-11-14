package com.nttdata.bank_ms_credit.application.dto;

public record CustomerEligibility(Long customerNumber, String customerType, boolean eligible) {
}
