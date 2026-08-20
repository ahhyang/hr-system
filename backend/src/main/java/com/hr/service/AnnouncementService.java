package com.hr.service;

import com.hr.domain.Announcement;
import com.hr.domain.AnnouncementAcknowledgement;
import com.hr.dto.Dtos;
import com.hr.exception.NotFoundException;
import com.hr.repository.AnnouncementAcknowledgementRepository;
import com.hr.repository.AnnouncementRepository;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementAcknowledgementRepository acknowledgementRepository;
    private final SecurityUtils securityUtils;

    public List<Dtos.AnnouncementResponse> listActive() {
        UUID tenantId = securityUtils.tenantId();
        UUID employeeId = securityUtils.employeeId();
        return announcementRepository.findByTenantIdAndActiveTrueOrderByPublishedAtDesc(tenantId).stream()
                .map(a -> toResponse(a, isAcknowledged(tenantId, a.getId(), employeeId)))
                .toList();
    }

    public Dtos.AnnouncementResponse get(UUID id) {
        Announcement a = getEntity(id);
        return toResponse(a, isAcknowledged(securityUtils.tenantId(), id, securityUtils.employeeId()));
    }

    @Transactional
    public Dtos.AnnouncementResponse create(Dtos.AnnouncementRequest request) {
        Announcement a = announcementRepository.save(Announcement.builder()
                .tenantId(securityUtils.tenantId())
                .title(request.title())
                .content(request.content())
                .requiresAcknowledgement(request.requiresAcknowledgement())
                .publishedBy(securityUtils.userId())
                .publishedAt(Instant.now())
                .active(true)
                .build());
        return toResponse(a, false);
    }

    @Transactional
    public Dtos.AnnouncementResponse update(UUID id, Dtos.AnnouncementRequest request) {
        Announcement a = getEntity(id);
        a.setTitle(request.title());
        a.setContent(request.content());
        a.setRequiresAcknowledgement(request.requiresAcknowledgement());
        announcementRepository.save(a);
        return toResponse(a, isAcknowledged(securityUtils.tenantId(), id, securityUtils.employeeId()));
    }

    @Transactional
    public void delete(UUID id) {
        Announcement a = getEntity(id);
        a.setActive(false);
        announcementRepository.save(a);
    }

    @Transactional
    public Dtos.AnnouncementResponse acknowledge(UUID id) {
        UUID tenantId = securityUtils.tenantId();
        UUID employeeId = securityUtils.employeeId();
        Announcement a = getEntity(id);
        if (!a.isRequiresAcknowledgement()) {
            return toResponse(a, true);
        }
        acknowledgementRepository.findByTenantIdAndAnnouncementIdAndEmployeeId(tenantId, id, employeeId)
                .orElseGet(() -> acknowledgementRepository.save(AnnouncementAcknowledgement.builder()
                        .tenantId(tenantId)
                        .announcementId(id)
                        .employeeId(employeeId)
                        .acknowledgedAt(Instant.now())
                        .build()));
        return toResponse(a, true);
    }

    private Announcement getEntity(UUID id) {
        return announcementRepository.findById(id)
                .filter(a -> a.getTenantId().equals(securityUtils.tenantId()))
                .orElseThrow(() -> new NotFoundException("Announcement not found"));
    }

    private boolean isAcknowledged(UUID tenantId, UUID announcementId, UUID employeeId) {
        if (employeeId == null) return false;
        return acknowledgementRepository.findByTenantIdAndAnnouncementIdAndEmployeeId(tenantId, announcementId, employeeId).isPresent();
    }

    private Dtos.AnnouncementResponse toResponse(Announcement a, boolean acknowledged) {
        return new Dtos.AnnouncementResponse(a.getId(), a.getTitle(), a.getContent(),
                a.isRequiresAcknowledgement(), a.getPublishedAt(), acknowledged);
    }
}
