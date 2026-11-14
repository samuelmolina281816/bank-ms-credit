package com.nttdata.bank_ms_credit.framework.adapter.output.repository;

import com.nttdata.bank_ms_credit.application.port.output.CreditOperationCodeGenerator;
import com.nttdata.bank_ms_credit.domain.model.CreditMovementType;
import io.reactivex.rxjava3.core.Single;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class CreditOperationCodeGeneratorAdapter implements CreditOperationCodeGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT).withZone(ZoneOffset.UTC);
    private final DatabaseClient databaseClient;

    public CreditOperationCodeGeneratorAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Single<String> generate(CreditMovementType type, Instant occurredAt) {
        return Single.fromPublisher(databaseClient.sql("""
                        INSERT INTO credit_code_counters(counter_date, counter_key, last_value)
                        VALUES (:counterDate, :counterKey, 1)
                        ON CONFLICT (counter_date, counter_key)
                        DO UPDATE SET last_value = credit_code_counters.last_value + 1
                        RETURNING last_value
                        """)
                .bind("counterDate", occurredAt.atZone(ZoneOffset.UTC).toLocalDate())
                .bind("counterKey", "MOVEMENT-" + type.name())
                .map((row, metadata) -> row.get("last_value", Integer.class))
                .one())
                .map(sequence -> String.format(Locale.ROOT, "CRE-%s-%s-%06d",
                        type.code(), DATE_TIME_FORMAT.format(occurredAt), sequence));
    }
}
