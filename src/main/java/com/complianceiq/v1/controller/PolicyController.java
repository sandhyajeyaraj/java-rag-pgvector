package com.complianceiq.v1.controller;

import com.complianceiq.common.model.ApiResponse;
import com.complianceiq.v1.model.Policy;
import com.complianceiq.v1.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Policy>>> getAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {

        List<Policy> policies;
        if (keyword != null) {
            policies = policyService.searchPolicies(keyword);
        } else if (category != null) {
            policies = policyService.getPoliciesByCategory(category);
        } else {
            policies = policyService.getAllPolicies();
        }
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Policy>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(policyService.getPolicyById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Policy>> create(@RequestBody Policy policy) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(policyService.createPolicy(policy)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Policy>> update(@PathVariable Long id, @RequestBody Policy policy) {
        return ResponseEntity.ok(ApiResponse.success(policyService.updatePolicy(id, policy)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
