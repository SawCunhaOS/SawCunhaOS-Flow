
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

package br.com.sawcunhaos.organization.api.delegate.position;

import br.com.sawcunhaos.organization.api.controller.PositionApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreatePositionRequest;
import br.com.sawcunhaos.organization.api.dto.CreatePositionWorkScheduleRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.DayOfWeek;
import br.com.sawcunhaos.organization.api.dto.GetAllPositionsResponse;
import br.com.sawcunhaos.organization.api.dto.GetPositionResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.PositionWorkSchedule;
import br.com.sawcunhaos.organization.api.dto.UpdatePositionRequest;
import br.com.sawcunhaos.organization.api.dto.UpdatePositionWorkScheduleRequest;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.CreatePositionUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.CreatePositionWorkScheduleUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.DeletePositionWorkScheduleUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.DisablePositionUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.EnablePositionUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.FindAllPositionUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.FindPositionUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.GetAllPositionWorkScheduleUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.UpdatePositionUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.position.UpdatePositionWorkScheduleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionDelegate implements PositionApiDelegate {

    private final FindPositionUseCase findPositionUseCase;
    private final FindAllPositionUseCase findAllPositionUseCase;
    private final DisablePositionUseCase disablePositionUseCase;
    private final EnablePositionUseCase enablePositionUseCase;
    private final CreatePositionUseCase createPositionUseCase;
    private final UpdatePositionUseCase updatePositionUseCase;
    private final GetAllPositionWorkScheduleUseCase getAllPositionWorkScheduleUseCase;
    private final CreatePositionWorkScheduleUseCase createPositionWorkScheduleUseCase;
    private final UpdatePositionWorkScheduleUseCase updatePositionWorkScheduleUseCase;
    private final DeletePositionWorkScheduleUseCase deletePositionWorkScheduleUseCase;

    @Override
    public GetPositionResponse getPositionById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetPositionResponse.builder()
                .data(findPositionUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllPositionsResponse getAllPositions(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<Long> departmentId, Optional<Boolean> active) {
        return findAllPositionUseCase.execute(paginationFilter, departmentId.orElse(null), active.orElse(null));
    }

    @Override
    public Void activatePosition(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        enablePositionUseCase.execute(id);
        return null;
    }

    @Override
    public CreateResponse createPosition(CreatePositionRequest createPositionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createPositionUseCase.execute(createPositionRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void inactivatePosition(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        disablePositionUseCase.execute(id);
        return null;
    }

    @Override
    public Void updatePosition(Long id, UpdatePositionRequest updatePositionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updatePositionUseCase.execute(id, updatePositionRequest);
        return null;
    }

    @Override
    public List<PositionWorkSchedule> getAllPositionWorkSchedule(Long positionId, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return getAllPositionWorkScheduleUseCase.execute(positionId);
    }

    @Override
    public CreateResponse createPositionWorkSchedule(Long positionId, CreatePositionWorkScheduleRequest createPositionWorkScheduleRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(createPositionWorkScheduleUseCase.execute(positionId, createPositionWorkScheduleRequest))
                        .build())
                .build();
    }

    @Override
    public Void updatePositionWorkSchedule(Long positionId, DayOfWeek dayOfWeek, UpdatePositionWorkScheduleRequest updatePositionWorkScheduleRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updatePositionWorkScheduleUseCase.execute(positionId, dayOfWeek, updatePositionWorkScheduleRequest);
        return null;
    }

    @Override
    public Void deletePositionWorkSchedule(Long positionId, DayOfWeek dayOfWeek, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        deletePositionWorkScheduleUseCase.execute(positionId, dayOfWeek);
        return null;
    }
}
