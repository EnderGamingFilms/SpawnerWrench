package me.endergamingfilms.spawnerwrench.utils;

import me.endergamingfilms.spawnerwrench.SpawnerWrench;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import static me.endergamingfilms.spawnerwrench.utils.MessageUtils.NL;

public class Responses {
    private final SpawnerWrench plugin;
    public Responses(@NotNull final SpawnerWrench instance) {
        plugin = instance;
    }

    /** |-------------- Basic Responses --------------| */
    public String noPerms() {
        return plugin.messageUtils.format("&7Unknown Command", false);
    }

    public String nonPlayer() {
        return plugin.messageUtils.format("&cOnly players can run this command.");
    }

    public String invalidPlayer() {
        return plugin.messageUtils.format("&cThat player is offline or invalid.");
    }

    public String giveUsage() {
        return plugin.messageUtils.format("&6/wrench give &f[playerName] - &eGives a wrench to a player.", false);
    }

    public String getUsage() {
        return plugin.messageUtils.format("&6/wrench get &f- &eGives you a wrench", false);
    }

    public String wrenchUsed() {
        return plugin.messageUtils.format("&7Your wrench disappears. You picked up the spawner.");
    }

    public String areaProtected() {
        return plugin.messageUtils.format("&cYou do not have access to this area.");
    }

    public TextComponent getHelp() {
        TextComponent message = new TextComponent();
        message.addExtra(plugin.messageUtils.colorize("&7-------------------- &6Wrench Help &7--------------------" + NL));
        message.addExtra(giveUsage() + NL);
        message.addExtra(getUsage() + NL);
        message.addExtra(plugin.messageUtils.colorize("       &7Author: " + plugin.getDescription().getAuthors().get(0) +
                "&7       |       Version: " + plugin.getDescription().getVersion()));
        return message;
    }
}
