package stockox_subscription_service.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import stockox_subscription_service.dto.response.PlanResponse;
import stockox_subscription_service.entity.Plan;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface PlanMapper {
    PlanResponse toResponse(Plan plan);

    List<PlanResponse> toResponseList(List<Plan> plans);
}
