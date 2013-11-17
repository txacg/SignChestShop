package net.skycraftmc.SignChestShop;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.server.v1_6_R3.NBTBase;
import net.minecraft.server.v1_6_R3.NBTTagCompound;
import net.minecraft.server.v1_6_R3.NBTTagList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_6_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a SignChestShop.
 */
public class Shop 
{
	NBTTagCompound data;
	ArrayList<InventoryView> transactions = new ArrayList<InventoryView>();
	HashMap<InventoryView, Double>price = new HashMap<InventoryView, Double>();
	ArrayList<InventoryView> edit = new ArrayList<InventoryView>();
	ArrayList<InventoryView> storage = new ArrayList<InventoryView>();
	Shop(NBTTagCompound data)
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
	 * Returns the location of this shop.
	 * @return This shop's location
	 */
	public Location getLocation()
	{
		return new Location(Bukkit.getWorld(getWorld()), getX(), getY(), getZ());
	}
	
	/**
	 * Returns the block of this shop. This is the sign itself, not the block it is attached to.
	 * @return The block of this shop.
	 */
	public Block getBlock()
	{
		return getLocation().getBlock();
	}
	
	/**
	 * Returns the contents of this shop. The slots in the shop that
	 * contain no items will be null.
	 * @return The contents of this shop.
	 */
	public ItemStack[] getContents()
	{
		NBTTagList ilist = data.getList("items");
		ItemStack[] i = new ItemStack[ilist.size()];
		for(int a = 0; a < ilist.size(); a ++)
		{
			NBTTagCompound c = (NBTTagCompound) ilist.get(a);
			if(c.c().size() == 0)i[a] = null;
			else i[a] = CraftItemStack.asCraftMirror((net.minecraft.server.v1_6_R3.ItemStack.createStack(c)));
		}
		return i;
	}
	
	/**
	 * @param index - The index of the item
	 * @return The price of the item, -1 if it is display only, or -2 if no item exists at the index.
	 */
	public double getPrice(int index)
	{
		NBTTagList ilist = data.getList("items");
		if(index >= ilist.size() || index < 0)
			throw new ArrayIndexOutOfBoundsException(index);
		NBTTagCompound c = (NBTTagCompound) ilist.get(index);
		if(c.c().size() == 0)
			return -2;
		if(!c.hasKey("tag"))return -1;
		NBTTagCompound tag = c.getCompound("tag");
		if(!tag.hasKey("scs_price"))return -1;
		return tag.getDouble("scs_price");
	}
	
	/**
	 * Sets the price of the item at the specified index.
	 * Use -1 for display only and 0 for free.
	 * @param index - The index of the item
	 * @param price - The price of the item
	 */
	public void setPrice(int index, double price)
	{
		NBTTagList ilist = data.getList("items");
		if(index >= ilist.size() || index < 0)
			throw new ArrayIndexOutOfBoundsException(index);
		NBTTagCompound c = (NBTTagCompound) ilist.get(index);
		if(c.c().size() == 0)
			throw new IllegalArgumentException("index " + index + " contains no item");
		if(!c.hasKey("tag"))c.setCompound("tag", new NBTTagCompound());
		NBTTagCompound tag = c.getCompound("tag");
		if(price >= 0)tag.setDouble("scs_price", price);
		else tag.remove("scs_price");
	}
	
	/**
	 * Sets the item at the specified index.
	 * @param index - The index of the item.
	 * @param item - The {@link ItemStack}.
	 * @param retainPrice - If this is true, then the price will be kept.
	 */
	public void setItem(int index, ItemStack item, boolean retainPrice)
	{
		NBTTagList ilist = data.getList("items");
		if(index >= ilist.size() || index < 0)
			throw new ArrayIndexOutOfBoundsException(index);
		NBTTagCompound old = (NBTTagCompound) ilist.get(index);
		if(item == null)
		{
			old.c().clear();
			return;
		}
		net.minecraft.server.v1_6_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
		NBTTagCompound c = new NBTTagCompound();
		nms.save(c);
		for(Object o:c.c())
		{
			NBTBase b = (NBTBase)o;
			String name = b.getName();
			if(name.equals("scs_price"))
			{
				if(!retainPrice)old.remove("scs_price");
			}
			else if(old.hasKey(name))old.set(name, b);
			else old.remove(name);
		}
	}
	
	/**
	 * Sets the item at the specified index.
	 * This has the same effect as {@link #setItem(int, ItemStack, boolean)}
	 * with retainPrice as false.
	 * @param index - The index of the item.
	 * @param item - The {@link ItemStack}.
	 */
	public void setItem(int index, ItemStack item)
	{
		setItem(index, item, false);
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
		String ostring = getOwner() != null ? " " + (b ? "from" : "to") + " " + getOwner() : "";
		Inventory i = SignChestShopPlugin.inst.getShop(data, true, (b ? "Buy" : "Sell") + ostring);
		InventoryView iv = player.openInventory(i);
		transactions.add(iv);
		return iv;
	}
	
	/**
	 * Reopens all currently open transactions.
	 */
	public void update()
	{
		ArrayList<Player> p = new ArrayList<Player>();
		for(InventoryView iv: transactions)
		{
			p.add((Player) iv.getPlayer());
			iv.close();
		}
		for(Player pl : p)
			open(pl);
	}
	
	/**
	 * @return An array of all {@link org.bukkit.InventoryView}s currently open.
	 */
	public InventoryView[] getBrowsing()
	{
		return transactions.toArray(new InventoryView[transactions.size()]);
	}
	
	/**
	 * @return The owner of this shop, or null if this shop has no owner.
	 */
	public String getOwner()
	{
		if(!data.hasKey("owner"))return null;
		return data.getString("owner");
	}
	
	/**
	 * Sets the owner of this shop.
	 * @param owner - The new owner of this shop, or null to remove the owner.
	 */
	public void setOwner(String owner)
	{
		if(owner == null)
			data.remove("owner");
		else data.setString("owner", owner);
		update();
	}
	
	/**
	 * Sets the shop to be limited or not.
	 * @param value - If the shop can run out of stock
	 */
	public void setLimited(boolean value)
	{
		data.setBoolean("limited", value);
	}
	
	/**
	 * @return True if the shop can run out of stock, otherwise false.
	 */
	public boolean isLimited()
	{
		return data.getBoolean("limited");
	}
	
	/**
	 * @return The contents of the shop's storage.
	 */
	public ItemStack[] getStorage()
	{
		NBTTagList ilist = data.getList("storage");
		if(ilist.size() == 0)
			return new ItemStack[0];
		ItemStack[] i = new ItemStack[ilist.size()];
		for(int a = 0; a < ilist.size(); a ++)
		{
			NBTTagCompound c = (NBTTagCompound) ilist.get(a);
			if(c.c().size() == 0)i[a] = null;
			else i[a] = CraftItemStack.asCraftMirror((net.minecraft.server.v1_6_R3.ItemStack.createStack(c)));
		}
		return i;
	}
	
	/**
	 * Sets the contents of the shop's storage
	 */
	public void setStorage(ItemStack[] storage)
	{
		NBTTagList list = new NBTTagList();
		for(ItemStack i: storage)
		{
			list.add(i == null ? new NBTTagCompound() : CraftItemStack.asNMSCopy(i).save(new NBTTagCompound()));
		}
		data.set("storage", list);
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
