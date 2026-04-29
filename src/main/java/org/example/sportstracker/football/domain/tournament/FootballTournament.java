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
        if (!stages.isEmpty()) {
            TournamentStage lastStage = stages.get(stages.size() - 1);
            List<Competitor> winners = lastStage.getAdvancingCompetitors();

            if (winners.size() == 1) {
                overallWinner = winners.get(0);
            }
        }

        System.out.println(">>> TOURNAMENT FINISHED: " + name + " <<<");
    }

    @Override
    public Competitor getOverallWinner() {
        return this.overallWinner;
    }
}