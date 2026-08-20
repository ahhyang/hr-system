package com.hr.service;

import com.hr.domain.AttendanceRecord;
import com.hr.domain.AttendanceRule;
import com.hr.domain.enums.AttendanceStatus;
import com.hr.dto.Dtos;
import com.hr.exception.BadRequestException;
import com.hr.exception.NotFoundException;
import com.hr.repository.AttendanceRecordRepository;
import com.hr.repository.AttendanceRuleRepository;
import com.hr.repository.EmployeeRepository;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceRuleRepository attendanceRuleRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public Dtos.ClockResponse clockIn() {
        UUID tenantId = securityUtils.tenantId();
        UUID employeeId = requireEmployeeId();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        AttendanceRecord record = attendanceRecordRepository
                .findByTenantIdAndEmployeeIdAndWorkDate(tenantId, employeeId, today)
                .orElseGet(() -> AttendanceRecord.builder()
                        .tenantId(tenantId)
                        .employeeId(employeeId)
                        .workDate(today)
                        .build());

        if (record.getClockIn() != null) {
            throw new BadRequestException("Already clocked in today");
        }

        Instant now = Instant.now();
        record.setClockIn(now);
        AttendanceRule rule = getRule(tenantId);
        int lateMinutes = calculateLateMinutes(now, rule);
        record.setLateMinutes(lateMinutes);
        record.setStatus(lateMinutes > 0 ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        attendanceRecordRepository.save(record);

        return new Dtos.ClockResponse(record.getId(), record.getClockIn(), record.getClockOut(),
                record.getStatus().name(), lateMinutes > 0 ? "Clocked in late by " + lateMinutes + " minutes" : "Clocked in successfully");
    }

    @Transactional
    public Dtos.ClockResponse clockOut() {
        UUID tenantId = securityUtils.tenantId();
        UUID employeeId = requireEmployeeId();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        AttendanceRecord record = attendanceRecordRepository
                .findByTenantIdAndEmployeeIdAndWorkDate(tenantId, employeeId, today)
                .orElseThrow(() -> new BadRequestException("Must clock in before clocking out"));

        if (record.getClockOut() != null) {
            throw new BadRequestException("Already clocked out today");
        }

        Instant now = Instant.now();
        record.setClockOut(now);
        AttendanceRule rule = getRule(tenantId);
        BigDecimal workHours = calculateWorkHours(record.getClockIn(), now);
        record.setWorkHours(workHours);
        record.setOtMinutes(calculateOtMinutes(workHours, rule));
        attendanceRecordRepository.save(record);

        return new Dtos.ClockResponse(record.getId(), record.getClockIn(), record.getClockOut(),
                record.getStatus().name(), "Clocked out. Work hours: " + workHours);
    }

    public Dtos.AttendanceMonthResponse getMyMonthlySummary(String month) {
        return getMonthlySummary(securityUtils.employeeId(), month);
    }

    public Dtos.AttendanceMonthResponse getMonthlySummary(UUID employeeId, String month) {
        UUID tenantId = securityUtils.tenantId();
        YearMonth ym = month != null ? YearMonth.parse(month) : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Dtos.AttendanceDayResponse> days = attendanceRecordRepository
                .findByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, employeeId, start, end).stream()
                .map(this::toDayResponse)
                .toList();

        BigDecimal totalHours = days.stream()
                .map(Dtos.AttendanceDayResponse::workHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int lateCount = (int) days.stream().filter(d -> d.lateMinutes() > 0).count();
        int lateMinutes = days.stream().mapToInt(Dtos.AttendanceDayResponse::lateMinutes).sum();
        int otMinutes = days.stream().mapToInt(Dtos.AttendanceDayResponse::otMinutes).sum();

        return new Dtos.AttendanceMonthResponse(ym.toString(), totalHours, lateCount, lateMinutes, otMinutes, days);
    }

    public List<Dtos.AttendanceDayResponse> getTeamAttendance(UUID managerId, LocalDate date) {
        UUID tenantId = securityUtils.tenantId();
        LocalDate workDate = date != null ? date : LocalDate.now(ZoneOffset.UTC);
        return employeeRepository.findByTenantIdAndManagerId(tenantId, managerId).stream()
                .map(e -> attendanceRecordRepository.findByTenantIdAndEmployeeIdAndWorkDate(tenantId, e.getId(), workDate)
                        .map(this::toDayResponse)
                        .orElse(new Dtos.AttendanceDayResponse(workDate, null, null, BigDecimal.ZERO, 0, 0, AttendanceStatus.ABSENT.name())))
                .toList();
    }

    public long countPresentToday() {
        UUID tenantId = securityUtils.tenantId();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return attendanceRecordRepository.findByTenantIdAndWorkDate(tenantId, today).stream()
                .filter(r -> r.getClockIn() != null)
                .count();
    }

    public long countLateToday() {
        return attendanceRecordRepository.countLateByTenantAndDate(securityUtils.tenantId(), LocalDate.now(ZoneOffset.UTC));
    }

    public int getMonthlyLateCount(UUID employeeId) {
        YearMonth ym = YearMonth.now();
        return (int) attendanceRecordRepository
                .findByTenantIdAndEmployeeIdAndWorkDateBetween(securityUtils.tenantId(), employeeId,
                        ym.atDay(1), ym.atEndOfMonth()).stream()
                .filter(r -> r.getLateMinutes() > 0)
                .count();
    }

    public int getMonthlyOtMinutes(UUID employeeId, String yearMonth) {
        YearMonth ym = yearMonth != null ? YearMonth.parse(yearMonth) : YearMonth.now();
        return attendanceRecordRepository
                .findByTenantIdAndEmployeeIdAndWorkDateBetween(securityUtils.tenantId(), employeeId,
                        ym.atDay(1), ym.atEndOfMonth()).stream()
                .mapToInt(AttendanceRecord::getOtMinutes)
                .sum();
    }

    public BigDecimal getTodayWorkHours(UUID employeeId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return attendanceRecordRepository.findByTenantIdAndEmployeeIdAndWorkDate(securityUtils.tenantId(), employeeId, today)
                .map(AttendanceRecord::getWorkHours)
                .orElse(BigDecimal.ZERO);
    }

    public boolean isClockedInToday(UUID employeeId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return attendanceRecordRepository.findByTenantIdAndEmployeeIdAndWorkDate(securityUtils.tenantId(), employeeId, today)
                .map(r -> r.getClockIn() != null && r.getClockOut() == null)
                .orElse(false);
    }

    public Instant getClockInTimeToday(UUID employeeId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return attendanceRecordRepository.findByTenantIdAndEmployeeIdAndWorkDate(securityUtils.tenantId(), employeeId, today)
                .map(AttendanceRecord::getClockIn)
                .orElse(null);
    }

    private AttendanceRule getRule(UUID tenantId) {
        return attendanceRuleRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new NotFoundException("Attendance rules not configured"));
    }

    private int calculateLateMinutes(Instant clockIn, AttendanceRule rule) {
        ZonedDateTime zdt = clockIn.atZone(ZoneOffset.UTC);
        LocalTime clockTime = zdt.toLocalTime();
        LocalTime deadline = rule.getWorkStart().plusMinutes(rule.getGraceMinutes());
        if (clockTime.isAfter(deadline)) {
            return (int) Duration.between(rule.getWorkStart(), clockTime).toMinutes();
        }
        return 0;
    }

    private BigDecimal calculateWorkHours(Instant clockIn, Instant clockOut) {
        long minutes = Duration.between(clockIn, clockOut).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private int calculateOtMinutes(BigDecimal workHours, AttendanceRule rule) {
        BigDecimal standard = rule.getStandardHours();
        if (workHours.compareTo(standard) <= 0) return 0;
        BigDecimal otHours = workHours.subtract(standard);
        return otHours.multiply(BigDecimal.valueOf(60)).intValue();
    }

    private UUID requireEmployeeId() {
        UUID employeeId = securityUtils.employeeId();
        if (employeeId == null) throw new BadRequestException("No employee profile linked to user");
        return employeeId;
    }

    private Dtos.AttendanceDayResponse toDayResponse(AttendanceRecord r) {
        return new Dtos.AttendanceDayResponse(r.getWorkDate(), r.getClockIn(), r.getClockOut(),
                r.getWorkHours() != null ? r.getWorkHours() : BigDecimal.ZERO,
                r.getLateMinutes(), r.getOtMinutes(), r.getStatus().name());
    }
}
