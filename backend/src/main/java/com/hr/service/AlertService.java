package com.hr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hr.domain.AiAlert;
import com.hr.domain.Employee;
import com.hr.domain.enums.EmploymentStatus;
import com.hr.repository.AiAlertRepository;
import com.hr.repository.AttendanceRecordRepository;
import com.hr.repository.EmployeeRepository;
import com.hr.repository.LeaveRequestRepository;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AiAlertRepository aiAlertRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public List<AiAlert> runAnalytics(UUID tenantId) {
        List<AiAlert> alerts = new ArrayList<>();
        alerts.addAll(detectAttendanceDecline(tenantId));
        alerts.addAll(detectOtSpike(tenantId));
        alerts.addAll(detectLeavePatterns(tenantId));
        return alerts;
    }

    public List<Map<String, Object>> getActiveAlerts() {
        return getActiveAlertsForTenant(securityUtils.tenantId());
    }

    public List<Map<String, Object>> getActiveAlertsForTenant(UUID tenantId) {
        return aiAlertRepository.findByTenantIdAndAcknowledgedFalseOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toMap)
                .toList();
    }

    @Transactional
    public void acknowledgeAlert(UUID alertId) {
        aiAlertRepository.findById(alertId)
                .filter(a -> a.getTenantId().equals(securityUtils.tenantId()))
                .ifPresent(a -> {
                    a.setAcknowledged(true);
                    aiAlertRepository.save(a);
                });
    }

    private List<AiAlert> detectAttendanceDecline(UUID tenantId) {
        List<AiAlert> alerts = new ArrayList<>();
        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);
        LocalDate currStart = current.atDay(1);
        LocalDate prevStart = previous.atDay(1);
        LocalDate prevEnd = previous.atEndOfMonth();

        for (Employee emp : employeeRepository.findByTenantIdAndEmploymentStatus(tenantId, EmploymentStatus.ACTIVE)) {
            long currLate = attendanceRecordRepository
                    .findByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, emp.getId(), currStart, LocalDate.now(ZoneOffset.UTC)).stream()
                    .filter(r -> r.getLateMinutes() > 0).count();
            long prevLate = attendanceRecordRepository
                    .findByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, emp.getId(), prevStart, prevEnd).stream()
                    .filter(r -> r.getLateMinutes() > 0).count();

            if (prevLate > 0 && currLate > prevLate * 1.5) {
                alerts.add(saveAlert(tenantId, "ATTENDANCE_DECLINE", "WARNING",
                        "Attendance decline: " + emp.getFullName(),
                        emp.getFullName() + " late arrivals increased from " + prevLate + " to " + currLate,
                        Map.of("employeeId", emp.getId(), "prevLate", prevLate, "currLate", currLate)));
            }
        }
        return alerts;
    }

    private List<AiAlert> detectOtSpike(UUID tenantId) {
        List<AiAlert> alerts = new ArrayList<>();
        YearMonth ym = YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = LocalDate.now(ZoneOffset.UTC);

        Map<UUID, Integer> otByEmployee = new HashMap<>();
        attendanceRecordRepository.findByTenantIdAndWorkDateBetween(tenantId, start, end).forEach(r -> {
            if (r.getOtMinutes() > 0) {
                otByEmployee.merge(r.getEmployeeId(), r.getOtMinutes(), Integer::sum);
            }
        });

        double avgOt = otByEmployee.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        for (Map.Entry<UUID, Integer> entry : otByEmployee.entrySet()) {
            if (avgOt > 0 && entry.getValue() > avgOt * 2) {
                employeeRepository.findById(entry.getKey()).ifPresent(emp -> alerts.add(saveAlert(
                        tenantId, "OT_SPIKE", "WARNING",
                        "Overtime spike: " + emp.getFullName(),
                        emp.getFullName() + " OT minutes (" + entry.getValue() + ") exceed team average",
                        Map.of("employeeId", emp.getId(), "otMinutes", entry.getValue()))));
            }
        }
        return alerts;
    }

    private List<AiAlert> detectLeavePatterns(UUID tenantId) {
        List<AiAlert> alerts = new ArrayList<>();
        leaveRequestRepository.findByTenantIdAndStatusIn(tenantId,
                List.of(com.hr.domain.enums.LeaveRequestStatus.APPROVED)).stream()
                .filter(lr -> lr.getStartDate().getDayOfWeek().getValue() == 5)
                .collect(java.util.stream.Collectors.groupingBy(com.hr.domain.LeaveRequest::getEmployeeId, java.util.stream.Collectors.counting()))
                .forEach((empId, count) -> {
                    if (count >= 3) {
                        employeeRepository.findById(empId).ifPresent(emp -> alerts.add(saveAlert(
                                tenantId, "LEAVE_PATTERN", "INFO",
                                "Leave pattern detected: " + emp.getFullName(),
                                emp.getFullName() + " frequently takes leave on Fridays (" + count + " times)",
                                Map.of("employeeId", emp.getId(), "fridayLeaves", count))));
                    }
                });
        return alerts;
    }

    private AiAlert saveAlert(UUID tenantId, String type, String severity, String title, String message, Map<String, Object> metadata) {
        try {
            return aiAlertRepository.save(AiAlert.builder()
                    .tenantId(tenantId)
                    .alertType(type)
                    .severity(severity)
                    .title(title)
                    .message(message)
                    .metadata(objectMapper.writeValueAsString(metadata))
                    .acknowledged(false)
                    .build());
        } catch (Exception e) {
            return aiAlertRepository.save(AiAlert.builder()
                    .tenantId(tenantId)
                    .alertType(type)
                    .severity(severity)
                    .title(title)
                    .message(message)
                    .acknowledged(false)
                    .build());
        }
    }

    private Map<String, Object> toMap(AiAlert alert) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", alert.getId());
        m.put("type", alert.getAlertType());
        m.put("severity", alert.getSeverity());
        m.put("title", alert.getTitle());
        m.put("message", alert.getMessage());
        m.put("createdAt", alert.getCreatedAt());
        return m;
    }
}
