package stockox_subscription_service.exception;

import lombok.Getter;

@Getter
public class PaymentVerificationException extends RuntimeException {

    private final Reason reason;
    private final String errorCode;
    private final String razorpayOrderId;
    private final String razorpayPaymentId;

    public enum Reason {
        INVALID_SIGNATURE_FORMAT,
        ORDER_NOT_FOUND,
        TENANT_MISMATCH,
        SIGNATURE_MISMATCH,
        CRYPTO_ERROR
    }

    private PaymentVerificationException(
            Reason reason,
            String errorCode,
            String razorpayOrderId,
            String razorpayPaymentId,
            String message) {

        super(message);
        this.reason = reason;
        this.errorCode = errorCode;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
    }

    /**
     * Missing / invalid signature or order id.
     */
    public static PaymentVerificationException invalidFormat(
            String orderId) {

        return new PaymentVerificationException(
                Reason.INVALID_SIGNATURE_FORMAT,
                "INVALID_FORMAT",
                orderId,
                null,
                "Payment verification failed: invalid or missing order ID '"
                        + orderId + "'"
        );
    }

    /**
     * Order not found.
     */
    public static PaymentVerificationException orderNotFound(
            String orderId) {

        return new PaymentVerificationException(
                Reason.ORDER_NOT_FOUND,
                "ORDER_NOT_FOUND",
                orderId,
                null,
                "Payment verification failed: no pending order found for ID '"
                        + orderId + "'"
        );
    }

    /**
     * Tenant mismatch.
     */
    public static PaymentVerificationException tenantMismatch(
            String orderId,
            String paymentId) {

        return new PaymentVerificationException(
                Reason.TENANT_MISMATCH,
                "TENANT_MISMATCH",
                orderId,
                paymentId,
                "Payment verification failed: tenant mismatch for order '"
                        + orderId + "' and payment '" + paymentId + "'"
        );
    }

    /**
     * Signature mismatch.
     */
    public static PaymentVerificationException signatureMismatch(
            String orderId,
            String paymentId) {

        return new PaymentVerificationException(
                Reason.SIGNATURE_MISMATCH,
                "SIGNATURE_MISMATCH",
                orderId,
                paymentId,
                "Payment verification failed: signature mismatch for order '"
                        + orderId + "' and payment '" + paymentId + "'"
        );
    }

    /**
     * Crypto error.
     */
    public static PaymentVerificationException cryptoError(
            String orderId,
            String cause) {

        return new PaymentVerificationException(
                Reason.CRYPTO_ERROR,
                "CRYPTO_ERROR",
                orderId,
                null,
                "Payment verification failed: cryptographic error for order '"
                        + orderId + "': " + cause
        );
    }
}