package org.example.sportstracker.football.domain.result;

import lombok.Builder;
import lombok.Data;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.result.Result;
import org.example.sportstracker.football.domain.competitor.Team;

@Data
@Builder
public class FootballResult implements Result {
    private Team winner;
    private boolean isDraw;
    private boolean wonViaPenalties;

    @Builder.Default
    private boolean completedNormally = true;

    @Builder.Default
    private boolean walkover = false;

    private Team walkoverLoser;

    @Override
    public Competitor getWinner() {
        return winner;
    }

    @Override
    public boolean isDraw() {
        return isDraw;
    }

    @Override
    public boolean isCompletedNormally() {
        return completedNormally;
    }
}