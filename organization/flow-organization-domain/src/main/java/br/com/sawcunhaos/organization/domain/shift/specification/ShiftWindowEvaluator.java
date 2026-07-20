
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

package br.com.sawcunhaos.organization.domain.shift.specification;

import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Único ponto do domínio que converte um {@link Instant} em hora local de uma filial e
 * avalia se cai dentro de uma Jornada de Trabalho (turno). Não repetir {@code atZone()} em
 * nenhum outro lugar — Filter/Use Case futuros (Epic 5) chamam este serviço.
 */
public interface ShiftWindowEvaluator {

    /**
     * @param instant   momento avaliado
     * @param zoneId    fuso efetivo da filial (ver {@code CompanyService.resolveEffectiveZoneId})
     * @param startTime início do turno (inclusive)
     * @param lunchStart início do almoço (inclusive, excluído do turno)
     * @param lunchEnd  fim do almoço (inclusive, excluído do turno)
     * @param endTime   fim do turno (inclusive)
     * @return {@code true} se dentro do turno e fora do almoço; trata turno que cruza meia-noite ({@code endTime < startTime})
     */
    boolean isWithinShift(
            @NonNull Instant instant,
            @NonNull ZoneId zoneId,
            @NonNull LocalTime startTime,
            @NonNull LocalTime lunchStart,
            @NonNull LocalTime lunchEnd,
            @NonNull LocalTime endTime
    );
}
