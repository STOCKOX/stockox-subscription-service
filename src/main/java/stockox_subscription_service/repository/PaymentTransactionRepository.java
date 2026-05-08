package stockox_subscription_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import stockox_subscription_service.entity.PaymentTransaction;
import stockox_subscription_service.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);

    Optional<PaymentTransaction> findByRazorpayPaymentId(String razorpayPaymentId);

    Page<PaymentTransaction> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<PaymentTransaction> findByTenantIdAndStatus(UUID tenantId, PaymentStatus status);

}
