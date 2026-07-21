package ma.farragh.backend.admin;

import ma.farragh.backend.admin.dto.AdminActionLogResponseDto;
import ma.farragh.backend.admin.dto.AdminUserResponseDto;
import ma.farragh.backend.admin.dto.RequestsAnalyticsSummaryDto;
import ma.farragh.backend.admin.dto.RequestsTimeSeriesPointDto;
import ma.farragh.backend.auth.Role;
import ma.farragh.backend.requests.RequestStatus;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import ma.farragh.backend.shared.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.searchRequests(status, createdFrom, createdTo, pageable(page, size));
    }

    @GetMapping("/analytics/requests/summary")
    public RequestsAnalyticsSummaryDto requestsAnalyticsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return adminService.getRequestsSummary(from, to);
    }

    @GetMapping("/analytics/requests/timeseries")
    public List<RequestsTimeSeriesPointDto> requestsAnalyticsTimeSeries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "DAY") AnalyticsGranularity granularity) {
        return adminService.getRequestsTimeSeries(from, to, granularity);
    }

    @GetMapping("/analytics/requests/export")
    public ResponseEntity<String> requestsAnalyticsExport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "DAY") AnalyticsGranularity granularity) {
        String csv = adminService.exportRequestsTimeSeriesCsv(from, to, granularity);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"requests-analytics.csv\"")
                .body(csv);
    }

    @PostMapping("/users/{id}/deactivate")
    public AdminUserResponseDto deactivateUser(@PathVariable UUID id) {
        return adminService.deactivateUser(CurrentUser.id(), id);
    }

    @PostMapping("/users/{id}/reactivate")
    public AdminUserResponseDto reactivateUser(@PathVariable UUID id) {
        return adminService.reactivateUser(CurrentUser.id(), id);
    }

    @GetMapping("/action-log")
    public Page<AdminActionLogResponseDto> actionLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listActionLog(pageable(page, size));
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(page, size);
    }
}
