
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

package br.com.sawcunhaos.organization.domain.access.login.dto;

import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import lombok.Builder;

/**
 * Dados de entrada para criar um {@code Login} — nasce sempre em PENDING_APPROVAL.
 * {@code employeeId} só é usado quando {@code type == EMPLOYEE}.
 */
@Builder
public record LoginInput(
        String login,
        Long profileId,
        LoginType type,
        Long employeeId
) {
}
