package pl.kingdomcore.managers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InviteManager {
    private final Map<UUID, Invite> invites = new ConcurrentHashMap<>();
    private final Duration ttl;

    public InviteManager(Duration ttl) {
        this.ttl = ttl;
    }

    public void invite(UUID player, UUID kingdomId) {
        invites.put(player, new Invite(kingdomId, Instant.now().plus(ttl)));
    }

    public Optional<UUID> consume(UUID player) {
        Invite invite = invites.remove(player);
        if (invite == null || invite.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(invite.kingdomId());
    }

    private record Invite(UUID kingdomId, Instant expiresAt) {
    }
}
