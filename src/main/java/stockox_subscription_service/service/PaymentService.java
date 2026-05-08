package stockox_subscription_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;   // ✅ FIXED: was java.awt.print.Pageable (AWT — wrong!)
import stockox_subscription_service.dto.request.CreatePaymentOrderRequest;
import stockox_subscription_service.dto.request.VerifyPaymentRequest;
import stockox_subscription_service.dto.response.PaymentOrderResponse;
import stockox_subscription_service.dto.response.PaymentTransactionResponse;
import stockox_subscription_service.dto.response.SubscriptionResponse;

import java.util.UUID;

public interface PaymentService {

    /**
     * Step 1 — create a Razorpay order.
     * Returns order ID + public key for the frontend JS SDK to open checkout.
     */
    PaymentOrderResponse createOrder(UUID tenantId, CreatePaymentOrderRequest request);

    /**
     * Step 2 — verify Razorpay payment signature and activate the subscription.
     * Called after the user completes payment in the Razorpay popup.
     */
    SubscriptionResponse verifyAndActivate(UUID tenantId, VerifyPaymentRequest request);

    /** Paginated billing history for the tenant. */
    Page<PaymentTransactionResponse> getPaymentHistory(UUID tenantId, Pageable pageable);
}