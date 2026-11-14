package com.nttdata.bank_ms_credit.application.usecase;

import com.nttdata.bank_ms_credit.application.dto.CreateCreditCommand;
import com.nttdata.bank_ms_credit.application.dto.CreditCreationContext;
import com.nttdata.bank_ms_credit.application.dto.CustomerDocument;
import com.nttdata.bank_ms_credit.application.dto.PayCreditCommand;
import com.nttdata.bank_ms_credit.application.port.input.CreditInputPort;
import com.nttdata.bank_ms_credit.application.port.input.CreditPaymentInputPort;
import com.nttdata.bank_ms_credit.application.port.input.CreditQueryInputPort;
import com.nttdata.bank_ms_credit.application.port.output.CreditMovementRepositoryPort;
import com.nttdata.bank_ms_credit.application.port.output.CreditNumberGenerator;
import com.nttdata.bank_ms_credit.application.port.output.CreditOperationCodeGenerator;
import com.nttdata.bank_ms_credit.application.port.output.CreditRepositoryPort;
import com.nttdata.bank_ms_credit.application.port.output.CreditStrategy;
import com.nttdata.bank_ms_credit.application.port.output.CreditTransactionPort;
import com.nttdata.bank_ms_credit.application.port.output.CustomerValidationPort;
import com.nttdata.bank_ms_credit.domain.exception.BusinessRuleException;
import com.nttdata.bank_ms_credit.domain.exception.ConflictException;
import com.nttdata.bank_ms_credit.domain.exception.ResourceNotFoundException;
import com.nttdata.bank_ms_credit.domain.model.Credit;
import com.nttdata.bank_ms_credit.domain.model.CreditDisbursement;
import com.nttdata.bank_ms_credit.domain.model.CreditMovement;
import com.nttdata.bank_ms_credit.domain.model.CreditMovementType;
import com.nttdata.bank_ms_credit.domain.model.CreditPayment;
import com.nttdata.bank_ms_credit.domain.model.CreditStatus;
import com.nttdata.bank_ms_credit.domain.model.CreditType;
import com.nttdata.bank_ms_credit.domain.model.Money;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class CreditUseCase implements CreditInputPort, CreditPaymentInputPort, CreditQueryInputPort {

    private final CreditRepositoryPort creditRepository;
    private final CreditMovementRepositoryPort movementRepository;
    private final CustomerValidationPort customerValidation;
    private final CreditNumberGenerator creditNumberGenerator;
    private final CreditOperationCodeGenerator operationCodeGenerator;
    private final CreditTransactionPort transaction;
    private final List<CreditStrategy> strategies;
    private final Clock clock;

    public CreditUseCase(CreditRepositoryPort creditRepository,
                         CreditMovementRepositoryPort movementRepository,
                         CustomerValidationPort customerValidation,
                         CreditNumberGenerator creditNumberGenerator,
                         CreditOperationCodeGenerator operationCodeGenerator,
                         CreditTransactionPort transaction,
                         List<CreditStrategy> strategies,
                         Clock clock) {
        this.creditRepository = creditRepository;
        this.movementRepository = movementRepository;
        this.customerValidation = customerValidation;
        this.creditNumberGenerator = creditNumberGenerator;
        this.operationCodeGenerator = operationCodeGenerator;
        this.transaction = transaction;
        this.strategies = List.copyOf(strategies);
        this.clock = clock;
    }

    @Override
    public Single<Credit> create(CreateCreditCommand command) {
        return Single.fromPublisher(createReactive(command));
    }

    private Mono<Credit> createReactive(CreateCreditCommand command) {
        return Mono.defer(() -> {
            validateCreateCommand(command);
            return Mono.from(customerValidation.validate(command.customerDocument()).toFlowable())
                    .flatMap(eligibility -> {
                        if (!eligibility.eligible()) {
                            return Mono.error(new BusinessRuleException("Customer is not eligible for credit"));
                        }
                        if (!command.creditType().name().equals(eligibility.customerType())) {
                            return Mono.error(new BusinessRuleException(
                                    "Credit type must match the customer type"));
                        }
                        return createForCustomer(command, eligibility.customerNumber());
                    });
        });
    }

    private Mono<Credit> createForCustomer(CreateCreditCommand command, Long customerNumber) {
        CreditCreationContext context = new CreditCreationContext(customerNumber, command.creditType());
        CreditStrategy strategy = strategies.stream()
                .filter(candidate -> candidate.supports(command.creditType()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Unsupported credit type"));
        strategy.validateCreation(context);
        Mono<Boolean> eligibility = command.creditType() == CreditType.PERSONAL
                ? creditRepository.existsActivePersonalCredit(customerNumber)
                : Mono.just(false);
        return eligibility.defaultIfEmpty(false).flatMap(hasActive -> {
            if (hasActive) {
                return Mono.error(new ConflictException(
                        "Customer already has an active personal credit"));
            }
            Instant now = clock.instant();
            LocalDate today = LocalDate.ofInstant(now, clock.getZone());
            return Mono.from(creditNumberGenerator.generate(command.creditType(), today).toFlowable())
                    .flatMap(creditNumber -> Mono.from(operationCodeGenerator
                                    .generate(CreditMovementType.DISBURSEMENT, now).toFlowable())
                            .flatMap(operationCode -> {
                                Credit credit = new Credit(null, creditNumber, customerNumber,
                                        command.creditType(), CreditStatus.ACTIVE, command.amount(),
                                        command.amount(), now, now, null);
                                CreditDisbursement disbursement = new CreditDisbursement(null,
                                        operationCode, "DISBURSEMENT-" + creditNumber, null,
                                        command.amount(), command.amount(), now);
                                return transaction.execute(creditRepository.save(credit)
                                        .flatMap(savedCredit -> movementRepository.save(
                                                        new CreditDisbursement(disbursement.id(),
                                                                disbursement.operationCode(),
                                                                disbursement.idempotencyKey(),
                                                                savedCredit.id(),
                                                                disbursement.amount(),
                                                                disbursement.debtAfter(),
                                                                disbursement.occurredAt()))
                                                .thenReturn(savedCredit)));
                            }));
        });
    }

    @Override
    public Single<Credit> updateStatus(String creditNumber, String status) {
        return Single.fromPublisher(Mono.defer(() -> {
            if (!"ACTIVE".equals(status) && !"CLOSED".equals(status)) {
                return Mono.error(new BusinessRuleException("Unsupported credit status"));
            }
            return getCredit(creditNumber).flatMap(credit -> {
                if ("CLOSED".equals(status)) {
                    return creditRepository.update(credit.close(clock.instant()));
                }
                if (credit.status() == CreditStatus.ACTIVE) {
                    return Mono.just(credit);
                }
                return Mono.error(new BusinessRuleException("Credit status transition is not allowed"));
            });
        }));
    }

    @Override
    public Completable close(String creditNumber) {
        return Completable.fromPublisher(closeReactive(creditNumber));
    }

    private Mono<Void> closeReactive(String creditNumber) {
        return getCredit(creditNumber)
                .flatMap(credit -> creditRepository.update(credit.close(clock.instant())))
                .then();
    }

    @Override
    public Maybe<Credit> findByCreditNumber(String creditNumber) {
        return Maybe.fromPublisher(creditRepository.findByCreditNumber(creditNumber));
    }

    @Override
    public Flowable<Credit> findAll() {
        return Flowable.fromPublisher(creditRepository.findAll());
    }

    @Override
    public Flowable<Credit> findByCustomerNumber(Long customerNumber) {
        return Flowable.fromPublisher(creditRepository.findByCustomerNumber(customerNumber));
    }

    @Override
    public Single<Long> findCustomerNumberByDocument(String documentType, String documentNumber) {
        validateCustomerDocument(documentType, documentNumber);
        return customerValidation.findCustomerNumber(new CustomerDocument(documentType, documentNumber));
    }

    @Override
    public Single<Money> getOutstandingDebt(String creditNumber) {
        return Single.fromPublisher(getCredit(creditNumber).map(Credit::outstandingDebt));
    }

    @Override
    public Flowable<CreditMovement> getMovements(String creditNumber) {
        return Flowable.fromPublisher(getCredit(creditNumber)
                .flatMapMany(credit -> movementRepository.findByCreditId(credit.id())));
    }

    @Override
    public Single<CreditMovement> pay(PayCreditCommand command) {
        return Single.fromPublisher(payReactive(command));
    }

    private Mono<CreditMovement> payReactive(PayCreditCommand command) {
        return Mono.defer(() -> {
            validatePaymentCommand(command);
            return getCredit(command.creditNumber()).flatMap(credit ->
                    movementRepository.findByIdempotencyKey(command.idempotencyKey())
                            .flatMap(existing -> existingPayment(existing, credit, command))
                            .switchIfEmpty(Mono.defer(() -> performPayment(credit, command))));
        });
    }

    private Mono<CreditMovement> existingPayment(CreditMovement existing, Credit credit,
                                                  PayCreditCommand command) {
        if (existing.movementType() != CreditMovementType.PAYMENT
                || !existing.creditId().equals(credit.id())
                || existing.amount().amount().compareTo(command.amount().amount()) != 0
                || !(existing instanceof CreditPayment payment)
                || payment.paymentSource() != command.paymentSource()) {
            return Mono.error(new ConflictException(
                    "Idempotency key was already used for another operation"));
        }
        return Mono.just(existing);
    }

    private Mono<CreditMovement> performPayment(Credit credit, PayCreditCommand command) {
        Instant now = clock.instant();
        return Mono.from(operationCodeGenerator.generate(CreditMovementType.PAYMENT, now).toFlowable())
                .flatMap(operationCode -> {
                    Credit.PaymentResult result;
                    try {
                        result = credit.pay(command.amount(), command.paymentSource(), operationCode,
                                command.idempotencyKey(), now);
                    } catch (RuntimeException exception) {
                        return Mono.error(exception);
                    }
                    Mono<CreditMovement> transactionWork = transaction.execute(
                            creditRepository.update(result.credit())
                                    .then(movementRepository.save(result.movement())));
                    return transactionWork.onErrorResume(failure ->
                            movementRepository.findByIdempotencyKey(command.idempotencyKey())
                                    .flatMap(existing -> existingPayment(existing, credit, command))
                                    .switchIfEmpty(Mono.error(failure)));
                });
    }

    private Mono<Credit> getCredit(String creditNumber) {
        return creditRepository.findByCreditNumber(creditNumber)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Credit not found: " + creditNumber)));
    }

    private void validateCreateCommand(CreateCreditCommand command) {
        if (command == null || command.customerDocument() == null || command.creditType() == null
                || command.amount() == null || !command.amount().isPositive()) {
            throw new BusinessRuleException(
                    "Credit request fields are required and amount must be positive");
        }
        validateCustomerDocument(command.customerDocument().documentType(),
                command.customerDocument().documentNumber());
    }

    private void validatePaymentCommand(PayCreditCommand command) {
        if (command == null || command.amount() == null || command.paymentSource() == null
                || command.idempotencyKey() == null || command.idempotencyKey().length() < 8
                || command.idempotencyKey().length() > 100) {
            throw new BusinessRuleException("Invalid payment request");
        }
    }

    private void validateCustomerDocument(String documentType, String documentNumber) {
        if (documentType == null || !documentType.matches("DNI|RUC|CE|PASSPORT")
                || documentNumber == null || documentNumber.length() > 30
                || !documentNumber.matches("[0-9]+")) {
            throw new BusinessRuleException("Customer document type or number is invalid");
        }
    }
}
