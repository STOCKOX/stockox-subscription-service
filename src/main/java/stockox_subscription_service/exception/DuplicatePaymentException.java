// DuplicatePaymentException.java
package stockox_subscription_service.exception;

import lombok.Getter;

@Getter
public class DuplicatePaymentException extends RuntimeException {

    private final String razorpayOrderId;
    private final String existingPaymentId;

    public DuplicatePaymentException(String razorpayOrderId, String existingPaymentId) {
        super(String.format(
                "Payment for order '%s' was already processed (paymentId: %s).",
                razorpayOrderId, existingPaymentId));

        this.razorpayOrderId = razorpayOrderId;
        this.existingPaymentId = existingPaymentId;
    }
}