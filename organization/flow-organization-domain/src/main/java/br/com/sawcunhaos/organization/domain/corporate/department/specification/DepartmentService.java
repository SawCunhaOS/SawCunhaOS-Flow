
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

package br.com.sawcunhaos.organization.domain.corporate.department.specification;

import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentInput;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.internal.Department;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {

    DepartmentOutput create(@NonNull DepartmentInput departmentInput);
    void update(@NonNull DepartmentInput departmentInput);
    DepartmentOutput findById(@NonNull Long departmentId);
    Page<DepartmentOutput> findAll(@NonNull Boolean active, @NonNull Pageable pageable);
    void enable(@NonNull Long departmentId);
    void disable(@NonNull Long departmentId);

    Department findDepartmentById(@NonNull Long departmentId);

}
