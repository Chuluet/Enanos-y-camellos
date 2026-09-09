package com.example.enanosycamellos.race.repository;

import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IRaceRepository extends JpaRepository<Race, UUID> {

    List<Race> findAllByOrderByScheduledDateTimeAsc();

    List<Race> findAllByStatusOrderByScheduledDateTimeAsc(RaceStatus status);

    List<Race> findAllByRaceTypeOrderByScheduledDateTimeAsc(RaceType raceType);

    List<Race> findAllByStatusAndRaceTypeOrderByScheduledDateTimeAsc(RaceStatus status, RaceType raceType);
}