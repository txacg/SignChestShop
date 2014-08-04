package co.technius.signchestshop;

import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NBTTagList;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import co.technius.signchestshop.Shop.ShopMode;

/**
 * This class contains API methods
 * for SignChestShop
 */
@Deprecated
public class SignChestShopAPI {

    private final SignChestShopPlugin plugin;

    protected SignChestShopAPI(final SignChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @deprecated This method will do nothing for sell shops. Use {@link Shop#open(Player)} instead.
     *             Opens a buy shop for the specified player.
     * @param player - The player who is purchasing the items
     * @param block - The shop
     * @return The {@link org.bukkit.inventory.InventoryView} associated with the transaction,
     *         or null if the shop is not found or if the shop is not a buy shop.
     * @see Shop#open(Player)
     */
    @Deprecated
    public InventoryView beginBuying(final Player player, final Block block) {
        final Shop s = getShop(block);
        if (s == null)
            return null;
        return s.getMode() != ShopMode.BUY ? null : s.open(player);
    }

    /**
     * Returns the shop at the given location.
     *
     * @param block - The block where the shop is located
     * @return The shop, or null if not found
     */
    public Shop getShop(final Block block) {
        return plugin.getShop(block);
    }

    /**
     * Returns the shop at the given location.
     * Same as calling getShop(loc.getBlock());
     *
     * @param loc - The location of the shop
     * @return The shop, or null if not found
     * @see #getShop(Block)
     */
    public Shop getShop(final Location loc) {
        return getShop(loc.getBlock());
    }

    /**
     * Returns the inventory of the shop that a player would see.
     * If you are trying to have a player start a transaction, use {@link #getShop(Block)} and then
     * call the shop object's {@link Shop#open(Player)} method.
     * This method has the same effect as calling getShopInventory(block, showPrice, "Buy")
     *
     * @param block - The shop's location
     * @param showPrice - If items should get "Price: &lt;price&gt;" appended to their lore
     * @return The inventory the player would see
     * @see #getShopInventory(Block, boolean, String)
     */
    public Inventory getShopInventory(final Block block, final boolean showPrice) {
        return getShopInventory(block, showPrice, "Buy");
    }

    /**
     * Returns the inventory of the shop that a player would see.
     * If you are trying to have a player start a transaction, use {@link #getShop(Block)} and then
     * call the shop object's {@link Shop#open(Player)} method.
     *
     * @param block - The shop's location
     * @param showPrice - If items should get "Price: &lt;price&gt;" appended to their lore
     * @param title - The title of the shop
     * @return The inventory the player would see
     * @see #getShopInventory(Block, boolean)
     */
    public Inventory getShopInventory(final Block block, final boolean showPrice, final String title) {
        final NBTTagCompound c = plugin.getShopData(block);
        return c == null ? null : plugin.getShop(c, showPrice, title);
    }

    /**
     * Returns all shops.
     *
     * @return All shops.
     */
    public Shop[] getShops() {
        final NBTTagList shops = plugin.data.getList("Shops", 10);
        final Shop[] a = new Shop[shops.size()];
        for (int i = 0; i < shops.size(); i++) {
            a[i] = new Shop(shops.get(i));
        }
        return a;
    }
}
