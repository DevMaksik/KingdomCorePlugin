package pl.kingdomcore.managers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private final Map<String, Instant> cooldowns = new ConcurrentHashMap<>();

    public Optional<Duration> remaining(UUID player, String key) {
        Instant until = cooldowns.get(player + ":" + key);
        if (until == null || until.isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(Instant.now(), until));
    }

    public void put(UUID player, String key, Duration duration) {
        cooldowns.put(player + ":" + key, Instant.now().plus(duration));
    }
}
