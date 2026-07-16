package ma.farragh.backend.municipality.dto;

/**
 * When {@code overlapWarning} is true and the caller did not set {@code confirmOverlap} on the
 * request, nothing is persisted and {@code subscription} is null - the frontend shows the warning
 * and lets the user resubmit with confirmOverlap=true (UX Flow 3 error path).
 */
public record SubscribeResultDto(
        boolean overlapWarning,
        SubscriptionResponseDto subscription
) {
}
