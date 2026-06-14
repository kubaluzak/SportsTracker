package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public class RedCardEvent extends FootballMatchEvent {

    public RedCardEvent(Competitor actor, int minute, String description) {
        super(null, null, actor, description, minute, List.of());
    }

    @Override
    public FootballEventTypeId getEventTypeId() {
        return FootballEventTypeId.RED_CARD;
    }

    @Override
    public void applyTo(FootballMatch.FootballScore score) {
        score.addRedCardFor(getActor());
    }

    @Override
    public void undoFrom(FootballMatch.FootballScore score) {
        score.removeRedCardFor(getActor());
    }

    @Override
    public boolean canBeUndone() {
        return true;
    }
}