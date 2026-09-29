package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock private AnimalRepository animalRepository;
    @Mock private SpecialistRepository specialistRepository;
    @Mock private TreatmentRepository treatmentRepository;
    @Mock private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    // -------------------------------------------------------
    // TEST 5: Tratamiento válido → save() ejecutado
    // -------------------------------------------------------
    @Test
    void shouldRegisterTreatmentSuccessfully() {
        // ARRANGE
        Animal animal = buildAnimal("AN-001", RescueStatus.IN_REHABILITATION,
                LocalDate.of(2026, 8, 20));
        Specialist specialist = buildSpecialist("SPEC-001", true);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        TreatmentResponse expectedResponse = new TreatmentResponse(
                1L, "AN-001", "SPEC-001",
                request.performedAt(),
                TreatmentType.WOUND_CARE,
                request.description()
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Treatment.class)))
                .thenReturn(expectedResponse);

        // ACT
        TreatmentResponse result = service.register(request);

        // ASSERT
        assertThat(result).isEqualTo(expectedResponse);
        verify(treatmentRepository).save(any(Treatment.class));
    }

    // -------------------------------------------------------
    // TEST 6: Especialista inactivo → BusinessRuleException
    //          y NUNCA save()
    // -------------------------------------------------------
    @Test
    void shouldThrowBusinessRuleExceptionWhenSpecialistIsInactive() {
        // ARRANGE
        Animal animal = buildAnimal("AN-001", RescueStatus.IN_REHABILITATION,
                LocalDate.of(2026, 8, 20));
        Specialist specialist = buildSpecialist("SPEC-001", false); // inactivo

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.OBSERVATION, "Test"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");

        verify(treatmentRepository, never()).save(any());
    }

    // -------------------------------------------------------
    // TEST 7: Animal con caso RELEASED → BusinessRuleException
    // -------------------------------------------------------
    @Test
    void shouldThrowBusinessRuleExceptionWhenAnimalIsReleased() {
        // ARRANGE
        Animal animal = buildAnimal("AN-001", RescueStatus.RELEASED,
                LocalDate.of(2026, 8, 20));
        Specialist specialist = buildSpecialist("SPEC-001", true);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.OBSERVATION, "Test"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RELEASED");

        verify(treatmentRepository, never()).save(any());
    }

    // -------------------------------------------------------
    // TEST adicional: Animal inexistente → ResourceNotFoundException
    // -------------------------------------------------------
    @Test
    void shouldThrowResourceNotFoundWhenAnimalDoesNotExist() {
        // ARRANGE
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-999", "SPEC-001",
                LocalDateTime.now(),
                TreatmentType.OBSERVATION, "Test"
        );

        when(animalRepository.findByAnimalCode("AN-999"))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-999");

        verify(treatmentRepository, never()).save(any());
    }

    // -------------------------------------------------------
    // TEST adicional: Fecha anterior al rescate → BusinessRuleException
    // -------------------------------------------------------
    @Test
    void shouldThrowBusinessRuleExceptionWhenTreatmentDateIsBeforeRescueDate() {
        // ARRANGE
        Animal animal = buildAnimal("AN-001", RescueStatus.IN_REHABILITATION,
                LocalDate.of(2026, 8, 20));
        Specialist specialist = buildSpecialist("SPEC-001", true);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001",
                LocalDateTime.of(2026, 8, 15, 9, 0), // ANTES del rescate
                TreatmentType.WOUND_CARE, "Test"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rescue date");

        verify(treatmentRepository, never()).save(any());
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private Animal buildAnimal(String animalCode, RescueStatus status, LocalDate rescueDate) {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setStatus(status);
        rescueCase.setRescueDate(rescueDate);
        rescueCase.setRescueLocation("Caribbean Sea");

        Animal animal = new Animal();
        animal.setAnimalCode(animalCode);
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        animal.setRescueCase(rescueCase);

        return animal;
    }

    private Specialist buildSpecialist(String professionalCode, boolean active) {
        Specialist specialist = new Specialist();
        specialist.setProfessionalCode(professionalCode);
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setEmail("elena.vargas@deepblue.org");
        specialist.setActive(active);
        return specialist;
    }
}
