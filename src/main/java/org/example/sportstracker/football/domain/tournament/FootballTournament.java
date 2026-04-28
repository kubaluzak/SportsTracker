package org.example.sportstracker.football.domain.tournament;

import lombok.Data;
import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.tournament.Tournament;
import org.example.sportstracker.core.domain.tournament.TournamentStage;

import java.util.ArrayList;
import java.util.List;

@Data
public class FootballTournament implements Tournament {
    private String name;
    private List<TournamentStage> stages = new ArrayList<>();

    private Competitor overallWinner;

    public void addStage(TournamentStage stage) {
        stages.add(stage);
    }

    @Override
    public void startTournament() {
        System.out.println(">>> TOURNAMENT STARTED: " + name + " <<<");
    }

    @Override
    public void endTournament() {
        System.out.println(">>> TOURNAMENT FINISHED: " + name + " <<<");
    }

    @Override
    public Competitor getOverallWinner() {
        return this.overallWinner;
    }
}