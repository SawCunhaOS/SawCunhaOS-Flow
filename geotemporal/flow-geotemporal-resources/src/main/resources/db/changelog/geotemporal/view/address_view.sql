create or replace view scos.IFA_V_ADDRESS as
    select address.address_id       id,
           address.zip_code         zip_code,
           street_type.street_type  street_type,
           address.street           street,
           concat(
              street_type.street_type, ' ',   address.street,   ', ',
              address.complement,      ' ',   dist."name",      ' ',
              city."name",             ' - ', state."name",     ' / ',
              country."name",          ' - ', address.zip_code
           )                                                       as street_full,
           address.complement       complement,
           address.geolocation      geolocation,
           dist."name"              district_name,
           dist.acronym             district_acronym,
           dist.code                district_code,
           city."name"              city_name,
           city.acronym             city_acronym,
           city.code                city_code,
           state."name"             state_name,
           state.acronym            state_acronym,
           state.code               state_code,
           country."name"           country_name,
           country.acronym          country_acronym,
           country.code             country_code
    from   ifa_address address
           join ifa_street_type street_type on address.street_type_id  = street_type.street_type_id
           join ifa_district dist           on address.district_id     = dist.district_id
           join ifa_city city               on dist.city_id            = city.city_id
           join ifa_state state             on city.state_id 	       = state.state_id
           join ifa_country country         on state.country_id        = country.country_id;
