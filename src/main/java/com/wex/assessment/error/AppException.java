package com.wex.assessment.error;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    public AppException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public AppException(ErrorCode code, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AppException badRequest(String message) {
        return new AppException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    public static AppException purchaseNotFound(String purchaseId) {
        return new AppException(
                ErrorCode.PURCHASE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "purchase \"%s\" was not found".formatted(purchaseId)
        );
    }

    public static AppException routeNotFound(String method, String path) {
        return new AppException(
                ErrorCode.ROUTE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "no route found for %s %s".formatted(method, path)
        );
    }

    public static AppException methodNotAllowed(String method, String path) {
        return new AppException(
                ErrorCode.METHOD_NOT_ALLOWED,
                HttpStatus.METHOD_NOT_ALLOWED,
                "method %s is not allowed for %s".formatted(method, path)
        );
    }

    public static AppException unsupportedMediaType(String message) {
        return new AppException(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                message
        );
    }

    public static AppException notAcceptable(String message) {
        return new AppException(
                ErrorCode.NOT_ACCEPTABLE,
                HttpStatus.NOT_ACCEPTABLE,
                message
        );
    }

    public static AppException unsupportedCurrency(String currencyCode) {
        return new AppException(
                ErrorCode.UNSUPPORTED_CURRENCY,
                HttpStatus.BAD_REQUEST,
                "currency \"%s\" is not supported for conversion".formatted(currencyCode)
        );
    }

    public static AppException noRateAvailable(String currencyCode, String purchaseDate) {
        return new AppException(
                ErrorCode.NO_RATE_AVAILABLE,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "no exchange rate is available for currency \"%s\" on or before %s within the prior 6 calendar months"
                        .formatted(currencyCode, purchaseDate)
        );
    }

    public static AppException internal(String message, Throwable cause) {
        return new AppException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
