package stockox_subscription_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockox_subscription_service.enums.PlanTier;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanResponse {
    private UUID id;
    private PlanTier tier;
    private String name;
    private String displayName;
    private String description;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;

    // Limits — null = unlimited
    private Integer maxProducts;
    private Integer maxUsers;
    private Integer maxWarehouses;
    private Integer maxInvoicesPerMonth;

    // Feature flags
    private boolean hasPdfExport;
    private boolean hasExcelExport;
    private boolean hasApiAccess;
    private boolean hasAiForecasting;
    private boolean hasWhatsappAlerts;
    private boolean hasBarcodeScan;
    private boolean hasAdvancedReports;
    private boolean hasBatchExpiryTracking;
    private boolean hasAuditLogs;
    private boolean hasGstReports;
    private boolean hasAutoReplenishment;
    private boolean hasCustomBranding;
    private boolean hasPrioritySupport;

    private int sortOrder;
}
