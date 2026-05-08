// PaymentGatewayException.java
package stockox_subscription_service.exception;

import lombok.Getter;

@Getter
public class PaymentGatewayException extends RuntimeException {

    private final String gatewayErrorCode;
    private final String gatewayMessage;

    public PaymentGatewayException(String gatewayMessage) {
        super("Payment gateway error: " + gatewayMessage);
        this.gatewayErrorCode = "GATEWAY_ERROR";
        this.gatewayMessage = gatewayMessage;
    }

    public PaymentGatewayException(String errorCode, String gatewayMessage) {
        super("Payment gateway error [" + errorCode + "]: " + gatewayMessage);
        this.gatewayErrorCode = errorCode;
        this.gatewayMessage = gatewayMessage;
    }
}