package stockox_subscription_service.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stockox_subscription_service.dto.response.PlanResponse;
import stockox_subscription_service.entity.Plan;
import stockox_subscription_service.enums.PlanTier;
import stockox_subscription_service.exception.ResourceNotFoundException;
import stockox_subscription_service.mapper.PlanMapper;
import stockox_subscription_service.repository.PlanRepository;
import stockox_subscription_service.service.PlanService;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class PlanServiceImpl implements PlanService {
    private final PlanRepository planRepository;
    private final PlanMapper planMapper;


    @Override
    public List<PlanResponse> getAllActivePlan() {
        List<Plan> plans = planRepository.findByActiveTrueOrderBySortOrderAsc();
        log.debug("Returning {} active plans", plans.size());
        return planMapper.toResponseList(plans);

    }

    @Override
    public PlanResponse getPlanByTier(PlanTier tier) {
        Plan plan = planRepository.findByTier(tier)
                .orElseThrow(()-> new ResourceNotFoundException("Plan not found for tier: " + tier));
        return planMapper.toResponse(plan);
    }

    @Override
    public PlanResponse getPlanById(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(()-> new ResourceNotFoundException("Plan Not found: "+planId));
        return planMapper.toResponse(plan);
    }
}