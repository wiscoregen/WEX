package com.wex.assessment.web;

import com.wex.assessment.error.AppException;
import com.wex.assessment.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorEnvelope> handleAppException(AppException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiErrorEnvelope.from(exception.getCode(), exception.getMessage(), exception.getStatus().value()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorEnvelope> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return build(AppException.unsupportedMediaType("Content-Type must be application/json"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorEnvelope> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return badRequest("invalid JSON payload");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorEnvelope> handleMissingParameter(MissingServletRequestParameterException exception) {
        return badRequest(exception.getParameterName() + " query parameter is required");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorEnvelope> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return badRequest("invalid request parameter");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorEnvelope> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        String path = exception.getBody().getInstance() == null ? "requested route" : exception.getBody().getInstance().toString();
        return build(AppException.methodNotAllowed(exception.getMethod(), path));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorEnvelope> handleNotAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return build(AppException.notAcceptable("requested response media type is not supported"));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorEnvelope> handleNoHandler(Exception exception) {
        if (exception instanceof NoHandlerFoundException noHandlerFoundException) {
            return build(AppException.routeNotFound(noHandlerFoundException.getHttpMethod(), noHandlerFoundException.getRequestURL()));
        }

        if (exception instanceof NoResourceFoundException noResourceFoundException) {
            return build(AppException.routeNotFound("GET", noResourceFoundException.getResourcePath()));
        }

        return build(AppException.badRequest("route not found"));
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiErrorEnvelope> handleMissingPathVariable(MissingPathVariableException exception) {
        return badRequest("required path variable is missing");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorEnvelope> handleUnexpectedException(Exception exception) {
        LOGGER.error("unexpected server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorEnvelope.from(ErrorCode.INTERNAL_ERROR, "unexpected server error", 500));
    }

    private ResponseEntity<ApiErrorEnvelope> badRequest(String message) {
        return build(AppException.badRequest(message));
    }

    private ResponseEntity<ApiErrorEnvelope> build(AppException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiErrorEnvelope.from(exception.getCode(), exception.getMessage(), exception.getStatus().value()));
    }
}
