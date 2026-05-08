// InvalidPlanException.java
package stockox_subscription_service.exception;

import lombok.Getter;
import stockox_subscription_service.enums.PlanTier;

@Getter
public class InvalidPlanException extends RuntimeException {

    private final PlanTier requestedTier;
    private final String reason;

    public InvalidPlanException(PlanTier requestedTier, String reason) {
        super(String.format("Invalid plan '%s': %s", requestedTier.name(), reason));
        this.requestedTier = requestedTier;
        this.reason = reason;
    }

    public static InvalidPlanException freePlanPayment(PlanTier tier) {
        return new InvalidPlanException(
                tier,
                "This plan is free and does not require a payment."
        );
    }

    public static InvalidPlanException alreadyOnPlan(PlanTier tier) {
        return new InvalidPlanException(
                tier,
                "You are already subscribed to this plan."
        );
    }
}