package com.hr.repository;

import com.hr.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    Optional<AttendanceRecord> findByTenantIdAndEmployeeIdAndWorkDate(UUID tenantId, UUID employeeId, LocalDate workDate);
    List<AttendanceRecord> findByTenantIdAndEmployeeIdAndWorkDateBetween(UUID tenantId, UUID employeeId, LocalDate start, LocalDate end);
    List<AttendanceRecord> findByTenantIdAndWorkDate(UUID tenantId, LocalDate workDate);
    List<AttendanceRecord> findByTenantIdAndWorkDateBetween(UUID tenantId, LocalDate start, LocalDate end);
    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.tenantId = :tenantId AND a.workDate = :date AND a.lateMinutes > 0")
    long countLateByTenantAndDate(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);
}
