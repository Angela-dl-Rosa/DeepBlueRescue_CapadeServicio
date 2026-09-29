package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock private AnimalRepository animalRepository;
    @Mock private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldFindAnimalByCode() {
        Animal animal = buildAnimal("AN-001", RescueStatus.IN_REHABILITATION);
        AnimalResponse expected = new AnimalResponse(
                1L, "AN-001", "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.FEMALE, "RES-001", RescueStatus.IN_REHABILITATION);

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(mapper.toResponse(animal)).thenReturn(expected);

        AnimalResponse result = service.findByCode("AN-001");

        assertThat(result).isEqualTo(expected);
        verify(animalRepository).findByAnimalCode("AN-001");
    }

    @Test
    void shouldThrowResourceNotFoundWhenAnimalDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-999");
    }

    @Test
    void shouldReturnTrueWhenAnimalCanReceiveTreatment() {
        Animal animal = buildAnimal("AN-001", RescueStatus.IN_REHABILITATION);
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        boolean result = service.canReceiveTreatment("AN-001");

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueWhenAnimalIsUnderEvaluation() {
        Animal animal = buildAnimal("AN-001", RescueStatus.UNDER_EVALUATION);
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-001")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenAnimalIsReleased() {
        Animal animal = buildAnimal("AN-001", RescueStatus.RELEASED);
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-001")).isFalse();
    }

    @Test
    void shouldFindAnimalsInRehabilitation() {
        Animal a1 = buildAnimal("AN-001", RescueStatus.IN_REHABILITATION);
        Animal a2 = buildAnimal("AN-002", RescueStatus.IN_REHABILITATION);

        AnimalResponse r1 = new AnimalResponse(1L, "AN-001", "Green Sea Turtle",
                "Chelonia mydas", AnimalSex.FEMALE, "RES-001", RescueStatus.IN_REHABILITATION);
        AnimalResponse r2 = new AnimalResponse(2L, "AN-002", "Loggerhead",
                "Caretta caretta", AnimalSex.MALE, "RES-002", RescueStatus.IN_REHABILITATION);

        when(animalRepository.findByRescueCase_Status(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(a1, a2));
        when(mapper.toResponse(a1)).thenReturn(r1);
        when(mapper.toResponse(a2)).thenReturn(r2);

        List<AnimalResponse> results = service.findAnimalsInRehabilitation();

        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(r1, r2);
    }

    private Animal buildAnimal(String code, RescueStatus status) {
        RescueCase rc = new RescueCase();
        rc.setCaseCode("RES-001");
        rc.setStatus(status);
        rc.setRescueDate(LocalDate.of(2026, 8, 20));
        rc.setRescueLocation("Caribbean Sea");

        Animal a = new Animal();
        a.setAnimalCode(code);
        a.setCommonName("Green Sea Turtle");
        a.setScientificName("Chelonia mydas");
        a.setSex(AnimalSex.FEMALE);
        a.setRescueCase(rc);
        return a;
    }
}
