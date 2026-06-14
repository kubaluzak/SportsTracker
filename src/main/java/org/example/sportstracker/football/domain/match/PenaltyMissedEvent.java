package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class PenaltyMissedEvent extends FootballMatchEvent {

    public PenaltyMissedEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public FootballEventTypeId getEventTypeId() {
        return FootballEventTypeId.PENALTY_MISSED;
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Nietrafiony karny w czasie meczu nie zmienia wyniku.
    }
}