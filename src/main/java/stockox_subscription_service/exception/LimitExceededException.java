// LimitExceededException.java
package stockox_subscription_service.exception;

import lombok.Getter;
import stockox_subscription_service.enums.PlanTier;

@Getter
public class LimitExceededException extends RuntimeException {

    private final String limitType;
    private final int currentUsage;
    private final int limit;
    private final PlanTier currentTier;

    public LimitExceededException(String limitType, int currentUsage, int limit, PlanTier currentTier) {
        super(String.format(
                "%s limit reached (%d/%d) on %s plan. Please upgrade to continue.",
                limitType,
                currentUsage,
                limit,
                currentTier.name()
        ));

        this.limitType = limitType;
        this.currentUsage = currentUsage;
        this.limit = limit;
        this.currentTier = currentTier;
    }
}