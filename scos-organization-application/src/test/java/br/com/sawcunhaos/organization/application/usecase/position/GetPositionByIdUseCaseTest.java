
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
import br.com.sawcunhaos.organization.application.dto.PositionDTO;
import br.com.sawcunhaos.organization.application.mapper.position.PositionMapper;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.department.Position;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import br.com.sawcunhaos.organization.domain.service.department.PositionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPositionByIdUseCase Tests")
class GetPositionByIdUseCaseTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionDomainService positionDomainService;

    @Mock
    private PositionMapper positionMapper;

    @InjectMocks
    private GetPositionByIdUseCase getPositionByIdUseCase;

    private Position position;
    private PositionDTO positionDTO;

    @BeforeEach
    void setUp() {
        position = Position.builder()
                .id(1L)
                .code("DEV")
                .description("Developer Position")
                .build();

        positionDTO = PositionDTO.builder()
                .id(1L)
                .code("DEV")
                .description("Developer Position")
                .departmentId(1L)
                .build();
    }

    @Test
    @DisplayName("Should get position by id successfully")
    void shouldGetPositionByIdSuccessfully() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(1L);
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(positionMapper.toPositionDTO(position)).thenReturn(positionDTO);

        // When
        PositionDTO result = getPositionByIdUseCase.execute(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("DEV");
        assertThat(result.getDescription()).isEqualTo("Developer Position");

        verify(positionDomainService, times(1)).validatePositionExistsValidation(1L);
        verify(positionRepository, times(1)).findById(1L);
        verify(positionMapper, times(1)).toPositionDTO(position);
    }

    @Test
    @DisplayName("Should throw exception when position does not exist")
    void shouldThrowExceptionWhenPositionDoesNotExist() {
        // Given
        doThrow(new ScosException(ExceptionCodeError.SCOS_POSITION_001))
                .when(positionDomainService)
                .validatePositionExistsValidation(99L);

        // When / Then
        assertThatThrownBy(() -> getPositionByIdUseCase.execute(99L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_001.getCode());

        verify(positionDomainService, times(1)).validatePositionExistsValidation(99L);
        verify(positionRepository, never()).findById(99L);
    }

    @Test
    @DisplayName("Should handle position not found on repository call")
    void shouldHandlePositionNotFoundOnRepositoryCall() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(1L);
        when(positionRepository.findById(1L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> getPositionByIdUseCase.execute(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_001.getCode());

        verify(positionRepository, times(1)).findById(1L);
        verify(positionMapper, never()).toPositionDTO(any());
    }

    @Test
    @DisplayName("Should return position DTO with all fields")
    void shouldReturnPositionDTOWithAllFields() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(1L);
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(positionMapper.toPositionDTO(position)).thenReturn(positionDTO);

        // When
        PositionDTO result = getPositionByIdUseCase.execute(1L);

        // Then
        assertThat(result.getId()).isEqualTo(positionDTO.getId());
        assertThat(result.getCode()).isEqualTo(positionDTO.getCode());
        assertThat(result.getDescription()).isEqualTo(positionDTO.getDescription());
        assertThat(result.getDepartmentId()).isEqualTo(positionDTO.getDepartmentId());
    }

}

