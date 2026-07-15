package ma.farragh.backend.admin;

import ma.farragh.backend.admin.dto.AdminUserResponseDto;
import ma.farragh.backend.auth.Role;
import ma.farragh.backend.requests.RequestStatus;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Page<AdminUserResponseDto> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.searchUsers(email, role, pageable(page, size));
    }

    @GetMapping("/requests")
    public Page<RequestResponseDto> searchRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.searchRequests(status, pageable(page, size));
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(page, size);
    }
}
