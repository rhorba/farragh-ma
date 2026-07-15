package ma.farragh.backend.admin;

import ma.farragh.backend.admin.dto.AdminUserResponseDto;
import ma.farragh.backend.auth.Role;
import ma.farragh.backend.auth.User;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.requests.PickupRequestRepository;
import ma.farragh.backend.requests.RequestStatus;
import ma.farragh.backend.requests.RequestsService;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PickupRequestRepository pickupRequestRepository;

    public AdminService(UserRepository userRepository, PickupRequestRepository pickupRequestRepository) {
        this.userRepository = userRepository;
        this.pickupRequestRepository = pickupRequestRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> searchUsers(String email, Role role, Pageable pageable) {
        return userRepository.search(blankToNull(email), role, pageable).map(AdminService::toUserDto);
    }

    @Transactional(readOnly = true)
    public Page<RequestResponseDto> searchRequests(RequestStatus status, Pageable pageable) {
        return pickupRequestRepository.search(status, pageable).map(RequestsService::toDto);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static AdminUserResponseDto toUserDto(User u) {
        return new AdminUserResponseDto(
                u.getId(), u.getEmail(), u.getRole(), u.getFullName(), u.getPhone(), u.isActive(), u.getCreatedAt());
    }
}
