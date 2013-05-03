package net.skycraftmc.SignChestShop;

import net.minecraft.server.v1_5_R3.NBTTagCompound;
import net.minecraft.server.v1_5_R3.NBTTagList;

import org.bukkit.craftbukkit.v1_5_R3.inventory.CraftItemStack;
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
				CraftItemStack.asCraftMirror((net.minecraft.server.v1_5_R3.ItemStack.createStack((NBTTagCompound)ilist.get(a))));
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
	public ShopMode getMode()
	{
		return ShopMode.getByID(data.getInt("mode"));
	}
	/**
	 * Sets the {@link ShopMode} of this shop.
	 */
	public void setMode(ShopMode mode)
	{
		if(mode == null)throw new IllegalArgumentException("The shop mode is null.");
		data.setInt("mode", mode.ID);
	}
	/**
	 * Opens the shop for the specified player.
	 * @param player - The player
	 * @return The {@link org.bukkit.inventory.InventoryView} associated with this transaction.
	 */
	public InventoryView open(Player player)
	{
		int m = getMode().ID;
		boolean b = m == 0;
		Inventory i = SignChestShopPlugin.inst.getShop(data, b, (b ? "Buy" : "Sell"));
		InventoryView iv = player.openInventory(i);
		if(m == ShopMode.BUY.ID)SignChestShopPlugin.inst.buy.add(iv);
		else if(m == ShopMode.SELL.ID)SignChestShopPlugin.inst.sell.add(iv);
		return iv;
	}
	/**
	 * Represents the modes of a shop.
	 */
	public enum ShopMode
	{
		/**
		 * Represents a shop that players buy from.
		 */
		BUY, 
		/**
		 * Represents a shop that players sell to.
		 */
		SELL;
		/**
		 * The ID of this shop mode.
		 * @see #getID()
		 */
		public final int ID = ordinal();
		/**
		 * Finds and returns a {@link ShopMode} with the specified ID, or null if not found.
		 * @param id - The ID of the shop mode
		 * @return The ShopMode with the specified ID, or null if not found.
		 */
		public static ShopMode getByID(int id)
		{
			if(id < 0 || id >= values().length)return null;
			return values()[id];
		}
		/**
		 * Finds and returns a {@link ShopMode} with the specified name, or null if not found.
		 * @param name - The ID of the shop mode
		 * @return The ShopMode with the specified name, or null if not found.
		 */
		public static ShopMode getByName(String name)
		{
			for(ShopMode m:values())
			{
				if(m.name().equalsIgnoreCase(name))return m;
			}
			return null;
		}
		/**
		 * @return The ID of this shop mode.  Equal to this {@link ShopMode}'s {@link #ID} variable.
		 * @see #ID
		 */
		public int getID()
		{
			return ID;
		}
	}
}
