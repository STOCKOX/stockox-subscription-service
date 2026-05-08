package stockox_subscription_service.service;

import stockox_subscription_service.dto.response.AllLimitResponse;
import stockox_subscription_service.dto.response.LimitCheckResponse;

import java.util.UUID;

public interface LimitEnforcerService {

    /**
     * Used before creating a new product.
     * Checks whether tenant has remaining product quota.
     */
    LimitCheckResponse checkProductLimit(UUID tenantId);

    /**
     * Used before adding/inviting a new user.
     * Checks whether seat/user limit is available.
     */
    LimitCheckResponse checkUserLimit(UUID tenantId, int currentUserCount);

    /**
     * Used before creating a new warehouse.
     * Checks whether warehouse limit is reached.
     */
    LimitCheckResponse checkWarehouseLimit(UUID tenantId, int currentWarehouseCount);

    /**
     * Used before generating an invoice.
     * Checks monthly invoice creation limit.
     */
    LimitCheckResponse checkInvoiceLimit(UUID tenantId, int currentMonthInvoiceCount);

    /**
     * Used to verify access of premium features.
     * Example: PDF export, reports, barcode, etc.
     */
    LimitCheckResponse checkFeatureAccess(UUID tenantId, String featureKey);

    /**
     * Used by dashboard/frontend.
     * Returns all plan limits with current usage.
     */
    AllLimitResponse getAllLimits(UUID tenantId);
}
