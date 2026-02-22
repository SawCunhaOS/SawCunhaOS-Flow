
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
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.CreatePositionDTO;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePositionUseCase Integration Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class CreatePositionUseCaseIntegrationTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionDomainService positionDomainService;

    @Mock
    private PositionMapper positionMapper;

    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private CreatePositionUseCase createPositionUseCase;

    private CreatePositionDTO validCreateDTO;
    private Position mappedPosition;
    private Position savedPosition;

    @BeforeEach
    void setUp() {
        validCreateDTO = CreatePositionDTO.builder()
                .code("DEV")
                .description("Developer Position")
                .departmentId(1L)
                .build();

        mappedPosition = Position.builder()
                .code("DEV")
                .description("Developer Position")
                .build();

        savedPosition = Position.builder()
                .id(1L)
                .code("DEV")
                .description("Developer Position")
                .build();
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    @Test
    @DisplayName("Should create position successfully with valid data")
    void shouldCreatePositionSuccessfullyWithValidData() {
        // Given
        doNothing().when(positionDomainService).validateDepartmentExistsValidation(validCreateDTO.getDepartmentId());
        doNothing().when(positionDomainService).validatePositionCodeExistsValidation(validCreateDTO.getCode());
        when(positionMapper.toPosition(validCreateDTO)).thenReturn(mappedPosition);
        when(positionRepository.persist(any(Position.class))).thenReturn(savedPosition);

        // When
        Long positionId = createPositionUseCase.execute(validCreateDTO);

        // Then
        assertThat(positionId).isNotNull();
        assertThat(positionId).isEqualTo(1L);

        // Verify interactions
        verify(positionDomainService, times(1)).validateDepartmentExistsValidation(validCreateDTO.getDepartmentId());
        verify(positionDomainService, times(1)).validatePositionCodeExistsValidation(validCreateDTO.getCode());
        verify(positionMapper, times(1)).toPosition(validCreateDTO);
        verify(positionRepository, times(1)).persist(any(Position.class));
    }

    @Test
    @DisplayName("Should throw exception when department does not exist")
    void shouldThrowExceptionWhenDepartmentDoesNotExist() {
        // Given
        doThrow(new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_001))
                .when(positionDomainService)
                .validateDepartmentExistsValidation(validCreateDTO.getDepartmentId());

        // When / Then
        assertThatThrownBy(() -> createPositionUseCase.execute(validCreateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_001.getCode());

        // Verify that mapper and repository were never called
        verify(positionDomainService, times(1)).validateDepartmentExistsValidation(validCreateDTO.getDepartmentId());
        verify(positionDomainService, never()).validatePositionCodeExistsValidation(any());
        verify(positionMapper, never()).toPosition(any(CreatePositionDTO.class));
        verify(positionRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should throw exception when position code already exists")
    void shouldThrowExceptionWhenPositionCodeAlreadyExists() {
        // Given
        doNothing().when(positionDomainService).validateDepartmentExistsValidation(validCreateDTO.getDepartmentId());
        doThrow(new ScosException(ExceptionCodeError.SCOS_POSITION_002))
                .when(positionDomainService)
                .validatePositionCodeExistsValidation(validCreateDTO.getCode());

        // When / Then
        assertThatThrownBy(() -> createPositionUseCase.execute(validCreateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_002.getCode());

        // Verify that mapper and repository were never called
        verify(positionDomainService, times(1)).validateDepartmentExistsValidation(validCreateDTO.getDepartmentId());
        verify(positionDomainService, times(1)).validatePositionCodeExistsValidation(validCreateDTO.getCode());
        verify(positionMapper, never()).toPosition(any(CreatePositionDTO.class));
        verify(positionRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should validate department before code validation")
    void shouldValidateDepartmentBeforeCodeValidation() {
        // Given
        doNothing().when(positionDomainService).validateDepartmentExistsValidation(validCreateDTO.getDepartmentId());
        doNothing().when(positionDomainService).validatePositionCodeExistsValidation(validCreateDTO.getCode());
        when(positionMapper.toPosition(validCreateDTO)).thenReturn(mappedPosition);
        when(positionRepository.persist(any(Position.class))).thenReturn(savedPosition);

        // When
        createPositionUseCase.execute(validCreateDTO);

        // Then - verify order of operations
        var inOrder = inOrder(positionDomainService, positionMapper, positionRepository);
        inOrder.verify(positionDomainService).validateDepartmentExistsValidation(validCreateDTO.getDepartmentId());
        inOrder.verify(positionDomainService).validatePositionCodeExistsValidation(validCreateDTO.getCode());
        inOrder.verify(positionMapper).toPosition(validCreateDTO);
        inOrder.verify(positionRepository).persist(any(Position.class));
    }

    @Test
    @DisplayName("Should handle position with special characters in code")
    void shouldHandlePositionWithSpecialCharactersInCode() {
        // Given
        CreatePositionDTO dtoWithSpecialChars = CreatePositionDTO.builder()
                .code("DEV-001")
                .description("Developer Position Level 1")
                .departmentId(1L)
                .build();

        Position mappedPos = Position.builder()
                .code("DEV-001")
                .description("Developer Position Level 1")
                .build();

        Position savedPos = Position.builder()
                .id(2L)
                .code("DEV-001")
                .description("Developer Position Level 1")
                .build();

        doNothing().when(positionDomainService).validateDepartmentExistsValidation(dtoWithSpecialChars.getDepartmentId());
        doNothing().when(positionDomainService).validatePositionCodeExistsValidation(dtoWithSpecialChars.getCode());
        when(positionMapper.toPosition(dtoWithSpecialChars)).thenReturn(mappedPos);
        when(positionRepository.persist(any(Position.class))).thenReturn(savedPos);

        // When
        Long positionId = createPositionUseCase.execute(dtoWithSpecialChars);

        // Then
        assertThat(positionId).isNotNull();
        assertThat(positionId).isEqualTo(2L);

        verify(positionDomainService, times(1)).validatePositionCodeExistsValidation("DEV-001");
    }

    @Test
    @DisplayName("Should handle position with long description")
    void shouldHandlePositionWithLongDescription() {
        // Given
        String longDescription = "This is a very long description for the position that contains " +
                "detailed information about its responsibilities, scope, and organizational structure";

        CreatePositionDTO dtoWithLongDesc = CreatePositionDTO.builder()
                .code("ANALYST")
                .description(longDescription)
                .departmentId(1L)
                .build();

        Position mappedPos = Position.builder()
                .code("ANALYST")
                .description(longDescription)
                .build();

        Position savedPos = Position.builder()
                .id(3L)
                .code("ANALYST")
                .description(longDescription)
                .build();

        doNothing().when(positionDomainService).validateDepartmentExistsValidation(dtoWithLongDesc.getDepartmentId());
        doNothing().when(positionDomainService).validatePositionCodeExistsValidation(dtoWithLongDesc.getCode());
        when(positionMapper.toPosition(dtoWithLongDesc)).thenReturn(mappedPos);
        when(positionRepository.persist(any(Position.class))).thenReturn(savedPos);

        // When
        Long positionId = createPositionUseCase.execute(dtoWithLongDesc);

        // Then
        assertThat(positionId).isNotNull();
        assertThat(positionId).isEqualTo(3L);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).persist(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo(longDescription);
    }

    @Test
    @DisplayName("Should handle repository persistence failure")
    void shouldHandleRepositoryPersistenceFailure() {
        // Given
        doNothing().when(positionDomainService).validateDepartmentExistsValidation(validCreateDTO.getDepartmentId());
        doNothing().when(positionDomainService).validatePositionCodeExistsValidation(validCreateDTO.getCode());
        when(positionMapper.toPosition(validCreateDTO)).thenReturn(mappedPosition);
        when(positionRepository.persist(any(Position.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // When / Then
        assertThatThrownBy(() -> createPositionUseCase.execute(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection error");

        verify(positionRepository, times(1)).persist(any(Position.class));
    }

}

