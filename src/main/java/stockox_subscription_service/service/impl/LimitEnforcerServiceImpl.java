package stockox_subscription_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stockox_subscription_service.dto.response.AllLimitResponse;
import stockox_subscription_service.dto.response.LimitCheckResponse;
import stockox_subscription_service.entity.Plan;
import stockox_subscription_service.entity.Subscription;
import stockox_subscription_service.exception.BadRequestException;
import stockox_subscription_service.exception.ResourceNotFoundException;
import stockox_subscription_service.fiegn.ProductServiceFeignClient;
import stockox_subscription_service.repository.PlanRepository;
import stockox_subscription_service.repository.SubscriptionRepository;
import stockox_subscription_service.service.LimitEnforcerService;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)


public class LimitEnforcerServiceImpl implements LimitEnforcerService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final ProductServiceFeignClient productClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /** Cache plan limits for 5 minutes to avoid DB hit on every product add or at every check at call. */
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);



    @Override
    public LimitCheckResponse checkProductLimit(UUID tenantId) {
        Plan plan = getCachedPlan(tenantId);
        // Unlimited plan
        if(plan.getMaxProducts() == null) {
            return LimitCheckResponse.allowed("PRODUCTS", -1, null, plan.getTier());
        }
        int currentCount = fetchProductCount(tenantId);
        if(currentCount >= plan.getMaxProducts()) {
            return LimitCheckResponse.blocked("PRODUCTS", currentCount, plan.getMaxProducts(), plan.getTier());
        }
        return LimitCheckResponse.allowed("PRODUCTS", currentCount, plan.getMaxProducts(), plan.getTier());

    }

    @Override
    public LimitCheckResponse checkUserLimit(UUID tenantId, int currentUserCount) {
        Plan plan = getCachedPlan(tenantId);

        if (plan.getMaxUsers() == null) {
            return LimitCheckResponse.allowed("USERS", currentUserCount, null, plan.getTier());
        }
        if (currentUserCount >= plan.getMaxUsers()) {
            return LimitCheckResponse.blocked("USERS", currentUserCount, plan.getMaxUsers(), plan.getTier());
        }
        return LimitCheckResponse.allowed("USERS", currentUserCount, plan.getMaxUsers(), plan.getTier());
    }

    @Override
    public LimitCheckResponse checkWarehouseLimit(UUID tenantId, int currentWarehouseCount) {
        Plan plan = getCachedPlan(tenantId);

        if (plan.getMaxWarehouses() == null) {
            return LimitCheckResponse.allowed("WAREHOUSES", currentWarehouseCount, null, plan.getTier());
        }
        if (currentWarehouseCount >= plan.getMaxWarehouses()) {
            return LimitCheckResponse.blocked("WAREHOUSES", currentWarehouseCount,
                    plan.getMaxWarehouses(), plan.getTier());
        }
        return LimitCheckResponse.allowed("WAREHOUSES", currentWarehouseCount,
                plan.getMaxWarehouses(), plan.getTier());
    }

    @Override
    public LimitCheckResponse checkInvoiceLimit(UUID tenantId, int currentMonthInvoiceCount) {
        Plan plan = getCachedPlan(tenantId);

        if (plan.getMaxInvoicesPerMonth() == null) {
            return LimitCheckResponse.allowed("INVOICES", currentMonthInvoiceCount, null, plan.getTier());
        }
        if (currentMonthInvoiceCount >= plan.getMaxInvoicesPerMonth()) {
            return LimitCheckResponse.blocked("INVOICES", currentMonthInvoiceCount,
                    plan.getMaxInvoicesPerMonth(), plan.getTier());
        }
        return LimitCheckResponse.allowed("INVOICES", currentMonthInvoiceCount,
                plan.getMaxInvoicesPerMonth(), plan.getTier());
    }

    @Override
    public LimitCheckResponse checkFeatureAccess(UUID tenantId, String featureKey) {
        Plan plan = getCachedPlan(tenantId);
        boolean hasAccess = resolveFeatureFlag(plan, featureKey);

        if (!hasAccess) {
            return LimitCheckResponse.builder()
                    .allowed(false)
                    .limitType("FEATURE")
                    .upgradeRequired(true)
                    .currentTier(plan.getTier())
                    .message("Feature '" + featureKey + "' is not available on the "
                            + plan.getTier().name() + " plan. Please upgrade.")
                    .build();
        }
        return LimitCheckResponse.builder()
                .allowed(true)
                .limitType("FEATURE")
                .upgradeRequired(false)
                .currentTier(plan.getTier())
                .message("Access granted.")
                .build();
    }

    @Override
    public AllLimitResponse getAllLimits(UUID tenantId) {
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription for tenant: " + tenantId));
        Plan plan = sub.getPlan();

        int productCount = fetchProductCount(tenantId);

        return AllLimitResponse.builder()
                .currentTier(plan.getTier())
                .subscriptionStatus(sub.getStatus())
                // Products
                .productsUsed(productCount)
                .productsLimit(plan.getMaxProducts())
                .productsLimitReached(plan.getMaxProducts() != null && productCount >= plan.getMaxProducts())
                // Users (product-service doesn't track users, frontend passes current count)
                .usersLimit(plan.getMaxUsers())
                // Warehouses
                .warehousesLimit(plan.getMaxWarehouses())
                // Invoices
                .invoicesLimit(plan.getMaxInvoicesPerMonth())
                // Feature flags
                .canExportPdf(plan.isHasPdfExport())
                .canExportExcel(plan.isHasExcelExport())
                .canAccessApi(plan.isHasApiAccess())
                .canUseAiForecasting(plan.isHasAiForecasting())
                .canUseWhatsapp(plan.isHasWhatsappAlerts())
                .canUseBarcodes(plan.isHasBarcodeScan())
                .canViewAdvancedReports(plan.isHasAdvancedReports())
                .canUseBatchExpiry(plan.isHasBatchExpiryTracking())
                .canViewAuditLogs(plan.isHasAuditLogs())
                .canUseGstReports(plan.isHasGstReports())
                .canUseAutoReplenishment(plan.isHasAutoReplenishment())
                .canUseCustomBranding(plan.isHasCustomBranding())
                .hasPrioritySupport(plan.isHasPrioritySupport())
                .build();
    }
    // Helpers Method
    // *Reads plan from Redis if cached, otherwise hits DB and caches for 5 min.

    private Plan getCachedPlan(UUID tenantId) {
        String cacheKey = "plan:limits:"+tenantId;

        try{
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if(cached instanceof Plan plan) {
                log.debug("Plan cache HIT for tenant={}", tenantId);
                return plan;
            }
        }catch (Exception e){
            log.warn("Redis read failed for tenant={}, falling back to DB: {}", tenantId, e.getMessage());
        }
        Plan plan = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription for tenant: " + tenantId))
                .getPlan();

        try {
            redisTemplate.opsForValue().set(cacheKey, plan, CACHE_TTL);
            log.debug("Plan cache SET for tenant={} tier={}", tenantId, plan.getTier());
        } catch (Exception e) {
            log.warn("Redis write failed for tenant={}: {}", tenantId, e.getMessage());
        }

        return plan;
    }


    // Fetch Product count
    private int fetchProductCount(UUID tenantId) {
        try {
            return productClient.getProductCount(tenantId).count();
        } catch (Exception e) {
            log.error("Could not fetch product count for tenant={}: {}", tenantId, e.getMessage());
            // Fail open — don't block the user if product-service is temporarily down
            return 0;
        }
    }



    private boolean resolveFeatureFlag(Plan plan, String featureKey) {
        return switch (featureKey) {
            case "hasPdfExport" -> plan.isHasPdfExport();
            case "hasExcelExport" -> plan.isHasExcelExport();
            case "hasApiAccess" -> plan.isHasApiAccess();
            case "hasAiForecasting" -> plan.isHasAiForecasting();
            case "hasWhatsappAlerts" -> plan.isHasWhatsappAlerts();
            case "hasBarcodeScan" -> plan.isHasBarcodeScan();
            case "hasAdvancedReports" -> plan.isHasAdvancedReports();
            case "hasBatchExpiryTracking" -> plan.isHasBatchExpiryTracking();
            case "hasAuditLogs" -> plan.isHasAuditLogs();
            case "hasGstReports" -> plan.isHasGstReports();
            case "hasAutoReplenishment" -> plan.isHasAutoReplenishment();
            case "hasCustomBranding" -> plan.isHasCustomBranding();
            case "hasPrioritySupport" -> plan.isHasPrioritySupport();
            default -> throw new BadRequestException("Unknown feature key: " + featureKey);
        };
    }
}
