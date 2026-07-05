package ma.farragh.backend.requests;

import jakarta.validation.Valid;
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

    public RequestsController(RequestsService requestsService) {
        this.requestsService = requestsService;
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
        return requestsService.getMine(CurrentUser.id(), id);
    }

    @PostMapping("/{id}/cancel")
    public RequestResponseDto cancel(@PathVariable UUID id) {
        return requestsService.cancel(CurrentUser.id(), id);
    }
}
