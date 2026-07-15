package ma.farragh.backend.payments;

import java.util.UUID;

/**
 * ADR-2: payment call sites depend only on this interface. MockCmiGateway implements it now;
 * a real CmiGateway implements it later, selected via CMI_MODE - zero call-site changes at cutover.
 */
public interface PaymentGateway {
    PaymentResult charge(UUID pickupRequestId, int amountCents, String currency);

    record PaymentResult(PaymentStatus status, String providerRef) {
    }
}
