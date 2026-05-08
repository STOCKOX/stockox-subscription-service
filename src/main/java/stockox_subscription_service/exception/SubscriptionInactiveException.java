package stockox_subscription_service.exception;

import lombok.Getter;
import stockox_subscription_service.enums.SubscriptionStatus;

@Getter
public class SubscriptionInactiveException extends RuntimeException {

    private final SubscriptionStatus currentStatus;

    public SubscriptionInactiveException(String message,
                                         SubscriptionStatus currentStatus) {
        super(message);
        this.currentStatus = currentStatus;
    }

    public static SubscriptionInactiveException of(
            SubscriptionStatus status) {

        return new SubscriptionInactiveException(
                "Subscription is not active.",
                status
        );
    }

    public static SubscriptionInactiveException expired() {
        return new SubscriptionInactiveException(
                "Your subscription has expired. Please renew.",
                SubscriptionStatus.EXPIRED
        );
    }

    public static SubscriptionInactiveException cancelled() {
        return new SubscriptionInactiveException(
                "Your subscription has been cancelled.",
                SubscriptionStatus.CANCELLED
        );
    }

    public static SubscriptionInactiveException pastDue() {
        return new SubscriptionInactiveException(
                "Payment is overdue. Please complete payment.",
                SubscriptionStatus.PAST_DUE
        );
    }

    public static SubscriptionInactiveException trial() {
        return new SubscriptionInactiveException(
                "You are currently on trial plan.",
                SubscriptionStatus.TRIAL
        );
    }
}