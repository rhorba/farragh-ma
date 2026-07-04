package ma.farragh.backend.shared.exception;

import java.util.List;

public record ApiError(ErrorBody error) {

    public record ErrorBody(String code, String message, List<FieldDetail> details, String requestId) {
    }

    public record FieldDetail(String field, String message) {
    }

    public static ApiError of(String code, String message, String requestId) {
        return new ApiError(new ErrorBody(code, message, List.of(), requestId));
    }

    public static ApiError of(String code, String message, List<FieldDetail> details, String requestId) {
        return new ApiError(new ErrorBody(code, message, details, requestId));
    }
}
