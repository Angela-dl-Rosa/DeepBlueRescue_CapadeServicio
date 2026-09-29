package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    // -------------------------------------------------------
    // TEST 1: findByCode — caso encontrado → retorna DTO
    // -------------------------------------------------------
    @Test
    void shouldFindRescueCaseByCode() {
        // ARRANGE
        RescueCase rescueCase = buildRescueCase("RES-001", RescueStatus.ADMITTED);

        RescueCaseResponse expectedResponse = new RescueCaseResponse(
                1L, "RES-001", LocalDate.now(), "Caribbean Sea",
                RescueStatus.ADMITTED, "CTR-001", "AN-001"
        );

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase))
                .thenReturn(expectedResponse);

        // ACT
        RescueCaseResponse result = service.findByCode("RES-001");

        // ASSERT
        assertThat(result).isEqualTo(expectedResponse);
        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    // -------------------------------------------------------
    // TEST 2: findByCode — caso inexistente → ResourceNotFoundException
    // -------------------------------------------------------
    @Test
    void shouldThrowResourceNotFoundWhenCaseDoesNotExist() {
        // ARRANGE
        when(repository.findByCaseCode("RES-999"))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RES-999");

        verify(mapper, never()).toResponse(any());
    }

    // -------------------------------------------------------
    // TEST 3: findByStatus — retorna lista de DTOs
    // -------------------------------------------------------
    @Test
    void shouldFindRescueCasesByStatus() {
        // ARRANGE
        RescueCase rc1 = buildRescueCase("RES-001", RescueStatus.ADMITTED);
        RescueCase rc2 = buildRescueCase("RES-002", RescueStatus.ADMITTED);

        RescueCaseResponse r1 = new RescueCaseResponse(
                1L, "RES-001", LocalDate.now(), "Loc1",
                RescueStatus.ADMITTED, "CTR-001", "AN-001");
        RescueCaseResponse r2 = new RescueCaseResponse(
                2L, "RES-002", LocalDate.now(), "Loc2",
                RescueStatus.ADMITTED, "CTR-001", "AN-002");

        when(repository.findByStatusOrderByRescueDateAsc(RescueStatus.ADMITTED))
                .thenReturn(List.of(rc1, rc2));
        when(mapper.toResponse(rc1)).thenReturn(r1);
        when(mapper.toResponse(rc2)).thenReturn(r2);

        // ACT
        List<RescueCaseResponse> results = service.findByStatus(RescueStatus.ADMITTED);

        // ASSERT
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(r1, r2);
    }

    // -------------------------------------------------------
    // TEST 4: changeStatus — transición válida → save() ejecutado
    // -------------------------------------------------------
    @Test
    void shouldChangeStatusWhenTransitionIsValid() {
        // ARRANGE
        RescueCase rescueCase = buildRescueCase("RES-001", RescueStatus.ADMITTED);

        RescueCaseResponse expectedResponse = new RescueCaseResponse(
                1L, "RES-001", LocalDate.now(), "Caribbean Sea",
                RescueStatus.UNDER_EVALUATION, "CTR-001", "AN-001"
        );

        ChangeRescueStatusRequest request =
                new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION);

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase))
                .thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase))
                .thenReturn(expectedResponse);

        // ACT
        RescueCaseResponse result = service.changeStatus("RES-001", request);

        // ASSERT
        assertThat(result.status()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        verify(repository).save(rescueCase);
    }

    // -------------------------------------------------------
    // TEST 5: changeStatus — transición inválida → BusinessRuleException
    //         y NUNCA se llama save()
    // -------------------------------------------------------
    @Test
    void shouldThrowBusinessRuleExceptionWhenTransitionIsInvalid() {
        // ARRANGE
        RescueCase rescueCase = buildRescueCase("RES-001", RescueStatus.ADMITTED);

        ChangeRescueStatusRequest request =
                new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE);

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.changeStatus("RES-001", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ADMITTED")
                .hasMessageContaining("READY_FOR_RELEASE");

        // Lo más importante: save nunca debe llamarse
        verify(repository, never()).save(any());
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private RescueCase buildRescueCase(String caseCode, RescueStatus status) {
        RescueCase rc = new RescueCase();
        rc.setCaseCode(caseCode);
        rc.setStatus(status);
        rc.setRescueDate(LocalDate.now());
        rc.setRescueLocation("Caribbean Sea");
        return rc;
    }
}
