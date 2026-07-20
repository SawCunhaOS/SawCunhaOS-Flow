
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable;

import org.jspecify.annotations.NonNull;

/** Inativa um motivo de bloqueio ativo — não remove vínculos existentes em histórico (UC-130). */
public interface DisableReasonDisableUseCase {
    void execute(@NonNull Long id);
}
