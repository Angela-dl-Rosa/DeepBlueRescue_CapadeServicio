package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;
    private final SpecialistRepository specialistRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(
            AnimalRepository animalRepository,
            SpecialistRepository specialistRepository,
            TreatmentRepository treatmentRepository,
            TreatmentMapper mapper) {
        this.animalRepository    = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository  = treatmentRepository;
        this.mapper               = mapper;
    }

    @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
        return treatmentRepository
                .findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TreatmentResponse register(CreateTreatmentRequest request) {

        // Regla 1: El animal debe existir
        Animal animal = animalRepository
                .findByAnimalCode(request.animalCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found: " + request.animalCode()
                ));

        // Regla 2: El especialista debe existir
        Specialist specialist = specialistRepository
                .findByProfessionalCode(request.specialistCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialist not found: " + request.specialistCode()
                ));

        // Regla 3: El especialista debe estar activo
        if (!Boolean.TRUE.equals(specialist.getActive())) {
            throw new BusinessRuleException(
                    "Specialist " + request.specialistCode() + " is not active."
            );
        }

        // Regla 4: El caso no puede estar RELEASED ni CLOSED
        RescueStatus caseStatus = animal.getRescueCase().getStatus();
        if (caseStatus == RescueStatus.RELEASED || caseStatus == RescueStatus.CLOSED) {
            throw new BusinessRuleException(
                    "Cannot register treatment: the rescue case status is " + caseStatus + "."
            );
        }

        // Regla 5: La fecha del tratamiento no puede ser anterior a la fecha de rescate
        LocalDateTime performedAt = request.performedAt();
        LocalDateTime rescueDateTime = animal.getRescueCase().getRescueDate().atStartOfDay();
        if (performedAt.isBefore(rescueDateTime)) {
            throw new BusinessRuleException(
                    "Treatment date (" + performedAt.toLocalDate()
                    + ") cannot be before the rescue date ("
                    + animal.getRescueCase().getRescueDate() + ")."
            );
        }

        // Crear y guardar el tratamiento
        Treatment treatment = new Treatment();
        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);
        treatment.setPerformedAt(performedAt);
        treatment.setType(request.type());
        treatment.setDescription(request.description());

        Treatment saved = treatmentRepository.save(treatment);

        return mapper.toResponse(saved);
    }
}
