
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

package br.com.sawcunhaos.organization.domain.corporate.position.internal;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Repository
public interface PositionWorkScheduleRepository extends BaseJpaRepository<PositionWorkSchedule, Long>, JpaSpecificationExecutor<PositionWorkSchedule>, QuerydslPredicateExecutor<PositionWorkSchedule> {

    QPositionWorkSchedule positionWorkSchedule = QPositionWorkSchedule.positionWorkSchedule;

    Optional<PositionWorkSchedule> findByPositionIdAndDayOfWeek(Long positionId, DayOfWeek dayOfWeek);

    boolean existsByPositionIdAndDayOfWeek(Long positionId, DayOfWeek dayOfWeek);

    default List<PositionWorkSchedule> findAllByPositionId(Long positionId) {
        Iterable<PositionWorkSchedule> found = findAll(
                positionWorkSchedule.position.id.eq(positionId),
                positionWorkSchedule.dayOfWeek.asc()
        );
        return StreamSupport.stream(found.spliterator(), false).toList();
    }
}
