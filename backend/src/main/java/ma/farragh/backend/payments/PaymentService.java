package ma.farragh.backend.payments;

import ma.farragh.backend.payments.dto.PaymentResponseDto;
import ma.farragh.backend.requests.RequestStatus;
import ma.farragh.backend.requests.RequestsService;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import ma.farragh.backend.shared.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    // No pricing model exists yet (not in PRD/database doc) - Story 5.1 only requires a payment
    // *record* to exist, so this is a flat placeholder amount pending a real pricing engine.
    private static final int MOCK_AMOUNT_CENTS = 5000;
    private static final String CURRENCY = "MAD";
    private static final String PROVIDER = "CMI";

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final RequestsService requestsService;

    public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway, RequestsService requestsService) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.requestsService = requestsService;
    }

    @Transactional
    public PaymentResponseDto pay(UUID requesterId, UUID requestId) {
        RequestResponseDto request = requestsService.getMine(requesterId, requestId);
        if (request.status() != RequestStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.CONFLICT, "REQUEST_NOT_COMPLETED",
                    "Payment is only allowed once the request is completed.");
        }
        if (paymentRepository.findByPickupRequestId(requestId).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAYMENT_ALREADY_EXISTS",
                    "This request has already been paid.");
        }

        PaymentGateway.PaymentResult result = paymentGateway.charge(requestId, MOCK_AMOUNT_CENTS, CURRENCY);
        Payment payment = new Payment(requestId, MOCK_AMOUNT_CENTS, CURRENCY, PROVIDER, PaymentMode.MOCK,
                result.status(), result.providerRef());

        try {
            paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            // Concurrent double-submit: the DB's UNIQUE(pickup_request_id) constraint is the
            // real source of truth, same principle as Sprint 3's race-guarded accept().
            throw new BusinessException(HttpStatus.CONFLICT, "PAYMENT_ALREADY_EXISTS",
                    "This request has already been paid.");
        }

        return toDto(payment);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResponseDto> findByRequestId(UUID requestId) {
        return paymentRepository.findByPickupRequestId(requestId).map(PaymentService::toDto);
    }

    private static PaymentResponseDto toDto(Payment p) {
        return new PaymentResponseDto(p.getId(), p.getPickupRequestId(), p.getAmountCents(), p.getCurrency(),
                p.getProvider(), p.getMode(), p.getStatus(), p.getProviderRef(), p.getCreatedAt());
    }
}
