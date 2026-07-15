package ma.farragh.backend.requests;

import ma.farragh.backend.auth.User;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.requests.dto.CreateRequestDto;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import ma.farragh.backend.shared.exception.BusinessException;
import ma.farragh.backend.shared.materials.MaterialType;
import ma.farragh.backend.shared.materials.MaterialTypeRepository;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RequestsService {

    private static final int WGS84_SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    private final PickupRequestRepository pickupRequestRepository;
    private final MaterialTypeRepository materialTypeRepository;
    private final UserRepository userRepository;

    public RequestsService(PickupRequestRepository pickupRequestRepository,
                            MaterialTypeRepository materialTypeRepository,
                            UserRepository userRepository) {
        this.pickupRequestRepository = pickupRequestRepository;
        this.materialTypeRepository = materialTypeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RequestResponseDto create(UUID requesterId, CreateRequestDto dto) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));
        MaterialType materialType = materialTypeRepository.findByCode(dto.materialTypeCode())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "UNKNOWN_MATERIAL_TYPE",
                        "Unknown material type: " + dto.materialTypeCode()));

        Point location = GEOMETRY_FACTORY.createPoint(
                new org.locationtech.jts.geom.Coordinate(dto.longitude(), dto.latitude()));

        PickupRequest request = new PickupRequest(
                requester, materialType, dto.quantityDesc(), dto.addressText(), location, dto.photoUrl());
        pickupRequestRepository.save(request);
        return toDto(request);
    }

    @Transactional(readOnly = true)
    public List<RequestResponseDto> listMine(UUID requesterId) {
        return pickupRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId).stream()
                .map(RequestsService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestResponseDto getMine(UUID requesterId, UUID requestId) {
        PickupRequest request = ownedRequestOrThrow(requesterId, requestId);
        return toDto(request);
    }

    @Transactional
    public RequestResponseDto cancel(UUID requesterId, UUID requestId) {
        PickupRequest request = ownedRequestOrThrow(requesterId, requestId);
        if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "A " + request.getStatus() + " request cannot be cancelled.");
        }
        request.setStatus(RequestStatus.CANCELLED);
        return toDto(request);
    }

    private PickupRequest ownedRequestOrThrow(UUID requesterId, UUID requestId) {
        return pickupRequestRepository.findByIdAndRequesterId(requestId, requesterId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Request not found."));
    }

    public static RequestResponseDto toDto(PickupRequest r) {
        return toDto(r, null);
    }

    public static RequestResponseDto toDto(PickupRequest r, String paymentStatus) {
        return new RequestResponseDto(
                r.getId(),
                r.getMaterialType().getCode(),
                r.getQuantityDesc(),
                r.getAddressText(),
                r.getLocation().getY(),
                r.getLocation().getX(),
                r.getStatus(),
                r.getPhotoUrl(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                paymentStatus);
    }
}
