package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class FoulEvent extends FootballMatchEvent {

    public FoulEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        // Faul sam w sobie nie zmienia wyniku.
    }
}