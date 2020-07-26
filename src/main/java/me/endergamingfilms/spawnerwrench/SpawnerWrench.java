package me.endergamingfilms.spawnerwrench;

import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import me.endergamingfilms.spawnerwrench.utils.MessageUtils;
import me.endergamingfilms.spawnerwrench.utils.Protections;
import me.endergamingfilms.spawnerwrench.utils.Responses;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Objects;

public final class SpawnerWrench extends JavaPlugin implements Listener, CommandExecutor {
    public final MessageUtils messageUtils = new MessageUtils(this);
    public final Responses respond = new Responses(this);
    public final Protections protections = new Protections(this);
    private final ItemStack spawnerWrenchItem = new ItemStack(Material.DIAMOND_PICKAXE);
    public final NamespacedKey key = new NamespacedKey(this, "isWrench");

    @Override
    public void onEnable() {
        // Create Spawner-Wrench Item
        createWrenchItem();

        // Register Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);

        // Setup Region Protections
        if (pm.isPluginEnabled("Towny")) { // Enable Towny Protection
            protections.setupTownyProtection();
        }
        if (pm.isPluginEnabled("WorldGuard")) { // Enable WorldGuard Protection
            protections.setupWgProtection();
        }
        if (pm.isPluginEnabled("GriefPrevention")) { // Enable GriefPrevention Protection
            protections.setupGpProtection();
        }

    }

    private void createWrenchItem() {
        ItemMeta meta = spawnerWrenchItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(messageUtils.colorize("&b&lSpawner Wrench"));
        meta.setLore(Collections.singletonList(messageUtils.colorize("&7Right-click to break spawner")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "true");
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        spawnerWrenchItem.setItemMeta(meta);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 1 && args[0].equalsIgnoreCase("give")) {
            if (getServer().getPlayer(args[1]) != null) {
                Player passPlayer = getServer().getPlayer(args[1]);
                if (args.length > 2) { // Give player multiple wrenches
                    int amount = messageUtils.checkAmount(args[2]);
                    giveWrench(passPlayer, amount);
                } else { // Give player single wrench
                    giveWrench(passPlayer);
                }
                return true;
            } else {
                messageUtils.send(sender, respond.invalidPlayer());
                return false;
            }
        }

        if (!(sender instanceof Player)) {
            messageUtils.send(sender, respond.nonPlayer());
            return false;
        }

        Player player = (Player) sender;
        if (args.length < 1) {
            messageUtils.send(player, respond.getHelp());
            return false;
        }

        if (args[0].equalsIgnoreCase("get")) {
            // Check if player has permission
            if (!player.hasPermission("spawnerwrench.get")) {
                messageUtils.send(player, respond.noPerms());
                return false;
            }

            giveWrench(player);
        } else if (args[0].equalsIgnoreCase("give")) {
            // Check if player has permission
            if (!player.hasPermission("spawnerwrench.give")) {
                messageUtils.send(player, respond.noPerms());
                return false;
            }

            if (args.length > 1) {
                if (getServer().getPlayer(args[1]) != null) {
                    Player passPlayer = getServer().getPlayer(args[1]);
                    if (args.length > 2) { // Give player multiple wrenches
                        int amount = messageUtils.checkAmount(args[2]);
                        giveWrench(passPlayer, amount);
                    } else { // Give player single wrench
                        giveWrench(passPlayer);
                    }
                } else {
                    messageUtils.send(player, respond.invalidPlayer());
                }
            } else {
                messageUtils.send(player, respond.giveUsage());
            }
        } else {
            messageUtils.send(player, respond.getHelp());
        }

        return true;
    }

    @EventHandler
    void onBlockInteract(PlayerInteractEvent event) {
        // Only Run Code for main hand
        if(event.getHand() == EquipmentSlot.OFF_HAND) return;
        // If no item in hand then return
        Player player = event.getPlayer();
        if ((player.getItemInHand().getType() == Material.AIR)) return;
        // If the item has no item meta then return
        if (player.getItemInHand().getItemMeta() == null) return;
        // Only Check Right Hand (main hand)
        PersistentDataContainer container = player.getItemInHand().getItemMeta().getPersistentDataContainer();
        if (container.has(key, PersistentDataType.STRING)) {
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                Block clickedBlock = event.getClickedBlock();
                if (clickedBlock != null && clickedBlock.getType() == Material.SPAWNER) {
                    // Check if clicked block is protected
                    if (!protections.isLocationProtected(clickedBlock.getLocation(), player)) return;
                    // Remove spawner only if wrench is removed
                    if (takeWrench(player)) {
                        // Give player spawner and break one on ground
                        BlockState blockState = clickedBlock.getState();
                        StackedSpawner spawner = WildStackerAPI.getStackedSpawner((CreatureSpawner) blockState);
                        player.getInventory().addItem(spawner.getDropItem());
                        clickedBlock.setType(Material.AIR);
                        WildStackerAPI.getWildStacker().getSystemManager().removeStackObject(spawner);
                        // Play sound and send player message
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, SoundCategory.AMBIENT, 1.0f, 1.3f);
                        messageUtils.send(player, respond.wrenchUsed());
                    }
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    public void giveWrench(Player player) {
        player.getInventory().addItem(spawnerWrenchItem);
    }

    public void giveWrench(Player player, int amount) {
        ItemStack multiWrench = new ItemStack(spawnerWrenchItem);
        multiWrench.setAmount(amount);
        player.getInventory().addItem(multiWrench);
    }

    public boolean takeWrench(Player player) {
        for (ItemStack stack : player.getInventory()) {
            if (stack != null && stack.getType() != Material.AIR) {
                if (stack.getItemMeta() != null) {
                    // If the wrench item is found
                    if (stack.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        if (stack.getAmount() > 1) { // Case 1 - wrench item is in stack > 1
                            stack.setAmount(stack.getAmount() - 1);
                            return true;
                        } else { // Case 2 - wrench is in stack == 1
                            stack.setAmount(0);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
