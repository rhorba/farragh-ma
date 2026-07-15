package ma.farragh.backend.requests;

import jakarta.validation.Valid;
import ma.farragh.backend.payments.PaymentService;
import ma.farragh.backend.payments.dto.PaymentResponseDto;
import ma.farragh.backend.requests.dto.CreateRequestDto;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import ma.farragh.backend.shared.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
@PreAuthorize("hasRole('HOUSEHOLD_SME')")
public class RequestsController {

    private final RequestsService requestsService;
    private final PaymentService paymentService;

    public RequestsController(RequestsService requestsService, PaymentService paymentService) {
        this.requestsService = requestsService;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<RequestResponseDto> create(@Valid @RequestBody CreateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestsService.create(CurrentUser.id(), dto));
    }

    @GetMapping
    public List<RequestResponseDto> listMine() {
        return requestsService.listMine(CurrentUser.id());
    }

    @GetMapping("/{id}")
    public RequestResponseDto getMine(@PathVariable UUID id) {
        RequestResponseDto request = requestsService.getMine(CurrentUser.id(), id);
        String paymentStatus = paymentService.findByRequestId(id)
                .map(p -> p.status().name())
                .orElse(null);
        return new RequestResponseDto(request.id(), request.materialTypeCode(), request.quantityDesc(),
                request.addressText(), request.latitude(), request.longitude(), request.status(),
                request.photoUrl(), request.createdAt(), request.updatedAt(), paymentStatus);
    }

    @PostMapping("/{id}/cancel")
    public RequestResponseDto cancel(@PathVariable UUID id) {
        return requestsService.cancel(CurrentUser.id(), id);
    }

    @PostMapping("/{id}/payment")
    public PaymentResponseDto pay(@PathVariable UUID id) {
        return paymentService.pay(CurrentUser.id(), id);
    }
}
