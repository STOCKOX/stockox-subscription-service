package stockox_subscription_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import stockox_subscription_service.dto.response.ApiResponse;
import stockox_subscription_service.enums.SubscriptionStatus;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Helper method to check if client expects PDF
    private boolean expectsPdf(HttpServletRequest request) {
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        return acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_PDF_VALUE);
    }

    // Helper method to create PDF error response
    private ResponseEntity<byte[]> createPdfErrorResponse(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .contentType(MediaType.TEXT_PLAIN)
                .body(message.getBytes());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Not found: {}", ex.getMessage());

        if (expectsPdf(request)) {
            return createPdfErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());

        if (expectsPdf(request)) {
            return createPdfErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized: {}", ex.getMessage());

        if (expectsPdf(request)) {
            return createPdfErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<?> handleLimitExceeded(LimitExceededException ex, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errorCode", "LIMIT_EXCEEDED");
        body.put("message", ex.getMessage());
        body.put("limitType", ex.getLimitType());
        body.put("currentUsage", ex.getCurrentUsage());
        body.put("limit", ex.getLimit());
        body.put("currentTier", ex.getCurrentTier().name());
        body.put("upgradeRequired", true);

        if (expectsPdf(request)) {
            return createPdfErrorResponse("Subscription limit exceeded: " + ex.getMessage(),
                    HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<?> handlePaymentVerification(
            PaymentVerificationException ex, HttpServletRequest request) {

        String message;
        String action;
        HttpStatus status = HttpStatus.BAD_REQUEST;

        switch (ex.getReason()) {
            case SIGNATURE_MISMATCH:
                message = "Payment verification failed.";
                action = "CONTACT_SUPPORT";
                break;
            case ORDER_NOT_FOUND:
                message = "Payment order not found.";
                action = "CREATE_NEW_ORDER";
                break;
            case TENANT_MISMATCH:
                message = "Payment does not belong to your account.";
                action = "CONTACT_SUPPORT";
                status = HttpStatus.FORBIDDEN;
                break;
            case CRYPTO_ERROR:
                message = "Technical error during payment verification.";
                action = "CONTACT_SUPPORT";
                break;
            case INVALID_SIGNATURE_FORMAT:
                message = "Invalid payment signature.";
                action = "RETRY_PAYMENT";
                break;
            default:
                message = "Payment verification failed.";
                action = "RETRY_PAYMENT";
        }

        if (expectsPdf(request)) {
            return createPdfErrorResponse(message, status);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errorCode", "PAYMENT_VERIFICATION_FAILED");
        body.put("reason", ex.getReason().name());
        body.put("message", message);
        body.put("action", action);
        body.put("razorpayOrderId", ex.getRazorpayOrderId());

        if (ex.getRazorpayPaymentId() != null) {
            body.put("razorpayPaymentId", ex.getRazorpayPaymentId());
        }

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<?> handleGateway(
            PaymentGatewayException ex, HttpServletRequest request) {

        if (expectsPdf(request)) {
            return createPdfErrorResponse("Payment gateway error: " + ex.getMessage(),
                    HttpStatus.BAD_GATEWAY);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errorCode", "PAYMENT_GATEWAY_ERROR");
        body.put("message", ex.getMessage());
        body.put("gatewayErrorCode", ex.getGatewayErrorCode());
        body.put("retryable", true);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<?> handleDuplicate(
            DuplicatePaymentException ex, HttpServletRequest request) {

        if (expectsPdf(request)) {
            return createPdfErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errorCode", "DUPLICATE_PAYMENT");
        body.put("message", ex.getMessage());
        body.put("razorpayOrderId", ex.getRazorpayOrderId());
        body.put("existingPaymentId", ex.getExistingPaymentId());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidPlanException.class)
    public ResponseEntity<?> handlePlan(
            InvalidPlanException ex, HttpServletRequest request) {

        if (expectsPdf(request)) {
            return createPdfErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errorCode", "INVALID_PLAN");
        body.put("message", ex.getMessage());
        body.put("requestedTier", ex.getRequestedTier().name());
        body.put("reason", ex.getReason());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(PaymentExpiredException.class)
    public ResponseEntity<?> handleExpired(
            PaymentExpiredException ex, HttpServletRequest request) {

        if (expectsPdf(request)) {
            return createPdfErrorResponse(ex.getMessage(), HttpStatus.GONE);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errorCode", "PAYMENT_EXPIRED");
        body.put("message", ex.getMessage());
        body.put("razorpayOrderId", ex.getRazorpayOrderId());
        body.put("action", "CREATE_NEW_ORDER");

        return ResponseEntity.status(HttpStatus.GONE).body(body);
    }

    @ExceptionHandler(SubscriptionInactiveException.class)
    public ResponseEntity<?> handleInactive(
            SubscriptionInactiveException ex, HttpServletRequest request) {

        String action = "RENEW_SUBSCRIPTION";
        if (ex.getCurrentStatus() == SubscriptionStatus.TRIAL) {
            action = "UPGRADE_PLAN";
        }

        if (expectsPdf(request)) {
            return createPdfErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errorCode", "SUBSCRIPTION_INACTIVE");
        body.put("message", ex.getMessage());
        body.put("currentStatus", ex.getCurrentStatus().name());
        body.put("action", action);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        String message = "You do not have permission to perform this action.";

        if (expectsPdf(request)) {
            return createPdfErrorResponse(message, HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed.")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMismatch(
            MethodArgumentTypeMismatchException ex) {

        String msg = "Invalid value '" + ex.getValue()
                + "' for parameter '" + ex.getName() + "'";

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);

        String message = "Something went wrong. Please try again later.";

        if (expectsPdf(request)) {
            return createPdfErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }
}