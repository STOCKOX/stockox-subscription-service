package stockox_subscription_service.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync

public class AsyncConfig {
    // Spring Boot's auto-configured thread pool is sufficient for this service.
    // Override here if you need custom pool sizing in prod.
}
