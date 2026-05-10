package stockox_subscription_service.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stockox_subscription_service.entity.Plan;
import stockox_subscription_service.entity.Subscription;
import stockox_subscription_service.enums.PlanTier;
import stockox_subscription_service.enums.SubscriptionStatus;
import stockox_subscription_service.repository.PlanRepository;
import stockox_subscription_service.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Component

public class SubscriptionExpiryScheduler {
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Runs every night at 02:00 AM.
     * 1. Marks ACTIVE subscriptions past their endDate as EXPIRED.
     * 2. Downgrades expired paid plans back to STARTER so the tenant
     *    can still log in but features are restricted.
     * 3. Marks TRIAL subscriptions past their trialEndDate as EXPIRED.
     * 4. Evicts Redis plan cache for every affected tenant so the
     *    next request reads fresh limits from DB.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void processExpiredSubscriptions() {
        log.info("SubscriptionExpiryScheduler — starting nightly run");

        LocalDateTime now = LocalDateTime.now();
        int expiredCount  = 0;
        int trialCount    = 0;

        // ── 1. Expire active paid subscriptions ──────────────────────────
        java.util.List<Subscription> expiredPaid = subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, now);

        Plan starterPlan = planRepository.findByTier(PlanTier.STARTER)
                .orElse(null);

        List<Subscription> paidToSave = new java.util.ArrayList<>();

        for (Subscription sub : expiredPaid) {
            log.warn("Expiring subscription — tenant={} plan={} endDate={}",
                    sub.getTenantId(), sub.getPlan().getTier(), sub.getEndDate());

            sub.setStatus(SubscriptionStatus.EXPIRED);

            // Downgrade to STARTER if the plan has a price (was paid)
            if (starterPlan != null
                    && sub.getPlan().getPriceMonthly() != null
                    && sub.getPlan().getPriceMonthly().signum() > 0) {
                sub.setPlan(starterPlan);
                log.info("  Downgraded tenant={} to STARTER", sub.getTenantId());
            }

            paidToSave.add(sub);
            evictCache(sub.getTenantId().toString());
            expiredCount++;
        }
        // Batch-save all expired paid subscriptions in one round-trip
        subscriptionRepository.saveAll(paidToSave);

        // ── 2. Expire trial subscriptions ────────────────────────────────
        List<Subscription> expiredTrials = subscriptionRepository
                .findByStatusAndTrialEndDateBefore(SubscriptionStatus.TRIAL, now);

        List<Subscription> trialsToSave = new java.util.ArrayList<>();

        for (Subscription sub : expiredTrials) {
            log.info("Trial expired — tenant={} trialEnd={}", sub.getTenantId(), sub.getTrialEndDate());
            sub.setStatus(SubscriptionStatus.EXPIRED);

            // Keep on STARTER plan (they were already on it during trial)
            trialsToSave.add(sub);
            evictCache(sub.getTenantId().toString());
            trialCount++;
        }
        // Batch-save all expired trial subscriptions in one round-trip
        subscriptionRepository.saveAll(trialsToSave);

        log.info("SubscriptionExpiryScheduler — done. Expired paid={}, trials={}", expiredCount, trialCount);
    }

    /**
     * Runs every day at 10:00 AM.
     * Sends renewal reminder emails 3 days before a subscription expires.
     * TODO: Wire in notification-service Feign client here in Phase 5.
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void sendRenewalReminders() {
        LocalDateTime from = LocalDateTime.now().plusDays(2);
        LocalDateTime to   = LocalDateTime.now().plusDays(4);

        List<Subscription> expiringSoon = subscriptionRepository
                .findExpiringSoon(SubscriptionStatus.ACTIVE, from, to);

        for (Subscription sub : expiringSoon) {
            log.info("Renewal reminder due — tenant={} plan={} expiresOn={}",
                    sub.getTenantId(), sub.getPlan().getTier(), sub.getEndDate());
            // TODO: call notification-service.sendRenewalReminderEmail(tenantId, endDate)
        }

        log.debug("Renewal reminder check — {} subscriptions expiring soon", expiringSoon.size());
    }

    private void evictCache(String tenantId) {
        try {
            redisTemplate.delete("plan:limits:" + tenantId);
        } catch (Exception e) {
            log.warn("Failed to evict Redis cache for tenant={}: {}", tenantId, e.getMessage());
        }
    }
}
