package stockox_subscription_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class CancelRequest {
    @Size(max = 300, message = "Reason cannot exceed 300 characters")
    private String reason;

}
