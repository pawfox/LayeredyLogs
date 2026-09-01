package xyz.herberto.layeredyLogs;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.herberto.layeredyLogs.commands.LayeredyLogsCommand;

import java.util.Arrays;

public final class LayeredyLogs extends JavaPlugin {
    @Getter public static LayeredyLogs instance;
    @Getter public static String loggingToken;

    @Override
    public void onEnable() {
        instance = this;

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");

        Arrays.asList(
                new LayeredyLogsCommand()
        ).forEach(command -> commandManager.registerCommand(command, true));

        loggingToken = getConfig().getString("layeredy-logs-token", "log_tok_1234567890abcdef");

        if(loggingToken.isEmpty() || loggingToken.equals("log_tok_1234567890abcdef")) {
            getLogger().warning("The logging token is not set in the config.yml file. Get a token at https://app.layeredy.com/dashboard/logs");
            getLogger().warning("Your server will NOT send logs until it is configured with a valid token.");
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
