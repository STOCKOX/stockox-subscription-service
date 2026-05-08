package stockox_subscription_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockox_subscription_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

public class PaymentTransactionResponse {
    private UUID id;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String planName;
    private BigDecimal baseAmount;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private String currency;
    private PaymentStatus status;
    private LocalDateTime billingPeriodStart;
    private LocalDateTime billingPeriodEnd;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
