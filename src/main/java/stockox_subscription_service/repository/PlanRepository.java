package stockox_subscription_service.repository;

import stockox_subscription_service.entity.Plan;
import stockox_subscription_service.enums.PlanTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByTier(PlanTier tier);

    List<Plan> findByActiveTrueOrderBySortOrderAsc();

    boolean existsByTier(PlanTier tier);
}