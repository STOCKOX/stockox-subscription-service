package stockox_subscription_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockox_subscription_service.enums.PlanTier;
import stockox_subscription_service.enums.SubscriptionStatus;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllLimitResponse {
    private PlanTier currentTier;
    private SubscriptionStatus subscriptionStatus;

    // Product usage
    private int productsUsed;
    private Integer productsLimit;      // null = unlimited
    private boolean productsLimitReached;

    // User seats
    private int usersUsed;
    private Integer usersLimit;
    private boolean usersLimitReached;

    // Warehouses
    private int warehousesUsed;
    private Integer warehousesLimit;
    private boolean warehousesLimitReached;

    // Invoice usage (current month)
    private int invoicesThisMonth;
    private Integer invoicesLimit;
    private boolean invoicesLimitReached;

    // Feature access flags passed directly to frontend
    private boolean canExportPdf;
    private boolean canExportExcel;
    private boolean canAccessApi;
    private boolean canUseAiForecasting;
    private boolean canUseWhatsapp;
    private boolean canUseBarcodes;
    private boolean canViewAdvancedReports;
    private boolean canUseBatchExpiry;
    private boolean canViewAuditLogs;
    private boolean canUseGstReports;
    private boolean canUseAutoReplenishment;
    private boolean canUseCustomBranding;
    private boolean hasPrioritySupport;
}

