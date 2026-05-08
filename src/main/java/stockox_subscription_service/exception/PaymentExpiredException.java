// PaymentExpiredException.java
package stockox_subscription_service.exception;

import lombok.Getter;

@Getter
public class PaymentExpiredException extends RuntimeException {

    private final String razorpayOrderId;

    public PaymentExpiredException(String razorpayOrderId) {
        super("Payment order '" + razorpayOrderId + "' has expired. Please initiate a new payment.");
        this.razorpayOrderId = razorpayOrderId;
    }
}