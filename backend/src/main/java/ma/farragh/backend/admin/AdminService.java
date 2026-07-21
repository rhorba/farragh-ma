package ma.farragh.backend.admin;

import ma.farragh.backend.admin.dto.AdminActionLogResponseDto;
import ma.farragh.backend.admin.dto.AdminUserResponseDto;
import ma.farragh.backend.admin.dto.RequestsAnalyticsSummaryDto;
import ma.farragh.backend.admin.dto.RequestsTimeSeriesPointDto;
import ma.farragh.backend.auth.Role;
import ma.farragh.backend.auth.User;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.requests.PickupRequestRepository;
import ma.farragh.backend.requests.RequestStatus;
import ma.farragh.backend.requests.RequestsService;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import ma.farragh.backend.shared.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final AdminActionLogRepository adminActionLogRepository;

    public AdminService(UserRepository userRepository, PickupRequestRepository pickupRequestRepository,
                         AdminActionLogRepository adminActionLogRepository) {
        this.userRepository = userRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.adminActionLogRepository = adminActionLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> searchUsers(String email, Role role, Pageable pageable) {
        return userRepository.search(blankToNull(email), role, pageable).map(AdminService::toUserDto);
    }

    @Transactional(readOnly = true)
    public Page<RequestResponseDto> searchRequests(RequestStatus status, Instant createdFrom, Instant createdTo, Pageable pageable) {
        return pickupRequestRepository.search(status, createdFrom, createdTo, pageable).map(RequestsService::toDto);
    }

    private static final Duration DEFAULT_ANALYTICS_RANGE = Duration.ofDays(90);

    @Transactional(readOnly = true)
    public RequestsAnalyticsSummaryDto getRequestsSummary(Instant from, Instant to) {
        Instant[] range = normalizeRange(from, to);
        Map<RequestStatus, Long> counts = new EnumMap<>(RequestStatus.class);
        for (RequestStatus status : RequestStatus.values()) {
            counts.put(status, 0L);
        }
        long total = 0;
        for (PickupRequestRepository.StatusCountRow row : pickupRequestRepository.countByStatusInRange(range[0], range[1])) {
            counts.put(RequestStatus.valueOf(row.getStatus()), row.getCnt());
            total += row.getCnt();
        }
        return new RequestsAnalyticsSummaryDto(range[0], range[1], total, counts);
    }

    @Transactional(readOnly = true)
    public List<RequestsTimeSeriesPointDto> getRequestsTimeSeries(Instant from, Instant to, AnalyticsGranularity granularity) {
        Instant[] range = normalizeRange(from, to);
        String unit = granularity.sqlUnit();
        Map<Instant, Long> created = toBucketMap(pickupRequestRepository.countCreatedByBucket(range[0], range[1], unit));
        Map<Instant, Long> completed = toBucketMap(pickupRequestRepository.countCompletedByBucket(range[0], range[1], unit));

        Map<Instant, Long> allBuckets = new TreeMap<>(created);
        completed.forEach((bucket, count) -> allBuckets.putIfAbsent(bucket, 0L));

        return allBuckets.keySet().stream()
                .sorted()
                .map(bucket -> new RequestsTimeSeriesPointDto(bucket, created.getOrDefault(bucket, 0L), completed.getOrDefault(bucket, 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportRequestsTimeSeriesCsv(Instant from, Instant to, AnalyticsGranularity granularity) {
        StringBuilder csv = new StringBuilder("bucket,created,completed\n");
        for (RequestsTimeSeriesPointDto point : getRequestsTimeSeries(from, to, granularity)) {
            csv.append(point.bucket()).append(',').append(point.created()).append(',').append(point.completed()).append('\n');
        }
        return csv.toString();
    }

    private static Instant[] normalizeRange(Instant from, Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(DEFAULT_ANALYTICS_RANGE);
        if (!effectiveFrom.isBefore(effectiveTo)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "'from' must be before 'to'.");
        }
        return new Instant[]{effectiveFrom, effectiveTo};
    }

    private static Map<Instant, Long> toBucketMap(List<PickupRequestRepository.BucketCountRow> rows) {
        Map<Instant, Long> map = new TreeMap<>();
        for (PickupRequestRepository.BucketCountRow row : rows) {
            map.put(row.getBucket(), row.getCnt());
        }
        return map;
    }

    @Transactional
    public AdminUserResponseDto deactivateUser(UUID adminId, UUID targetId) {
        if (adminId.equals(targetId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "SELF_DEACTIVATION_NOT_ALLOWED",
                    "You cannot deactivate your own account.");
        }
        User admin = findUserOrThrow(adminId);
        User target = findUserOrThrow(targetId);
        target.setActive(false);
        adminActionLogRepository.save(new AdminActionLog(admin, target, AdminActionType.DEACTIVATE));
        return toUserDto(target);
    }

    @Transactional
    public AdminUserResponseDto reactivateUser(UUID adminId, UUID targetId) {
        User admin = findUserOrThrow(adminId);
        User target = findUserOrThrow(targetId);
        target.setActive(true);
        adminActionLogRepository.save(new AdminActionLog(admin, target, AdminActionType.REACTIVATE));
        return toUserDto(target);
    }

    @Transactional(readOnly = true)
    public Page<AdminActionLogResponseDto> listActionLog(Pageable pageable) {
        return adminActionLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(AdminService::toActionLogDto);
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static AdminUserResponseDto toUserDto(User u) {
        return new AdminUserResponseDto(
                u.getId(), u.getEmail(), u.getRole(), u.getFullName(), u.getPhone(), u.isActive(), u.getCreatedAt());
    }

    private static AdminActionLogResponseDto toActionLogDto(AdminActionLog log) {
        return new AdminActionLogResponseDto(
                log.getId(), log.getAdmin().getEmail(), log.getTarget().getEmail(),
                log.getAction().name(), log.getCreatedAt());
    }
}
