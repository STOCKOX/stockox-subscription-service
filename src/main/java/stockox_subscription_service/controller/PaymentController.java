package stockox_subscription_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stockox_subscription_service.dto.request.CreatePaymentOrderRequest;
import stockox_subscription_service.dto.request.VerifyPaymentRequest;
import stockox_subscription_service.dto.response.ApiResponse;
import stockox_subscription_service.dto.response.PaymentOrderResponse;
import stockox_subscription_service.dto.response.PaymentTransactionResponse;
import stockox_subscription_service.dto.response.SubscriptionResponse;
import stockox_subscription_service.helper.SubscriptionContextHelper;  // ✅ FIXED: was security package
import stockox_subscription_service.service.BillingInvoiceService;
import stockox_subscription_service.service.PaymentService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService            paymentService;
    private final BillingInvoiceService     billingInvoiceService;
    private final SubscriptionContextHelper ctx;

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        UUID tenantId = ctx.getTenantId();
        log.info("Create Razorpay order - tenant={} plan={}", tenantId, request.getPlanTier());
        PaymentOrderResponse response = paymentService.createOrder(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("Payment order created.", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        UUID tenantId = ctx.getTenantId();
        log.info("Verify payment - tenant={} orderId={}", tenantId, request.getRazorpayOrderId());
        SubscriptionResponse response = paymentService.verifyAndActivate(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success(
                "Payment successful! Your plan has been activated. Invoice sent to your email.",
                response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<PaymentTransactionResponse>>> getHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID tenantId = ctx.getTenantId();
        Page<PaymentTransactionResponse> history = paymentService.getPaymentHistory(
                tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success("Payment history fetched.", history));
    }

    @GetMapping("/{transactionId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID transactionId) {
        UUID tenantId = ctx.getTenantId();
        log.info("Invoice download - tenant={} txn={}", tenantId, transactionId);
        byte[] pdf = billingInvoiceService.generateInvoicePdf(transactionId, tenantId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stockox-invoice-" + transactionId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{transactionId}/resend-invoice")
    public ResponseEntity<ApiResponse<Void>> resendInvoice(@PathVariable UUID transactionId) {
        UUID tenantId = ctx.getTenantId();
        billingInvoiceService.resendInvoiceEmail(transactionId, tenantId);
        return ResponseEntity.ok(
                ApiResponse.success("Invoice re-sent to your registered email address."));
    }
}