package stockox_subscription_service.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import stockox_subscription_service.dto.response.SubscriptionResponse;
import stockox_subscription_service.entity.Subscription;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = PlanMapper.class
)

public interface SubscriptionMapper {
    @Mapping(target = "accessAllowed",   expression = "java(subscription.isAccessAllowed())")
    @Mapping(target = "daysUntilExpiry", expression = "java(computeDaysUntilExpiry(subscription))")
    SubscriptionResponse toResponse(Subscription subscription);

    default long computeDaysUntilExpiry(Subscription subscription) {
        if (subscription.getEndDate() == null) return -1L;
        return ChronoUnit.DAYS.between(LocalDateTime.now(), subscription.getEndDate());
    }
}
