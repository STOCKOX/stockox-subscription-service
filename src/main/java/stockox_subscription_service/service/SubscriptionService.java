package stockox_subscription_service.service;

import stockox_subscription_service.dto.request.CancelRequest;
import stockox_subscription_service.dto.request.SubscribeRequest;
import stockox_subscription_service.dto.request.UpgradeRequest;
import stockox_subscription_service.dto.response.SubscriptionResponse;
import stockox_subscription_service.entity.Subscription;

import java.util.UUID;

public interface SubscriptionService {

    SubscriptionResponse getCurrentSubscription(UUID tenantId);

    //Todo:- For Data Seeder at the time of signup assign free or trail plan to company or user
    SubscriptionResponse createStarterSubscription(UUID tenantId);

    // Switch to paid plan after success of payment

    SubscriptionResponse activatePlan(UUID tenantId, SubscribeRequest request);

    /** Upgrade or downgrade — takes effect immediately. */
    SubscriptionResponse changePlan(UUID tenantId, UpgradeRequest request);

    /** Cancel at end of current billing period. */
    SubscriptionResponse cancelSubscription(UUID tenantId, CancelRequest request);

    /** Internal helper — returns raw entity (used by LimitEnforcerService). */
    Subscription getSubscriptionEntity(UUID tenantId);

}
