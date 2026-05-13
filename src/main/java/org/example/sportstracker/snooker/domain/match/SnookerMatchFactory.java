package org.example.sportstracker.snooker.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;
import org.example.sportstracker.core.domain.match.Match;
import org.example.sportstracker.core.domain.match.MatchFactory;
import org.example.sportstracker.snooker.domain.competitor.Player;
import org.example.sportstracker.snooker.domain.strategy.SnookerMatchResolutionStrategy;

public class SnookerMatchFactory implements MatchFactory {
    private final int defaultBestOfFrames;

    public SnookerMatchFactory(int defaultBestOfFrames) {
        this.defaultBestOfFrames = defaultBestOfFrames;
    }

    @Override
    public Match createLeagueMatch(Competitor home, Competitor away) {
        // In Snooker, "Home" and "Away" are abstract; they just map to P1 and P2
        return new SnookerMatch((Player) home, (Player) away, defaultBestOfFrames, new SnookerMatchResolutionStrategy(defaultBestOfFrames));
    }

    @Override
    public Match createKnockoutMatch(Competitor home, Competitor away) {
        return new SnookerMatch((Player) home, (Player) away, defaultBestOfFrames, new SnookerMatchResolutionStrategy(defaultBestOfFrames));
    }
}