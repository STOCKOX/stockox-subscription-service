package stockox_subscription_service.config;


import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration

public class RazorpayConfig {
    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        log.info("Initializing Razorpay client with keyId: {}...", keyId.substring(0, Math.min(8, keyId.length())));
        return new RazorpayClient(keyId, keySecret);
    }
}
