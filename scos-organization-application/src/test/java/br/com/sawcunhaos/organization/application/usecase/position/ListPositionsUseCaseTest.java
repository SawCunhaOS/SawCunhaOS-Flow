
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

import br.com.sawcunhaos.organization.application.dto.PositionDTO;
import br.com.sawcunhaos.organization.application.mapper.position.PositionMapper;
import br.com.sawcunhaos.organization.domain.model.department.Position;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListPositionsUseCase Tests")
class ListPositionsUseCaseTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionMapper positionMapper;

    @InjectMocks
    private ListPositionsUseCase listPositionsUseCase;

    private Pageable pageable;
    private List<Position> positions;
    private List<PositionDTO> positionDTOs;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        positions = new ArrayList<>();
        positions.add(Position.builder()
                .id(1L)
                .code("DEV")
                .description("Developer Position")
                .build());
        positions.add(Position.builder()
                .id(2L)
                .code("ANALYST")
                .description("Analyst Position")
                .build());

        positionDTOs = new ArrayList<>();
        positionDTOs.add(PositionDTO.builder()
                .id(1L)
                .code("DEV")
                .description("Developer Position")
                .departmentId(1L)
                .build());
        positionDTOs.add(PositionDTO.builder()
                .id(2L)
                .code("ANALYST")
                .description("Analyst Position")
                .departmentId(1L)
                .build());
    }

    @Test
    @DisplayName("Should list positions with pagination successfully")
    void shouldListPositionsWithPaginationSuccessfully() {
        // Given
        Page<Position> positionsPage = new PageImpl<>(positions, pageable, 2);
        when(positionRepository.findAll(pageable)).thenReturn(positionsPage);
        when(positionMapper.toPositionDTO(positions.get(0))).thenReturn(positionDTOs.get(0));
        when(positionMapper.toPositionDTO(positions.get(1))).thenReturn(positionDTOs.get(1));

        // When
        Page<PositionDTO> result = listPositionsUseCase.execute(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);

        verify(positionRepository, times(1)).findAll(pageable);
        verify(positionMapper, times(2)).toPositionDTO(any());
    }

    @Test
    @DisplayName("Should return empty page when no positions exist")
    void shouldReturnEmptyPageWhenNoPositionsExist() {
        // Given
        Page<Position> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        when(positionRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<PositionDTO> result = listPositionsUseCase.execute(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(positionRepository, times(1)).findAll(pageable);
        verify(positionMapper, never()).toPositionDTO(any());
    }

    @Test
    @DisplayName("Should handle different page numbers")
    void shouldHandleDifferentPageNumbers() {
        // Given
        Pageable page2 = PageRequest.of(1, 10);
        Page<Position> positionsPage = new PageImpl<>(positions, page2, 25);
        when(positionRepository.findAll(page2)).thenReturn(positionsPage);
        when(positionMapper.toPositionDTO(positions.get(0))).thenReturn(positionDTOs.get(0));
        when(positionMapper.toPositionDTO(positions.get(1))).thenReturn(positionDTOs.get(1));

        // When
        Page<PositionDTO> result = listPositionsUseCase.execute(page2);

        // Then
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(25);

        verify(positionRepository, times(1)).findAll(page2);
    }

    @Test
    @DisplayName("Should handle different page sizes")
    void shouldHandleDifferentPageSizes() {
        // Given
        Pageable pageable20 = PageRequest.of(0, 20);
        Page<Position> positionsPage = new PageImpl<>(positions, pageable20, 2);
        when(positionRepository.findAll(pageable20)).thenReturn(positionsPage);
        when(positionMapper.toPositionDTO(positions.get(0))).thenReturn(positionDTOs.get(0));
        when(positionMapper.toPositionDTO(positions.get(1))).thenReturn(positionDTOs.get(1));

        // When
        Page<PositionDTO> result = listPositionsUseCase.execute(pageable20);

        // Then
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getContent()).hasSize(2);

        verify(positionRepository, times(1)).findAll(pageable20);
    }

    @Test
    @DisplayName("Should maintain position order in paginated result")
    void shouldMaintainPositionOrderInPaginatedResult() {
        // Given
        Page<Position> positionsPage = new PageImpl<>(positions, pageable, 2);
        when(positionRepository.findAll(pageable)).thenReturn(positionsPage);
        when(positionMapper.toPositionDTO(positions.get(0))).thenReturn(positionDTOs.get(0));
        when(positionMapper.toPositionDTO(positions.get(1))).thenReturn(positionDTOs.get(1));

        // When
        Page<PositionDTO> result = listPositionsUseCase.execute(pageable);

        // Then
        assertThat(result.getContent())
                .extracting(PositionDTO::getCode)
                .containsExactly("DEV", "ANALYST");
    }

}

