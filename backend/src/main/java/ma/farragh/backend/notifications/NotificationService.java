package ma.farragh.backend.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailSender emailSender;

    public NotificationService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    public void notifyRequestAccepted(String requesterEmail, UUID requestId) {
        send(requesterEmail, "Votre demande a été acceptée",
                "Votre demande de collecte (" + requestId + ") a été acceptée par un recycleur.");
    }

    @Async
    public void notifyRequestCompleted(String requesterEmail, UUID requestId) {
        send(requesterEmail, "Votre collecte est terminée",
                "Votre demande de collecte (" + requestId + ") a été marquée comme terminée. Merci d'utiliser Farragh !");
    }

    private void send(String to, String subject, String body) {
        try {
            emailSender.send(to, subject, body);
        } catch (RuntimeException e) {
            // Async, no queue (System Design decision) - a failed notification must never affect
            // the status transition that triggered it, so we log and drop rather than retry/rethrow.
            log.warn("Failed to send notification email to {}: {}", to, e.getMessage());
        }
    }
}
