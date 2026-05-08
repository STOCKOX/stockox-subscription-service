package stockox_subscription_service.service;

import stockox_subscription_service.entity.PaymentTransaction;

import java.util.UUID;

public interface BillingInvoiceService {

    /**
     * Generates a PDF invoice for the given transaction and emails it
     * to the tenant's registered email address.
     * Called automatically by PaymentServiceImpl after successful payment.
     *
     * @param transaction  the completed PaymentTransaction entity
     * @param tenantEmail  email address to send the invoice to
     * @param tenantName   company name printed on the invoice header
     */
    void sendInvoiceEmail(PaymentTransaction transaction, String tenantEmail, String tenantName);

    /**
     * Generates a PDF invoice as raw bytes — used by the download endpoint.
     * Validates that the transaction belongs to the requesting tenant.
     *
     * @param transactionId  UUID of the PaymentTransaction
     * @param tenantId       must match transaction.tenantId (security check)
     * @return PDF bytes ready to stream as application/pdf
     */
    byte[] generateInvoicePdf(UUID transactionId, UUID tenantId);

    /**
     * Re-sends an existing invoice email for an already-successful transaction.
     * Useful when user reports not receiving the original email.
     */
    void resendInvoiceEmail(UUID transactionId, UUID tenantId);
}