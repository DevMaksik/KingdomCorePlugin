package pl.kingdomcore;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.kingdomcore.commands.KingdomAdminCommand;
import pl.kingdomcore.commands.KingdomCommand;
import pl.kingdomcore.database.DatabaseProvider;
import pl.kingdomcore.database.JdbcDatabaseProvider;
import pl.kingdomcore.database.KingdomRepository;
import pl.kingdomcore.economy.EconomyService;
import pl.kingdomcore.gui.KingdomMenu;
import pl.kingdomcore.listeners.PlayerProgressListener;
import pl.kingdomcore.managers.InviteManager;
import pl.kingdomcore.placeholder.KingdomPlaceholderExpansion;
import pl.kingdomcore.services.AuditService;
import pl.kingdomcore.services.BossService;
import pl.kingdomcore.services.KingdomService;
import pl.kingdomcore.services.MessageService;
import pl.kingdomcore.services.QuestService;
import pl.kingdomcore.services.RankingService;
import pl.kingdomcore.services.UpgradeService;
import pl.kingdomcore.tasks.AutosaveTask;
import pl.kingdomcore.tasks.QuestResetTask;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;

public class KingdomCorePlugin extends JavaPlugin {
    private MessageService messageService;
    private DatabaseProvider databaseProvider;
    private KingdomRepository kingdomRepository;
    private EconomyService economyService;
    private InviteManager inviteManager;
    private UpgradeService upgradeService;
    private QuestService questService;
    private AuditService auditService;
    private KingdomService kingdomService;
    private RankingService rankingService;
    private BossService bossService;
    private KingdomMenu kingdomMenu;
    private KingdomCommand kingdomCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("kingdoms.yml");

        messageService = new MessageService(this);
        databaseProvider = new JdbcDatabaseProvider(this);
        try {
            databaseProvider.init();
        } catch (SQLException exception) {
            getLogger().severe("Database initialization failed: " + exception.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        kingdomRepository = new KingdomRepository(databaseProvider);
        economyService = new EconomyService(this, kingdomRepository);
        inviteManager = new InviteManager(Duration.ofSeconds(getConfig().getLong("kingdom.invite-expire-seconds", 120)));
        upgradeService = new UpgradeService(getConfig());
        questService = new QuestService(this, upgradeService);
        auditService = new AuditService(this, kingdomRepository);
        kingdomService = new KingdomService(this, kingdomRepository, economyService, inviteManager, upgradeService, questService, auditService);
        rankingService = new RankingService(kingdomService);
        bossService = new BossService(this);
        kingdomMenu = new KingdomMenu(this);

        try {
            kingdomService.load();
        } catch (SQLException exception) {
            getLogger().severe("Could not load kingdoms: " + exception.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        registerCommands();
        Bukkit.getPluginManager().registerEvents(kingdomMenu, this);
        Bukkit.getPluginManager().registerEvents(bossService, this);
        Bukkit.getPluginManager().registerEvents(new PlayerProgressListener(this), this);

        long autosave = Math.max(1, getConfig().getLong("kingdom.autosave-minutes", 5)) * 60L * 20L;
        new AutosaveTask(this).runTaskTimerAsynchronously(this, autosave, autosave);
        new QuestResetTask(this).runTaskTimer(this, 20L * 60L, 20L * 60L);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KingdomPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }
        getLogger().info("KingdomCore enabled.");
    }

    @Override
    public void onDisable() {
        if (kingdomService != null) {
            kingdomService.saveAllSync();
        }
        if (databaseProvider != null) {
            databaseProvider.shutdown();
        }
    }

    private void registerCommands() {
        kingdomCommand = new KingdomCommand(this);
        KingdomAdminCommand adminCommand = new KingdomAdminCommand(this);
        PluginCommand kingdom = Objects.requireNonNull(getCommand("kingdom"));
        kingdom.setExecutor(kingdomCommand);
        kingdom.setTabCompleter(kingdomCommand);
        PluginCommand admin = Objects.requireNonNull(getCommand("kingdomadmin"));
        admin.setExecutor(adminCommand);
        admin.setTabCompleter(adminCommand);
    }

    private void saveResourceIfMissing(String resource) {
        if (!new java.io.File(getDataFolder(), resource).exists()) {
            saveResource(resource, false);
        }
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public UpgradeService getUpgradeService() {
        return upgradeService;
    }

    public QuestService getQuestService() {
        return questService;
    }

    public KingdomService getKingdomService() {
        return kingdomService;
    }

    public RankingService getRankingService() {
        return rankingService;
    }

    public BossService getBossService() {
        return bossService;
    }

    public KingdomMenu getKingdomMenu() {
        return kingdomMenu;
    }

    public KingdomCommand getKingdomCommand() {
        return kingdomCommand;
    }
}
