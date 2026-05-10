package stockox_subscription_service.entity;

import stockox_subscription_service.enums.PlanTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "plans",
        indexes = {
                @Index(name = "idx_plan_tier",       columnList = "tier"),
                @Index(name = "idx_plan_active_sort", columnList = "is_active, sort_order")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, unique = true, length = 20)
    private PlanTier tier;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "price_monthly", precision = 10, scale = 2)
    private BigDecimal priceMonthly;   // null = free

    @Column(name = "price_yearly", precision = 10, scale = 2)
    private BigDecimal priceYearly;

    // Feature limits — null means unlimited
    @Column(name = "max_products")
    private Integer maxProducts;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_warehouses")
    private Integer maxWarehouses;

    @Column(name = "max_invoices_per_month")
    private Integer maxInvoicesPerMonth;

    // Feature flags
    @Column(name = "has_pdf_export", nullable = false)
    @Builder.Default
    private boolean hasPdfExport = false;

    @Column(name = "has_excel_export", nullable = false)
    @Builder.Default
    private boolean hasExcelExport = false;

    @Column(name = "has_api_access", nullable = false)
    @Builder.Default
    private boolean hasApiAccess = false;

    @Column(name = "has_ai_forecasting", nullable = false)
    @Builder.Default
    private boolean hasAiForecasting = false;

    @Column(name = "has_whatsapp_alerts", nullable = false)
    @Builder.Default
    private boolean hasWhatsappAlerts = false;

    @Column(name = "has_barcode_scanning", nullable = false)
    @Builder.Default
    private boolean hasBarcodeScan = false;

    @Column(name = "has_advanced_reports", nullable = false)
    @Builder.Default
    private boolean hasAdvancedReports = false;

    @Column(name = "has_batch_expiry_tracking", nullable = false)
    @Builder.Default
    private boolean hasBatchExpiryTracking = false;

    @Column(name = "has_audit_logs", nullable = false)
    @Builder.Default
    private boolean hasAuditLogs = false;

    @Column(name = "has_gst_reports", nullable = false)
    @Builder.Default
    private boolean hasGstReports = false;

    @Column(name = "has_auto_replenishment", nullable = false)
    @Builder.Default
    private boolean hasAutoReplenishment = false;

    @Column(name = "has_custom_branding", nullable = false)
    @Builder.Default
    private boolean hasCustomBranding = false;

    @Column(name = "has_priority_support", nullable = false)
    @Builder.Default
    private boolean hasPrioritySupport = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}