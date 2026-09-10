package com.example.enanosycamellos.result.mapper;

import com.example.enanosycamellos.competitor.mapper.CompetitorMapper;
import com.example.enanosycamellos.race.mapper.RaceMapper;
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.result.dto.RaceResultResponse;
import com.example.enanosycamellos.result.entity.RaceResult;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.team.mapper.TeamMapper;

import java.util.List;

/**
 * Translates between {@link RaceResult} and its DTOs. Building the entity
 * from the request is NOT here (same reasoning as RegistrationMapper): the
 * service needs to load the actual RaceRegistration first, so that step
 * lives in RaceResultService instead.
 */
public final class RaceResultMapper {

    private RaceResultMapper() {
    }

    public static RaceResult toEntity(RaceRegistration registration, Integer startPosition,
                                       Integer finalPosition, Double completionTime, Double penaltyTime,
                                       ResultStatus status, String notes, String recordedBy) {
        return RaceResult.builder()
                .registration(registration)
                .startPosition(startPosition)
                .finalPosition(finalPosition)
                .completionTime(completionTime)
                .penaltyTime(penaltyTime != null ? penaltyTime : 0.0)
                .status(status)
                .notes(notes)
                .recordedBy(recordedBy)
                .build();
    }

    public static RaceResultResponse toResponse(RaceResult result) {
        if (result == null) return null;
        RaceRegistration registration = result.getRegistration();
        return new RaceResultResponse(
                result.getId(),
                registration.getId(),
                RaceMapper.toSummary(registration.getRace()),
                CompetitorMapper.toSummary(registration.getCompetitor()),
                TeamMapper.toSummary(registration.getTeam()),
                result.getStartPosition(),
                result.getFinalPosition(),
                result.getCompletionTime(),
                result.getPenaltyTime(),
                result.getStatus(),
                result.getNotes(),
                result.getRecordedBy(),
                result.getRecordedAt()
        );
    }

    public static List<RaceResultResponse> toResponseList(List<RaceResult> results) {
        if (results == null) return List.of();
        return results.stream().map(RaceResultMapper::toResponse).toList();
    }
}