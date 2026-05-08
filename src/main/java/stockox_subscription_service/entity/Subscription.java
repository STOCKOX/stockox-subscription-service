package stockox_subscription_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import stockox_subscription_service.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "subscriptions",
        indexes = {
                @Index(name = "idx_sub_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_sub_status", columnList = "status")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // References tenants.id from user-service (cross-service, no FK)
    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private boolean autoRenew = true;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    // Razorpay subscription ID for recurring billing
    @Column(name = "razorpay_subscription_id", length = 100)
    private String razorpaySubscriptionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Convenience helpers
    @Transient
    public boolean isExpiredOrCancelled() {
        return status == SubscriptionStatus.EXPIRED || status == SubscriptionStatus.CANCELLED;
    }

    @Transient
    public boolean isAccessAllowed() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL;
    }
}