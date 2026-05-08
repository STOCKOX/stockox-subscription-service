package stockox_subscription_service.service;

import stockox_subscription_service.dto.response.PlanResponse;
import stockox_subscription_service.enums.PlanTier;

import java.util.List;
import java.util.UUID;

public interface PlanService {
    List<PlanResponse> getAllActivePlan();
    PlanResponse getPlanByTier(PlanTier tier);
    PlanResponse getPlanById(UUID planId);
}