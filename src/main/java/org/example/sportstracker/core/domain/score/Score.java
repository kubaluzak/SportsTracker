package org.example.sportstracker.core.domain.score;

import org.example.sportstracker.core.domain.match.MatchEvent;

public interface Score {
    String display();
    void update(MatchEvent event);
}