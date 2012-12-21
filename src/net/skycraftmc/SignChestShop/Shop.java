package net.skycraftmc.SignChestShop;

import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a SignChestShop.
 * These objects are not stored in main
 * plugin, so use SignChestShopAPI.getShop(Block b) or
 * SignChestShopAPI.getShop(Location l) to retrieve them.
 * @see SignChestShopAPI#getShop(Location)
 * @see SignChestShopAPI#getShop(Block)
 */
public class Shop 
{
	public static int BUY = 0;
	public static int SELL = 1;
	private NBTTagCompound data;
	public Shop(NBTTagCompound data)
	{
		if(!data.hasKey("x") || !data.hasKey("y") || !data.hasKey("z") || !data.hasKey("world") ||
				!data.hasKey("items"))
			throw new IllegalArgumentException("Invalid shop data");
		this.data = data;
	}
	/**
	 * Returns the x-coordinate of this shop
	 * @return This shop's x-coordinate
	 */
	public double getX()
	{
		return data.getDouble("x");
	}
	/**
	 * Returns the y-coordinate of this shop
	 * @return This shop's y-coordinate
	 */
	public double getY()
	{
		return data.getDouble("y");
	}
	/**
	 * Returns the z-coordinate of this shop
	 * @return This shop's z-coordinate
	 */
	public double getZ()
	{
		return data.getDouble("z");
	}
	/**
	 * Returns the name of this shop's world
	 * @return This shop's world
	 */
	public String getWorld()
	{
		return data.getString("world");
	}
	/**
	 * Returns the contents of this shop. 
	 * Length of the array varies based on the contents.
	 * @return The shop's contents
	 */
	public ItemStack[] getContents()
	{
		NBTTagList ilist = data.getList("items");
		ItemStack[] i = new ItemStack[ilist.size()];
		for(int a = 0; a < ilist.size(); a ++)i[a] = 
				new CraftItemStack(net.minecraft.server.ItemStack.a((NBTTagCompound)ilist.get(a)));
		return i;
	}
	public boolean equals(Object o)
	{
		if(!(o instanceof Shop))return false;
		Shop s = (Shop)o;
		if(s.getX() != getX())return false;
		if(s.getY() != getY())return false;
		if(s.getZ() != getZ())return false;
		return s.getWorld().equals(getWorld());
	}
	protected NBTTagCompound getData()
	{
		return data;
	}
	/**
	 * @return The mode of the shop, such as BUY or SELL
	 */
	public int getMode()
	{
		return data.getInt("mode");
	}
	/**
	 * Opens the shop for the specified player.
	 * @param player - The player
	 * @return The {@link org.bukkit.inventory.InventoryView} associated with this transaction.
	 */
	public InventoryView open(Player player)
	{
		int m = getMode();
		boolean b = m == 0;
		Inventory i = SignChestShopPlugin.inst.getShop(data, b, (b ? "Buy" : "Sell"));
		InventoryView iv = player.openInventory(i);
		if(m == BUY)SignChestShopPlugin.inst.buy.add(iv);
		else if(m == SELL)SignChestShopPlugin.inst.sell.add(iv);
		return iv;
	}
}
