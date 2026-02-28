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

package br.com.sawcunhaos.organization.api.controller.department;

import br.com.sawcunhaos.organization.api.controller.DepartmentApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateDepartmentRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.Department;
import br.com.sawcunhaos.organization.api.dto.GetAllDepartmentsResponse;
import br.com.sawcunhaos.organization.api.dto.GetDepartmentResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.UpdateDepartmentRequest;
import br.com.sawcunhaos.organization.api.enumeration.DepartmentOrder;
import br.com.sawcunhaos.organization.application.dto.CreateDepartmentDTO;
import br.com.sawcunhaos.organization.application.dto.DepartmentDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateDepartmentDTO;
import br.com.sawcunhaos.organization.application.usecase.department.CreateDepartmentUseCase;
import br.com.sawcunhaos.organization.application.usecase.department.DeleteDepartmentUseCase;
import br.com.sawcunhaos.organization.application.usecase.department.GetDepartmentByIdUseCase;
import br.com.sawcunhaos.organization.application.usecase.department.ListDepartmentsUseCase;
import br.com.sawcunhaos.organization.application.usecase.department.UpdateDepartmentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static br.com.sawcunhaos.organization.api.utils.PaginatioUtils.createPageable;
import static br.com.sawcunhaos.organization.api.utils.PaginatioUtils.createScosPaginated;

/**
 * Implementação do delegado para operações de Department (Departamento).
 *
 * Responsabilidades:
 * - Receber requisições HTTP via OpenAPI delegate pattern
 * - Converter DTOs de entrada (API) para DTOs de aplicação
 * - Orquestrar chamadas aos use cases
 * - Mapear respostas de domínio/aplicação para DTOs de API
 * - Retornar respostas no formato esperado (200, 201, 204)
 *
 * Padrão seguido: mesma estrutura que PositionApiDelegateImp
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DepartmentApiDelegateImp implements DepartmentApiDelegate {

    private final ListDepartmentsUseCase listDepartmentsUseCase;
    private final CreateDepartmentUseCase createDepartmentUseCase;
    private final GetDepartmentByIdUseCase getDepartmentByIdUseCase;
    private final UpdateDepartmentUseCase updateDepartmentUseCase;
    private final DeleteDepartmentUseCase deleteDepartmentUseCase;

    @Override
    public GetAllDepartmentsResponse getAllDepartments(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting all departments with pagination: {}", paginationFilter);
        Page<DepartmentDTO> departmentDTOPage = listDepartmentsUseCase.execute(
                createPageable(paginationFilter, DepartmentOrder.ID)
        );

        return GetAllDepartmentsResponse.builder()
                .data(
                        departmentDTOPage.getContent().stream().map(
                                departmentDTO -> Department.builder()
                                        .id(departmentDTO.getId())
                                        .code(departmentDTO.getCode())
                                        .description(departmentDTO.getDescription())
                                        .active(departmentDTO.isActive())
                                        .build()
                        ).toList()
                )
                .paginatedDTO(
                        createScosPaginated(departmentDTOPage)
                )
                .build();
    }

    @Override
    public CreateResponse createDepartment(CreateDepartmentRequest createDepartmentRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Creating department with code: {}", createDepartmentRequest.code());

        CreateDepartmentDTO createDepartmentDTO = CreateDepartmentDTO.builder()
                .code(createDepartmentRequest.code())
                .description(createDepartmentRequest.description())
                .build();

        Long departmentId = createDepartmentUseCase.execute(createDepartmentDTO);

        return CreateResponse.builder()
                .data(Create.builder()
                        .id(departmentId)
                        .build())
                .build();
    }

    @Override
    public GetDepartmentResponse getDepartmentById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting department by id: {}", id);

        DepartmentDTO departmentDTO = getDepartmentByIdUseCase.execute(id);

        return GetDepartmentResponse.builder()
                .data(Department.builder()
                        .id(departmentDTO.getId())
                        .code(departmentDTO.getCode())
                        .description(departmentDTO.getDescription())
                        .active(departmentDTO.isActive())
                        .build())
                .build();
    }

    @Override
    public Void updateDepartment(Long id, UpdateDepartmentRequest updateDepartmentRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Updating department with id: {} and code: {}", id, updateDepartmentRequest.code());

        UpdateDepartmentDTO updateDepartmentDTO = UpdateDepartmentDTO.builder()
                .departmentId(id)
                .code(updateDepartmentRequest.code())
                .description(updateDepartmentRequest.description())
                .build();

        updateDepartmentUseCase.execute(updateDepartmentDTO);

        return null;
    }

    @Override
    public Void deleteDepartment(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Deleting department with id: {}", id);

        deleteDepartmentUseCase.execute(id);

        return null;
    }
}

