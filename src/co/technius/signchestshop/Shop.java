package co.technius.signchestshop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.server.v1_7_R1.NBTBase;
import net.minecraft.server.v1_7_R1.NBTTagCompound;
import net.minecraft.server.v1_7_R1.NBTTagList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftItemStack;
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
	HashSet<InventoryView> transactions = new HashSet<InventoryView>();
	HashMap<InventoryView, Double>price = new HashMap<InventoryView, Double>();
	HashSet<InventoryView> edit = new HashSet<InventoryView>();
	HashSet<InventoryView> storage = new HashSet<InventoryView>();
	Inventory storageinv;
	private SignChestShopPlugin scs;
	Shop(NBTTagCompound data)
	{
		if(!data.hasKey("x") || !data.hasKey("y") || !data.hasKey("z") || !data.hasKey("world") ||
				!data.hasKey("items"))
			throw new IllegalArgumentException("Invalid shop data");
		this.data = data;
		storageinv = new CraftInventoryCustom(null, 27, "Storage");
		scs = SignChestShopPlugin.inst;
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
		NBTTagList ilist = data.getList("items", 10);
		ItemStack[] i = new ItemStack[ilist.size()];
		for(int a = 0; a < ilist.size(); a ++)
		{
			NBTTagCompound c = (NBTTagCompound) ilist.get(a);
			if(c.c().size() == 0)i[a] = null;
			else i[a] = CraftItemStack.asCraftMirror((net.minecraft.server.v1_7_R1.ItemStack.createStack(c)));
		}
		return i;
	}
	
	net.minecraft.server.v1_7_R1.ItemStack getRawItem(int index)
	{
		NBTTagList ilist = data.getList("items", 10);
		if(index >= ilist.size() || index < 0)
			return null;
		return net.minecraft.server.v1_7_R1.ItemStack.createStack(ilist.get(index));
	}
	
	/**
	 * Returns the item at the specified slot.
	 * @param index - The index of the item
	 * @param withPriceText - If set to true, then the item will have the price lore attached
	 * @return The item, or null if the specified index does not contain an item.
	 */
	public ItemStack getItem(int index, boolean withPriceText)
	{
		NBTTagList ilist = data.getList("items", 10);
		if(index >= ilist.size() || index < 0)
			return null;
		NBTTagCompound tag = ilist.get(index);
		net.minecraft.server.v1_7_R1.ItemStack item = 
			net.minecraft.server.v1_7_R1.ItemStack.createStack(tag);
		if(withPriceText)
			scs.addPrice(item);
		else if(item.tag != null)
			scs.stripSCSData(item, false);
		return CraftItemStack.asCraftMirror(item);
	}
	
	/**
	 * Returns the item at the specified slot.  Has the same
	 * effect as calling {@link #getItem(int, boolean)} with
	 * withPriceText set to false.
	 * @param index - The index of the item
	 * @return The item, or null if the specified index does not contain an item.
	 */
	public ItemStack getItem(int index)
	{
		return getItem(index, false);
	}
	
	/**
	 * @param index - The index of the item
	 * @return The price of the item, -1 if it is display only, or -2 if no item exists at the index.
	 */
	public double getPrice(int index)
	{
		NBTTagList ilist = data.getList("items", 10);
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
		NBTTagList ilist = data.getList("items", 10);
		if(index >= ilist.size() || index < 0)
			throw new ArrayIndexOutOfBoundsException(index);
		NBTTagCompound c = (NBTTagCompound) ilist.get(index);
		if(c.c().size() == 0)
			throw new IllegalArgumentException("index " + index + " contains no item");
		if(!c.hasKey("tag"))c.set("tag", new NBTTagCompound());
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
		setItem(index, CraftItemStack.asNMSCopy(item), retainPrice);
	}
	
	protected void setItem(int index, net.minecraft.server.v1_7_R1.ItemStack item, boolean retainPrice)
	{
		NBTTagList ilist = data.getList("items", 10);
		if(index >= ilist.size() || index < 0)
			throw new ArrayIndexOutOfBoundsException(index);
		NBTTagCompound old = (NBTTagCompound) ilist.get(index);
		if(item == null)
		{
			old.c().clear();
			return;
		}
		setItem(old, item, retainPrice);
	}
	
	protected void setItem(NBTTagCompound old, net.minecraft.server.v1_7_R1.ItemStack nms, boolean retainPrice)
	{
		NBTTagCompound c = new NBTTagCompound();
		nms.save(c);
		for(Object o:c.c())
		{
			String name = (String) o;
			NBTBase b = c.get(name);
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
	
	protected void loadData()
	{
		NBTTagList l = data.getList("storage", 10);
		for(int i = 0; i < l.size(); i ++)
		{
			storageinv.setItem(i, CraftItemStack.asCraftMirror(
					net.minecraft.server.v1_7_R1.ItemStack.createStack((NBTTagCompound) l.get(i))));
		}
	}
	
	protected void finishData()
	{
		NBTTagList list = new NBTTagList();
		for(ItemStack i: storageinv.getContents())
		{
			list.add(i == null ? new NBTTagCompound() : CraftItemStack.asNMSCopy(i).save(new NBTTagCompound()));
		}
		data.set("storage", list);
	}
	
	@SuppressWarnings("unchecked")
	protected void finishEverything()
	{
		finishData();
		ArrayList<Set<InventoryView>> lists = 
				new ArrayList<Set<InventoryView>>();
		lists.addAll(Arrays.asList(transactions, edit, storage, price.keySet()));
		Iterator<InventoryView> it;
		for(Set<InventoryView> l:lists)
		{
			for(it = l.iterator(); it.hasNext();)
			{
				it.next().close();
				it.remove();
			}
		}
	}
	
	/**
	 * @return The {@link ShopMode} of the shop
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
		String title = scs.cm.doShopTitle(this);
		Inventory i = scs.getShop(data, true, title.length() > 32 ? title.substring(0, 32) : title);
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
	 * @return The title of this shop, or null if this shop has no title.
	 */
	public String getTitle()
	{
		if(!data.hasKey("title"))return null;
		return data.getString("title");
	}
	
	/**
	 * Sets the title of this shop.
	 * @param title - The new title of this shop, or null to remove the title.
	 */
	public void setTitle(String title)
	{
		if(title == null)
			data.remove("title");
		else data.setString("title", title);
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
	 * @return An {@link Inventory} representing the contents of the shop's storage.
	 */
	public Inventory getStorage()
	{
		return storageinv;
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
