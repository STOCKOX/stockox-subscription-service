package stockox_subscription_service.controller;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stockox_subscription_service.dto.request.VerifyPaymentRequest;
import stockox_subscription_service.dto.response.ApiResponse;
import stockox_subscription_service.entity.PaymentTransaction;
import stockox_subscription_service.entity.Plan;
import stockox_subscription_service.entity.Subscription;
import stockox_subscription_service.enums.PaymentStatus;
import stockox_subscription_service.enums.PlanTier;
import stockox_subscription_service.exception.ResourceNotFoundException;
import stockox_subscription_service.repository.PaymentTransactionRepository;
import stockox_subscription_service.repository.PlanRepository;
import stockox_subscription_service.repository.SubscriptionRepository;
import stockox_subscription_service.service.BillingInvoiceService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * ─────────────────────────────────────────────────────────────
 *  TESTING ONLY — sirf dev profile mein active hoga
 *  Production mein automatically disabled ho jayega
 * ─────────────────────────────────────────────────────────────
 *
 *  Purpose: ₹1 ki real Razorpay payment karke invoice email test karo.
 *
 *  Flow:
 *   1. POST /test/payment/create-order  →  razorpayOrderId + keyId milega
 *   2. Razorpay checkout se ₹1 pay karo (test card ya UPI)
 *   3. POST /test/payment/verify        →  invoice email jayegi
 */
@Slf4j
@RestController
@RequestMapping("/test/payment")
@RequiredArgsConstructor
@Profile("dev")   // ← CRITICAL: sirf dev profile mein chalega, prod mein nahi
public class TestPaymentController {

    private final RazorpayClient               razorpayClient;
    private final PlanRepository               planRepository;
    private final SubscriptionRepository       subscriptionRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final BillingInvoiceService        billingInvoiceService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    // ── 1. ₹1 ka order banao ─────────────────────────────────────────

    /**
     * POST /test/payment/create-order
     *
     * Body:
     * {
     *   "tenantId": "your-tenant-uuid",
     *   "email":    "tumhara@email.com",
     *   "name":     "Tumhara Naam"
     * }
     *
     * Response mein razorpayOrderId aur keyId milega —
     * ise HTML test page mein paste karo.
     */
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTestOrder(
            @RequestBody Map<String, String> body) {

        UUID tenantId = UUID.fromString(body.get("tenantId"));
        String email  = body.getOrDefault("email", "test@stockox.com");
        String name   = body.getOrDefault("name",  "Test User");

        // Sirf ₹1 — 100 paise — hardcoded, koi change nahi ho sakta
        long FIXED_PAISE = 100L;
        BigDecimal ONE_RUPEE = BigDecimal.ONE;

        // Plan fetch karo (PRO use karenge for invoice details)
        Plan plan = planRepository.findByTier(PlanTier.PRO)
                .orElseThrow(() -> new ResourceNotFoundException("PRO plan not found"));

        // Subscription fetch karo
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No subscription found for tenant: " + tenantId));

        // Razorpay order banao
        String razorpayOrderId;
        try {
            JSONObject req = new JSONObject();
            req.put("amount",   FIXED_PAISE);
            req.put("currency", "INR");
            req.put("receipt",  "test_" + tenantId.toString().replace("-", "").substring(0, 10));
            req.put("notes",    new JSONObject()
                    .put("purpose",  "INVOICE_EMAIL_TEST")
                    .put("tenantId", tenantId.toString())
                    .put("email",    email));

            razorpayOrderId = razorpayClient.orders.create(req).get("id");
            log.info("[TEST] ₹1 order created: {} for tenant={} email={}", razorpayOrderId, tenantId, email);

        } catch (RazorpayException e) {
            log.error("[TEST] Razorpay order failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Razorpay order creation failed: " + e.getMessage()));
        }

        // DB mein transaction save karo
        PaymentTransaction txn = PaymentTransaction.builder()
                .tenantId(tenantId)
                .subscription(sub)
                .plan(plan)
                .razorpayOrderId(razorpayOrderId)
                .baseAmount(ONE_RUPEE)
                .gstAmount(BigDecimal.ZERO)       // ₹1 test — no GST
                .totalAmount(ONE_RUPEE)
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .billingPeriodStart(LocalDateTime.now())
                .billingPeriodEnd(LocalDateTime.now().plusMonths(1))
                .build();

        transactionRepository.save(txn);

        // Response — HTML test page mein paste karna hai
        Map<String, Object> response = Map.of(
                "razorpayOrderId", razorpayOrderId,
                "razorpayKeyId",   razorpayKeyId,
                "amountPaise",     FIXED_PAISE,
                "amountDisplay",   "₹1.00 (Test only)",
                "tenantId",        tenantId.toString(),
                "email",           email,
                "name",            name,
                "note",            "Real ₹1 payment hogi — test card use karo: 4111111111111111"
        );

