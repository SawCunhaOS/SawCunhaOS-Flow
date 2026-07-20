
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

package br.com.sawcunhaos.organization.domain.corporate.position.specification;

import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionInput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.Position;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PositionService {

    PositionOutput create(@NonNull PositionInput positionInput);
    void update(@NonNull PositionInput positionInput);
    PositionOutput findById(@NonNull Long positionId);
    Page<PositionOutput> findAll(@NonNull Long departmentId, @NonNull Boolean active, @NonNull Pageable pageable);
    void enable(@NonNull Long positionId);
    void disable(@NonNull Long positionId);

    Position findPositionById(@NonNull Long positionId);

}
