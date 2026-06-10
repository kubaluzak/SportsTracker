package org.example.sportstracker.football.domain.match;

import java.util.List;

public record ScoreSnapshot(
        int minute,
        String eventId,
        String eventName,
        String description,
        List<String> relatedEventIds,
        int homeGoals,
        int awayGoals,
        int homePenalties,
        int awayPenalties,
        int homeYellowCards,
        int awayYellowCards,
        int homeRedCards,
        int awayRedCards,
        String display
) {
}