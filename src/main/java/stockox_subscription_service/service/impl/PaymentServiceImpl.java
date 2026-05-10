package stockox_subscription_service.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;          // FIXED: correct import
import org.springframework.stereotype.Service;
import stockox_subscription_service.dto.request.CreatePaymentOrderRequest;
import stockox_subscription_service.dto.request.SubscribeRequest;
import stockox_subscription_service.dto.request.VerifyPaymentRequest;
import stockox_subscription_service.dto.response.PaymentOrderResponse;
import stockox_subscription_service.dto.response.PaymentTransactionResponse;
import stockox_subscription_service.dto.response.SubscriptionResponse;
import stockox_subscription_service.entity.PaymentTransaction;
import stockox_subscription_service.entity.Plan;
import stockox_subscription_service.entity.Subscription;
import stockox_subscription_service.enums.PaymentStatus;
import stockox_subscription_service.enums.SubscriptionStatus;
import stockox_subscription_service.exception.DuplicatePaymentException;       // NEW
import stockox_subscription_service.exception.InvalidPlanException;             // NEW
import stockox_subscription_service.exception.PaymentGatewayException;          // NEW
import stockox_subscription_service.exception.PaymentVerificationException;     // FIXED: was missing
import stockox_subscription_service.exception.ResourceNotFoundException;
import stockox_subscription_service.mapper.PaymentMapper;
import stockox_subscription_service.repository.PaymentTransactionRepository;
import stockox_subscription_service.repository.PlanRepository;
import stockox_subscription_service.service.BillingInvoiceService;
import stockox_subscription_service.service.PaymentService;
import stockox_subscription_service.service.SubscriptionService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RazorpayClient               razorpayClient;
    private final PlanRepository               planRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final SubscriptionService          subscriptionService;
    private final PaymentMapper                paymentMapper;
    private final BillingInvoiceService        billingInvoiceService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    @Value("${subscription.gst-rate:0.18}")
    private double gstRate;

    // Cached once at startup — the Razorpay secret never changes at runtime
    private SecretKeySpec razorpaySigningKey;

    @PostConstruct
    private void initRazorpaySigningKey() {
        this.razorpaySigningKey = new SecretKeySpec(
                razorpaySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    // ── Step 1: Create Razorpay order ─────────────────────────────────
    @Override
    @Transactional
    public PaymentOrderResponse createOrder(UUID tenantId, CreatePaymentOrderRequest request) {
        Plan plan = planRepository.findByTier(request.getPlanTier())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan not found: " + request.getPlanTier()));

        // Guard: free plan cannot be purchased
        if (plan.getPriceMonthly() == null
                || plan.getPriceMonthly().compareTo(BigDecimal.ZERO) == 0) {
            throw InvalidPlanException.freePlanPayment(request.getPlanTier());
        }

        // Guard: already on this plan
        Subscription existing = subscriptionService.getSubscriptionEntity(tenantId);
        if (existing.getPlan().getTier() == request.getPlanTier()
                && existing.getStatus() == SubscriptionStatus.ACTIVE) {
            throw InvalidPlanException.alreadyOnPlan(request.getPlanTier());
        }

        BigDecimal base  = plan.getPriceMonthly();
        BigDecimal gst   = base.multiply(BigDecimal.valueOf(gstRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.add(gst).setScale(2, RoundingMode.HALF_UP);
        long paise       = total.multiply(BigDecimal.valueOf(100)).longValue();

        Order razorpayOrder;
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", paise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "stockox_" + tenantId.toString().replace("-", "").substring(0, 12));
            orderRequest.put("notes", new JSONObject()
                    .put("tenantId", tenantId.toString())
                    .put("planTier", plan.getTier().name()));
            razorpayOrder = razorpayClient.orders.create(orderRequest);
            log.info("Razorpay order created: {} tenant={} plan={} amount={}",
                    razorpayOrder.get("id"), tenantId, plan.getTier(), total);
        } catch (RazorpayException e) {
            log.error("Razorpay SDK error tenant={}: {}", tenantId, e.getMessage());
            throw new PaymentGatewayException(e.getMessage());  // FIXED: proper exception
        }

        PaymentTransaction txn = PaymentTransaction.builder()
                .tenantId(tenantId)
                .subscription(existing)
                .plan(plan)
                .razorpayOrderId(razorpayOrder.get("id"))
                .baseAmount(base)
                .gstAmount(gst)
                .totalAmount(total)
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .billingPeriodStart(LocalDateTime.now())
                .billingPeriodEnd(LocalDateTime.now().plusMonths(1))
                .build();

        PaymentTransaction saved = transactionRepository.save(txn);

        return PaymentOrderResponse.builder()
                .transactionId(saved.getId())
                .razorpayOrderId(razorpayOrder.get("id"))
                .razorpayKeyId(razorpayKeyId)
                .baseAmount(base)
                .gstAmount(gst)
                .totalAmount(total)
                .currency("INR")
                .planName(plan.getDisplayName())
                .description(plan.getDisplayName() + " Plan — Monthly Subscription")
                .build();
    }

    // ── Step 2: Verify signature & activate subscription ──────────────
    @Override
    @Transactional
    public SubscriptionResponse verifyAndActivate(UUID tenantId, VerifyPaymentRequest request) {

        // Guard: null signature
        if (request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank()) {
            throw PaymentVerificationException.invalidFormat(request.getRazorpayOrderId());
        }

        // Find transaction first — gives ORDER_NOT_FOUND before signature error
        PaymentTransaction txn = transactionRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        PaymentVerificationException.orderNotFound(request.getRazorpayOrderId()));

        // Tenant ownership check
        if (!txn.getTenantId().equals(tenantId)) {
            log.error("SECURITY: tenant {} tried to claim payment of tenant {} orderId={}",
                    tenantId, txn.getTenantId(), request.getRazorpayOrderId());
            throw PaymentVerificationException.tenantMismatch(
                    request.getRazorpayOrderId(), request.getRazorpayPaymentId());
        }

        // Duplicate payment guard
        if (txn.getStatus() == PaymentStatus.SUCCESS) {
            throw new DuplicatePaymentException(
                    request.getRazorpayOrderId(), txn.getRazorpayPaymentId());
        }

        // Verify HMAC-SHA256
        verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        // Mark success
        txn.setStatus(PaymentStatus.SUCCESS);
        txn.setRazorpayPaymentId(request.getRazorpayPaymentId());
        txn.setRazorpaySignature(request.getRazorpaySignature());
        txn.setPaidAt(LocalDateTime.now());
        transactionRepository.save(txn);

        // Activate plan
        SubscribeRequest subscribeRequest = new SubscribeRequest(txn.getPlan().getTier());
        SubscriptionResponse response = subscriptionService.activatePlan(tenantId, subscribeRequest);

        log.info("Payment SUCCESS tenant={} plan={} paymentId={}",
                tenantId, txn.getPlan().getTier(), request.getRazorpayPaymentId());

        // Send invoice async — TODO: replace hardcoded email with Feign call to user-service
        billingInvoiceService.sendInvoiceEmail(txn, "admin@yourdomain.com", "Your Company");

        return response;
    }



    // ── Billing history ────────────────────────────────────────────────
    @Transactional
    @Override
    public Page<PaymentTransactionResponse> getPaymentHistory(UUID tenantId, Pageable pageable) {
        return transactionRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(paymentMapper::toResponse);
    }

    // ── HMAC-SHA256 verification ───────────────────────────────────────
    private void verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String payload  = orderId + "|" + paymentId;
            Mac mac         = Mac.getInstance("HmacSHA256");
            // Use pre-built key — avoids constructing SecretKeySpec on every verification call
            mac.init(razorpaySigningKey);
            String expected = HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

            if (!expected.equals(signature)) {
                log.error("Signature MISMATCH orderId={}", orderId);
                throw PaymentVerificationException.signatureMismatch(orderId, paymentId);
            }
            log.debug("Signature OK orderId={}", orderId);

        } catch (PaymentVerificationException e) {
            throw e;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw PaymentVerificationException.cryptoError(orderId, e.getMessage());
        } catch (Exception e) {
            throw PaymentVerificationException.cryptoError(orderId, e.getMessage());
        }
    }
}