package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

public final class FootballEvents {

    private FootballEvents() {
        // Klasa narzędziowa, nie tworzymy obiektów.
    }

    public static FootballMatchEvent goalScored(Competitor actor, int minute, String description) {
        return new GoalScoredEvent(actor, minute, description);
    }

    public static FootballMatchEvent penaltyScored(Competitor actor, int minute, String description) {
        return new PenaltyScoredEvent(actor, minute, description);
    }

    public static FootballMatchEvent penaltyMissed(Competitor actor, int minute, String description) {
        return new PenaltyMissedEvent(actor, minute, description);
    }

    public static FootballMatchEvent shootoutPenaltyScored(Competitor actor, int minute, String description) {
        return new ShootoutPenaltyScoredEvent(actor, minute, description);
    }

    public static FootballMatchEvent shootoutPenaltyMissed(Competitor actor, int minute, String description) {
        return new ShootoutPenaltyMissedEvent(actor, minute, description);
    }

    public static FootballMatchEvent yellowCard(Competitor actor, int minute, String description) {
        return new YellowCardEvent(actor, minute, description);
    }

    public static FootballMatchEvent redCard(Competitor actor, int minute, String description) {
        return new RedCardEvent(actor, minute, description);
    }

    public static FootballMatchEvent foul(Competitor actor, int minute, String description) {
        return new FoulEvent(actor, minute, description);
    }

    public static FootballMatchEvent substitution(Competitor actor, int minute, String description) {
        return new SubstitutionEvent(actor, minute, description);
    }

    public static FootballMatchEvent goalDisallowed(
            Competitor actor,
            int minute,
            String description,
            List<String> relatedEventIds
    ) {
        return new GoalDisallowedEvent(actor, minute, description, relatedEventIds);
    }

    public static FootballMatchEvent penaltyDisallowed(
            Competitor actor,
            int minute,
            String description,
            List<String> relatedEventIds
    ) {
        return new PenaltyDisallowedEvent(actor, minute, description, relatedEventIds);
    }

    public static FootballMatchEvent eventInvalidated(
            Competitor actor,
            int minute,
            String description,
            List<String> relatedEventIds
    ) {
        return new EventInvalidatedEvent(actor, minute, description, relatedEventIds);
    }

    public static FootballMatchEvent varReview(
            Competitor actor,
            int minute,
            String description,
            List<String> relatedEventIds
    ) {
        return new VarReviewEvent(actor, minute, description, relatedEventIds);
    }
}