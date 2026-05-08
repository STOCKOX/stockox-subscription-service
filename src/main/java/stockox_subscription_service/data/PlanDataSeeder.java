package stockox_subscription_service.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import stockox_subscription_service.entity.Plan;
import stockox_subscription_service.enums.PlanTier;
import stockox_subscription_service.repository.PlanRepository;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor

public class PlanDataSeeder implements CommandLineRunner {
    private final PlanRepository planRepository;

    @Override
    public void run(String... args) {
        log.info("PlanDataSeeder — checking plans...");
        int seeded = 0;

        seeded += seedIfMissing(buildStarter());
        seeded += seedIfMissing(buildPro());
        seeded += seedIfMissing(buildBusiness());

        if (seeded == 0) {
            log.info("  All plans already exist — skipping.");
        } else {
            log.info("PlanDataSeeder complete — {} plan(s) seeded.", seeded);
        }
    }


    private Plan buildStarter() {
        return Plan.builder()
                .tier(PlanTier.STARTER)
                .name("Starter")
                .displayName("Starter")
                .description("Chhote business ke liye")
                .priceMonthly(BigDecimal.ZERO)
                .priceYearly(BigDecimal.ZERO)
                // Limits
                .maxProducts(500)
                .maxUsers(3)
                .maxWarehouses(1)
                .maxInvoicesPerMonth(50)
                // Features
                .hasPdfExport(false)
                .hasExcelExport(false)
                .hasApiAccess(false)
                .hasAiForecasting(false)
                .hasWhatsappAlerts(false)
                .hasBarcodeScan(false)
                .hasAdvancedReports(false)
                .hasBatchExpiryTracking(false)
                .hasAuditLogs(false)
                .hasGstReports(false)
                .hasAutoReplenishment(false)
                .hasCustomBranding(false)
                .hasPrioritySupport(false)
                .active(true)
                .sortOrder(1)
                .build();
    }

    private Plan buildPro() {
        return Plan.builder()
                .tier(PlanTier.PRO)
                .name("Pro")
                .displayName("Pro")
                .description("Growing businesses ke liye")
                .priceMonthly(new BigDecimal("999.00"))
                .priceYearly(new BigDecimal("9990.00"))  // ~2 months free
                // Limits
                .maxProducts(null)          // unlimited
                .maxUsers(15)
                .maxWarehouses(5)
                .maxInvoicesPerMonth(null)  // unlimited
                // Features
                .hasPdfExport(true)
                .hasExcelExport(true)
                .hasApiAccess(false)
                .hasAiForecasting(false)
                .hasWhatsappAlerts(false)
                .hasBarcodeScan(false)
                .hasAdvancedReports(true)
                .hasBatchExpiryTracking(true)
                .hasAuditLogs(true)
                .hasGstReports(true)
                .hasAutoReplenishment(false)
                .hasCustomBranding(false)
                .hasPrioritySupport(false)
                .active(true)
                .sortOrder(2)
                .build();
    }

    private Plan buildBusiness() {
        return Plan.builder()
                .tier(PlanTier.BUSINESS)
                .name("Business")
                .displayName("Business")
                .description("Badi companies ke liye")
                .priceMonthly(new BigDecimal("2999.00"))
                .priceYearly(new BigDecimal("29990.00"))
                // Limits — all unlimited
                .maxProducts(null)
                .maxUsers(null)
                .maxWarehouses(null)
                .maxInvoicesPerMonth(null)
                // Features — all on
                .hasPdfExport(true)
                .hasExcelExport(true)
                .hasApiAccess(true)
                .hasAiForecasting(true)
                .hasWhatsappAlerts(true)
                .hasBarcodeScan(true)
                .hasAdvancedReports(true)
                .hasBatchExpiryTracking(true)
                .hasAuditLogs(true)
                .hasGstReports(true)
                .hasAutoReplenishment(true)
                .hasCustomBranding(true)
                .hasPrioritySupport(true)
                .active(true)
                .sortOrder(3)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Helper
    // ------------------------------------------------------------------ //

    private int seedIfMissing(Plan plan) {
        if (planRepository.existsByTier(plan.getTier())) {
            log.debug("  Plan {} already exists — skipping.", plan.getTier());
            return 0;
        }
        planRepository.save(plan);
        log.info("  Seeded plan: {} (₹{})", plan.getTier(), plan.getPriceMonthly());
        return 1;
    }
}
