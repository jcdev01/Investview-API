package jairo.investviewapi.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> errors
) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, List.of());
    }

    public static ErrorResponse of(
            int status,
            String error,
            String message,
            String path,
            List<FieldError> errors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, errors);
    }
}
