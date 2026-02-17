
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
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.organization.application.dto.PositionDTO;
import br.com.sawcunhaos.organization.application.mapper.position.PositionMapper;
import br.com.sawcunhaos.organization.domain.model.department.Position;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import br.com.sawcunhaos.organization.domain.service.department.PositionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_POSITION_001;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetPositionByIdUseCase implements ScosBaseUseCase<Long, PositionDTO> {

    private final PositionRepository positionRepository;
    private final PositionDomainService positionDomainService;
    private final PositionMapper positionMapper;

    @Override
    public PositionDTO execute(@NonNull Long positionId) {
        log.info("Getting position by id: {}", positionId);

        // Validate position exists
        positionDomainService.validatePositionExistsValidation(positionId);

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new ScosException(SCOS_POSITION_001));

        return positionMapper.toPositionDTO(position);
    }
}

