package stockox_subscription_service.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stockox_subscription_service.entity.Subscription;
import stockox_subscription_service.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByTenantId(UUID tenantId);

    boolean existsByTenantId(UUID tenantId);

    // Used by expiry scheduler
    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDateTime cutoff);

    // Used by expiry scheduler for trial end
    List<Subscription> findByStatusAndTrialEndDateBefore(SubscriptionStatus status, LocalDateTime cutoff);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.endDate BETWEEN :from AND :to")
    List<Subscription> findExpiringSoon(
            @Param("status") SubscriptionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to

    );
}
