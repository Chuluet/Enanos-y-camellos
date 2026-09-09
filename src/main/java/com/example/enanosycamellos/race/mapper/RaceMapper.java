package com.example.enanosycamellos.race.mapper;

import com.example.enanosycamellos.race.dto.RaceRequest;
import com.example.enanosycamellos.race.dto.RaceResponse;
import com.example.enanosycamellos.race.dto.RaceSummaryResponse;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;

import java.util.List;

public final class RaceMapper {

    private RaceMapper() {
    }

    public static Race toEntity(RaceRequest request) {
        if (request == null) return null;
        return Race.builder()
                .name(request.name())
                .description(request.description())
                .scheduledDateTime(request.scheduledDateTime())
                .startLocation(request.startLocation())
                .finishLocation(request.finishLocation())
                .distanceMeters(request.distanceMeters())
                .maxParticipants(request.maxParticipants())
                .raceType(request.raceType())
                .status(RaceStatus.DRAFT)
                .organizer(request.organizer())
                .registrationDeadline(request.registrationDeadline())
                .build();
    }

    public static RaceResponse toResponse(Race race) {
        if (race == null) return null;
        return new RaceResponse(
                race.getId(),
                race.getName(),
                race.getDescription(),
                race.getScheduledDateTime(),
                race.getStartLocation(),
                race.getFinishLocation(),
                race.getDistanceMeters(),
                race.getMaxParticipants(),
                race.getRaceType(),
                race.getStatus(),
                race.getOrganizer(),
                race.getRegistrationDeadline(),
                race.getCreatedAt(),
                race.getUpdatedAt()
        );
    }

    public static RaceSummaryResponse toSummary(Race race) {
        if (race == null) return null;
        return new RaceSummaryResponse(
                race.getId(),
                race.getName(),
                race.getRaceType(),
                race.getStatus(),
                race.getScheduledDateTime()
        );
    }

    public static List<RaceResponse> toResponseList(List<Race> races) {
        if (races == null) return List.of();
        return races.stream().map(RaceMapper::toResponse).toList();
    }
}