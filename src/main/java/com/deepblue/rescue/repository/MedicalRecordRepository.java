package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByAnimal_AnimalCode(String animalCode);
}
