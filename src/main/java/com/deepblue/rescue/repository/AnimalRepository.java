package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    // Paso 34-A: buscar por animalCode
    Optional<Animal> findByAnimalCode(String animalCode);

    // Paso 34-B: buscar por nombre común (contiene, ignora mayúsculas)
    List<Animal> findByCommonNameContainingIgnoreCase(String commonName);

    // Paso 35: animales según status de su rescueCase
    List<Animal> findByRescueCase_Status(RescueStatus status);

    // Paso 36: animales de un centro específico (navega 3 niveles)
    List<Animal> findByRescueCase_RescueCenter_Code(String centerCode);

    // Paso 75-77: RETO SIN GUÍA
    // Animales en rehabilitación que hayan recibido al menos un tratamiento
    // realizado por un especialista con determinada expertise
    @Query("""
        select distinct a
        from Animal a
        join a.rescueCase rc
        join a.treatments t
        join t.specialist s
        join s.expertiseAreas e
        where rc.status = :status
          and lower(e.name) = lower(:expertiseName)
        """)
    List<Animal> findByRescueCaseStatusAndSpecialistExpertise(
            @Param("status") RescueStatus status,
            @Param("expertiseName") String expertiseName);
}
