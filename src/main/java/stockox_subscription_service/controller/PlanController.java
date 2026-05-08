package stockox_subscription_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import stockox_subscription_service.dto.response.ApiResponse;
import stockox_subscription_service.dto.response.PlanResponse;
import stockox_subscription_service.service.PlanService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor

public class PlanController {
    private final PlanService planService;


    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getAllPlans() {
        List<PlanResponse> plans = planService.getAllActivePlan();
        return ResponseEntity.ok(ApiResponse.success("Plans fetched successfully.", plans));
    }
}
