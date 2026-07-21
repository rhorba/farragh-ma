package ma.farragh.backend.admin;

import ma.farragh.backend.admin.dto.AdminActionLogResponseDto;
import ma.farragh.backend.admin.dto.AdminUserResponseDto;
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
    public Page<RequestResponseDto> searchRequests(RequestStatus status, Pageable pageable) {
        return pickupRequestRepository.search(status, pageable).map(RequestsService::toDto);
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
