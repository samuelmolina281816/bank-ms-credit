package com.nttdata.bank_ms_credit.application.usecase;

import com.nttdata.bank_ms_credit.application.dto.CreateCreditCommand;
import com.nttdata.bank_ms_credit.application.dto.CustomerDocument;
import com.nttdata.bank_ms_credit.application.dto.CustomerEligibility;
import com.nttdata.bank_ms_credit.application.dto.PayCreditCommand;
import com.nttdata.bank_ms_credit.application.port.output.CreditMovementRepositoryPort;
import com.nttdata.bank_ms_credit.application.port.output.CreditNumberGenerator;
import com.nttdata.bank_ms_credit.application.port.output.CreditOperationCodeGenerator;
import com.nttdata.bank_ms_credit.application.port.output.CreditRepositoryPort;
import com.nttdata.bank_ms_credit.application.port.output.CreditStrategy;
import com.nttdata.bank_ms_credit.application.port.output.CreditTransactionPort;
import com.nttdata.bank_ms_credit.application.port.output.CustomerValidationPort;
import com.nttdata.bank_ms_credit.domain.exception.ConflictException;
import com.nttdata.bank_ms_credit.domain.exception.BusinessRuleException;
import com.nttdata.bank_ms_credit.domain.model.Credit;
import com.nttdata.bank_ms_credit.domain.model.CreditMovementType;
import com.nttdata.bank_ms_credit.domain.model.CreditPayment;
import com.nttdata.bank_ms_credit.domain.model.CreditStatus;
import com.nttdata.bank_ms_credit.domain.model.CreditType;
import com.nttdata.bank_ms_credit.domain.model.Money;
import com.nttdata.bank_ms_credit.domain.model.PaymentSource;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class CreditUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-02T18:00:00Z");
    private CreditRepositoryPort creditRepository;
    private CreditMovementRepositoryPort movementRepository;
    private CustomerValidationPort customerValidation;
    private CreditNumberGenerator creditNumberGenerator;
    private CreditOperationCodeGenerator operationCodeGenerator;
    private CreditTransactionPort transaction;
    private CreditUseCase useCase;

    @BeforeEach
    void setUp() {
        creditRepository = mock(CreditRepositoryPort.class);
        movementRepository = mock(CreditMovementRepositoryPort.class);
        customerValidation = mock(CustomerValidationPort.class);
        creditNumberGenerator = mock(CreditNumberGenerator.class);
        operationCodeGenerator = mock(CreditOperationCodeGenerator.class);
        transaction = new CreditTransactionPort() {
            @Override
            public <T> Mono<T> execute(Mono<T> work) {
                return work;
            }
        };
        List<CreditStrategy> strategies = List.of(
                new PersonalCreditStrategy(), new BusinessCreditStrategy());
        useCase = new CreditUseCase(creditRepository, movementRepository, customerValidation,
                creditNumberGenerator, operationCodeGenerator, transaction, strategies,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsCreditAndDisbursementInTransaction() {
        Money amount = money("250.00");
        Credit expected = new Credit(21L, "CRE-PER-20261102-000001", 10000001L,
                CreditType.PERSONAL, CreditStatus.ACTIVE, amount, amount, NOW, NOW, 0L);
        when(customerValidation.validate(any())).thenReturn(
                Single.just(new CustomerEligibility(10000001L, "PERSONAL", true)));
        when(creditRepository.existsActivePersonalCredit(10000001L)).thenReturn(Mono.just(false));
        when(creditNumberGenerator.generate(CreditType.PERSONAL, NOW.atZone(ZoneOffset.UTC).toLocalDate()))
                .thenReturn(Single.just(expected.creditNumber()));
        when(operationCodeGenerator.generate(CreditMovementType.DISBURSEMENT, NOW))
                .thenReturn(Single.just("CRE-DIS-20261102-180000-000001"));
        when(creditRepository.save(any())).thenReturn(Mono.just(expected));
        when(movementRepository.save(any())).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        Credit created = useCase.create(new CreateCreditCommand(
                new CustomerDocument("DNI", "12345678"), CreditType.PERSONAL, amount)).blockingGet();

        assertEquals(expected, created);
        verify(movementRepository).save(any());
    }

    @Test
    void rejectsSecondActivePersonalCreditBeforePersistence() {
        when(customerValidation.validate(any())).thenReturn(
                Single.just(new CustomerEligibility(10000001L, "PERSONAL", true)));
        when(creditRepository.existsActivePersonalCredit(10000001L)).thenReturn(Mono.just(true));

        assertThrows(ConflictException.class, () -> useCase.create(new CreateCreditCommand(
                new CustomerDocument("DNI", "12345678"), CreditType.PERSONAL, money("10.00")))
                .blockingGet());

        verify(creditRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void rejectsInactiveCustomer() {
        when(customerValidation.validate(any())).thenReturn(
                Single.just(new CustomerEligibility(10000001L, "PERSONAL", false)));

        assertThrows(BusinessRuleException.class, () -> useCase.create(new CreateCreditCommand(
                new CustomerDocument("DNI", "12345678"), CreditType.PERSONAL, money("10.00")))
                .blockingGet());
    }

    @Test
    void rejectsCreditTypeThatDoesNotMatchCustomerType() {
        when(customerValidation.validate(any())).thenReturn(
                Single.just(new CustomerEligibility(10000001L, "BUSINESS", true)));

        assertThrows(BusinessRuleException.class, () -> useCase.create(new CreateCreditCommand(
                new CustomerDocument("DNI", "12345678"), CreditType.PERSONAL, money("10.00")))
                .blockingGet());
    }

    @Test
    void paymentUpdatesDebtAndPersistsMovement() {
        Credit original = new Credit(21L, "CRE-PER-20261102-000001", 10000001L,
                CreditType.PERSONAL, CreditStatus.ACTIVE, money("100.00"), money("100.00"),
                NOW, NOW, 0L);
        when(creditRepository.findByCreditNumber(original.creditNumber())).thenReturn(Mono.just(original));
        when(movementRepository.findByIdempotencyKey("payment-key-0001")).thenReturn(Mono.empty());
        when(operationCodeGenerator.generate(CreditMovementType.PAYMENT, NOW))
                .thenReturn(Single.just("CRE-PAY-20261102-180000-000001"));
        when(creditRepository.update(any())).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));
        when(movementRepository.save(any())).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        var movement = useCase.pay(new PayCreditCommand(original.creditNumber(),
                "payment-key-0001", money("25.00"), PaymentSource.EXTERNAL)).blockingGet();

        assertEquals(new BigDecimal("75.00"), movement.debtAfter().amount());
        verify(creditRepository).update(any());
        verify(movementRepository).save(any());
    }

    @Test
    void repeatedPaymentReturnsPreviouslyPersistedMovement() {
        Credit credit = new Credit(21L, "CRE-PER-20261102-000001", 10000001L,
                CreditType.PERSONAL, CreditStatus.ACTIVE, money("100.00"), money("100.00"),
                NOW, NOW, 0L);
        CreditPayment previousPayment = new CreditPayment(101L,
                "CRE-PAY-20261102-180000-000001", "payment-key-0001", credit.id(),
                money("25.00"), money("75.00"), NOW, PaymentSource.EXTERNAL);
        when(creditRepository.findByCreditNumber(credit.creditNumber())).thenReturn(Mono.just(credit));
        when(movementRepository.findByIdempotencyKey("payment-key-0001"))
                .thenReturn(Mono.just(previousPayment));

        var result = useCase.pay(new PayCreditCommand(credit.creditNumber(),
                "payment-key-0001", money("25.00"), PaymentSource.EXTERNAL)).blockingGet();

        assertEquals(previousPayment.operationCode(), result.operationCode());
        verify(creditRepository, never()).update(any());
        verify(movementRepository, never()).save(any());
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "PEN");
    }
}
