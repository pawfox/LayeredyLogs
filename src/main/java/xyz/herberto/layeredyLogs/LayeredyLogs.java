package xyz.herberto.layeredyLogs;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.herberto.layeredyLogs.commands.LayeredyLogsCommand;
import xyz.herberto.layeredyLogs.logging.LayeredyLogAppender;

import java.util.Arrays;

public final class LayeredyLogs extends JavaPlugin {
    @Getter
    private static LayeredyLogs instance;
    private static String loggingToken;
    private LayeredyLogAppender layeredyAppender;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

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

        layeredyAppender = new LayeredyLogAppender(
                loggingToken,
                loggingToken,
                "minecraft-server"
        );
        layeredyAppender.install();

    }


    @Override
    public void onDisable() {
        if (layeredyAppender != null) {
            layeredyAppender.uninstall();
        }
    }
}