        return ResponseEntity.ok(ApiResponse.success("₹1 test order created. Ab HTML page se pay karo.", response));
    }

    // ── 2. Payment verify karo + invoice bhejo ────────────────────────

    /**
     * POST /test/payment/verify
     *
     * Body:
     * {
     *   "tenantId":          "your-tenant-uuid",
     *   "email":             "tumhara@email.com",
     *   "name":              "Tumhara Naam",
     *   "razorpayOrderId":   "order_XXXXXX",
     *   "razorpayPaymentId": "pay_XXXXXX",
     *   "razorpaySignature": "signature_from_razorpay"
     * }
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyTestPayment(
            @RequestBody Map<String, String> body) {

        String razorpayOrderId   = body.get("razorpayOrderId");
        String razorpayPaymentId = body.get("razorpayPaymentId");
        String razorpaySignature = body.get("razorpaySignature");
        String email             = body.getOrDefault("email", "test@stockox.com");
        String name              = body.getOrDefault("name",  "Test User");
        UUID tenantId            = UUID.fromString(body.get("tenantId"));

        // 1. Signature verify karo (same HMAC-SHA256 logic)
        verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

        // 2. Transaction DB mein update karo
        PaymentTransaction txn = transactionRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found for orderId: " + razorpayOrderId));

        txn.setStatus(PaymentStatus.SUCCESS);
        txn.setRazorpayPaymentId(razorpayPaymentId);
        txn.setRazorpaySignature(razorpaySignature);
        txn.setPaidAt(LocalDateTime.now());
        transactionRepository.save(txn);

        log.info("[TEST] Payment verified — sending invoice email to: {}", email);

        // 3. Invoice email bhejo — ye main cheez hai jo test kar rahe ho
        billingInvoiceService.sendInvoiceEmail(txn, email, name);

        log.info("[TEST] Invoice email dispatched to: {}", email);

        return ResponseEntity.ok(ApiResponse.success(
                "✅ Payment verified! Invoice email bhej di gayi: " + email,
                Map.of(
                        "status",        "SUCCESS",
                        "invoiceSentTo", email,
                        "transactionId", txn.getId().toString(),
                        "paymentId",     razorpayPaymentId,
                        "note",          "Apna email check karo — PDF invoice attached hogi"
                )
        ));
    }

    // ── 3. Direct email test — bina payment ke sirf email check karo ──

    /**
     * POST /test/payment/send-invoice-direct
     *
     * Sirf email test karna hai? Payment bhi nahi karni?
     * Ek existing successful transaction ka ID do — email ja4ayegi.
     *
     * Body:
     * {
     *   "transactionId": "existing-txn-uuid",
     *   "tenantId":      "your-tenant-uuid",
     *   "email":         "tumhara@email.com",
     *   "name":          "Tumhara Naam"
     * }
     */
    @PostMapping("/send-invoice-direct")
    public ResponseEntity<ApiResponse<String>> sendInvoiceDirect(
            @RequestBody Map<String, String> body) {

        UUID txnId    = UUID.fromString(body.get("transactionId"));
        UUID tenantId = UUID.fromString(body.get("tenantId"));
        String email  = body.getOrDefault("email", "test@stockox.com");
        String name   = body.getOrDefault("name",  "Test User");

        PaymentTransaction txn = transactionRepository.findById(txnId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + txnId));

        if (!txn.getTenantId().equals(tenantId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tenant mismatch — ye transaction tumhara nahi hai"));
        }

        billingInvoiceService.sendInvoiceEmail(txn, email, name);

        log.info("[TEST] Direct invoice email sent to: {}", email);

        return ResponseEntity.ok(ApiResponse.success(
                "Invoice email sent directly to: " + email + " (bina payment ke)", null));
    }

    // ── HMAC-SHA256 Signature verification ───────────────────────────

    private void verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload  = orderId + "|" + paymentId;
            Mac mac         = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    razorpaySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

            if (!expected.equals(signature)) {
                log.error("[TEST] Signature MISMATCH orderId={}", orderId);
                throw new IllegalArgumentException(
                        "Invalid Razorpay signature. orderId=" + orderId);
            }
            log.debug("[TEST] Signature OK for orderId={}", orderId);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Signature verification failed: " + e.getMessage(), e);
        }
    }

    @PostConstruct
    public void init() {
        log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.warn("  [TEST MODE] TestPaymentController is ACTIVE");
        log.warn("  Endpoints:");
        log.warn("  POST /test/payment/create-order");
        log.warn("  POST /test/payment/verify");
        log.warn("  POST /test/payment/send-invoice-direct");
        log.warn("  ⚠️  Ye sirf dev profile mein available hai");
        log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}