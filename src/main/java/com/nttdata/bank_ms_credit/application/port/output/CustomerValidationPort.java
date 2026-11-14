package com.nttdata.bank_ms_credit.application.port.output;

import com.nttdata.bank_ms_credit.application.dto.CustomerDocument;
import com.nttdata.bank_ms_credit.application.dto.CustomerEligibility;
import io.reactivex.rxjava3.core.Single;

public interface CustomerValidationPort {

    Single<CustomerEligibility> validate(CustomerDocument document);

    Single<Long> findCustomerNumber(CustomerDocument document);
}
