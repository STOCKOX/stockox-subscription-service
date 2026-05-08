package stockox_subscription_service.fiegn;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import stockox_subscription_service.config.FeignConfig;

import java.util.UUID;

@FeignClient(
        name = "product-service",
        url  = "${services.product-service.url}",
        configuration = FeignConfig.class
)

public interface ProductServiceFeignClient {
    @GetMapping("/internal/products/count")
    ProductCountResponse getProductCount(@RequestHeader("X-Tenant-Id") UUID tenantId);

    record ProductCountResponse(int count) {}
}
