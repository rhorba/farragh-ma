package ma.farragh.backend.notifications;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    @Test
    void notifyRequestAcceptedSendsAnEmailToTheRequester() {
        EmailSender emailSender = mock(EmailSender.class);
        NotificationService service = new NotificationService(emailSender);
        UUID requestId = UUID.randomUUID();

        service.notifyRequestAccepted("household@example.com", requestId);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("household@example.com"), subjectCaptor.capture(), bodyCaptor.capture());
        assertThat(subjectCaptor.getValue()).contains("acceptée");
        assertThat(bodyCaptor.getValue()).contains(requestId.toString());
    }

    @Test
    void notifyRequestCompletedSendsAnEmailToTheRequester() {
        EmailSender emailSender = mock(EmailSender.class);
        NotificationService service = new NotificationService(emailSender);
        UUID requestId = UUID.randomUUID();

        service.notifyRequestCompleted("household@example.com", requestId);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("household@example.com"), subjectCaptor.capture(), anyString());
        assertThat(subjectCaptor.getValue()).contains("terminée");
    }

    @Test
    void aFailedSendIsSwallowedAndDoesNotPropagate() {
        EmailSender emailSender = mock(EmailSender.class);
        doThrow(new RuntimeException("SMTP down")).when(emailSender).send(anyString(), anyString(), anyString());
        NotificationService service = new NotificationService(emailSender);

        service.notifyRequestAccepted("household@example.com", UUID.randomUUID());
        // No exception propagated - the transition that triggered this must not be affected.
    }
}
