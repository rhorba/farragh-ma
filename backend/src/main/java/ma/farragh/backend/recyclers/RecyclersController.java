package ma.farragh.backend.recyclers;

import jakarta.validation.Valid;
import ma.farragh.backend.recyclers.dto.DeclareMaterialsDto;
import ma.farragh.backend.recyclers.dto.DeclareZoneDto;
import ma.farragh.backend.recyclers.dto.MaterialsResponseDto;
import ma.farragh.backend.recyclers.dto.ZoneResponseDto;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import ma.farragh.backend.shared.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recyclers")
@PreAuthorize("hasRole('RECYCLER')")
public class RecyclersController {

    private final RecyclersService recyclersService;

    public RecyclersController(RecyclersService recyclersService) {
        this.recyclersService = recyclersService;
    }

    @PostMapping("/zone")
    public ZoneResponseDto declareZone(@Valid @RequestBody DeclareZoneDto dto) {
        return recyclersService.declareZone(CurrentUser.id(), dto);
    }

    @GetMapping("/zone")
    public ZoneResponseDto getMyZone() {
        return recyclersService.getMyZone(CurrentUser.id());
    }

    @PutMapping("/materials")
    public MaterialsResponseDto declareMaterials(@Valid @RequestBody DeclareMaterialsDto dto) {
        return recyclersService.declareMaterials(CurrentUser.id(), dto);
    }

    @GetMapping("/materials")
    public MaterialsResponseDto getMyMaterials() {
        return recyclersService.getMyMaterials(CurrentUser.id());
    }

    @GetMapping("/feed")
    public List<RequestResponseDto> getMatchedFeed() {
        return recyclersService.getMatchedFeed(CurrentUser.id());
    }

    @PostMapping("/feed/{id}/accept")
    public RequestResponseDto accept(@PathVariable UUID id) {
        return recyclersService.accept(CurrentUser.id(), id);
    }
}
