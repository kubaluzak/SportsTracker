package org.example.sportstracker.snooker.domain.result;

import lombok.Builder;
import lombok.Data;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.snooker.domain.competitor.Player;

@Data
@Builder
public class SnookerResult implements Result {
    private Player winner;

    @Builder.Default
    private boolean completedNormally = true;

    @Builder.Default
    private boolean walkover = false;

    private Player walkoverLoser;

    @Override
    public Competitor getWinner() {
        return winner;
    }

    @Override
    public boolean isDraw() {
        return false; // Snooker matches cannot end in a draw
    }

    @Override
    public boolean isCompletedNormally() {
        return completedNormally;
    }
}