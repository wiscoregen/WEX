package com.wex.assessment.web;

import com.wex.assessment.error.AppException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("${server.error.path:${error.path:/error}}")
public class ApiErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public ApiErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping
    public ResponseEntity<ApiErrorEnvelope> error(HttpServletRequest request) {
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                new org.springframework.web.context.request.ServletWebRequest(request),
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE)
        );

        int statusCode = getStatusCode(request, attributes);
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        AppException exception = mapError(status, request, attributes);
        return ResponseEntity.status(exception.getStatus())
                .body(ApiErrorEnvelope.from(exception.getCode(), exception.getMessage(), exception.getStatus().value()));
    }

    private int getStatusCode(HttpServletRequest request, Map<String, Object> attributes) {
        Object statusFromRequest = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusFromRequest instanceof Integer status) {
            return status;
        }

        Object statusFromAttributes = attributes.get("status");
        if (statusFromAttributes instanceof Integer status) {
            return status;
        }

        return 500;
    }

    private AppException mapError(HttpStatus status, HttpServletRequest request, Map<String, Object> attributes) {
        String path = requestPath(request, attributes);
        String method = request.getMethod();

        return switch (status) {
            case NOT_FOUND -> AppException.routeNotFound(method, path);
            case METHOD_NOT_ALLOWED -> AppException.methodNotAllowed(method, path);
            case UNSUPPORTED_MEDIA_TYPE -> AppException.unsupportedMediaType("Content-Type must be application/json");
            case NOT_ACCEPTABLE -> AppException.notAcceptable("requested response media type is not supported");
            case BAD_REQUEST -> AppException.badRequest(readMessage(attributes, "request could not be processed"));
            default -> AppException.internal("unexpected server error", null);
        };
    }

    private String requestPath(HttpServletRequest request, Map<String, Object> attributes) {
        Object pathAttribute = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (pathAttribute instanceof String path && !path.isBlank()) {
            return path;
        }

        Object path = attributes.get("path");
        if (path instanceof String stringPath && !stringPath.isBlank()) {
            return stringPath;
        }

        return request.getRequestURI();
    }

    private String readMessage(Map<String, Object> attributes, String fallback) {
        Object message = attributes.get("message");
        if (message instanceof String stringMessage && !stringMessage.isBlank() && !"No message available".equals(stringMessage)) {
            return stringMessage;
        }

        return fallback;
    }
}

