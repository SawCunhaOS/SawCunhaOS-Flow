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

package br.com.sawcunhaos.organization.api.controller.positions;

import br.com.sawcunhaos.organization.api.controller.PositionApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreatePositionRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.Department;
import br.com.sawcunhaos.organization.api.dto.GetAllPositionsResponse;
import br.com.sawcunhaos.organization.api.dto.GetPositionResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.Position;
import br.com.sawcunhaos.organization.api.dto.UpdatePositionRequest;
import br.com.sawcunhaos.organization.api.enumeration.PositionOrder;
import br.com.sawcunhaos.organization.application.dto.CreatePositionDTO;
import br.com.sawcunhaos.organization.application.dto.PositionDTO;
import br.com.sawcunhaos.organization.application.dto.UpdatePositionDTO;
import br.com.sawcunhaos.organization.application.usecase.position.CreatePositionUseCase;
import br.com.sawcunhaos.organization.application.usecase.position.DeletePositionUseCase;
import br.com.sawcunhaos.organization.application.usecase.position.GetPositionByIdUseCase;
import br.com.sawcunhaos.organization.application.usecase.position.ListPositionsUseCase;
import br.com.sawcunhaos.organization.application.usecase.position.UpdatePositionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static br.com.sawcunhaos.organization.api.utils.PaginatioUtils.createPageable;
import static br.com.sawcunhaos.organization.api.utils.PaginatioUtils.createScosPaginated;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionApiDelegateImp implements PositionApiDelegate {

    private final ListPositionsUseCase listPositionsUseCase;
    private final CreatePositionUseCase createPositionUseCase;
    private final GetPositionByIdUseCase getPositionByIdUseCase;
    private final UpdatePositionUseCase updatePositionUseCase;
    private final DeletePositionUseCase deletePositionUseCase;

    @Override
    public GetAllPositionsResponse getAllPositions(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting all positions with pagination: {}", paginationFilter);
        Page<PositionDTO> positionDTOPage = listPositionsUseCase.execute(
                createPageable(paginationFilter, PositionOrder.ID)
        );

        return GetAllPositionsResponse.builder()
                .data(
                        positionDTOPage.getContent().stream().map(
                                positionDTO -> Position.builder()
                                        .id(positionDTO.getId())
                                        .code(positionDTO.getCode())
                                        .description(positionDTO.getDescription())
                                        .build()
                        ).toList()
                )
                .paginatedDTO(
                        createScosPaginated(positionDTOPage)
                )
                .build();
    }

    @Override
    public CreateResponse createPosition(CreatePositionRequest createPositionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Creating position with code: {}", createPositionRequest.code());

        CreatePositionDTO createPositionDTO = CreatePositionDTO.builder()
                .code(createPositionRequest.code())
                .description(createPositionRequest.description())
                .departmentId(createPositionRequest.departmentId())
                .build();

        Long positionId = createPositionUseCase.execute(createPositionDTO);

        return CreateResponse.builder()
                .data(Create.builder()
                        .id(positionId)
                        .build())
                .build();
    }

    @Override
    public GetPositionResponse getPositionById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting position by id: {}", id);

        PositionDTO positionDTO = getPositionByIdUseCase.execute(id);


        return GetPositionResponse.builder()
                .data(Position.builder()
                        .id(positionDTO.getId())
                        .code(positionDTO.getCode())
                        .description(positionDTO.getDescription())
                        .department(
                                Department.builder()
                                        .id(positionDTO.getDepartmentId())
                                        .code(positionDTO.getDepartmentCode())
                                        .description(positionDTO.getDepartmentDescription())
                                        .build()
                        )
                        .build())
                .build();
    }

    @Override
    public Void updatePosition(Long id, UpdatePositionRequest updatePositionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Updating position with id: {} and code: {}", id, updatePositionRequest.code());

        UpdatePositionDTO updatePositionDTO = UpdatePositionDTO.builder()
                .positionId(id)
                .code(updatePositionRequest.code())
                .description(updatePositionRequest.description())
                .departmentId(updatePositionRequest.departmentId())
                .build();

        updatePositionUseCase.execute(updatePositionDTO);

        return null;
    }

    @Override
    public Void deletePosition(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Deleting position with id: {}", id);

        deletePositionUseCase.execute(id);

        return null;
    }
}
