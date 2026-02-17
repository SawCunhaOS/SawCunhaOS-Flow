
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Organization
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.organization.application.usecase.position;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.application.dto.UpdatePositionDTO;
import br.com.sawcunhaos.organization.application.mapper.position.PositionMapper;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.department.Position;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import br.com.sawcunhaos.organization.domain.service.department.PositionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePositionUseCase Tests")
class UpdatePositionUseCaseTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionDomainService positionDomainService;

    @Mock
    private PositionMapper positionMapper;

    @InjectMocks
    private UpdatePositionUseCase updatePositionUseCase;

    private UpdatePositionDTO validUpdateDTO;
    private Position mappedPosition;
    private Position existingPosition;

    @BeforeEach
    void setUp() {
        validUpdateDTO = UpdatePositionDTO.builder()
                .positionId(1L)
                .code("DEV-SENIOR")
                .description("Senior Developer Position")
                .departmentId(1L)
                .build();

        mappedPosition = Position.builder()
                .id(1L)
                .code("DEV-SENIOR")
                .description("Senior Developer Position")
                .build();

        existingPosition = Position.builder()
                .id(1L)
                .code("DEV")
                .description("Developer Position")
                .build();
    }

    @Test
    @DisplayName("Should update position successfully with valid data")
    void shouldUpdatePositionSuccessfullyWithValidData() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(validUpdateDTO.getPositionId());
        doNothing().when(positionDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(positionDomainService).validatePositionCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getPositionId());
        when(positionMapper.toPosition(validUpdateDTO)).thenReturn(mappedPosition);
        when(positionRepository.findById(1L)).thenReturn(Optional.of(existingPosition));

        // When
        Void result = updatePositionUseCase.execute(validUpdateDTO);

        // Then
        assertThat(result).isNull();
        verify(positionDomainService, times(1)).validatePositionExistsValidation(validUpdateDTO.getPositionId());
        verify(positionDomainService, times(1)).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        verify(positionDomainService, times(1)).validatePositionCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getPositionId());
        verify(positionRepository, times(1)).update(any(Position.class));
    }

    @Test
    @DisplayName("Should throw exception when position does not exist")
    void shouldThrowExceptionWhenPositionDoesNotExist() {
        // Given
        doThrow(new ScosException(ExceptionCodeError.SCOS_POSITION_001))
                .when(positionDomainService)
                .validatePositionExistsValidation(validUpdateDTO.getPositionId());

        // When / Then
        assertThatThrownBy(() -> updatePositionUseCase.execute(validUpdateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_001.getCode());

        verify(positionDomainService, times(1)).validatePositionExistsValidation(validUpdateDTO.getPositionId());
        verify(positionRepository, never()).findById(anyLong());
        verify(positionRepository, never()).update((Position) any());
    }

    @Test
    @DisplayName("Should throw exception when department does not exist")
    void shouldThrowExceptionWhenDepartmentDoesNotExist() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(validUpdateDTO.getPositionId());
        doThrow(new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_001))
                .when(positionDomainService)
                .validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());

        // When / Then
        assertThatThrownBy(() -> updatePositionUseCase.execute(validUpdateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_001.getCode());

        verify(positionDomainService, times(1)).validatePositionExistsValidation(validUpdateDTO.getPositionId());
        verify(positionDomainService, times(1)).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        verify(positionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when position code is duplicate")
    void shouldThrowExceptionWhenPositionCodeIsDuplicate() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(validUpdateDTO.getPositionId());
        doNothing().when(positionDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doThrow(new ScosException(ExceptionCodeError.SCOS_POSITION_002))
                .when(positionDomainService)
                .validatePositionCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getPositionId());

        // When / Then
        assertThatThrownBy(() -> updatePositionUseCase.execute(validUpdateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_002.getCode());

        verify(positionDomainService, times(1)).validatePositionExistsValidation(validUpdateDTO.getPositionId());
        verify(positionDomainService, times(1)).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        verify(positionDomainService, times(1)).validatePositionCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getPositionId());
        verify(positionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should update position fields correctly")
    void shouldUpdatePositionFieldsCorrectly() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(validUpdateDTO.getPositionId());
        doNothing().when(positionDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(positionDomainService).validatePositionCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getPositionId());
        when(positionMapper.toPosition(validUpdateDTO)).thenReturn(mappedPosition);
        when(positionRepository.findById(1L)).thenReturn(Optional.of(existingPosition));

        // When
        updatePositionUseCase.execute(validUpdateDTO);

        // Then
        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).update(captor.capture());

        Position updatedPosition = captor.getValue();
        assertThat(updatedPosition.getCode()).isEqualTo("DEV-SENIOR");
        assertThat(updatedPosition.getDescription()).isEqualTo("Senior Developer Position");
    }

    @Test
    @DisplayName("Should handle position not found on findById")
    void shouldHandlePositionNotFoundOnFindById() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(validUpdateDTO.getPositionId());
        doNothing().when(positionDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(positionDomainService).validatePositionCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getPositionId());
        when(positionMapper.toPosition(validUpdateDTO)).thenReturn(mappedPosition);
        when(positionRepository.findById(1L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> updatePositionUseCase.execute(validUpdateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_001.getCode());

        verify(positionRepository, never()).update((Position) any());
    }

}

