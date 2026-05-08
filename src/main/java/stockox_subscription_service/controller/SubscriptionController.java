package stockox_subscription_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stockox_subscription_service.dto.request.CancelRequest;
import stockox_subscription_service.dto.request.UpgradeRequest;
import stockox_subscription_service.dto.response.ApiResponse;
import stockox_subscription_service.dto.response.SubscriptionResponse;
import stockox_subscription_service.helper.SubscriptionContextHelper;  // ✅ correct package
import stockox_subscription_service.service.SubscriptionService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService       subscriptionService;
    private final SubscriptionContextHelper ctx;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getCurrentSubscription() {
        UUID tenantId = ctx.getTenantId();
        SubscriptionResponse response = subscriptionService.getCurrentSubscription(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Subscription fetched successfully.", response));
    }

    @PutMapping("/upgrade")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> changePlan(
            @Valid @RequestBody UpgradeRequest request) {
        UUID tenantId = ctx.getTenantId();
        log.info("Plan change request - tenant={} targetTier={}", tenantId, request.getTargetTier());
        SubscriptionResponse response = subscriptionService.changePlan(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("Plan updated successfully.", response));
    }

    @DeleteMapping("/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(
            @Valid @RequestBody(required = false) CancelRequest request) {
        UUID tenantId = ctx.getTenantId();
        CancelRequest safeRequest = request != null ? request : new CancelRequest();
        log.info("Cancellation request - tenant={}", tenantId);
        SubscriptionResponse response = subscriptionService.cancelSubscription(tenantId, safeRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Subscription cancelled. Access continues until the end of your billing period.",
                response));
    }

    /**
     * Internal endpoint called by user-service after tenant registration.
     * ✅ FIXED: was @PostMapping("/internal/subscriptions/init/{tenantId}")
     * which resolved to /api/v1/subscriptions/internal/subscriptions/init/{tenantId} — WRONG.
     * Correct URL: /api/v1/subscriptions/init/{tenantId}
     */
    @PostMapping("/init/{tenantId}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> initSubscription(
            @PathVariable UUID tenantId) {
        log.info("Initialising STARTER subscription for new tenant={}", tenantId);
        SubscriptionResponse response = subscriptionService.createStarterSubscription(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Starter subscription created.", response));
    }
}