package stockox_subscription_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stockox_subscription_service.dto.response.AllLimitResponse;
import stockox_subscription_service.dto.response.ApiResponse;
import stockox_subscription_service.dto.response.LimitCheckResponse;
import stockox_subscription_service.helper.SubscriptionContextHelper;  // ✅ FIXED: was security package
import stockox_subscription_service.service.LimitEnforcerService;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LimitController {

    private final LimitEnforcerService      limitEnforcerService;
    private final SubscriptionContextHelper ctx;

    // ── Frontend endpoints (JWT authenticated) ─────────────────────────

    @GetMapping("/api/v1/subscriptions/limits")
    public ResponseEntity<ApiResponse<AllLimitResponse>> getAllLimits() {
        UUID tenantId = ctx.getTenantId();
        AllLimitResponse limits = limitEnforcerService.getAllLimits(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Limits fetched successfully.", limits));
    }

    @GetMapping("/api/v1/subscriptions/limits/feature")
    public ResponseEntity<ApiResponse<LimitCheckResponse>> checkFeature(
            @RequestParam String key) {
        UUID tenantId = ctx.getTenantId();
        LimitCheckResponse result = limitEnforcerService.checkFeatureAccess(tenantId, key);
        return ResponseEntity.ok(ApiResponse.success("Feature check complete.", result));
    }

    // ── Internal endpoints (called by other microservices via Feign) ───

    @GetMapping("/internal/limits/products")
    public ResponseEntity<LimitCheckResponse> checkProductLimitInternal(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        LimitCheckResponse result = limitEnforcerService.checkProductLimit(tenantId);
        log.debug("Product limit check - tenant={} allowed={}", tenantId, result.isAllowed());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/internal/limits/users")
    public ResponseEntity<LimitCheckResponse> checkUserLimitInternal(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam int currentCount) {
        LimitCheckResponse result = limitEnforcerService.checkUserLimit(tenantId, currentCount);
        log.debug("User limit check - tenant={} currentCount={} allowed={}", tenantId, currentCount, result.isAllowed());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/internal/limits/warehouses")
    public ResponseEntity<LimitCheckResponse> checkWarehouseLimitInternal(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam int currentCount) {
        LimitCheckResponse result = limitEnforcerService.checkWarehouseLimit(tenantId, currentCount);
        log.debug("Warehouse limit check - tenant={} currentCount={} allowed={}", tenantId, currentCount, result.isAllowed());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/internal/limits/invoices")
    public ResponseEntity<LimitCheckResponse> checkInvoiceLimitInternal(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam int currentCount) {
        LimitCheckResponse result = limitEnforcerService.checkInvoiceLimit(tenantId, currentCount);
        log.debug("Invoice limit check - tenant={} currentCount={} allowed={}", tenantId, currentCount, result.isAllowed());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/internal/limits/feature")
    public ResponseEntity<LimitCheckResponse> checkFeatureInternal(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam String key) {
        LimitCheckResponse result = limitEnforcerService.checkFeatureAccess(tenantId, key);
        log.debug("Feature check - tenant={} key={} allowed={}", tenantId, key, result.isAllowed());
        return ResponseEntity.ok(result);
    }
}