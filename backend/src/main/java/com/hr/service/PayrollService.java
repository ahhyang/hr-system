package com.hr.service;

import com.hr.domain.*;
import com.hr.domain.enums.EmploymentStatus;
import com.hr.dto.Dtos;
import com.hr.exception.BadRequestException;
import com.hr.exception.NotFoundException;
import com.hr.repository.*;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final BigDecimal SOCSO_RATE = new BigDecimal("0.005");
    private static final BigDecimal EIS_EMPLOYEE_RATE = new BigDecimal("0.002");
    private static final BigDecimal EIS_EMPLOYER_RATE = new BigDecimal("0.002");

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final PayrollProfileRepository payrollProfileRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceService attendanceService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    @Transactional
    public PayrollRun generatePayrollRun(String yearMonth) {
        UUID tenantId = securityUtils.tenantId();
        String ym = yearMonth != null ? yearMonth : YearMonth.now().toString();

        payrollRunRepository.findByTenantIdAndYearMonth(tenantId, ym).ifPresent(r -> {
            throw new BadRequestException("Payroll run already exists for " + ym);
        });

        PayrollRun run = payrollRunRepository.save(PayrollRun.builder()
                .tenantId(tenantId)
                .yearMonth(ym)
                .status("COMPLETED")
                .runBy(securityUtils.userId())
                .build());

        BigDecimal totalPayroll = BigDecimal.ZERO;
        for (Employee employee : employeeRepository.findByTenantIdAndEmploymentStatus(tenantId, EmploymentStatus.ACTIVE)) {
            Payslip payslip = generatePayslip(tenantId, employee, run, ym);
            totalPayroll = totalPayroll.add(payslip.getNetSalary());
        }

        run.setTotalPayroll(totalPayroll);
        payrollRunRepository.save(run);
        auditService.log(tenantId, securityUtils.userId(), "PAYROLL_RUN", "PayrollRun", run.getId(), null, ym);
        return run;
    }

    private Payslip generatePayslip(UUID tenantId, Employee employee, PayrollRun run, String yearMonth) {
        PayrollProfile profile = payrollProfileRepository.findByTenantIdAndEmployeeId(tenantId, employee.getId())
                .orElseGet(() -> payrollProfileRepository.save(PayrollProfile.builder()
                        .tenantId(tenantId)
                        .employeeId(employee.getId())
                        .basicSalary(new BigDecimal("5000"))
                        .build()));

        BigDecimal basicSalary = profile.getBasicSalary();
        BigDecimal allowanceTotal = BigDecimal.ZERO;
        int otMinutes = attendanceService.getMonthlyOtMinutes(employee.getId(), yearMonth);
        BigDecimal otRate = basicSalary.divide(new BigDecimal("160"), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("1.5"));
        BigDecimal overtimePay = otRate.multiply(BigDecimal.valueOf(otMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        BigDecimal bonus = BigDecimal.ZERO;

        BigDecimal gross = basicSalary.add(allowanceTotal).add(overtimePay).add(bonus);
        BigDecimal epfEmployee = gross.multiply(profile.getEpfEmployeeRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal epfEmployer = gross.multiply(profile.getEpfEmployerRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal socso = gross.multiply(SOCSO_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal eis = profile.isEisEnabled()
                ? gross.multiply(EIS_EMPLOYEE_RATE.add(EIS_EMPLOYER_RATE)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal pcb = profile.getPcbAmount();
        BigDecimal netSalary = gross.subtract(epfEmployee).subtract(socso).subtract(eis).subtract(pcb);

        return payslipRepository.save(Payslip.builder()
                .tenantId(tenantId)
                .employeeId(employee.getId())
                .payrollRunId(run.getId())
                .yearMonth(yearMonth)
                .basicSalary(basicSalary)
                .allowanceTotal(allowanceTotal)
                .overtimePay(overtimePay)
                .bonus(bonus)
                .epfEmployee(epfEmployee)
                .epfEmployer(epfEmployer)
                .socso(socso)
                .eis(eis)
                .pcb(pcb)
                .grossSalary(gross)
                .netSalary(netSalary)
                .build());
    }

    public List<Dtos.PayslipResponse> myPayslips() {
        UUID employeeId = securityUtils.employeeId();
        if (employeeId == null) throw new BadRequestException("No employee profile linked to user");
        return payslipRepository.findByTenantIdAndEmployeeIdOrderByYearMonthDesc(securityUtils.tenantId(), employeeId)
                .stream().map(this::toResponse).toList();
    }

    public Dtos.PayslipResponse getLatestPayslip(UUID employeeId) {
        return payslipRepository.findByTenantIdAndEmployeeIdOrderByYearMonthDesc(securityUtils.tenantId(), employeeId).stream()
                .findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    public List<Dtos.PayslipResponse> getPayslipsForRun(UUID runId) {
        PayrollRun run = payrollRunRepository.findById(runId)
                .filter(r -> r.getTenantId().equals(securityUtils.tenantId()))
                .orElseThrow(() -> new NotFoundException("Payroll run not found"));
        return payslipRepository.findAll().stream()
                .filter(p -> p.getTenantId().equals(securityUtils.tenantId()) && p.getPayrollRunId().equals(run.getId()))
                .map(this::toResponse)
                .toList();
    }

    public List<PayrollRun> listRuns() {
        return payrollRunRepository.findByTenantIdOrderByYearMonthDesc(securityUtils.tenantId());
    }

    public BigDecimal getTotalPayrollForMonth(String yearMonth) {
        String ym = yearMonth != null ? yearMonth : YearMonth.now().toString();
        return payrollRunRepository.findByTenantIdAndYearMonth(securityUtils.tenantId(), ym)
                .map(PayrollRun::getTotalPayroll)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getTotalOvertimeForMonth(String yearMonth) {
        String ym = yearMonth != null ? yearMonth : YearMonth.now().toString();
        return payslipRepository.findAll().stream()
                .filter(p -> p.getTenantId().equals(securityUtils.tenantId()) && ym.equals(p.getYearMonth()))
                .map(Payslip::getOvertimePay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public PayrollProfile upsertProfile(UUID employeeId, PayrollProfile profile) {
        UUID tenantId = securityUtils.tenantId();
        return payrollProfileRepository.findByTenantIdAndEmployeeId(tenantId, employeeId)
                .map(existing -> {
                    existing.setBasicSalary(profile.getBasicSalary());
                    existing.setAllowances(profile.getAllowances());
                    existing.setEpfEmployeeRate(profile.getEpfEmployeeRate());
                    existing.setEpfEmployerRate(profile.getEpfEmployerRate());
                    existing.setSocsoCategory(profile.getSocsoCategory());
                    existing.setEisEnabled(profile.isEisEnabled());
                    existing.setPcbAmount(profile.getPcbAmount());
                    return payrollProfileRepository.save(existing);
                })
                .orElseGet(() -> payrollProfileRepository.save(PayrollProfile.builder()
                        .tenantId(tenantId)
                        .employeeId(employeeId)
                        .basicSalary(profile.getBasicSalary())
                        .epfEmployeeRate(profile.getEpfEmployeeRate() != null ? profile.getEpfEmployeeRate() : new BigDecimal("0.11"))
                        .epfEmployerRate(profile.getEpfEmployerRate() != null ? profile.getEpfEmployerRate() : new BigDecimal("0.12"))
                        .pcbAmount(profile.getPcbAmount() != null ? profile.getPcbAmount() : BigDecimal.ZERO)
                        .build()));
    }

    private Dtos.PayslipResponse toResponse(Payslip p) {
        return new Dtos.PayslipResponse(p.getId(), p.getYearMonth(), p.getBasicSalary(), p.getAllowanceTotal(),
                p.getOvertimePay(), p.getBonus(), p.getEpfEmployee(), p.getSocso(), p.getEis(), p.getPcb(), p.getNetSalary());
    }
}
