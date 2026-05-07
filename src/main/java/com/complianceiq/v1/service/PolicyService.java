package com.complianceiq.v1.service;

import com.complianceiq.v1.model.Policy;
import com.complianceiq.v1.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;

    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    public Policy getPolicyById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found with id: " + id));
    }

    @Transactional
    public Policy createPolicy(Policy policy) {
        return policyRepository.save(policy);
    }

    @Transactional
    public Policy updatePolicy(Long id, Policy updated) {
        Policy existing = getPolicyById(id);
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setCategory(updated.getCategory());
        existing.setStatus(updated.getStatus());
        existing.setEffectiveDate(updated.getEffectiveDate());
        return policyRepository.save(existing);
    }

    @Transactional
    public void deletePolicy(Long id) {
        if (!policyRepository.existsById(id)) {
            throw new EntityNotFoundException("Policy not found with id: " + id);
        }
        policyRepository.deleteById(id);
    }

    public List<Policy> searchPolicies(String keyword) {
        return policyRepository.searchByKeyword(keyword);
    }

    public List<Policy> getPoliciesByCategory(String category) {
        return policyRepository.findByCategory(category);
    }
}
