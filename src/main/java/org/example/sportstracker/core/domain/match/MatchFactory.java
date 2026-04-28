package org.example.sportstracker.core.domain.match;

import org.example.sportstracker.core.domain.competitor.Competitor;

public interface MatchFactory {
    Match createLeagueMatch(Competitor home, Competitor away);
    Match createKnockoutMatch(Competitor home, Competitor away);
}