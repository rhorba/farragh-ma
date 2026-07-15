package ma.farragh.backend.payments;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * CMI_MODE=mock (the only mode implemented this sprint - Story 5.1 Technical Notes,
 * ADR-2). Always succeeds; a real gateway implementing this same interface will have
 * genuine failure paths when CMI credentials are available.
 */
@Component
public class MockCmiGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(UUID pickupRequestId, int amountCents, String currency) {
        return new PaymentResult(PaymentStatus.SUCCEEDED, "MOCK-" + UUID.randomUUID());
    }
}
