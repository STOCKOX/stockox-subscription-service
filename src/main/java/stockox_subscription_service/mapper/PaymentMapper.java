package stockox_subscription_service.mapper;

import org.mapstruct.*;
import stockox_subscription_service.dto.response.PaymentTransactionResponse;
import stockox_subscription_service.entity.PaymentTransaction;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "planName", source = "plan.displayName")
    PaymentTransactionResponse toResponse(PaymentTransaction transaction);

    List<PaymentTransactionResponse> toResponseList(List<PaymentTransaction> transactions);
}