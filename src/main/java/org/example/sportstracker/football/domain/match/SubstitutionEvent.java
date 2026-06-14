package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class SubstitutionEvent extends FootballMatchEvent {

    public SubstitutionEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public FootballEventTypeId getEventTypeId() {
        return FootballEventTypeId.SUBSTITUTION;
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Zmiana zawodnika nie zmienia wyniku.
    }
}