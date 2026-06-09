package pl.kingdomcore.services;

import pl.kingdomcore.models.Kingdom;

import java.util.Comparator;
import java.util.List;

public class RankingService {
    private final KingdomService kingdomService;

    public RankingService(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
    }

    public List<Kingdom> topByPrestige(int limit) {
        return kingdomService.getKingdoms().stream()
                .sorted(Comparator.comparingLong(Kingdom::getPrestige).reversed())
                .limit(limit)
                .toList();
    }

    public List<Kingdom> topByMoney(int limit) {
        return kingdomService.getKingdoms().stream()
                .sorted(Comparator.comparingDouble(Kingdom::getBank).reversed())
                .limit(limit)
                .toList();
    }

    public List<Kingdom> topByKills(int limit) {
        return kingdomService.getKingdoms().stream()
                .sorted(Comparator.comparingLong((Kingdom kingdom) -> kingdom.getStats().getPlayerKills()).reversed())
                .limit(limit)
                .toList();
    }
}
