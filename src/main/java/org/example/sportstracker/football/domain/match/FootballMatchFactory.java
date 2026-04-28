package org.example.sportstracker.football.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.Match;
import org.example.sportstracker.core.domain.match.MatchFactory;
import org.example.sportstracker.football.domain.competitor.Team;
import org.example.sportstracker.football.domain.strategy.KnockoutMatchResolutionStrategy;
import org.example.sportstracker.football.domain.strategy.LeagueMatchResolutionStrategy;

public class FootballMatchFactory implements MatchFactory {
    @Override
    public Match createLeagueMatch(Competitor home, Competitor away) {
        return new FootballMatch((Team) home, (Team) away, new LeagueMatchResolutionStrategy());
    }

    @Override
    public Match createKnockoutMatch(Competitor home, Competitor away) {
        return new FootballMatch((Team) home, (Team) away, new KnockoutMatchResolutionStrategy());
    }
}