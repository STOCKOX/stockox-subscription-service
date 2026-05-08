package stockox_subscription_service.helper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stockox_subscription_service.exception.UnauthorizedException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionContextHelper {

    private final HttpServletRequest request;

    public UUID getTenantId() {
        Object attr = request.getAttribute("tenantId");
        if (attr == null) {
            throw new UnauthorizedException("Tenant context not found. Please log in again.");
        }
        return (UUID) attr;
    }

    public UUID getUserId() {
        Object attr = request.getAttribute("userId");
        if (attr == null) {
            throw new UnauthorizedException("User context not found. Please log in again.");
        }
        return (UUID) attr;
    }
}