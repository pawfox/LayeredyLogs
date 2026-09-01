package xyz.herberto.layeredyLogs.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import xyz.herberto.layeredyLogs.LayeredyLogs;

@CommandAlias("layeredylogs|logs")
public class LayeredyLogsCommand extends BaseCommand {
    private final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @HelpCommand
    @Default
    public void help(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @CommandPermission("layeredylogs.command.reload")
    @Description("Reload the Layeredy Logs config")
    public void reload(CommandSender sender) {
        LayeredyLogs.getInstance().reloadConfig();

        sender.sendMessage(LEGACY.deserialize("&aReloaded the Layeredy Logs config!"));
    }

}
