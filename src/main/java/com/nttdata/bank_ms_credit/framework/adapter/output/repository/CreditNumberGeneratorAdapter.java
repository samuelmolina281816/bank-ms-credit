package com.nttdata.bank_ms_credit.framework.adapter.output.repository;

import com.nttdata.bank_ms_credit.application.port.output.CreditNumberGenerator;
import com.nttdata.bank_ms_credit.domain.model.CreditType;
import io.reactivex.rxjava3.core.Single;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class CreditNumberGeneratorAdapter implements CreditNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private final DatabaseClient databaseClient;

    public CreditNumberGeneratorAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Single<String> generate(CreditType type, LocalDate date) {
        return Single.fromPublisher(databaseClient.sql("""
                        INSERT INTO credit_code_counters(counter_date, counter_key, last_value)
                        VALUES (:counterDate, :counterKey, 1)
                        ON CONFLICT (counter_date, counter_key)
                        DO UPDATE SET last_value = credit_code_counters.last_value + 1
                        RETURNING last_value
                        """)
                .bind("counterDate", date)
                .bind("counterKey", "CREDIT-" + type.name())
                .map((row, metadata) -> row.get("last_value", Integer.class))
                .one())
                .map(sequence -> {
                    return String.format(Locale.ROOT, "CRE-%s-%s-%06d",
                            type.code(), DATE_FORMAT.format(date), sequence);
                });
    }
}
