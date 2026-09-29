package com.deepblue.rescue;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.MedicalRecordRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // PASO 47 — FLYWAY

    @Test
    void flywayMigrationsShouldBeExecuted() {

        Integer migrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '2.1', '3')",
                Integer.class
        );

        // V1 + V2 + V2.1 (check constraint) + V3 (tracking_device_code)
        assertTrue(migrations >= 3, "Se esperaban al menos 3 migraciones Flyway, se encontraron: " + migrations);
    }

    // PASO 48 — MÉTODOS HEREDADOS

    @Test
    void inheritedRepositoryMethodsShouldWork() {

        RescueCenter center = createCenter("DB-CAR");

        RescueCenter saved = rescueCenterRepository.save(center);

        assertNotNull(saved.getId());
        assertTrue(rescueCenterRepository.findById(saved.getId()).isPresent());
        assertTrue(rescueCenterRepository.existsById(saved.getId()));
        assertEquals(1, rescueCenterRepository.count());
    }

    // PASO 49 — 1:N

    @Test
    void rescueCenterShouldHaveMultipleRescueCases() {

        RescueCenter center = createCenter("DB-CAR");

        RescueCase case1 = createCase(
                "RES-001",
                LocalDate.of(2026, 8, 1),
                RescueStatus.IN_REHABILITATION,
                center
        );

        RescueCase case2 = createCase(
                "RES-002",
                LocalDate.of(2026, 8, 2),
                RescueStatus.READY_FOR_RELEASE,
                center
        );

        center.addCase(case1);
        center.addCase(case2);

        rescueCenterRepository.save(center);
        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);

        assertEquals(2, center.getRescueCases().size());
        assertSame(center, case1.getRescueCenter());
        assertSame(center, case2.getRescueCenter());
    }

    // PASO 50 — RESCUE CASE 1:1 ANIMAL

    @Test
    void rescueCaseShouldHaveOneAnimal() {

        RescueCenter center = createCenter("DB-CAR");

        RescueCase rescueCase = createCase(
                "RES-2026-001",
                LocalDate.of(2026, 8, 1),
                RescueStatus.IN_REHABILITATION,
                center
        );

        Animal animal = createAnimal(
                "AN-2026-001",
                "Green Sea Turtle",
                "Chelonia mydas"
        );

        rescueCase.assignAnimal(animal);

        rescueCenterRepository.save(center);
        rescueCaseRepository.save(rescueCase);

        assertNotNull(rescueCase.getAnimal());
        assertSame(rescueCase, animal.getRescueCase());
        assertNotNull(
                animalRepository.findByAnimalCode("AN-2026-001").orElse(null)
        );
    }

    // PASO 51 — ANIMAL 1:1 MEDICAL RECORD + CASCADE

    @Test
    void animalShouldHaveOneMedicalRecordUsingCascade() {

        RescueCenter center = createCenter("DB-CAR");

        RescueCase rescueCase = createCase(
                "RES-2026-002",
                LocalDate.of(2026, 8, 2),
                RescueStatus.IN_REHABILITATION,
                center
        );

        Animal animal = createAnimal(
                "AN-2026-002",
                "Green Sea Turtle",
                "Chelonia mydas"
        );

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setInitialWeight(new BigDecimal("28.40"));
        medicalRecord.setInitialCondition("STABLE");
        medicalRecord.setInjuries("Left front flipper injury");
        medicalRecord.setObservations("Initial medical evaluation");

        animal.setRescueCase(rescueCase);
        rescueCase.setAnimal(animal);

        animal.assignMedicalRecord(medicalRecord);

        rescueCenterRepository.save(center);
        rescueCaseRepository.save(rescueCase);
        animalRepository.save(animal);

        assertNotNull(animal.getMedicalRecord());
        assertNotNull(animal.getMedicalRecord().getId());
        assertSame(animal, medicalRecord.getAnimal());
    }

    // PASO 52 — N:M

    @Test
    void specialistShouldHaveTwoExpertiseAreas() {

        Expertise trauma = expertiseRepository
                .findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehabilitation = expertiseRepository
                .findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SP-001");
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setEmail("elena.vargas@deepblue.com");
        specialist.setActive(true);

        specialist.addExpertise(trauma);
        specialist.addExpertise(rehabilitation);

        specialistRepository.save(specialist);

        assertEquals(2, specialist.getExpertiseAreas().size());
    }

    // PASO 53 — QUERY METHOD SIMPLE

    @Test
    void shouldFindCasesByStatus() {

        RescueCenter center = createCenter("DB-CAR");

        RescueCase case1 = createCase(
                "RES-001",
                LocalDate.of(2026, 8, 1),
                RescueStatus.IN_REHABILITATION,
                center
        );

        RescueCase case2 = createCase(
                "RES-002",
                LocalDate.of(2026, 8, 2),
                RescueStatus.READY_FOR_RELEASE,
                center
        );

        RescueCase case3 = createCase(
                "RES-003",
                LocalDate.of(2026, 8, 3),
                RescueStatus.IN_REHABILITATION,
                center
        );

        rescueCenterRepository.save(center);
        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);
        rescueCaseRepository.save(case3);

        List<RescueCase> result =
                rescueCaseRepository.findByStatusOrderByRescueDateAsc(
                        RescueStatus.IN_REHABILITATION
                );

        assertEquals(2, result.size());
        assertEquals("RES-001", result.get(0).getCaseCode());
        assertEquals("RES-003", result.get(1).getCaseCode());
    }

    // PASO 54 — QUERY METHOD NAVEGANDO RELACIONES

    @Test
    void shouldFindAnimalsByRescueCenterCode() {

        RescueCenter caribbean = createCenter("DB-CAR");
        RescueCenter pacific = createCenter("DB-PAC");

        RescueCase caseCaribbean = createCase(
                "RES-CAR-001",
                LocalDate.of(2026, 8, 1),
                RescueStatus.IN_REHABILITATION,
                caribbean
        );

        RescueCase casePacific = createCase(
                "RES-PAC-001",
                LocalDate.of(2026, 8, 2),
                RescueStatus.IN_REHABILITATION,
                pacific
        );

        Animal animalCaribbean = createAnimal(
                "AN-CAR-001",
                "Green Sea Turtle",
                "Chelonia mydas"
        );

        Animal animalPacific = createAnimal(
                "AN-PAC-001",
                "Bottlenose Dolphin",
                "Tursiops truncatus"
        );

        animalCaribbean.setRescueCase(caseCaribbean);
        animalPacific.setRescueCase(casePacific);

        caseCaribbean.setAnimal(animalCaribbean);
        casePacific.setAnimal(animalPacific);

        rescueCenterRepository.save(caribbean);
        rescueCenterRepository.save(pacific);

        rescueCaseRepository.save(caseCaribbean);
        rescueCaseRepository.save(casePacific);

        animalRepository.save(animalCaribbean);
        animalRepository.save(animalPacific);

        List<Animal> result =
                animalRepository.findByRescueCase_RescueCenter_Code("DB-CAR");

        assertEquals(1, result.size());
        assertEquals("AN-CAR-001", result.get(0).getAnimalCode());
    }

    // PASO 55 — JPQL SPECIALIST

    @Test
    void shouldFindActiveSpecialistsByExpertise() {

        Expertise trauma = expertiseRepository
                .findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehabilitation = expertiseRepository
                .findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Expertise marineMammals = expertiseRepository
                .findByNameIgnoreCase("Marine Mammals")
                .orElseThrow();

        Expertise marineBirds = expertiseRepository
                .findByNameIgnoreCase("Marine Birds")
                .orElseThrow();

        Specialist elena = createSpecialist(
                "SP-001",
                "Elena",
                "Vargas",
                true
        );

        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = createSpecialist(
                "SP-002",
                "Mateo",
                "Gomez",
                true
        );

        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = createSpecialist(
                "SP-003",
                "Sofia",
                "Perez",
                true
        );

        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);

        specialistRepository.save(elena);
        specialistRepository.save(mateo);
        specialistRepository.save(sofia);

        List<Specialist> result =
                specialistRepository.findActiveByExpertise("Trauma");

        assertEquals(2, result.size());

        assertTrue(
                result.stream()
                        .anyMatch(s -> s.getProfessionalCode().equals("SP-001"))
        );

        assertTrue(
                result.stream()
                        .anyMatch(s -> s.getProfessionalCode().equals("SP-003"))
        );
    }

    // PASO 56 — TREATMENTS

    @Test
    void shouldCreateTreatments() {

        RescueCenter center = createCenter("DB-CAR");

        RescueCase rescueCase = createCase(
                "RES-TREAT-001",
                LocalDate.of(2026, 8, 1),
                RescueStatus.IN_REHABILITATION,
                center
        );

        Animal animal = createAnimal(
                "AN-TREAT-001",
                "Green Sea Turtle",
                "Chelonia mydas"
        );

        animal.setRescueCase(rescueCase);
        rescueCase.setAnimal(animal);

        Specialist elena = createSpecialist(
                "SP-TREAT-001",
                "Elena",
                "Vargas",
                true
        );

        Specialist mateo = createSpecialist(
                "SP-TREAT-002",
                "Mateo",
                "Gomez",
                true
        );

        rescueCenterRepository.save(center);
        rescueCaseRepository.save(rescueCase);
        animalRepository.save(animal);
        specialistRepository.save(elena);
        specialistRepository.save(mateo);

        Treatment treatment1 = createTreatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                TreatmentType.WOUND_CARE,
                "Wound care"
        );

        Treatment treatment2 = createTreatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 10, 10, 0),
                TreatmentType.HYDRATION,
                "Hydration"
        );

        Treatment treatment3 = createTreatment(
                animal,
                mateo,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                TreatmentType.OBSERVATION,
                "Observation"
        );

        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);
        treatmentRepository.save(treatment3);

        assertEquals(3, treatmentRepository.count());
    }

    // PASO 57 — QUERY METHOD TREATMENTS

    @Test
    void shouldFindAnimalTreatmentsChronologically() {

        RescueCenter center = createCenter("DB-CAR");

        RescueCase rescueCase = createCase(
                "RES-TREAT-002",
                LocalDate.of(2026, 8, 1),
                RescueStatus.IN_REHABILITATION,
                center
        );

        Animal animal = createAnimal(
                "AN-TREAT-002",
                "Green Sea Turtle",
                "Chelonia mydas"
        );

        animal.setRescueCase(rescueCase);
        rescueCase.setAnimal(animal);

        Specialist elena = createSpecialist(
                "SP-TREAT-003",
                "Elena",
                "Vargas",
                true
        );

        Specialist mateo = createSpecialist(
                "SP-TREAT-004",
                "Mateo",
                "Gomez",
                true
        );

        rescueCenterRepository.save(center);
        rescueCaseRepository.save(rescueCase);
        animalRepository.save(animal);
        specialistRepository.save(elena);
        specialistRepository.save(mateo);

        Treatment treatment1 = createTreatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                TreatmentType.WOUND_CARE,
                "Wound care"
        );

        Treatment treatment2 = createTreatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 10, 10, 0),
                TreatmentType.HYDRATION,
                "Hydration"
        );

        Treatment treatment3 = createTreatment(
                animal,
                mateo,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                TreatmentType.OBSERVATION,
                "Observation"
        );

        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);
        treatmentRepository.save(treatment3);

        List<Treatment> result =
                treatmentRepository.findByAnimal_IdOrderByPerformedAtAsc(
                        animal.getId()
                );

        assertEquals(3, result.size());
        assertEquals(TreatmentType.WOUND_CARE, result.get(0).getType());
        assertEquals(TreatmentType.HYDRATION, result.get(1).getType());
        assertEquals(TreatmentType.OBSERVATION, result.get(2).getType());
    }

    // PASO 58 — JPQL INTERVAL

    @Test
    void shouldFindTreatmentsBetweenDates() {

        RescueCenter center = createCenter("DB-CAR");

        RescueCase rescueCase = createCase(
                "RES-TREAT-003",
                LocalDate.of(2026, 8, 1),
                RescueStatus.IN_REHABILITATION,
                center
        );

        Animal animal = createAnimal(
                "AN-TREAT-003",
                "Green Sea Turtle",
                "Chelonia mydas"
        );

        animal.setRescueCase(rescueCase);
        rescueCase.setAnimal(animal);

        Specialist specialist = createSpecialist(
                "SP-TREAT-005",
                "Elena",
                "Vargas",
                true
        );

        rescueCenterRepository.save(center);
        rescueCaseRepository.save(rescueCase);
        animalRepository.save(animal);
        specialistRepository.save(specialist);

        treatmentRepository.save(createTreatment(
                animal,
                specialist,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                TreatmentType.WOUND_CARE,
                "Wound care"
        ));

        treatmentRepository.save(createTreatment(
                animal,
                specialist,
                LocalDateTime.of(2026, 8, 10, 10, 0),
                TreatmentType.HYDRATION,
                "Hydration"
        ));

        treatmentRepository.save(createTreatment(
                animal,
                specialist,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                TreatmentType.OBSERVATION,
                "Observation"
        ));

        List<Treatment> result =
                treatmentRepository.findByPerformedAtBetween(
                        LocalDateTime.of(2026, 8, 5, 0, 0),
                        LocalDateTime.of(2026, 8, 15, 23, 59)
                );

        assertEquals(1, result.size());
        assertEquals(
                LocalDateTime.of(2026, 8, 10, 10, 0),
                result.get(0).getPerformedAt()
        );
    }

    // PASO 59 — PROBAR UNIQUE

    @Test
    void shouldRejectDuplicatedAnimalCode() {

        RescueCenter center = createCenter("DB-CAR");

        RescueCase rescueCase1 = createCase(
                "RES-UNIQUE-001",
                LocalDate.of(2026, 8, 1),
                RescueStatus.IN_REHABILITATION,
                center
        );

        RescueCase rescueCase2 = createCase(
                "RES-UNIQUE-002",
                LocalDate.of(2026, 8, 2),
                RescueStatus.IN_REHABILITATION,
                center
        );

        Animal animal1 = createAnimal(
                "AN-100",
                "Green Sea Turtle",
                "Chelonia mydas"
        );

        Animal animal2 = createAnimal(
                "AN-100",
                "Bottlenose Dolphin",
                "Tursiops truncatus"
        );

        animal1.setRescueCase(rescueCase1);
        animal2.setRescueCase(rescueCase2);

        rescueCenterRepository.save(center);
        rescueCaseRepository.save(rescueCase1);
        rescueCaseRepository.save(rescueCase2);

        animalRepository.saveAndFlush(animal1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> animalRepository.saveAndFlush(animal2)
        );
    }

    // PASO 60 — PROBAR FOREIGN KEY

    @Test
    void shouldRejectInvalidForeignKey() {

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("""
                    INSERT INTO animals
                    (animal_code, common_name, scientific_name, sex, rescue_case_id)
                    VALUES (
                        'AN-FK',
                        'Dolphin',
                        'Tursiops truncatus',
                        'UNKNOWN',
                        999999
                    )
                    """)
        );
    }
    // PASO 61 — PROBAR CHECK

