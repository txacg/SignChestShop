package net.skycraftmc.SignChestShop;

import net.minecraft.server.v1_4_R1.NBTTagCompound;
import net.minecraft.server.v1_4_R1.NBTTagList;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

/**
 * This class contains API methods
 * for SignChestShop
 */
public class SignChestShopAPI 
{
	private SignChestShopPlugin plugin;
	protected SignChestShopAPI(SignChestShopPlugin plugin)
	{
		this.plugin = plugin;
	}
	/**
	 * Returns the shop at the given location.
	 * @param block - The block where the shop is located
	 * @return The shop, or null if not found
	 */
	public Shop getShop(Block block)
	{
		NBTTagCompound c = plugin.getShopData(block);
		if(c == null)return null;
		return new Shop(c);
	}
	/**
	 * Returns the shop at the given location.
	 * Same as calling getShop(loc.getBlock());
	 * @param loc - The location of the shop
	 * @return The shop, or null if not found
	 * @see #getShop(Block)
	 */
	public Shop getShop(Location loc)
	{
		return getShop(loc.getBlock());
	}
	/**
	 * Returns the inventory of the shop that a player would see.
	 * If you are trying to have a player start a transaction, use {@link #getShop(Block)} and then
	 * call the shop object's {@link Shop#open(Player)} method.
	 * @param block - The shop's location
	 * @param showPrice - If items should get "Price: <price>" appended to their lore
	 * @param title - The title of the shop
	 * @return The inventory the player would see
	 * @see #getShopInventory(Block, boolean)
	 */
	public Inventory getShopInventory(Block block, boolean showPrice, String title)
	{
		NBTTagCompound c = plugin.getShopData(block);
		if(c == null)return null;
		return plugin.getShop(c, showPrice, title);
	}
	/**
	 * Returns the inventory of the shop that a player would see.
	 * If you are trying to have a player start a transaction, use {@link #getShop(Block)} and then
	 * call the shop object's {@link Shop#open(Player)} method.
	 * This method has the same effect as calling getShopInventory(block, showPrice, "Buy")
	 * @param block - The shop's location
	 * @param showPrice - If items should get "Price: <price>" appended to their lore
	 * @return The inventory the player would see
	 * @see #getShopInventory(Block, boolean, String)
	 */
	public Inventory getShopInventory(Block block, boolean showPrice)
	{
		return getShopInventory(block, showPrice, "Buy");
	}
	/**
	 * @deprecated This method will open a sell shop as a buy shop.  Use {@link Shop#open(Player)} instead.
	 * 
	 * Opens a buy shop for the specified player.
	 * @param player - The player who is purchasing the items
	 * @param block - The shop
	 * @return The {@link org.bukkit.inventory.InventoryView} associated with the transaction,
	 *  or null if the shop is not found.
	 *  @see Shop#open(Player)
	 */
	public InventoryView beginBuying(Player player, Block block)
	{
		Inventory i = getShopInventory(block, true);
		if(i == null)return null;
		InventoryView iv = player.openInventory(i);
		plugin.buy.add(iv);
		return iv;
	}
	/**
	 * Returns all shops.
	 * @return All shops.
	 */
	public Shop[] getShops()
	{
		NBTTagList shops = plugin.data.getList("Shops");
		Shop[] a = new Shop[shops.size()];
		for(int i = 0; i < shops.size(); i ++)
		{
			NBTTagCompound s = (NBTTagCompound)shops.get(i);
			a[i] = new Shop(s);
		}
		return a;
	}
}
