package com.example.enanosycamellos.registration.mapper;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.mapper.CompetitorMapper;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.mapper.RaceMapper;
import com.example.enanosycamellos.registration.dto.RaceRegistrationResponse;
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.mapper.TeamMapper;

import java.util.List;

/**
 * Translates between {@link RaceRegistration} and its DTOs. Building the
 * entity from the request is NOT here (unlike RaceMapper): the service needs
 * to load the actual Race/Competitor/Team entities from their repositories
 * first, so that construction step lives in RegistrationService instead.
 */
public final class RegistrationMapper {

    private RegistrationMapper() {
    }

    public static RaceRegistration toEntity(Race race, Competitor competitor, Team team,
                                             Integer startingPosition, String registeredBy) {
        return RaceRegistration.builder()
                .race(race)
                .competitor(competitor)
                .team(team)
                .startingPosition(startingPosition)
                .registeredBy(registeredBy)
                .status(RegistrationStatus.PENDING)
                .build();
    }

    public static RaceRegistrationResponse toResponse(RaceRegistration registration) {
        if (registration == null) return null;
        return new RaceRegistrationResponse(
                registration.getId(),
                RaceMapper.toSummary(registration.getRace()),
                CompetitorMapper.toSummary(registration.getCompetitor()),
                TeamMapper.toSummary(registration.getTeam()),
                registration.getRegistrationDate(),
                registration.getStatus(),
                registration.getStartingPosition(),
                registration.getValidationNotes(),
                registration.getRegisteredBy()
        );
    }

    public static List<RaceRegistrationResponse> toResponseList(List<RaceRegistration> registrations) {
        if (registrations == null) return List.of();
        return registrations.stream().map(RegistrationMapper::toResponse).toList();
    }
}