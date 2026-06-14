package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class ShootoutPenaltyMissedEvent extends FootballMatchEvent {

    public ShootoutPenaltyMissedEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public FootballEventTypeId getEventTypeId() {
        return FootballEventTypeId.SHOOTOUT_PENALTY_MISSED;
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Nietrafiony karny w serii nie zmienia wyniku.
    }
}