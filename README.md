# DeepBlue Rescue

Plataforma para la administración del rescate y rehabilitación de fauna marina.

---

## Descripción

DeepBlue Rescue permite a organizaciones marinas registrar casos de rescate de animales, llevar su historial médico, asignar especialistas y documentar los tratamientos recibidos durante el proceso de rehabilitación.

---

## Modelo de datos

```
RESCUE_CENTER  1 ──────── N  RESCUE_CASE
RESCUE_CASE    1 ──────── 1  ANIMAL
ANIMAL         1 ──────── 1  MEDICAL_RECORD
ANIMAL         1 ──────── N  TREATMENT
SPECIALIST     1 ──────── N  TREATMENT
SPECIALIST     N ──────── M  EXPERTISE
```

### Tablas principales

| Tabla | Descripción |
|---|---|
| `rescue_centers` | Centros de rescate y rehabilitación |
| `rescue_cases` | Casos de rescate (admisión, estado) |
| `animals` | Animales rescatados |
| `medical_records` | Expediente médico inicial de cada animal |
| `specialists` | Especialistas que atienden los casos |
| `expertise` | Catálogo de áreas de experiencia |
| `specialist_expertise` | Tabla asociativa N:M |
| `treatments` | Tratamientos realizados |

---

## Relaciones

- **RescueCenter 1:N RescueCase** — un centro gestiona múltiples casos; FK en `rescue_cases.rescue_center_id`
- **RescueCase 1:1 Animal** — un caso involucra exactamente un animal; FK UNIQUE en `animals.rescue_case_id`
- **Animal 1:1 MedicalRecord** — cada animal tiene un expediente; FK UNIQUE en `medical_records.animal_id`
- **Specialist N:M Expertise** — tabla intermedia `specialist_expertise` con PK compuesta
- **Animal 1:N Treatment** — un animal puede recibir múltiples tratamientos; FK en `treatments.animal_id`
- **Specialist 1:N Treatment** — un especialista realiza múltiples tratamientos; FK en `treatments.specialist_id`

---

## Tecnologías

- Java 21
- Spring Boot 4.1.x
- Spring Data JPA / Hibernate
- Flyway (migraciones de esquema)
- PostgreSQL
- Testcontainers (pruebas de integración con PostgreSQL real)
- MapStruct (capa de servicio — laboratorio 2)
- JUnit 5 + Mockito + AssertJ

---

## Cómo ejecutar la aplicación

La aplicación requiere una instancia de PostgreSQL. Configurar las variables de entorno:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/deepblue
export DB_USER=postgres
export DB_PASSWORD=postgres
```

Iniciar:

```bash
mvn spring-boot:run
```

---

## Cómo ejecutar los tests

Los tests de integración levantan automáticamente un contenedor PostgreSQL mediante Testcontainers. Solo se necesita Docker en ejecución.

```bash
# Todos los tests
mvn clean test

# Solo tests de integración de persistencia
mvn test -Dtest=PersistenceIntegrationTest

# Solo tests unitarios de servicios
mvn test -Dtest="RescueCaseServiceImplTest,TreatmentServiceImplTest,AnimalServiceImplTest"
```

Resultado esperado:

```
BUILD SUCCESS
```

---

## Flyway — gestión del esquema

Flyway ejecuta las migraciones en orden al iniciar la aplicación:

| Versión | Archivo | Contenido |
|---|---|---|
| V1 | `V1__create_schema.sql` | Creación de todas las tablas, FK, UNIQUE, CHECK e índices |
| V2 | `V2__insert_expertise_catalog.sql` | Catálogo inicial de especialidades |
| V3 | `V3__add_tracking_device_to_animal.sql` | Columna `tracking_device_code` en `animals` |

Se usa `ddl-auto: validate` — Hibernate **no** crea ni modifica tablas. Solo valida que el esquema de la BD coincida con las entidades JPA. Cualquier discrepancia produce error al arrancar.

---

## Testcontainers

`PersistenceIntegrationTest` levanta un contenedor `postgres:18-alpine` para cada ejecución de tests. Ventajas:

- pruebas contra PostgreSQL real (no H2)
- constraints reales (UNIQUE, FK, CHECK) son verificados
- entorno reproducible y aislado
- el contenedor se destruye al terminar los tests

---

## Query Methods implementados

| Repository | Método | Descripción |
|---|---|---|
| `RescueCenterRepository` | `findByCode(String)` | Busca centro por código |
| `RescueCaseRepository` | `findByCaseCode(String)` | Busca caso por código único |
| `RescueCaseRepository` | `findByStatusOrderByRescueDateAsc(RescueStatus)` | Casos por estado, ordenados |
| `RescueCaseRepository` | `findByRescueCenter_Code(String)` | Casos de un centro (navega relación) |
| `RescueCaseRepository` | `findByRescueDateAfterOrderByRescueDateDesc(LocalDate)` | Casos más recientes que una fecha |
| `AnimalRepository` | `findByAnimalCode(String)` | Animal por código único |
| `AnimalRepository` | `findByCommonNameContainingIgnoreCase(String)` | Búsqueda de nombre flexible |
| `AnimalRepository` | `findByRescueCase_Status(RescueStatus)` | Animales por estado de caso |
| `AnimalRepository` | `findByRescueCase_RescueCenter_Code(String)` | Animales de un centro (navega 3 niveles) |
| `ExpertiseRepository` | `findByNameIgnoreCase(String)` | Especialidad por nombre |
| `TreatmentRepository` | `findByAnimal_IdOrderByPerformedAtAsc(Long)` | Tratamientos de un animal (cronológico) |
| `TreatmentRepository` | `findByAnimalAnimalCodeOrderByPerformedAtAsc(String)` | Ídem por código de animal |
| `MedicalRecordRepository` | `findByAnimal_AnimalCode(String)` | Expediente por código de animal |

---

## Consultas JPQL implementadas (`@Query`)

| Repository | Método | Descripción |
|---|---|---|
| `SpecialistRepository` | `findActiveByExpertise(String)` | Especialistas activos con determinada expertise; `JOIN`, `LOWER`, `DISTINCT`, `ORDER BY` |
| `TreatmentRepository` | `findByPerformedAtBetween(LocalDateTime, LocalDateTime)` | Tratamientos en un intervalo de fechas |
| `TreatmentRepository` | `findByRescueCenterCode(String)` | Tratamientos de animales de un centro (navega 4 entidades) |
| `TreatmentRepository` | `findBySpecialistExpertise(String)` | Tratamientos por expertise del especialista (N:M) |
| `AnimalRepository` | `findByRescueCaseStatusAndSpecialistExpertise(RescueStatus, String)` | **Reto sin guía**: animales en rehabilitación tratados por especialistas con determinada expertise |
