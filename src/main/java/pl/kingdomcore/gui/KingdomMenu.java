package pl.kingdomcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.models.Kingdom;
import pl.kingdomcore.models.KingdomQuest;
import pl.kingdomcore.models.KingdomUpgradeType;
import pl.kingdomcore.models.SerializedLocation;
import pl.kingdomcore.services.KingdomService;
import pl.kingdomcore.utils.ItemBuilder;
import pl.kingdomcore.utils.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KingdomMenu implements Listener {
    private final KingdomCorePlugin plugin;

    public KingdomMenu(KingdomCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        Optional<Kingdom> optional = plugin.getKingdomService().getByPlayer(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(new Holder(MenuType.MAIN), 27, Text.color(plugin.getMessageService().raw("gui.title")));
        if (optional.isEmpty()) {
            inventory.setItem(13, new ItemBuilder(Material.BARRIER)
                    .name(plugin.getMessageService().raw("gui.no-kingdom"))
                    .lore(List.of(plugin.getMessageService().raw("gui.no-kingdom-lore")))
                    .build());
            player.openInventory(inventory);
            return;
        }
        Kingdom kingdom = optional.get();
        inventory.setItem(10, new ItemBuilder(Material.BOOK).name(plugin.getMessageService().raw("gui.info"))
                .lore(List.of("&7Nazwa: &f" + kingdom.getName(), "&7Poziom: &f" + kingdom.getLevel(), "&7Prestiż: &f" + kingdom.getPrestige())).build());
        inventory.setItem(11, new ItemBuilder(Material.PLAYER_HEAD).name(plugin.getMessageService().raw("gui.members"))
                .lore(kingdom.getMembers().values().stream().map(member -> "&7- &f" + member.getName() + " &8(" + member.getRole() + ")").toList()).build());
        inventory.setItem(12, new ItemBuilder(Material.GOLD_INGOT).name(plugin.getMessageService().raw("gui.bank"))
                .lore(List.of("&7Saldo: &e" + kingdom.getBank())).build());
        inventory.setItem(13, new ItemBuilder(Material.NETHER_STAR).name(plugin.getMessageService().raw("gui.upgrades"))
                .lore(List.of("&7Kliknij, aby kupić ulepszenia.")).build());
        inventory.setItem(14, new ItemBuilder(Material.WRITABLE_BOOK).name(plugin.getMessageService().raw("gui.quests"))
                .lore(List.of(plugin.getMessageService().raw("gui.quests-lore"))).build());
        inventory.setItem(15, new ItemBuilder(Material.DIAMOND).name(plugin.getMessageService().raw("gui.ranking"))
                .lore(plugin.getRankingService().topByPrestige(5).stream().map(k -> "&7" + k.getName() + ": &e" + k.getPrestige()).toList()).build());
        inventory.setItem(16, new ItemBuilder(Material.ENDER_PEARL).name(plugin.getMessageService().raw("gui.home"))
                .lore(List.of(plugin.getMessageService().raw("gui.home-click"))).build());
        player.openInventory(inventory);
    }

    private void openUpgrades(Player player, Kingdom kingdom) {
        Inventory inventory = Bukkit.createInventory(new Holder(MenuType.UPGRADES), 27, Text.color(plugin.getMessageService().raw("gui.upgrades-title")));
        int slot = 10;
        for (KingdomUpgradeType type : KingdomUpgradeType.values()) {
            int next = kingdom.getUpgradeLevel(type) + 1;
            List<String> lore = new ArrayList<>();
            lore.add("&7Poziom: &f" + kingdom.getUpgradeLevel(type) + "&8/&f" + plugin.getUpgradeService().maxLevel(type));
            lore.add("&7Koszt: &e" + plugin.getUpgradeService().moneyCost(type, next) + " monet");
            lore.add("&7Prestiż: &d" + plugin.getUpgradeService().prestigeCost(type, next));
            lore.add(plugin.getMessageService().raw("gui.upgrade-click"));
            inventory.setItem(slot++, new ItemBuilder(type.getIcon()).name("&b" + type.getDisplayName()).lore(lore).build());
        }
        player.openInventory(inventory);
    }

    private void openQuests(Player player, Kingdom kingdom) {
        Inventory inventory = Bukkit.createInventory(new Holder(MenuType.QUESTS), 36, Text.color(plugin.getMessageService().raw("gui.quests-title")));
        int slot = 10;
        for (KingdomQuest quest : kingdom.getQuests()) {
            inventory.setItem(slot++, new ItemBuilder(quest.isCompleted() ? Material.LIME_DYE : Material.PAPER)
                    .name((quest.isCompleted() ? "&a" : "&d") + quest.getName())
                    .lore(List.of(
                            "&7" + quest.getDescription(),
                            "&7Postęp: &f" + quest.getProgress() + "&8/&f" + quest.getTarget(),
                            "&7Nagroda: &e" + quest.getMoneyReward() + " &7+ &d" + quest.getPrestigeReward() + " prestiżu"
                    ))
                    .build());
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        Optional<Kingdom> optional = plugin.getKingdomService().getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            return;
        }
        Kingdom kingdom = optional.get();
        if (holder.type() == MenuType.MAIN) {
            if (event.getSlot() == 13) {
                openUpgrades(player, kingdom);
            } else if (event.getSlot() == 14) {
                openQuests(player, kingdom);
            } else if (event.getSlot() == 16) {
                SerializedLocation home = kingdom.getHome();
                if (home == null || home.toBukkit() == null) {
                    plugin.getMessageService().send(player, "kingdom.no-home");
                    return;
                }
                player.closeInventory();
                player.teleport(home.toBukkit());
            }
            return;
        }
        if (holder.type() == MenuType.UPGRADES && event.getCurrentItem() != null) {
            int index = event.getSlot() - 10;
            if (index >= 0 && index < KingdomUpgradeType.values().length) {
                KingdomUpgradeType type = KingdomUpgradeType.values()[index];
                var result = plugin.getKingdomService().buyUpgrade(player, type);
                if (result == KingdomService.Result.SUCCESS) {
                    plugin.getMessageService().send(player, "kingdom.upgrade-bought", Map.of("upgrade", type.getDisplayName(), "level", String.valueOf(kingdom.getUpgradeLevel(type))));
                    openUpgrades(player, kingdom);
                } else {
                    plugin.getKingdomCommand().sendResult(player, result);
                }
            }
        }
    }

    private record Holder(MenuType type) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private enum MenuType {
        MAIN,
        UPGRADES,
        QUESTS
    }
}
