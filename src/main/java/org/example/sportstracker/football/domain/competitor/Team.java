package org.example.sportstracker.football.domain.competitor;

import lombok.Builder;
import lombok.Data;
import org.example.sportstracker.core.domain.competitor.Competitor;

import java.util.List;

@Data
@Builder
public class Team implements Competitor {
    private String id;
    private String name;
    
    private List<String> players;
    private String manager;
}