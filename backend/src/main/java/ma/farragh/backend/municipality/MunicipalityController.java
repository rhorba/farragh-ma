package ma.farragh.backend.municipality;

import jakarta.validation.Valid;
import ma.farragh.backend.municipality.dto.SubscribeResultDto;
import ma.farragh.backend.municipality.dto.SubscribeZoneDto;
import ma.farragh.backend.municipality.dto.SubscriptionResponseDto;
import ma.farragh.backend.shared.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/municipality")
@PreAuthorize("hasRole('MUNICIPALITY')")
public class MunicipalityController {

    private final MunicipalityService municipalityService;

    public MunicipalityController(MunicipalityService municipalityService) {
        this.municipalityService = municipalityService;
    }

    @PostMapping("/subscriptions")
    public SubscribeResultDto subscribe(@Valid @RequestBody SubscribeZoneDto dto) {
        return municipalityService.subscribe(CurrentUser.id(), dto);
    }

    @GetMapping("/subscriptions")
    public List<SubscriptionResponseDto> listMySubscriptions() {
        return municipalityService.listMySubscriptions(CurrentUser.id());
    }
}
