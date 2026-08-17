
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

package br.com.sawcunhaos.organization.application.usecase.access.login;

import org.jspecify.annotations.NonNull;

/**
 * Abre a solicitação de reativação de um Login {@code INACTIVE}/{@code BLOCKED} (Story 3.3) -
 * o Login só transita para {@code ACTIVE} quando a solicitação é aprovada.
 */
public interface RequestLoginReactivationUseCase {

    void execute(@NonNull Long loginId);

}
