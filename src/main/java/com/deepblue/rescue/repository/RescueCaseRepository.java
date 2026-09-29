package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {

    // Paso 32-A: buscar por caseCode
    Optional<RescueCase> findByCaseCode(String caseCode);

    // Paso 32-B: buscar por status ordenado por fecha ASC
    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    // Paso 32-C: buscar por código de centro
    List<RescueCase> findByRescueCenter_Code(String code);

    // Paso 37: casos posteriores a una fecha, más recientes primero
    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate date);
}
