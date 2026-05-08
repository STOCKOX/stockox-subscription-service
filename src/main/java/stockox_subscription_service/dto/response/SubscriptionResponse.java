package stockox_subscription_service.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockox_subscription_service.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

public class SubscriptionResponse {
    private UUID id;
    private UUID tenantId;
    private PlanResponse plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime trialEndDate;
    private boolean autoRenew;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed helpers for frontend
    private boolean accessAllowed;
    private long daysUntilExpiry;
}
