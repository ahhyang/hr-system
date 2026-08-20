package com.hr.service;

import com.hr.domain.CompanyPolicy;
import com.hr.dto.Dtos;
import com.hr.exception.NotFoundException;
import com.hr.repository.CompanyPolicyRepository;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final CompanyPolicyRepository companyPolicyRepository;
    private final SecurityUtils securityUtils;

    public List<Dtos.PolicyResponse> listPolicies() {
        return companyPolicyRepository.findByTenantIdAndActiveTrue(securityUtils.tenantId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public Dtos.PolicyResponse getPolicy(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public Dtos.PolicyResponse createPolicy(Dtos.PolicyRequest request) {
        CompanyPolicy policy = companyPolicyRepository.save(CompanyPolicy.builder()
                .tenantId(securityUtils.tenantId())
                .category(request.category())
                .title(request.title())
                .content(request.content())
                .active(true)
                .build());
        return toResponse(policy);
    }

    @Transactional
    public Dtos.PolicyResponse updatePolicy(UUID id, Dtos.PolicyRequest request) {
        CompanyPolicy policy = getEntity(id);
        policy.setCategory(request.category());
        policy.setTitle(request.title());
        policy.setContent(request.content());
        companyPolicyRepository.save(policy);
        return toResponse(policy);
    }

    @Transactional
    public void deletePolicy(UUID id) {
        CompanyPolicy policy = getEntity(id);
        policy.setActive(false);
        companyPolicyRepository.save(policy);
    }

    public List<Dtos.PolicyResponse> searchPolicies(String query) {
        String q = query != null ? query.toLowerCase(Locale.ROOT) : "";
        return companyPolicyRepository.findByTenantIdAndActiveTrue(securityUtils.tenantId()).stream()
                .filter(p -> q.isBlank()
                        || p.getTitle().toLowerCase(Locale.ROOT).contains(q)
                        || p.getContent().toLowerCase(Locale.ROOT).contains(q)
                        || p.getCategory().toLowerCase(Locale.ROOT).contains(q))
                .map(this::toResponse)
                .toList();
    }

    private CompanyPolicy getEntity(UUID id) {
        return companyPolicyRepository.findById(id)
                .filter(p -> p.getTenantId().equals(securityUtils.tenantId()))
                .orElseThrow(() -> new NotFoundException("Policy not found"));
    }

    private Dtos.PolicyResponse toResponse(CompanyPolicy p) {
        return new Dtos.PolicyResponse(p.getId(), p.getCategory(), p.getTitle(), p.getContent());
    }
}