@Test
void shouldRejectInvalidRescueCaseStatus() {

    RescueCenter center = createCenter("DB-CHECK");

    rescueCenterRepository.save(center);

    assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update("""
                INSERT INTO rescue_cases
                (case_code, rescue_date, rescue_location, status, rescue_center_id)
                VALUES (
                    'RES-CHECK-001',
                    '2026-08-01',
                    'Santa Marta',
                    'INVALID_STATUS',
                    ?
                )
                """,
                center.getId()
            )
    );
}

    // PASO 37 — QUERY METHOD CON FECHAS

    @Test
    void shouldFindRescueCasesAfterDate() {

        RescueCenter center = createCenter("DB-CAR");
        rescueCenterRepository.save(center);

        RescueCase case1 = createCase("RES-DATE-001", LocalDate.of(2026, 7, 1),
                RescueStatus.ADMITTED, center);
        RescueCase case2 = createCase("RES-DATE-002", LocalDate.of(2026, 8, 15),
                RescueStatus.IN_REHABILITATION, center);
        RescueCase case3 = createCase("RES-DATE-003", LocalDate.of(2026, 9, 1),
                RescueStatus.ADMITTED, center);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);
        rescueCaseRepository.save(case3);

        List<RescueCase> result =
                rescueCaseRepository.findByRescueDateAfterOrderByRescueDateDesc(
                        LocalDate.of(2026, 8, 1)
                );

        // Solo los casos del 15-ago y 1-sep, más reciente primero
        assertEquals(2, result.size());
        assertEquals("RES-DATE-003", result.get(0).getCaseCode());
        assertEquals("RES-DATE-002", result.get(1).getCaseCode());
    }

    // PASOS 65-66 — RETO INTEGRADOR: escenario completo

    @Test
    void shouldPersistFullRescueScenario() {

        // Centro
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR-INT");
        center.setName("DeepBlue Caribbean");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        // Caso de rescate
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-100");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 18));
        rescueCase.setRescueLocation("Bahía Concha");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueCenter(center);
        rescueCaseRepository.save(rescueCase);

        // Animal
        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-100");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        animal.setRescueCase(rescueCase);
        rescueCase.setAnimal(animal);

        // Expediente médico
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setInitialWeight(new BigDecimal("27.80"));
        medicalRecord.setInitialCondition("STABLE");
        medicalRecord.setInjuries("Injury caused by fishing net");
        medicalRecord.setObservations("Possible plastic ingestion");
        animal.assignMedicalRecord(medicalRecord);

        animalRepository.save(animal);

        // Especialista y expertises
        Expertise marineReptiles = expertiseRepository
                .findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository
                .findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository
                .findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-001-INT");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena@deepblue.org");
        elena.setActive(true);
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        // Tratamientos
        Treatment t1 = createTreatment(animal, elena,
                LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
        Treatment t2 = createTreatment(animal, elena,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy");
        treatmentRepository.save(t1);
        treatmentRepository.save(t2);

        // ---- CONSULTAS (Paso 66) ----

        // Consulta 1: ¿Existe el caso?
        assertTrue(rescueCaseRepository.findByCaseCode("RES-2026-100").isPresent());

        // Consulta 2: Casos IN_REHABILITATION
        List<RescueCase> inRehab = rescueCaseRepository
                .findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertFalse(inRehab.isEmpty());

        // Consulta 3: Animales del centro DB-CAR-INT
        List<Animal> animalsCaribbean = animalRepository
                .findByRescueCase_RescueCenter_Code("DB-CAR-INT");
        assertEquals(1, animalsCaribbean.size());
        assertEquals("AN-2026-100", animalsCaribbean.get(0).getAnimalCode());

        // Consulta 4: Buscar por nombre común ignorando mayúsculas
        List<Animal> turtles = animalRepository
                .findByCommonNameContainingIgnoreCase("turtle");
        assertFalse(turtles.isEmpty());

        // Consulta 5: Especialistas con expertise Trauma
        List<Specialist> traumaSpecialists = specialistRepository
                .findActiveByExpertise("Trauma");
        assertTrue(traumaSpecialists.stream()
                .anyMatch(s -> s.getProfessionalCode().equals("SPEC-001-INT")));

        // Consulta 6: Tratamientos del animal ordenados cronológicamente
        List<Treatment> treatments = treatmentRepository
                .findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-2026-100");
        assertEquals(2, treatments.size());
        assertEquals(TreatmentType.WOUND_CARE, treatments.get(0).getType());
        assertEquals(TreatmentType.HYDRATION,  treatments.get(1).getType());

        // Consulta 7: Tratamientos por especialistas con expertise Rehabilitation
        List<Treatment> rehabTreatments = treatmentRepository
                .findBySpecialistExpertise("Rehabilitation");
        assertEquals(2, rehabTreatments.size());

        // Consulta 8: Tratamientos entre fechas
        List<Treatment> betweenDates = treatmentRepository
                .findByPerformedAtBetween(
                        LocalDateTime.of(2026, 8, 19, 0, 0),
                        LocalDateTime.of(2026, 8, 19, 23, 59)
                );
        assertEquals(1, betweenDates.size());

        // Expediente médico
        assertNotNull(animal.getMedicalRecord());
        assertEquals(new BigDecimal("27.80"), animal.getMedicalRecord().getInitialWeight());
    }

    // PASO 75-77 — RETO SIN GUÍA: animales en rehabilitación tratados por especialista con expertise X

    @Test
    void shouldFindAnimalsInRehabilitationTreatedBySpecialistWithExpertise() {

        RescueCenter center = createCenter("DB-CAR");
        rescueCenterRepository.save(center);

        RescueCase caseInRehab = createCase("RES-RETO-001", LocalDate.of(2026, 8, 1),
                RescueStatus.IN_REHABILITATION, center);
        RescueCase caseReleased = createCase("RES-RETO-002", LocalDate.of(2026, 8, 2),
                RescueStatus.RELEASED, center);

        rescueCaseRepository.save(caseInRehab);
        rescueCaseRepository.save(caseReleased);

        Animal animalInRehab = createAnimal("AN-RETO-001", "Green Sea Turtle", "Chelonia mydas");
        Animal animalReleased = createAnimal("AN-RETO-002", "Loggerhead Sea Turtle", "Caretta caretta");

        animalInRehab.setRescueCase(caseInRehab);
        caseInRehab.setAnimal(animalInRehab);
        animalReleased.setRescueCase(caseReleased);
        caseReleased.setAnimal(animalReleased);

        animalRepository.save(animalInRehab);
        animalRepository.save(animalReleased);

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();

        Specialist elena = createSpecialist("SP-RETO-001", "Elena", "Vargas", true);
        elena.addExpertise(trauma);
        specialistRepository.save(elena);

        // Tratamiento al animal EN rehabilitación (este sí debe aparecer)
        treatmentRepository.save(createTreatment(
                animalInRehab, elena,
                LocalDateTime.of(2026, 8, 5, 9, 0),
                TreatmentType.WOUND_CARE, "Wound care"
        ));
        // Tratamiento al animal RELEASED (este NO debe aparecer porque el caso no está en rehab)
        treatmentRepository.save(createTreatment(
                animalReleased, elena,
                LocalDateTime.of(2026, 8, 6, 9, 0),
                TreatmentType.OBSERVATION, "Final observation"
        ));

        List<Animal> result = animalRepository
                .findByRescueCaseStatusAndSpecialistExpertise(
                        RescueStatus.IN_REHABILITATION,
                        "Trauma"
                );

        assertEquals(1, result.size());
        assertEquals("AN-RETO-001", result.get(0).getAnimalCode());
    }

    // HELPERS

    private RescueCenter createCenter(String code) {

        RescueCenter center = new RescueCenter();

        center.setCode(code);
        center.setName(
                code.equals("DB-CAR")
                        ? "DeepBlue Caribbean Center"
                        : "DeepBlue Pacific Center"
        );
        center.setCity("Santa Marta");

        return center;
    }

    private RescueCase createCase(
            String code,
            LocalDate date,
            RescueStatus status,
            RescueCenter center
    ) {

        RescueCase rescueCase = new RescueCase();

        rescueCase.setCaseCode(code);
        rescueCase.setRescueDate(date);
        rescueCase.setRescueLocation("Santa Marta");
        rescueCase.setStatus(status);
        rescueCase.setRescueCenter(center);

        return rescueCase;
    }

    private Animal createAnimal(
            String code,
            String commonName,
            String scientificName
    ) {

        Animal animal = new Animal();

        animal.setAnimalCode(code);
        animal.setCommonName(commonName);
        animal.setScientificName(scientificName);
        animal.setSex(AnimalSex.UNKNOWN);

        return animal;
    }

    private Specialist createSpecialist(
            String professionalCode,
            String firstName,
            String lastName,
            boolean active
    ) {

        Specialist specialist = new Specialist();

        specialist.setProfessionalCode(professionalCode);
        specialist.setFirstName(firstName);
        specialist.setLastName(lastName);
        specialist.setEmail(
                professionalCode.toLowerCase() + "@deepblue.com"
        );
        specialist.setActive(active);

        return specialist;
    }

    private Treatment createTreatment(
            Animal animal,
            Specialist specialist,
            LocalDateTime performedAt,
            TreatmentType type,
            String description
    ) {

        Treatment treatment = new Treatment();

        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);
        treatment.setPerformedAt(performedAt);
        treatment.setType(type);
        treatment.setDescription(description);

        return treatment;
    }
}