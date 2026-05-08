package stockox_subscription_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockox_subscription_service.enums.PlanTier;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class SubscribeRequest {

    @NotNull(message = "Plan tier is required")
    private PlanTier planTier;

}
