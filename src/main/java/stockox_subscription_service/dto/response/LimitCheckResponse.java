package stockox_subscription_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockox_subscription_service.enums.PlanTier;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LimitCheckResponse {

    private boolean allowed;
    private String limitType;       // PRODUCTS, USERS, WAREHOUSES, INVOICES
    private Integer currentUsage;
    private Integer limit;          // null = unlimited
    private boolean upgradeRequired;
    private PlanTier currentTier;
    private String message;

    public static LimitCheckResponse allowed(String limitType, int currentUsage, Integer limit, PlanTier tier) {
        return LimitCheckResponse.builder()
                .allowed(true)
                .limitType(limitType)
                .currentUsage(currentUsage)
                .limit(limit)
                .upgradeRequired(false)
                .currentTier(tier)
                .message("Access granted.")
                .build();
    }

    public static LimitCheckResponse blocked(String limitType, int currentUsage, int limit, PlanTier tier) {
        return LimitCheckResponse.builder()
                .allowed(false)
                .limitType(limitType)
                .currentUsage(currentUsage)
                .limit(limit)
                .upgradeRequired(true)
                .currentTier(tier)
                .message(String.format(
                        "%s limit reached (%d/%d). Please upgrade your plan to continue.",
                        limitType, currentUsage, limit))
                .build();
    }
}
