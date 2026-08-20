package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public List<Dtos.AnnouncementResponse> list() {
        return announcementService.listActive();
    }

    @GetMapping("/{id}")
    public Dtos.AnnouncementResponse get(@PathVariable UUID id) {
        return announcementService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public Dtos.AnnouncementResponse create(@Valid @RequestBody Dtos.AnnouncementRequest request) {
        return announcementService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public Dtos.AnnouncementResponse update(@PathVariable UUID id, @Valid @RequestBody Dtos.AnnouncementRequest request) {
        return announcementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public void delete(@PathVariable UUID id) {
        announcementService.delete(id);
    }

    @PostMapping("/{id}/acknowledge")
    public Dtos.AnnouncementResponse acknowledge(@PathVariable UUID id) {
        return announcementService.acknowledge(id);
    }
}
