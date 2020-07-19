package me.endergamingfilms.spawnerwrench.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.endergamingfilms.spawnerwrench.SpawnerWrench;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import scala.Array;

import java.util.*;

public class Protections implements Listener {
    private final SpawnerWrench plugin;
    private TownyAPI townyInstance;
    private WorldGuard wgInstnace;
    private GriefPrevention gpInstance;

    public boolean hasTowny = false;
    public boolean hasWorldGuard = false;
    public boolean hasGriefPrevention = false;

    public Protections(@NotNull final SpawnerWrench instance) {
        plugin = instance;
    }

    public void setupTownyProtection() {
        townyInstance = TownyAPI.getInstance();
        hasTowny = true;
    }

    public void setupGpProtection() {
        gpInstance = GriefPrevention.instance;
        hasGriefPrevention = true;
    }

    public void setupWgProtection() {
        wgInstnace = WorldGuard.getInstance();
        hasWorldGuard = true;
    }

    public boolean isLocationProtected(Location location, Player player) {
        // Protection checks for WorldGuard/Towny/GriefPrevention
        if (hasWorldGuard) {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            boolean canBypass = wgInstnace.getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld());
            RegionContainer container = wgInstnace.getPlatform().getRegionContainer();
            BlockVector3 blockLoc = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            // Check if the player is a member of the region
            if (!Objects.requireNonNull(container.get(localPlayer.getWorld())).getApplicableRegions(blockLoc).isMemberOfAll(localPlayer)) {
                if (canBypass) // Allow bypass permission
                    return true;
                return false;
            }
        }

        if (hasTowny) {
            if (townyInstance.isTownyWorld(player.getWorld()) && !townyInstance.isWilderness(location)) {
                // If player has bypass perms (towny.admin)
                if (player.hasPermission("towny.admin")) return true;
                // Check if the player is in the town
                TownBlock townBlock = townyInstance.getTownBlock(location);
                try {
                    Town town = townBlock.getTown();
                    // If player is not in the town
                    if (!town.hasResident(player.getName())) {
                        plugin.messageUtils.send(player, plugin.respond.areaProtected());
                        return false;
                    } else { // If the player is in the town
                        // If player is mayor then (Allow)
                        if (player.getName().equals(town.getMayor().getName())) return true;
                        // If residents are not allows to destroy blocks in plot
                        if (!townBlock.getPermissions().getResidentPerm(TownyPermission.ActionType.DESTROY)) {
                            plugin.messageUtils.send(player, plugin.respond.areaProtected());
                            return false;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        if (hasGriefPrevention) {
            if (gpInstance.claimsEnabledForWorld(player.getWorld())) {
                // If Player is ignoring claims (Allow)
                if (gpInstance.dataStore.getPlayerData(player.getUniqueId()).ignoreClaims) return true;
                // If there is a claim at location
                Claim newClaim = gpInstance.dataStore.getClaimAt(location, true, null);
                if (newClaim != null) {
                    // Admin Claim (Deny)
                    if (newClaim.isAdminClaim()) {
                        plugin.messageUtils.send(player, plugin.respond.areaProtected());
                        return false;
                    }
                    // Check if player is owner of claim (this is a short)
                    if (newClaim.getOwnerName().equals(player.getDisplayName())) return true;
                    // Get players who have access to claim
                    ArrayList<ArrayList<String>> arrayLists = new ArrayList<>();
                    ArrayList<String> builders = new ArrayList<>();
                    ArrayList<String> accessors = new ArrayList<>();
                    ArrayList<String> managers = new ArrayList<>();
                    // Get claim data
                    newClaim.getPermissions(builders, null, accessors, managers);
                    // Add ArrayLists together
                    arrayLists.add(builders);
//                    arrayLists.add(accessors);
//                    arrayLists.add(managers);
                    // Go though all lists looking for player
                    boolean found = false;
                    for (ArrayList<String> list : arrayLists) {
                        for (String s : list)
                            if (s.contains(player.getUniqueId().toString()))
                                found = true;
                    }
                    // Return weather the player has access to the claim or not
                    if (!found)
                        plugin.messageUtils.send(player, plugin.respond.areaProtected());
                    return found;
                }
            }
        }
        // If location is not protected return true
        return true;
    }

}
