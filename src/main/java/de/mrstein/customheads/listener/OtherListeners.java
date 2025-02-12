package de.mrstein.customheads.listener;

import de.mrstein.customheads.CustomHeads;
import de.mrstein.customheads.updaters.SpigetFetcher;
import de.mrstein.customheads.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;

import static de.mrstein.customheads.utils.Utils.sendJSONMessage;

/*
 *  Project: CustomHeads in OtherListeners
 *     by LikeWhat
 */

public class OtherListeners implements Listener {

    public static HashMap<Player, Location> saveLoc = new HashMap<>();

    @EventHandler
    public void fireworkbreak(BlockBreakEvent e) {
        if (saveLoc.containsValue(e.getBlock().getLocation().add(.5, 0, .5))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (saveLoc.containsKey(e.getPlayer())) {
            saveLoc.get(e.getPlayer()).getBlock().setType(Material.AIR);
            saveLoc.remove(e.getPlayer());
        }
    }

    @EventHandler
    public void notifyUpdate(PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        if (Utils.hasPermission(player, "heads.admin") && CustomHeads.getHeadsConfig().get().getBoolean("update-notifications.onJoin")) {
            CustomHeads.getSpigetFetcher().fetchUpdates(new SpigetFetcher.FetchResult() {
                public void updateAvailable(SpigetFetcher.ResourceRelease release, SpigetFetcher.ResourceUpdate update) {
                    sendJSONMessage("[\"\",{\"text\":\"§6-- CustomHeads Updater --\n§eFound new Update!\n§7Version: §e" + release.getReleaseName() + "\n§7Whats new: §e" + update.getTitle() + "\n\"},{\"text\":\"§6§nClick here to download the Update\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://www.spigotmc.org/resources/29057\"}}]", player);
                }

                public void noUpdate() {
                }
            });
        }
    }

}
