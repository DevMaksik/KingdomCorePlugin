package pl.kingdomcore.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.database.KingdomRepository;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.UUID;

public class EconomyService {
    private final KingdomCorePlugin plugin;
    private final KingdomRepository repository;
    private final Object vaultProvider;
    private final Method vaultWithdraw;
    private final Method vaultDeposit;
    private final Method vaultBalance;
    private final Method vaultSuccess;

    public EconomyService(KingdomCorePlugin plugin, KingdomRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        VaultMethods vault = hookVault();
        this.vaultProvider = vault.provider;
        this.vaultWithdraw = vault.withdraw;
        this.vaultDeposit = vault.deposit;
        this.vaultBalance = vault.balance;
        this.vaultSuccess = vault.success;
    }

    public boolean isVaultEnabled() {
        return vaultProvider != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (isVaultEnabled()) {
            try {
                return ((Number) vaultBalance.invoke(vaultProvider, player)).doubleValue();
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().warning("Vault getBalance failed, using local economy: " + exception.getMessage());
            }
        }
        try {
            return repository.getBalance(player.getUniqueId(), plugin.getConfig().getDouble("economy.starting-balance", 250.0));
        } catch (SQLException exception) {
            plugin.getLogger().warning("Local economy read failed: " + exception.getMessage());
            return 0.0;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return false;
        }
        if (isVaultEnabled()) {
            try {
                Object response = vaultWithdraw.invoke(vaultProvider, player, amount);
                return (boolean) vaultSuccess.invoke(response);
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().warning("Vault withdraw failed, using local economy: " + exception.getMessage());
            }
        }
        double balance = getBalance(player);
        if (balance < amount) {
            return false;
        }
        return setLocal(player.getUniqueId(), balance - amount);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return false;
        }
        if (isVaultEnabled()) {
            try {
                Object response = vaultDeposit.invoke(vaultProvider, player, amount);
                return (boolean) vaultSuccess.invoke(response);
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().warning("Vault deposit failed, using local economy: " + exception.getMessage());
            }
        }
        return setLocal(player.getUniqueId(), getBalance(player) + amount);
    }

    public boolean setLocal(UUID uuid, double amount) {
        try {
            repository.setBalance(uuid, amount);
            return true;
        } catch (SQLException exception) {
            plugin.getLogger().warning("Local economy write failed: " + exception.getMessage());
            return false;
        }
    }

    private VaultMethods hookVault() {
        if (!plugin.getConfig().getBoolean("economy.prefer-vault", true)
                || plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return VaultMethods.empty();
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = plugin.getServer().getServicesManager().getRegistration(economyClass);
            if (registration == null) {
                return VaultMethods.empty();
            }
            Object provider = registration.getProvider();
            Method withdraw = provider.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            Method deposit = provider.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
            Method balance = provider.getClass().getMethod("getBalance", OfflinePlayer.class);
            Method success = Class.forName("net.milkbowl.vault.economy.EconomyResponse").getMethod("transactionSuccess");
            plugin.getLogger().info("Vault economy hooked.");
            return new VaultMethods(provider, withdraw, deposit, balance, success);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Vault hook failed, local economy enabled: " + exception.getMessage());
            return VaultMethods.empty();
        }
    }

    private record VaultMethods(Object provider, Method withdraw, Method deposit, Method balance, Method success) {
        static VaultMethods empty() {
            return new VaultMethods(null, null, null, null, null);
        }
    }
}
