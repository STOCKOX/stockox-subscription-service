package stockox_subscription_service.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalServiceInterceptor() {
        return template -> template.header("X-Internal-Service", "stockox-subscription-service");
    }
}