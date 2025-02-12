package de.mrstein.customheads.economy;

import de.mrstein.customheads.CustomHeads;
import de.mrstein.customheads.api.CustomHeadsPlayer;
import de.mrstein.customheads.category.Category;
import de.mrstein.customheads.category.CustomHead;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.plugin.RegisteredServiceProvider;

import static de.mrstein.customheads.utils.Utils.openCategory;
import static org.bukkit.Bukkit.getServer;

/*
 *  Project: CustomHeads in EconomyManager
 *     by LikeWhat
 */

@Getter
public class EconomyManager {

    private Economy economyPlugin;

    public EconomyManager() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economyPlugin = economyProvider.getProvider();
        }
    }

    public void buyCategory(CustomHeadsPlayer customHeadsPlayer, Category category, String prevMenu) {
        EconomyResponse economyResponse = economyPlugin.withdrawPlayer(customHeadsPlayer.unwrap(), category.getPrice());
        if (economyResponse.transactionSuccess()) {
            customHeadsPlayer.unlockCategory(category);
            openCategory(category, customHeadsPlayer.unwrap(), new String[]{"openMenu", prevMenu});
            customHeadsPlayer.unwrap().sendMessage(CustomHeads.getLanguageManager().ECONOMY_BUY_SUCCESSFUL.replace("{ITEM}", category.getPlainName()));
        } else {
            customHeadsPlayer.unwrap().sendMessage(CustomHeads.getLanguageManager().ECONOMY_BUY_FAILED.replace("{REASON}", economyResponse.errorMessage).replace("{ITEM}", category.getPlainName()));
        }
    }

    public void buyHead(CustomHeadsPlayer customHeadsPlayer, Category category, int id, String prevCategory, boolean permanent) {
        CustomHead customHead = CustomHeads.getApi().getHead(category, id);
        if (customHead == null) {
            CustomHeads.getInstance().getLogger().warning("Received invalid Head ID while buying (" + category.getId() + ":" + id + ")");
            return;
        }
        EconomyResponse economyResponse = economyPlugin.withdrawPlayer(customHeadsPlayer.unwrap(), customHead.getPrice());
        if (economyResponse.transactionSuccess()) {
            if (permanent) {
                customHeadsPlayer.unlockHead(category, id);
            } else {
                customHeadsPlayer.unwrap().getInventory().addItem(customHead.getPlainItem());
            }
            openCategory(category, customHeadsPlayer.unwrap(), new String[]{"openCategory", prevCategory});
            customHeadsPlayer.unwrap().sendMessage(CustomHeads.getLanguageManager().ECONOMY_BUY_SUCCESSFUL.replace("{ITEM}", customHead.getItemMeta().getDisplayName()));
        } else {
            customHeadsPlayer.unwrap().sendMessage(CustomHeads.getLanguageManager().ECONOMY_BUY_FAILED.replace("{REASON}", economyResponse.errorMessage).replace("{ITEM}", customHead.getItemMeta().getDisplayName()));
        }
    }

}
