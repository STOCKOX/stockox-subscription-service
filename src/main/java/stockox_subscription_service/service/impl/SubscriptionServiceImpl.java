package stockox_subscription_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stockox_subscription_service.dto.request.CancelRequest;
import stockox_subscription_service.dto.request.SubscribeRequest;
import stockox_subscription_service.dto.request.UpgradeRequest;
import stockox_subscription_service.dto.response.SubscriptionResponse;
import stockox_subscription_service.entity.Plan;
import stockox_subscription_service.entity.Subscription;
import stockox_subscription_service.enums.PlanTier;
import stockox_subscription_service.enums.SubscriptionStatus;
import stockox_subscription_service.exception.BadRequestException;
import stockox_subscription_service.exception.ResourceNotFoundException;
import stockox_subscription_service.mapper.SubscriptionMapper;
import stockox_subscription_service.repository.PlanRepository;
import stockox_subscription_service.repository.SubscriptionRepository;
import stockox_subscription_service.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository         planRepository;
    private final SubscriptionMapper     subscriptionMapper;

    // ✅ FIXED: was RedisTemplate<Object, Object> — Spring bean is <String, Object>
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${subscription.trial-days:14}")
    private int trialDays;

    // ── Public API ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(UUID tenantId) {
        Subscription sub = getSubscriptionEntity(tenantId);
        return subscriptionMapper.toResponse(sub);
    }

    @Override
    @Transactional
    public SubscriptionResponse createStarterSubscription(UUID tenantId) {
        if (subscriptionRepository.existsByTenantId(tenantId)) {
            throw new BadRequestException("Subscription already exists for tenant: " + tenantId);
        }

        Plan starterPlan = resolvePlan(PlanTier.STARTER);
        LocalDateTime now = LocalDateTime.now();

        Subscription sub = Subscription.builder()
                .tenantId(tenantId)
                .plan(starterPlan)
                .status(SubscriptionStatus.TRIAL)
                .startDate(now)
                .trialEndDate(now.plusDays(trialDays))
                .endDate(now.plusDays(trialDays))
                .autoRenew(false)
                .build();

        Subscription saved = subscriptionRepository.save(sub);
        log.info("Created STARTER trial subscription for tenant={} trialEnds={}",
                tenantId, saved.getTrialEndDate());
        return subscriptionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SubscriptionResponse activatePlan(UUID tenantId, SubscribeRequest request) {
        Subscription sub = getSubscriptionEntity(tenantId);
        Plan plan = resolvePlan(request.getPlanTier());

        LocalDateTime now = LocalDateTime.now();
        sub.setPlan(plan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(now);
        sub.setEndDate(now.plusMonths(1));
        sub.setAutoRenew(true);
        sub.setCancelledAt(null);
        sub.setCancellationReason(null);

        Subscription saved = subscriptionRepository.save(sub);
        evictPlanCache(tenantId);

        log.info("Activated {} plan for tenant={} until {}", plan.getTier(), tenantId, saved.getEndDate());
        return subscriptionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SubscriptionResponse changePlan(UUID tenantId, UpgradeRequest request) {
        Subscription sub         = getSubscriptionEntity(tenantId);
        Plan currentPlan         = sub.getPlan();
        Plan targetPlan          = resolvePlan(request.getTargetTier());

        if (currentPlan.getTier() == targetPlan.getTier()) {
            throw new BadRequestException(
                    "You are already on the " + request.getTargetTier() + " plan.");
        }

        boolean isUpgrade = targetPlan.getSortOrder() > currentPlan.getSortOrder();
        log.info("{} plan for tenant={}: {} -> {}",
                isUpgrade ? "Upgrading" : "Downgrading",
                tenantId, currentPlan.getTier(), targetPlan.getTier());

        LocalDateTime now = LocalDateTime.now();
        sub.setPlan(targetPlan);
        sub.setStatus(SubscriptionStatus.ACTIVE);

        if (isUpgrade) {
            sub.setEndDate(now.plusMonths(1));
        } else {
            if (sub.getEndDate() == null || sub.getEndDate().isBefore(now)) {
                sub.setEndDate(now.plusMonths(1));
            }
        }

        sub.setAutoRenew(true);
        sub.setCancelledAt(null);
        sub.setCancellationReason(null);

        Subscription saved = subscriptionRepository.save(sub);
        evictPlanCache(tenantId);
        return subscriptionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID tenantId, CancelRequest request) {
        Subscription sub = getSubscriptionEntity(tenantId);

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Subscription is already cancelled.");
        }

        sub.setAutoRenew(false);
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(LocalDateTime.now());
        sub.setCancellationReason(request.getReason());

        Subscription saved = subscriptionRepository.save(sub);
        evictPlanCache(tenantId);

        log.info("Cancelled subscription for tenant={} reason={}", tenantId, request.getReason());
        return subscriptionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Subscription getSubscriptionEntity(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No subscription found for tenant: " + tenantId));
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private Plan resolvePlan(PlanTier tier) {
        return planRepository.findByTier(tier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan not found for tier: " + tier));
    }

    private void evictPlanCache(UUID tenantId) {
        try {
            redisTemplate.delete("plan:limits:" + tenantId);
            log.debug("Evicted plan cache for tenant={}", tenantId);
        } catch (Exception e) {
            log.warn("Failed to evict Redis cache for tenant={}: {}", tenantId, e.getMessage());
        }
    }
}