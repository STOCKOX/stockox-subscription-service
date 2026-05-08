package stockox_subscription_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

public class PaymentOrderResponse {
    private UUID transactionId;         // our internal ID
    private String razorpayOrderId;     // pass to Razorpay JS SDK
    private String razorpayKeyId;       // public key — safe to expose
    private BigDecimal baseAmount;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;     // in INR (not paise)
    private String currency;
    private String planName;
    private String description;
}
