package pl.kingdomcore.models;

import java.util.EnumSet;
import java.util.Set;

public enum KingdomRole {
    OWNER(EnumSet.allOf(RolePermission.class)),
    OFFICER(EnumSet.of(
            RolePermission.INVITE,
            RolePermission.KICK,
            RolePermission.BANK_DEPOSIT,
            RolePermission.BANK_WITHDRAW,
            RolePermission.SET_HOME,
            RolePermission.USE_HOME,
            RolePermission.BUY_UPGRADES
    )),
    MEMBER(EnumSet.of(
            RolePermission.BANK_DEPOSIT,
            RolePermission.USE_HOME
    )),
    RECRUIT(EnumSet.noneOf(RolePermission.class));

    private final Set<RolePermission> permissions;

    KingdomRole(Set<RolePermission> permissions) {
        this.permissions = permissions;
    }

    public boolean has(RolePermission permission) {
        return permissions.contains(permission);
    }
}
