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

package br.com.sawcunhaos.organization.application.mapper.company;

import br.com.sawcunhaos.organization.application.dto.CompanyAddressDTO;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyAddressDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyAddressDTO;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.CompanyAddress;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyAddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", expression = "java(mapCompanyFromId(createCompanyAddressDTO.getCompanyId()))")
    @Mapping(target = "number", expression = "java(Long.parseLong(createCompanyAddressDTO.getNumber()))")
    @Mapping(target = "geolocation", expression = "java(mapToPoint(createCompanyAddressDTO.getLatitude(), createCompanyAddressDTO.getLongitude()))")
    CompanyAddress toCompanyAddress(CreateCompanyAddressDTO createCompanyAddressDTO);

    @Mapping(target = "id", source = "companyAddressId")
    @Mapping(target = "addressId", ignore = true)
    @Mapping(target = "company", expression = "java(mapCompanyFromId(updateCompanyAddressDTO.getCompanyId()))")
    @Mapping(target = "number", expression = "java(Long.parseLong(updateCompanyAddressDTO.getNumber()))")
    @Mapping(target = "geolocation", expression = "java(mapToPoint(updateCompanyAddressDTO.getLatitude(), updateCompanyAddressDTO.getLongitude()))")
    CompanyAddress toCompanyAddress(UpdateCompanyAddressDTO updateCompanyAddressDTO);

    @Mapping(target = "latitude", expression = "java(mapLatitude(companyAddress.getGeolocation()))")
    @Mapping(target = "longitude", expression = "java(mapLongitude(companyAddress.getGeolocation()))")
    CompanyAddressDTO toCompanyAddressDTO(CompanyAddress companyAddress);

    default Company mapCompanyFromId(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return Company.builder().id(companyId).build();
    }

    default Point mapToPoint(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        return factory.createPoint(new Coordinate(longitude, latitude));
    }

    default Double mapLatitude(Point point) {
        if (point == null) {
            return null;
        }
        return point.getY();
    }

    default Double mapLongitude(Point point) {
        if (point == null) {
            return null;
        }
        return point.getX();
    }

}
