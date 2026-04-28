package org.example.sportstracker.football.domain.stage;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.sportstracker.core.domain.match.Match;
import org.example.sportstracker.core.domain.match.MatchStatus;
import org.example.sportstracker.core.domain.tournament.AbstractTournamentStage;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.match.FootballMatch;
import org.example.sportstracker.football.domain.table.LeagueTable;
import org.example.sportstracker.football.domain.table.TableEntry;

import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class FootballLeagueStage extends AbstractTournamentStage {
    private LeagueTable table = new LeagueTable();
    private int pointsForWin = 3;

    public FootballLeagueStage(List<Team> teams) {
        teams.forEach(table::registerTeam);
    }

    public FootballLeagueStage(List<Team> teams, int pointsForWin) {
        this.pointsForWin = pointsForWin;
        teams.forEach(table::registerTeam);
    }

    @Override
    public void startStage() {
        System.out.println("\n>>> START FAZY LIGOWEJ <<<");
    }

    public void calculateStandings() {
        table.clearStats();

        for (Match match : stageMatches) {
            if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER) {
                table.processMatch((FootballMatch) match, pointsForWin);
            }
        }

        table.sortByPointsAndGoalDifference();
    }

    public List<Team> getRelegatedTeams() {
        return table.getRows().stream()
                .skip(Math.max(0, table.getRows().size() - 2))
                .map(TableEntry::getTeam)
                .collect(Collectors.toList());
    }

    @Override
    protected void verifyMatchesCompleted() {
        for (Match match : stageMatches) {
            if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.WALKOVER) {
                throw new IllegalStateException("Nie wszystkie mecze ligowe zostały zakończone");
            }
        }
    }

    @Override
    protected void processResults() {
        calculateStandings();
    }

    @Override
    protected void determineAdvancingTeams() {
        advancingTeams = table.getRows().stream()
                .limit(4)
                .map(TableEntry::getTeam)
                .collect(Collectors.toList());
    }

    @Override
    protected void printStageSummary() {
        System.out.println("\n>>> KONIEC FAZY LIGOWEJ - TABELA KOŃCOWA <<<");
        table.printTable();
    }
}