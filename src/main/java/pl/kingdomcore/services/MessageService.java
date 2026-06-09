package pl.kingdomcore.services;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.utils.Text;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MessageService {
    private final KingdomCorePlugin plugin;
    private FileConfiguration messages;

    public MessageService(KingdomCorePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public String raw(String path) {
        return messages.getString(path, path);
    }

    public String raw(String path, Map<String, String> placeholders) {
        return Text.raw(raw(path), placeholders);
    }

    public List<String> list(String path) {
        return messages.getStringList(path);
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Collections.emptyMap());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String prefix = messages.getString("prefix", "");
        sender.sendMessage(Text.color(prefix + raw(path, placeholders)));
    }

    public void sendLines(CommandSender sender, String path, Map<String, String> placeholders) {
        String prefix = messages.getString("prefix", "");
        for (String line : list(path)) {
            sender.sendMessage(Text.color(prefix + Text.raw(line, placeholders)));
        }
    }
}
