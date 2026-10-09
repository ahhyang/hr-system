package com.hr.config;

import com.hr.domain.*;
import com.hr.domain.enums.AttendanceStatus;
import com.hr.domain.enums.ClaimStatus;
import com.hr.domain.enums.EmploymentStatus;
import com.hr.domain.enums.LeaveRequestStatus;
import com.hr.domain.enums.UserRole;
import com.hr.repository.*;
import com.hr.service.LeaveService;
import com.hr.service.OnboardingService;
import com.hr.service.TenantBootstrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Order(100)
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_SLUG = "pulsetech";
    private static final String DEMO_PASSWORD = "Demo123!";
    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    private static final BigDecimal SOCSO_RATE = new BigDecimal("0.005");
    private static final BigDecimal EIS_RATE = new BigDecimal("0.004");

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantBootstrapService tenantBootstrapService;
    private final LeaveService leaveService;
    private final OnboardingService onboardingService;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveApprovalStepRepository leaveApprovalStepRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ClaimCategoryRepository claimCategoryRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final ClaimItemRepository claimItemRepository;
    private final PayrollProfileRepository payrollProfileRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final AnnouncementRepository announcementRepository;
    private final OnboardingTaskRepository onboardingTaskRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (tenantRepository.findBySlug(DEMO_SLUG).isPresent()) {
            log.info("Demo tenant '{}' already exists — skipping seed", DEMO_SLUG);
            return;
        }
        seed();
        log.info("Demo data seeded for PulseTech Solutions. Login with *@pulsetech.demo / {}", DEMO_PASSWORD);
    }

    void seed() {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name("PulseTech Solutions Sdn Bhd")
                .slug(DEMO_SLUG)
                .timezone("Asia/Kuala_Lumpur")
                .build());
        UUID tenantId = tenant.getId();

        tenantBootstrapService.bootstrapTenant(tenantId);

        Map<String, UUID> departments = seedDepartments(tenantId);
        Map<String, UUID> positions = seedPositions(tenantId);

        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        User adminUser = saveUser(tenantId, "admin@pulsetech.demo", hash, UserRole.COMPANY_ADMIN);
        User hrUser = saveUser(tenantId, "hr@pulsetech.demo", hash, UserRole.HR);
        User managerUser = saveUser(tenantId, "manager@pulsetech.demo", hash, UserRole.MANAGER);

        Employee admin = saveEmployee(tenantId, adminUser.getId(), "ADM001", "Sarah", "Tan",
                "admin@pulsetech.demo", departments.get("IT"), positions.get("CEO"), null,
                LocalDate.of(2022, 1, 15), 100);
        Employee hr = saveEmployee(tenantId, hrUser.getId(), "HR001", "Mei Ling", "Wong",
                "hr@pulsetech.demo", departments.get("HR"), positions.get("HR Manager"), admin.getId(),
                LocalDate.of(2022, 3, 1), 100);
        Employee manager = saveEmployee(tenantId, managerUser.getId(), "MGR001", "Ahmad", "Rahman",
                "manager@pulsetech.demo", departments.get("Sales"), positions.get("Sales Manager"), admin.getId(),
                LocalDate.of(2022, 6, 1), 100);

        User johnUser = saveUser(tenantId, "john@pulsetech.demo", hash, UserRole.EMPLOYEE);
        User amyUser = saveUser(tenantId, "amy@pulsetech.demo", hash, UserRole.EMPLOYEE);
        User davidUser = saveUser(tenantId, "david@pulsetech.demo", hash, UserRole.EMPLOYEE);
        User lisaUser = saveUser(tenantId, "lisa@pulsetech.demo", hash, UserRole.EMPLOYEE);

        Employee john = saveEmployee(tenantId, johnUser.getId(), "EMP001", "John", "Lim",
                "john@pulsetech.demo", departments.get("IT"), positions.get("Software Engineer"), admin.getId(),
                LocalDate.of(2023, 2, 1), 100);
        Employee amy = saveEmployee(tenantId, amyUser.getId(), "EMP002", "Amy", "Chen",
                "amy@pulsetech.demo", departments.get("Sales"), positions.get("Account Executive"), manager.getId(),
                LocalDate.of(2023, 5, 15), 100);
        Employee david = saveEmployee(tenantId, davidUser.getId(), "EMP003", "David", "Kumar",
                "david@pulsetech.demo", departments.get("Finance"), positions.get("Finance Executive"), admin.getId(),
                LocalDate.of(2023, 8, 1), 100);
        Employee lisa = saveEmployee(tenantId, lisaUser.getId(), "EMP004", "Lisa", "Ng",
                "lisa@pulsetech.demo", departments.get("IT"), positions.get("Software Engineer"), john.getId(),
                LocalDate.now().minusWeeks(2), 40);

        List<Employee> staff = List.of(admin, hr, manager, john, amy, david, lisa);
        for (Employee e : staff) {
            leaveService.allocateLeaveBalances(tenantId, e.getId(), LocalDate.now().getYear());
            onboardingService.createOnboardingTasks(tenantId, e.getId());
        }
        completeLisaOnboarding(tenantId, lisa.getId());

        seedSalaries(tenantId, admin.getId(), new BigDecimal("15000"));
        seedSalaries(tenantId, hr.getId(), new BigDecimal("8000"));
        seedSalaries(tenantId, manager.getId(), new BigDecimal("9000"));
        seedSalaries(tenantId, john.getId(), new BigDecimal("6500"));
        seedSalaries(tenantId, amy.getId(), new BigDecimal("5500"));
        seedSalaries(tenantId, david.getId(), new BigDecimal("6000"));
        seedSalaries(tenantId, lisa.getId(), new BigDecimal("5000"));

        seedAttendance(tenantId, john.getId(), amy.getId(), david.getId());
        seedLeaveData(tenantId, amy.getId(), john.getId(), david.getId(),
                managerUser.getId(), adminUser.getId(), hrUser.getId());
        seedClaims(tenantId, amy.getId(), john.getId(), manager.getId());
        seedPayroll(tenantId, staff, adminUser.getId());
        seedAnnouncements(tenantId, hrUser.getId());
    }

    private Map<String, UUID> seedDepartments(UUID tenantId) {
        Map<String, UUID> map = new HashMap<>();
        map.put("IT", departmentRepository.save(Department.builder().tenantId(tenantId).name("IT").code("IT").build()).getId());
        map.put("Sales", departmentRepository.save(Department.builder().tenantId(tenantId).name("Sales").code("SALES").build()).getId());
        map.put("HR", departmentRepository.save(Department.builder().tenantId(tenantId).name("Human Resources").code("HR").build()).getId());
        map.put("Finance", departmentRepository.save(Department.builder().tenantId(tenantId).name("Finance").code("FIN").build()).getId());
        return map;
    }

    private Map<String, UUID> seedPositions(UUID tenantId) {
        Map<String, UUID> map = new HashMap<>();
        map.put("CEO", positionRepository.save(Position.builder().tenantId(tenantId).title("Chief Executive Officer").build()).getId());
        map.put("HR Manager", positionRepository.save(Position.builder().tenantId(tenantId).title("HR Manager").build()).getId());
        map.put("Sales Manager", positionRepository.save(Position.builder().tenantId(tenantId).title("Sales Manager").build()).getId());
        map.put("Software Engineer", positionRepository.save(Position.builder().tenantId(tenantId).title("Software Engineer").build()).getId());
        map.put("Account Executive", positionRepository.save(Position.builder().tenantId(tenantId).title("Account Executive").build()).getId());
        map.put("Finance Executive", positionRepository.save(Position.builder().tenantId(tenantId).title("Finance Executive").build()).getId());
        return map;
    }

    private User saveUser(UUID tenantId, String email, String hash, UserRole role) {
        return userRepository.save(User.builder()
                .tenantId(tenantId)
                .email(email)
                .passwordHash(hash)
                .role(role)
                .build());
    }

    private Employee saveEmployee(UUID tenantId, UUID userId, String code, String first, String last,
                                  String email, UUID deptId, UUID posId, UUID managerId,
                                  LocalDate joinDate, int onboardingProgress) {
        return employeeRepository.save(Employee.builder()
                .tenantId(tenantId)
                .userId(userId)
                .employeeCode(code)
                .firstName(first)
                .lastName(last)
                .email(email)
                .phone("+6012" + String.format("%07d", Math.abs(code.hashCode()) % 10_000_000))
                .departmentId(deptId)
                .positionId(posId)
                .managerId(managerId)
                .joinDate(joinDate)
                .employmentStatus(EmploymentStatus.ACTIVE)
                .bankName("Maybank")
                .bankAccountNumber("5123" + Math.abs(code.hashCode()) % 10000000)
                .onboardingProgress(onboardingProgress)
                .build());
    }

    private void completeLisaOnboarding(UUID tenantId, UUID employeeId) {
        List<OnboardingTask> tasks = onboardingTaskRepository.findByTenantIdAndEmployeeId(tenantId, employeeId);
        int complete = 0;
        for (OnboardingTask task : tasks) {
            if (complete >= 2) break;
            task.setCompleted(true);
            task.setCompletedAt(Instant.now());
            onboardingTaskRepository.save(task);
            complete++;
        }
    }

    private void seedSalaries(UUID tenantId, UUID employeeId, BigDecimal salary) {
        payrollProfileRepository.save(PayrollProfile.builder()
                .tenantId(tenantId)
                .employeeId(employeeId)
                .basicSalary(salary)
                .pcbAmount(salary.compareTo(new BigDecimal("5000")) > 0 ? new BigDecimal("150") : new BigDecimal("50"))
                .build());
    }

    private void seedAttendance(UUID tenantId, UUID johnId, UUID amyId, UUID davidId) {
        LocalDate today = LocalDate.now(KL);
        LocalDate start = today.minusWeeks(3);
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            seedDay(tenantId, johnId, date, 9, 0, 18, 0, 0, 0, AttendanceStatus.PRESENT);
            seedDay(tenantId, amyId, date, 9, 20, 18, 0, 10, 0, AttendanceStatus.LATE);
            if (date.getDayOfWeek() == DayOfWeek.WEDNESDAY) {
                seedDay(tenantId, davidId, date, 9, 0, 19, 30, 0, 90, AttendanceStatus.PRESENT);
            } else {
                seedDay(tenantId, davidId, date, 9, 5, 18, 0, 5, 0, AttendanceStatus.LATE);
            }
        }
    }

    private void seedDay(UUID tenantId, UUID employeeId, LocalDate date,
                         int inH, int inM, int outH, int outM, int lateMin, int otMin, AttendanceStatus status) {
        Instant clockIn = atTime(date, inH, inM);
        Instant clockOut = atTime(date, outH, outM);
        BigDecimal hours = BigDecimal.valueOf(outH - inH + (outM - inM) / 60.0).setScale(2, RoundingMode.HALF_UP);

        attendanceRecordRepository.save(AttendanceRecord.builder()
                .tenantId(tenantId)
                .employeeId(employeeId)
                .workDate(date)
                .clockIn(clockIn)
                .clockOut(clockOut)
                .workHours(hours)
                .lateMinutes(lateMin)
                .otMinutes(otMin)
                .status(status)
                .build());
    }

    private void seedLeaveData(UUID tenantId, UUID amyId, UUID johnId, UUID davidId,
                               UUID salesManagerId, UUID adminId, UUID hrId) {
        LeaveType annual = leaveTypeRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .filter(lt -> "ANNUAL".equals(lt.getCode())).findFirst().orElseThrow();
        LeaveType medical = leaveTypeRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .filter(lt -> "MEDICAL".equals(lt.getCode())).findFirst().orElseThrow();

        useLeaveBalance(tenantId, amyId, annual.getId(), new BigDecimal("2"));
        useLeaveBalance(tenantId, johnId, annual.getId(), new BigDecimal("1"));

        LocalDate today = LocalDate.now(KL);

        LeaveRequest amyApproved = leaveRequestRepository.save(LeaveRequest.builder()
                .tenantId(tenantId)
                .employeeId(amyId)
                .leaveTypeId(annual.getId())
                .startDate(today.minusWeeks(2))
                .endDate(today.minusWeeks(2).plusDays(1))
                .days(new BigDecimal("2"))
                .reason("Family trip to Penang")
                .status(LeaveRequestStatus.APPROVED)
                .currentStep("COMPLETED")
                .submittedAt(Instant.now().minusSeconds(86400 * 14))
                .build());
        saveLeaveSteps(tenantId, amyApproved.getId(), salesManagerId, hrId, true);

        LeaveRequest johnPending = leaveRequestRepository.save(LeaveRequest.builder()
                .tenantId(tenantId)
                .employeeId(johnId)
                .leaveTypeId(annual.getId())
                .startDate(today.plusWeeks(1))
                .endDate(today.plusWeeks(1))
                .days(new BigDecimal("1"))
                .reason("Personal errand")
                .status(LeaveRequestStatus.PENDING_MANAGER)
                .currentStep("MANAGER")
                .submittedAt(Instant.now().minusSeconds(3600))
                .build());
        leaveApprovalStepRepository.save(LeaveApprovalStep.builder()
                .tenantId(tenantId)
                .leaveRequestId(johnPending.getId())
                .stepOrder(1)
                .approverRole("MANAGER")
                .approverId(adminId)
                .status("PENDING")
                .build());
        leaveApprovalStepRepository.save(LeaveApprovalStep.builder()
                .tenantId(tenantId)
                .leaveRequestId(johnPending.getId())
                .stepOrder(2)
                .approverRole("HR")
                .status("PENDING")
                .build());

        LeaveRequest davidMedical = leaveRequestRepository.save(LeaveRequest.builder()
                .tenantId(tenantId)
                .employeeId(davidId)
                .leaveTypeId(medical.getId())
                .startDate(today.minusDays(1))
                .endDate(today.minusDays(1))
                .days(BigDecimal.ONE)
                .reason("Flu — MC attached")
                .attachmentUrl("https://example.com/mc.pdf")
                .status(LeaveRequestStatus.PENDING_HR)
                .currentStep("HR")
                .submittedAt(Instant.now().minusSeconds(7200))
                .build());
        leaveApprovalStepRepository.save(LeaveApprovalStep.builder()
                .tenantId(tenantId)
                .leaveRequestId(davidMedical.getId())
                .stepOrder(1)
                .approverRole("MANAGER")
                .approverId(adminId)
                .status("APPROVED")
                .actedAt(Instant.now().minusSeconds(3600))
                .build());
        leaveApprovalStepRepository.save(LeaveApprovalStep.builder()
                .tenantId(tenantId)
                .leaveRequestId(davidMedical.getId())
                .stepOrder(2)
                .approverRole("HR")
                .approverId(hrId)
                .status("PENDING")
                .build());
    }

    private void useLeaveBalance(UUID tenantId, UUID employeeId, UUID leaveTypeId, BigDecimal days) {
        int year = LocalDate.now().getYear();
        leaveBalanceRepository.findByTenantIdAndEmployeeIdAndLeaveTypeIdAndYear(tenantId, employeeId, leaveTypeId, year)
                .ifPresent(b -> {
                    b.setUsed(days);
                    b.setRemaining(b.getAllocated().subtract(days));
                    leaveBalanceRepository.save(b);
                });
    }

    private void saveLeaveSteps(UUID tenantId, UUID requestId, UUID managerId, UUID hrId, boolean approved) {
        leaveApprovalStepRepository.save(LeaveApprovalStep.builder()
                .tenantId(tenantId)
                .leaveRequestId(requestId)
                .stepOrder(1)
                .approverRole("MANAGER")
                .approverId(managerId)
                .status(approved ? "APPROVED" : "PENDING")
                .actedAt(approved ? Instant.now().minusSeconds(86400) : null)
                .build());
        leaveApprovalStepRepository.save(LeaveApprovalStep.builder()
                .tenantId(tenantId)
                .leaveRequestId(requestId)
                .stepOrder(2)
                .approverRole("HR")
                .approverId(hrId)
                .status(approved ? "APPROVED" : "PENDING")
                .actedAt(approved ? Instant.now().minusSeconds(43200) : null)
                .build());
    }

    private void seedClaims(UUID tenantId, UUID amyId, UUID johnId, UUID managerId) {
        ClaimCategory transport = claimCategoryRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .filter(c -> "TRANSPORT".equals(c.getCode())).findFirst().orElseThrow();
        ClaimCategory meal = claimCategoryRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .filter(c -> "MEAL".equals(c.getCode())).findFirst().orElseThrow();

        ExpenseClaim amyClaim = expenseClaimRepository.save(ExpenseClaim.builder()
                .tenantId(tenantId)
                .employeeId(amyId)
                .title("Client visit — Grab receipts")
                .totalAmount(new BigDecimal("85.50"))
                .status(ClaimStatus.APPROVED)
                .submittedAt(Instant.now().minusSeconds(86400 * 5))
                .build());
        claimItemRepository.save(ClaimItem.builder()
                .tenantId(tenantId)
                .claimId(amyClaim.getId())
                .categoryId(transport.getId())
                .expenseDate(LocalDate.now().minusDays(6))
                .amount(new BigDecimal("85.50"))
                .description("Grab to client office, KL Sentral")
                .build());

        ExpenseClaim johnClaim = expenseClaimRepository.save(ExpenseClaim.builder()
                .tenantId(tenantId)
                .employeeId(johnId)
                .title("Team lunch")
                .totalAmount(new BigDecimal("42.00"))
                .status(ClaimStatus.PENDING)
                .submittedAt(Instant.now().minusSeconds(3600))
                .build());
        claimItemRepository.save(ClaimItem.builder()
                .tenantId(tenantId)
                .claimId(johnClaim.getId())
                .categoryId(meal.getId())
                .expenseDate(LocalDate.now().minusDays(1))
                .amount(new BigDecimal("42.00"))
                .description("Sprint review lunch")
                .build());
    }

    private void seedPayroll(UUID tenantId, List<Employee> staff, UUID runBy) {
        YearMonth current = YearMonth.now(KL);
        YearMonth previous = current.minusMonths(1);
        seedPayrollMonth(tenantId, staff, runBy, previous.toString());
        seedPayrollMonth(tenantId, staff, runBy, current.toString());
    }

    private void seedPayrollMonth(UUID tenantId, List<Employee> staff, UUID runBy, String yearMonth) {
        PayrollRun run = payrollRunRepository.save(PayrollRun.builder()
                .tenantId(tenantId)
                .yearMonth(yearMonth)
                .status("COMPLETED")
                .runBy(runBy)
                .build());

        BigDecimal total = BigDecimal.ZERO;
        for (Employee employee : staff) {
            Payslip slip = createPayslip(tenantId, employee, run, yearMonth);
            total = total.add(slip.getNetSalary());
        }
        run.setTotalPayroll(total);
        payrollRunRepository.save(run);
    }

    private Payslip createPayslip(UUID tenantId, Employee employee, PayrollRun run, String yearMonth) {
        PayrollProfile profile = payrollProfileRepository.findByTenantIdAndEmployeeId(tenantId, employee.getId())
                .orElseThrow();

        BigDecimal basic = profile.getBasicSalary();
        int otMinutes = attendanceRecordRepository
                .findByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, employee.getId(),
                        YearMonth.parse(yearMonth).atDay(1),
                        YearMonth.parse(yearMonth).atEndOfMonth())
                .stream().mapToInt(AttendanceRecord::getOtMinutes).sum();

        BigDecimal otRate = basic.divide(new BigDecimal("160"), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("1.5"));
        BigDecimal overtimePay = otRate.multiply(BigDecimal.valueOf(otMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        BigDecimal gross = basic.add(overtimePay);
        BigDecimal epfEmployee = gross.multiply(profile.getEpfEmployeeRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal epfEmployer = gross.multiply(profile.getEpfEmployerRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal socso = gross.multiply(SOCSO_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal eis = gross.multiply(EIS_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pcb = profile.getPcbAmount();
        BigDecimal net = gross.subtract(epfEmployee).subtract(socso).subtract(eis).subtract(pcb);

        return payslipRepository.save(Payslip.builder()
                .tenantId(tenantId)
                .employeeId(employee.getId())
                .payrollRunId(run.getId())
                .yearMonth(yearMonth)
                .basicSalary(basic)
                .overtimePay(overtimePay)
                .epfEmployee(epfEmployee)
                .epfEmployer(epfEmployer)
                .socso(socso)
                .eis(eis)
                .pcb(pcb)
                .grossSalary(gross)
                .netSalary(net)
                .build());
    }

    private void seedAnnouncements(UUID tenantId, UUID publishedBy) {
        announcementRepository.save(Announcement.builder()
                .tenantId(tenantId)
                .title("Merdeka Day Office Closure")
                .content("The office will be closed on 31 August for Merdeka Day. Wishing everyone a safe holiday!")
                .requiresAcknowledgement(false)
                .publishedBy(publishedBy)
                .publishedAt(Instant.now().minusSeconds(86400 * 3))
                .active(true)
                .build());

        announcementRepository.save(Announcement.builder()
                .tenantId(tenantId)
                .title("Updated Expense Claim Policy")
                .content("Please submit all expense claims within 30 days with valid receipts. Claims above RM 500 require pre-approval from your manager.")
                .requiresAcknowledgement(true)
                .publishedBy(publishedBy)
                .publishedAt(Instant.now().minusSeconds(86400))
                .active(true)
                .build());
    }

    private Instant atTime(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(KL).toInstant();
    }
}
