package org.example.sportstracker.snooker.domain.match;

import java.util.List;

public record SnookerScoreSnapshot(
        int frameNumber,
        String eventId,
        String eventName,
        String description,
        int pointsValue,
        List<String> relatedEventIds,
        int player1Frames,
        int player2Frames,
        int player1CurrentFramePoints,
        int player2CurrentFramePoints,
        int highestBreak,
        String highestBreakPlayerName,
        String display
) {
}