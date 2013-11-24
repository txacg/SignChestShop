package net.skycraftmc.SignChestShop;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.v1_6_R3.NBTBase;
import net.minecraft.server.v1_6_R3.NBTTagCompound;
import net.minecraft.server.v1_6_R3.NBTTagList;
import net.minecraft.server.v1_6_R3.NBTTagString;
import net.skycraftmc.SignChestShop.Shop.ShopMode;
import net.skycraftmc.SignChestShop.util.UpdateInformation;
import net.skycraftmc.SignChestShop.util.Updater;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SignChestShopPlugin extends JavaPlugin implements Listener
{
	private StringConfig config;
	private ConfigManager cm;
	protected NBTTagCompound data;
	protected ArrayList<Shop> shops = new ArrayList<Shop>();
	private HashMap<InventoryView, Block>create = new HashMap<InventoryView, Block>();
	private Economy econ;
	private Logger log;
	private SignChestShopAPI api;
	protected static SignChestShopPlugin inst;
	private boolean initsuccess = false;
	public void onEnable()
	{
		inst = this;
		log = getLogger();
		RegisteredServiceProvider<Economy>ecoprov = 
				getServer().getServicesManager().getRegistration(Economy.class);
		if(ecoprov == null)
		{
			log.warning("No economy plugin detected.  Disabling.");
			setEnabled(false);
			return;
		}
		econ = ecoprov.getProvider();
		String[] vercheck = getServer().getClass().getPackage().getName().split("[.]", 4);
		if(vercheck.length == 4)
		{
			if(!vercheck[3].equals("v1_6_R3"))getLogger().warning(
					"This version of SignChestShop may not be compatible with this version of CraftBukkit.");
		}
		else getLogger().warning(
				"This version of SignChestShop may not be compatible with this version of CraftBukkit.");
		if(!getDataFolder().exists())getDataFolder().mkdir();
		cm = new ConfigManager(this);
		File cfile = new File(getDataFolder(), "config.txt");
		config = new StringConfig(cfile);
		cm.config = config;
		if(!cfile.exists())
			cm.writeConfig();
		cm.loadConfig();
		File dat = new File(getDataFolder(), "data.dat");
		data = new NBTTagCompound();
		if(dat.exists())
		{
			boolean c = false;
			Exception e = null;
			try
			{
				DataInputStream dis = new DataInputStream(new FileInputStream(dat));
				NBTBase b = NBTBase.a(dis);
				if(b != null)
				{
					if(b instanceof NBTTagCompound)data = (NBTTagCompound)b;
					else c = true;
				}
				else c = true;
				dis.close();
			}
			catch(IOException ioe)
			{
				e = ioe;
				c = true;
			}
			if(c)
			{
				try
				{
					loadOld(dat);
					log.info("Converted old SignChestShopData");
				}
				catch(Exception ea)
				{
					log.warning("Failed to load/convert data!");
					e = ea;
				}
				if(e != null)e.printStackTrace();
			}
		}
		else data.set("Shops", new NBTTagList());
		integCheck();
		buildShops();
		getServer().getPluginManager().registerEvents(this, this);
		api = new SignChestShopAPI(this);
		initsuccess = true;
		if(config.getBoolean("updater.check", true))
		{
			getServer().getScheduler().runTaskAsynchronously(this, new Runnable(){
				public void run()
				{
					try {
						UpdateInformation info = Updater.findUpdate(getDescription().getVersion());
						if(info != null)
						{
							if(!("v" + getDescription().getVersion()).equals(info.getVersion()))
								getLogger().info("A new update is available: " + info.getVersion() + " (" + info.getType() + ")");
						}
					} catch (IOException e) {
						getLogger().warning("Failed to find update: " + e);
					} catch (XMLStreamException e) {
						getLogger().warning("Failed to find update: " + e);
					}
				}
			});
		}
	}
	
	/**
	 * Returns the plugin's API
	 * @return The API
	 */
	public SignChestShopAPI getAPI()
	{
		return api;
	}
	
	public void onDisable()
	{
		File f = getDataFolder();
		if(!f.exists())f.mkdir();
		for(Shop s: shops)
		{
			s.finishData();
			for(InventoryView i: s.transactions)i.close();
			for(InventoryView i: s.edit)i.close();
			for(InventoryView i: s.price.keySet())i.close();
			for(InventoryView i: s.storage)i.close();
			s.transactions.clear();
			s.edit.clear();
			s.price.clear();
			s.storage.clear();
		}
		for(Map.Entry<InventoryView, Block>k:create.entrySet())k.getKey().close();
		create.clear();
		shops.clear();
		if(initsuccess)
		{
			File dat = new File(f, "data.dat");
			try
			{
				DataOutputStream dos = new DataOutputStream(new FileOutputStream(dat));
				try
				{
					NBTBase.a(data, dos);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				dos.flush();
				dos.close();
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
		initsuccess = false;
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void interact(PlayerInteractEvent event)
	{
		if(event.isCancelled())return;
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK)return;
		Block b = event.getClickedBlock();
		if(b == null)return;
		if(b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN)return;
		//Sign s = (Sign)b.getState();
		Shop shop = getShop(b);
		if(shop == null)return;
		ShopMode mode = shop.getMode();
		if(mode == ShopMode.BUY)
		{
			if(!event.getPlayer().hasPermission("scs.buy") && config.getBoolean("buy.perms", Options.DEFAULT_BUY_PERMS))
			{
				event.getPlayer().sendMessage(color(config.getString("messages.buy.noperm", Messages.DEFAULT_BUY_NOPERM)));
				return;
			}
		}
		else if(mode == ShopMode.SELL)
		{
			if(!event.getPlayer().hasPermission("scs.sell") && config.getBoolean("sell.perms", Options.DEFAULT_SELL_PERMS))
			{
				event.getPlayer().sendMessage(color(config.getString("messages.sell.noperm", Messages.DEFAULT_SELL_NOPERM)));
				return;
			}
		}
		shop.open(event.getPlayer());
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void close(InventoryCloseEvent event)
	{
		Shop shp = null;
		for(Shop s: shops)
		{
			if(s.transactions.contains(event.getView()))
			{
				shp = s;
				s.transactions.remove(event.getView());
			}
			else if(s.transactions.contains(event.getView()))shp = s;
			else if(s.price.containsKey(event.getView()))shp = s;
			else if(s.edit.contains(event.getView()))shp = s;
			else if(s.storage.contains(event.getView()))shp = s;
		}
		if(!(event.getPlayer() instanceof Player))return;
		Player player = (Player)event.getPlayer();
		if(create.containsKey(event.getView()))
		{
			ArrayList<ItemStack>c = new ArrayList<ItemStack>();
			for(ItemStack i:event.getInventory().getContents())
			{
				c.add(i);
			}
			if(c.isEmpty())
			{
				player.sendMessage(varPlayer(config.getString("message.create.cancel", Messages.DEFAULT_CREATE_CANCEL), player));
				return;
			}
			Block b = create.get(event.getView());
			Sign s = (Sign)b.getState();
			boolean e = true;
			for(String x:s.getLines())
			{
				if(!x.trim().isEmpty())e = false;
			}
			Location bloc = b.getLocation();
			NBTTagCompound shop = new NBTTagCompound();
			shop.setDouble("x", bloc.getX());
			shop.setDouble("y", bloc.getY());
			shop.setDouble("z", bloc.getZ());
			shop.setString("world", bloc.getWorld().getName());
			NBTTagList items = new NBTTagList();
			for(ItemStack i:c)
			{
				NBTTagCompound copy = new NBTTagCompound();
				if(i != null)nmsStack(i).save(copy);
				items.add(copy);
			}
			shop.set("items", items);
			Shop shopvar = new Shop(shop);
			if(config.getBoolean("shop.auto.owner", Options.DEFAULT_SHOP_AUTO_OWNER))shopvar.setOwner(player.getName());
			shopvar.setLimited(config.getBoolean("shop.auto.limit", Options.DEFAULT_SHOP_AUTO_LIMIT));
			shops.add(shopvar);
			data.getList("Shops").add(shop);
			player.sendMessage(varPlayer(config.getString("message.create.success", Messages.DEFAULT_CREATE_SUCCESS), player));
			if(e)
			{
				s.setLine(0, ChatColor.AQUA + "Shop");
				s.setLine(1, "Right click to");
				s.setLine(2, "open!");
			}
			s.update();
			if(config.getBoolean("log.create", Options.DEFAULT_LOG_SHOP_CREATION))
					log.info(player.getName() + " created a SignChestShop at " + bloc.getX() + ", " + 
							bloc.getY() + ", " + bloc.getZ() + " at world " + bloc.getWorld().getName());
			create.remove(event.getView());
		}
		else if(shp == null)return;
		else if(shp.price.containsKey(event.getView()))
		{
			player.sendMessage(varPlayer(config.getString("message.price.cancel", Messages.DEFAULT_PRICE_CANCEL), player));
			shp.price.remove(event.getView());
			shp.update();
			return;
		}
		else if(shp.edit.contains(event.getView()))
		{
			NBTTagCompound shop = shp.data;
			NBTTagList items = new NBTTagList();
			Inventory inv = event.getView().getTopInventory();
			for(ItemStack i:inv.getContents())
			{
				if(i != null)
				{
					NBTTagCompound copy = new NBTTagCompound();
					nmsStack(i).save(copy);
					items.add(copy);
				}
				else items.add(new NBTTagCompound());
			}
			shop.set("items", items);
			player.sendMessage(varPlayer(config.getString("message.edit", Messages.DEFAULT_EDIT), player));
			shp.edit.remove(event.getView());
			shp.update();
		}
		else if(shp.storage.contains(event.getView()))
		{
			shp.storage.remove(event.getView());
		}
	}
	@EventHandler
	@SuppressWarnings("deprecation")
	public void click(InventoryClickEvent event)
	{
		Player player = null;
		if(event.getWhoClicked() instanceof Player)player = (Player)event.getWhoClicked();
		if(player == null)return;
		boolean top = event.getRawSlot() < event.getView().getTopInventory().getSize();
		Shop shop = null;
		boolean transaction = false;
		for(Shop s: shops)
		{
			if(shop != null)break;
			if(s.transactions.contains(event.getView()))
			{
				shop = s;
				transaction = true;
			}
			else if(s.edit.contains(event.getView()))shop = s;
			else if(s.price.containsKey(event.getView()))shop = s;
		}
		if(shop == null)return;
		if(transaction)
		{
			ShopMode mode = shop.getMode();
			String str = mode == ShopMode.BUY ? "buy" : "sell";
			if(top)
			{
				event.setCancelled(true);
				player.updateInventory();
				ItemStack current = event.getCurrentItem();
				ItemStack cursor = player.getItemOnCursor();
				if(current == null || current.getType() == Material.AIR)
				{
					if(mode == ShopMode.SELL)
						player.sendMessage(varPlayer(config.getString("message.sell.invalid", Messages.DEFAULT_SELL_INVALID), player));
					return;
				}
				net.minecraft.server.v1_6_R3.ItemStack currentNMS = nmsStack(current.clone());
				if(!currentNMS.getTag().hasKey("scs_price"))
					return;
				double price = currentNMS.getTag().getDouble("scs_price");
				if(config.getBoolean(str + ".permsid", mode == ShopMode.BUY ? Options.DEFAULT_BUY_PERMSID : Options.DEFAULT_SELL_PERMSID))
				{
					if(!player.hasPermission("scs." + str + "." + current.getTypeId()) && !player.hasPermission("scs." + str + ".*"))
					{
						player.sendMessage(varPlayer(config.getString("message." + str + ".noperm",  mode == ShopMode.BUY ? 
								Messages.DEFAULT_BUY_NOPERM : Messages.DEFAULT_SELL_NOPERM), player));
						return;
					}
				}
				
				if(cursor.getType() != Material.AIR)
				{
					if(!isSimilarUnstripped(currentNMS, nmsStack(cursor)))return;
				}
				
				String curname = (price == 1 ? econ.currencyNameSingular() : econ.currencyNamePlural());
				if(!curname.isEmpty())curname = " " + curname;
				
				if(shop.getMode() == ShopMode.BUY)
				{
					int a = current.getType().getMaxStackSize();
					int amount = 1;
					String buymode = config.getString("buy.mode", "single");
					if(buymode.equalsIgnoreCase("stack") || 
							(event.isShiftClick() && config.getBoolean("buy.shiftclick", true)))amount = shop.isLimited() ? current.getAmount() : a;
					else if(buymode.equalsIgnoreCase("amount"))amount = current.getAmount();
					else amount = 1;
					if(event.getAction() == InventoryAction.PICKUP_HALF || event.getAction() == InventoryAction.PICKUP_ONE)
						amount = 1;
					int iamount = player.getItemOnCursor().getAmount();
					if(amount + iamount > a)amount = a - iamount;
					price = price*amount;
					if(!econ.has(player.getName(), price))
					{
						player.sendMessage(varTrans(config.getString("message.buy.fail", Messages.DEFAULT_BUY_FAIL), player, shop,
								amount, price + curname, price));
						return;
					}
					if(price != 0)
					{
						econ.withdrawPlayer(player.getName(), price);
						player.sendMessage(varTrans(config.getString("message.buy.success", 
								Messages.DEFAULT_BUY_SUCCESS), player, shop, amount, price + curname, price));
					}
					else player.sendMessage(varTrans(config.getString("message.buy.free", 
							Messages.DEFAULT_BUY_FREE), player, shop, amount, price + curname, price));
					stripSCSData(currentNMS);
					ItemStack n = CraftItemStack.asCraftMirror(currentNMS);
					n.setAmount(amount + iamount);
					player.setItemOnCursor(n);
					if(shop.getOwner() != null)
						econ.depositPlayer(shop.getOwner(), price);
					if(shop.isLimited())
					{
						if(current.getAmount() == amount)
						{
							shop.setItem(event.getRawSlot(), null);
							event.getView().getTopInventory().setItem(event.getRawSlot(), null);
						}
						else 
						{
							net.minecraft.server.v1_6_R3.ItemStack sn = nmsStack(current);
							sn.count = current.getAmount() - amount;
							event.getView().getTopInventory().setItem(event.getRawSlot(), CraftItemStack.asCraftMirror(sn));
							removeLastLore(sn);
							shop.setItem(event.getRawSlot(), sn, true);
						}
					}
				}
				else if(shop.getMode() == ShopMode.SELL)
				{
					if(cursor.getType() == Material.AIR)return;
					int amount = cursor.getAmount();
					if(event.isRightClick())amount = 1;
					price *= amount;
					String owner = shop.getOwner();
					if(owner != null)
					{
						if(!econ.has(owner, price))
						{
							player.sendMessage(varTrans(config.getString("message.sell.fail", 
									Messages.DEFAULT_SELL_FAIL), player, shop, amount, price + curname, price));
							return;
						}
					}
					if(shop.isLimited())
					{
						Inventory storage = shop.getStorage();
						int freespace = 0;
						for(ItemStack i:storage.getContents())
						{
							if(i == null || i.getType() == Material.AIR)
							{
								freespace += cursor.getType().getMaxStackSize();
							}
							else if(i.isSimilar(cursor))
								freespace += i.getType().getMaxStackSize() - i.getAmount();
						}
						if(freespace < amount)
						{
							player.sendMessage(varTrans(config.getString("messages.sell.nospace", Messages.DEFAULT_SELL_NOSPACE), 
									player, shop, amount, price + curname, price));
							return;
						}
						ItemStack add = cursor.clone();
						add.setAmount(amount);
						storage.addItem(add);
					}
					if(price != 0)
					{
						econ.depositPlayer(player.getName(), price);
						if(owner != null)econ.withdrawPlayer(owner, price);
					}
					player.sendMessage(varTrans(config.getString("message.sell.success", 
							Messages.DEFAULT_SELL_SUCCESS), player, shop, amount, price + curname, price));
					if(amount < current.getAmount())
					{
						ItemStack n = cursor.clone();
						n.setAmount(n.getAmount() - amount);
						player.setItemOnCursor(n.getAmount() == 0 ? null : n);
					}
				}
			}
			else if((top && player.getItemOnCursor().getType() != Material.AIR && 
					event.getSlot() != -999) || (!top && event.getCurrentItem().getType() != Material.AIR
					&& event.isShiftClick()))
			{
				player.sendMessage(varPlayer(config.getString("message." + str + ".invalid", Messages.DEFAULT_SELL_INVALID), player));
				player.updateInventory();
				event.setCancelled(true);
			}
		}
		else if(shop.price.containsKey(event.getView()) && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR)
		{
			double p = shop.price.get(event.getView());
			if(!top)
			{
				player.sendMessage(varPlayer(config.getString("message.price.cancel", Messages.DEFAULT_PRICE_CANCEL), player));
				event.setCancelled(true);
				event.getView().close();
				return;
			}
			event.setCancelled(true);
			shop.setPrice(event.getSlot(), p);
			if(event.getAction() == InventoryAction.PICKUP_ALL)
			{
				shop.price.remove(event.getView());
				event.getView().close();
			}
			player.sendMessage(varPlayer(config.getString("message.price.set", Messages.DEFAULT_PRICE_SET), player));
		}
		else if(shop.edit.contains(event.getView()))
		{
			boolean sclick = top && event.isShiftClick();
			ItemStack i = player.getItemOnCursor().clone();
			if(i.getType() == Material.AIR && sclick)i = event.getCurrentItem();
			if(i.getType() != Material.AIR)
			{
				if(!top || sclick)
				{
					net.minecraft.server.v1_6_R3.ItemStack nms = nmsStack(i);
					stripSCSData(nms);
					event.setCursor(CraftItemStack.asCraftMirror(nms));
					final Player runnablePlayer = player;
					getServer().getScheduler().scheduleSyncDelayedTask(this,
						new Runnable()
						{
							public void run() 
							{
								runnablePlayer.updateInventory();
							}
					});
				}
			}
		}
	}
	
	private NBTTagList removeLastLore(NBTTagList lore)
	{
		NBTTagList newlore = new NBTTagList();
		for(int x = 0; x < lore.size() - 1; x ++)
		{
			newlore.add(lore.get(x));
		}
		return newlore;
	}
	
	private void removeLastLore(net.minecraft.server.v1_6_R3.ItemStack item)
	{
		NBTTagCompound display = item.tag.getCompound("display");
		NBTTagList lore = display.getList("Lore");
		if(lore.size() == 1)display.remove("Lore");
		else display.set("Lore", removeLastLore(lore));
		if(display.c().size() == 0)item.tag.remove("display");
		if(item.tag.c().size() == 0)item.setTag(null);
	}
	
	@SuppressWarnings("unused")
	private boolean isSimilar(net.minecraft.server.v1_6_R3.ItemStack stack1, net.minecraft.server.v1_6_R3.ItemStack stack2)
	{
		net.minecraft.server.v1_6_R3.ItemStack s1c = stack1.cloneItemStack();
		net.minecraft.server.v1_6_R3.ItemStack s2c = stack2.cloneItemStack();
		stripSCSData(s1c);
		stripSCSData(s2c);
		return CraftItemStack.asCraftMirror(s1c).isSimilar(CraftItemStack.asCraftMirror(s2c));
	}
	
	private boolean isSimilarUnstripped(net.minecraft.server.v1_6_R3.ItemStack display, net.minecraft.server.v1_6_R3.ItemStack unstr)
	{
		net.minecraft.server.v1_6_R3.ItemStack s1c = display.cloneItemStack();
		net.minecraft.server.v1_6_R3.ItemStack s2c = unstr.cloneItemStack();
		stripSCSData(s1c);
		return CraftItemStack.asCraftMirror(s1c).isSimilar(CraftItemStack.asCraftMirror(s2c));
	}
	
	private void stripSCSData(net.minecraft.server.v1_6_R3.ItemStack nms)
	{
		stripSCSData(nms, true);
	}
	
	private void stripSCSData(net.minecraft.server.v1_6_R3.ItemStack nms, boolean lastlore)
	{
		if(nms.tag == null)return;
		nms.tag.remove("scs_price");
		if(lastlore)removeLastLore(nms);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void explode(EntityExplodeEvent event)
	{
		Iterator<Block>it = event.blockList().iterator();
		while(it.hasNext())
		{
			Block b = it.next();
			NBTTagCompound c = getShopData(b);
			if(c != null)it.remove();
			else if(getAttachedShop(b).getValue() != null)it.remove();
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void breakBlock(BlockBreakEvent event)
	{
		Block b = event.getBlock();
		DKey<Block, NBTTagCompound> k = getAttachedShop(b);
		Block sb = k.getKey();
		NBTTagCompound c = k.getValue();
		if(c != null)
		{
			Player player = event.getPlayer();
			boolean a = player.hasPermission("signchestshop.create");
			if(a)player.sendMessage(color(config.getString("message.break.perm",
					Messages.DEFAULT_BREAK_PERM)));
			else player.sendMessage(varPlayer(config.getString(
					"message.break.noperm", Messages.DEFAULT_BREAK_NOPERM), player));
			event.setCancelled(true);
			sb.getState().update();
			return;
		}
	}
	
	private DKey<Block, NBTTagCompound> getAttachedShop(Block b)
	{
		Block sb = b;
		NBTTagCompound c = getShopData(b);
		if(c == null)
		{
			Block[] d = {b, b.getRelative(BlockFace.EAST), b.getRelative(BlockFace.WEST),
					b.getRelative(BlockFace.NORTH), b.getRelative(BlockFace.SOUTH),
					b.getRelative(BlockFace.UP)};
			for(Block s:d)
			{
				MaterialData md = s.getState().getData();
				if(!(md instanceof org.bukkit.material.Sign))continue;
				org.bukkit.material.Sign e = (org.bukkit.material.Sign)md;
				if(e.getAttachedFace() == s.getFace(b))
				{
					c = getShopData(s);
					sb = s;
				}
				if(c != null)break;
			}
		}
		return new DKey<Block, NBTTagCompound>(sb, c);
	}
	
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(args.length >= 1)
		{
			if(args[0].equalsIgnoreCase("create"))
			{
				if(noPerm(sender, "scs.create"))return true;
				if(noConsole(sender))return true;
				Player player = (Player)sender;
				Block b = player.getTargetBlock(null, 5);
				if(b == null)
					return msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				if(b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN)
					return msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				Sign s = (Sign)b.getState();
				boolean e = false;
				for(String x:s.getLines())
				{
					if(!x.isEmpty())e = true;
				}
				if(e && config.getBoolean("shop.forceempty", Options.DEFAULT_SHOP_FORCEEMPTY))
					return msg(sender, ChatColor.RED + "This sign must be empty!");
				NBTTagCompound sh = getShopData(b);
				if(sh != null)return msg(sender, ChatColor.RED + "There is already a shop here!");
				Inventory i = this.getServer().createInventory(null, 27, "Shop");
				create.put(player.openInventory(i), b);
				player.sendMessage(ChatColor.YELLOW + "Put all the items you want to " +
						"sell in the shop's inventory.");
			}
			else if(args[0].equalsIgnoreCase("break"))
			{
				if(noPerm(sender, "scs.create"))return true;
				if(noConsole(sender))return true;
				Player player = (Player)sender;
				Block b = player.getTargetBlock(null, 5);
				if(b == null)return msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				Shop s = api.getShop(b);
				if(s == null)
					return msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				if(checkOwner(player, s, "scs.bypass.break"))return true;
				Sign sign = (Sign)b.getState();
				sign.setLine(0, "");
				sign.setLine(1, "");
				sign.setLine(2, "");
				sign.setLine(3, "");
				sign.update();
				removeShop(s.data);
				player.sendMessage(ChatColor.YELLOW + "SignChestShop broken.");
			}
			else if(args[0].equalsIgnoreCase("price"))
			{
				if(noPerm(sender, "scs.create"))return true;
				if(noConsole(sender))return true;
				if(args.length != 2)return msg(sender, ChatColor.RED + "Usage: /scs price <price>");
				Player player = (Player)sender;
				Block b = player.getTargetBlock(null, 5);
				if(b == null)return msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				NBTTagCompound s = getShopData(b);
				if(s == null)
					return msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				if(checkOwner(player, getShopObject(s), "scs.bypass.price"))return true;
				double price;
				if(args[1].equalsIgnoreCase("free"))price = 0;
				else if(args[1].equalsIgnoreCase("display"))price = -1;
				else
				{
					try
					{
						price = Double.parseDouble(args[1]);
						if(price <= 0)return msg(sender, ChatColor.RED + 
								"Price must be a positive number!");
					}catch(NumberFormatException nfe)
					{
						return msg(sender, ChatColor.RED + 
								"\"" + args[1] + "\" is not a valid number.");
					}
				}
				Inventory i = getShop(s, true, "Price");
				getShop(b).price.put(player.openInventory(i), price);
			}
			else if(args[0].equalsIgnoreCase("edit"))
			{
				if(noPerm(sender, "scs.create"))return true;
				if(noConsole(sender))return true;
				Player player = (Player)sender;
				Block b = player.getTargetBlock(null, 5);
				if(b == null)return msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				NBTTagCompound s = getShopData(b);
				if(s == null)
					return msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				if(checkOwner(player, getShopObject(s), "scs.bypass.edit"))return true;
				Inventory i = getShop(s, false, "Edit");
				getShop(b).edit.add(player.openInventory(i));
			}
			else if(args[0].equalsIgnoreCase("reload"))
			{
				if(noPerm(sender, "scs.reload"))return true;
				try 
				{
					config.load();
					sender.sendMessage(ChatColor.GREEN + "Config reloaded successfully.");
				} 
				catch (IOException ioe)
				{
					sender.sendMessage(ChatColor.RED + "An error occured while reloading the config" + 
							(sender != getServer().getConsoleSender() ? ", check the console for details" : "") + "!");
					log.log(Level.SEVERE, "Could not load config, reverting to defaults", ioe);
				}
			}
			else if(args[0].equalsIgnoreCase("refresh"))
			{
				if(noPerm(sender, "scs.refresh"))return true;
				boolean s = cm.writeConfig();
				if(s)sender.sendMessage(ChatColor.GREEN + "Config file updated.");
				else sender.sendMessage(ChatColor.RED + "An error occured while updating the config" + 
						(sender != getServer().getConsoleSender() ? ", check the console for details" : "") + "!");
			}
			else if(args[0].equalsIgnoreCase("setmode"))
			{
				Shop s = checkTarget(sender, "scs.create", 2, 2, args.length, "scs setmode <mode>");
				if(s == null)
					return true;
				if(checkOwner((Player) sender, s, "scs.bypass.setmode"))return true;
				if(args[1].equalsIgnoreCase("buy"))s.setMode(ShopMode.BUY);
				else if(args[1].equalsIgnoreCase("sell"))s.setMode(ShopMode.SELL);
				else return msg(sender, ChatColor.RED + "Acceptable modes: buy, sell");
				sender.sendMessage(ChatColor.GREEN + "Mode set to " + args[1]);
			}
			else if(args[0].equalsIgnoreCase("storage"))
			{
				Shop s = checkTarget(sender, "scs.create", 1, 1, args.length, "scs storage");
				if(s == null)
					return true;
				Player player = (Player) sender;
				if(checkOwner(player, s, "scs.bypass.storage"))return true;
				s.storage.add(player.openInventory(s.getStorage()));
			}
			else if(args[0].equalsIgnoreCase("setowner"))
			{
				Shop s = checkTarget(sender, "scs.admin", 2, 2, args.length, "scs setowner <name>");
				if(s == null)
					return true;
				if(args[1].equalsIgnoreCase("none"))
				{
					s.setOwner(null);
					return msg(sender, ChatColor.GREEN + "This shop no longer has an owner.");
				}
				s.setOwner(args[1]);
				return msg(sender, ChatColor.GREEN + "The owner of this shop has been set to \"" + args[1] + "\"");
			}
			else if(args[0].equalsIgnoreCase("setlimited"))
			{
				Shop s = checkTarget(sender, "scs.admin", 2, 2, args.length, "scs setlimited <true/false>");
				if(s == null)
					return true;
				boolean l = args[1].equalsIgnoreCase("true");
				s.setLimited(l);
				return msg(sender, ChatColor.GREEN + "This shop is now " + (l ? "" : "un") + "limited.");
			}
			else if(args[0].equalsIgnoreCase("settitle"))
			{
				Shop s = checkTarget(sender, "scs.create", 1, Integer.MAX_VALUE, args.length, "scs settitle <name>");
				if(s == null)
					return true;
				Player player = (Player) sender;
				if(checkOwner(player, s, "scs.bypass.settitle"))return true;
				if(args[0].equalsIgnoreCase("none"))
				{
					s.setTitle(null);
					return msg(sender, varPlayer(config.getString("message.settitle.remove", Messages.DEFAULT_SETTITLE_REMOVE), player));
				}
				StringBuffer sb = new StringBuffer();
				sb.append(args[1]);
				for(int i = 2; i < args.length; i ++)
					sb.append(" ").append(args[i]);
				String n = sb.toString();
				if(n.length() > 32)
					return msg(sender, varPlayer(config.getString("message.settitle.fail", Messages.DEFAULT_SETTITLE_FAIL), player).replaceAll("<title>", n));
				s.setTitle(n);
				msg(sender, varPlayer(config.getString("message.settitle", Messages.DEFAULT_SETTITLE_SUCCESS), player).replaceAll("<title>", n));
			}
			else if(args[0].equalsIgnoreCase("help"))helpCmd(sender, args);
			else sender.sendMessage(ChatColor.GOLD + "Command unrecognized.  " +
					"Type " + ChatColor.AQUA + "/scs help" + ChatColor.GOLD + " for help");
		}
		else
		{
			sender.sendMessage(ChatColor.GOLD + "SignChestShop version " + 
					getDescription().getVersion());
			sender.sendMessage("Type " + ChatColor.AQUA + "/scs help" + ChatColor.GOLD + " for help");
		}
		return true;
	}
	private Shop checkTarget(CommandSender sender, String perm, int argmin, int argmax, int argc, String usage)
	{
		//TODO Find alternative to getTargetBlock
		if(noPerm(sender, "scs.admin"))return null;
		if(noConsole(sender))return null;
		if(argc < argmin || argc > argmax)
		{
			msg(sender, ChatColor.RED + "Usage: /" + usage);
			return null;
		}
		Player player = (Player)sender;
		@SuppressWarnings("deprecation")
		Block b = player.getTargetBlock(null, 5);
		if(b == null)
		{
			msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
			return null;
		}
		Shop s = getShop(b);
		if(s == null)
			msg(sender, varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
		return s;
	}
	private boolean checkOwner(Player player, Shop shop, String perm)
	{
		if(!player.hasPermission(perm) && !player.getName().equals(shop.getOwner()))
			return msg(player, varPlayer(config.getString("message.cmd.notowned", Messages.DEFAULT_CMD_NOTOWNED), player));
		return false;
	}
	private boolean noConsole(CommandSender sender)
	{
		if(!(sender instanceof Player))return true;
		return false;
	}
	private boolean msg(CommandSender sender, String msg)
	{
		sender.sendMessage(msg);
		return true;
	}
	private boolean helpCmd(CommandSender sender, String[] args)
	{
		sender.sendMessage(ChatColor.GOLD + "SignChestShop Help");
		msg(sender, def("/scs help", "Displays this menu"));
		if(sender.hasPermission("scs.create"))
		{
			msg(sender, def("/scs create", "Creates a shop"));
			msg(sender, def("/scs break", "Deletes a shop"));
			msg(sender, def("/scs price <price>", "Prices items in a shop"));
			msg(sender, def("/scs edit", "Edits a shop"));
			msg(sender, def("/scs setmode <mode>", "Sets the mode of a shop"));
			msg(sender, def("/scs storage", "Accesses a shop's storage"));
			msg(sender, def("/scs settitle <name>", "Sets the title of a shop"));
		}
		if(sender.hasPermission("scs.admin"))
		{
			msg(sender, def("/scs setowner <name>", "Sets the owner of a shop"));
			msg(sender, def("/scs setlimited <true/false>", "Sets shop item availability"));
		}
		if(sender.hasPermission("scs.reload"))msg(sender, def("/scs reload", "Reloads the config"));
		if(sender.hasPermission("scs.refresh"))msg(sender, def("/scs refresh", "Updates the config"));
		return true;
	}
	
	private String def(String a, String b)
	{
		return ChatColor.AQUA + a + ChatColor.DARK_RED + " - " + ChatColor.GOLD + b;
	}
	
	private boolean noPerm(CommandSender sender, String perm)
	{
		if(sender.hasPermission(perm))return false;
		sender.sendMessage(color(config.getString("message.cmd.noperm", Messages.DEFAULT_CMD_NOPERM)));
		return true;
	}
	
	protected net.minecraft.server.v1_6_R3.ItemStack nmsStack(ItemStack i)
	{
		return CraftItemStack.asNMSCopy(i);
	}
	
	protected NBTTagCompound getShopData(Block b)
	{
		Location bloc = b.getLocation();
		NBTTagList shops = data.getList("Shops");
		for(int i = 0; i < shops.size(); i ++)
		{
			NBTTagCompound d = (NBTTagCompound)shops.get(i);
			double x = d.getDouble("x");
			double y = d.getDouble("y");
			double z = d.getDouble("z");
			String world = d.getString("world");
			if(bloc.getX() != x || bloc.getY() != y || bloc.getZ() != z)continue;
			if(!bloc.getWorld().getName().equals(world))continue;
			if(b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN)
			{
				removeShop(d);
				return null;
			}
			return d;
		}
		return null;
	}
	
	protected void removeShop(NBTTagCompound s)
	{
		NBTTagList shops = data.getList("Shops");
		NBTTagList newshops = new NBTTagList();
		for(int i = 0; i < shops.size(); i ++)
		{
			NBTTagCompound c = (NBTTagCompound)shops.get(i);
			if(c != s)newshops.add(c);
			else
			{
				Iterator<Shop> it = this.shops.iterator();
				while(it.hasNext())
				{
					Shop sh = it.next();
					if(sh.data == c)it.remove();
				}
			}
		}
		data.set("Shops", newshops);
	}
	
	protected Shop getShop(Block block)
	{
		for(Shop shop: shops)
		{
			World w = getServer().getWorld(shop.getWorld());
			if(w == null)continue;
			Location loc = new Location(w, shop.getX(), shop.getY(), shop.getZ()).getBlock().getLocation();
			if(loc.equals(block.getLocation()))return shop;
		}
		return null;
	}
	
	protected Inventory getShop(NBTTagCompound shop, boolean displayprice, String title)
	{
		NBTTagList items = shop.getList("items");
		ArrayList<ItemStack>ilist = new ArrayList<ItemStack>();
		for(int i = 0; i <items.size(); i ++)
		{
			NBTTagCompound c = (NBTTagCompound) items.get(i).clone();
			if(c.c().size() == 0)
			{
				ilist.add(null);
				continue;
			}
			net.minecraft.server.v1_6_R3.ItemStack item = net.minecraft.server.v1_6_R3.ItemStack.createStack(c);
			CraftItemStack cis = CraftItemStack.asCraftMirror(item);
			if(displayprice)
			{
				NBTTagCompound tag = item.getTag();
				if(tag == null)item.setTag((tag = new NBTTagCompound()));
				if(!tag.hasKey("display"))tag.setCompound("display", new NBTTagCompound());
				NBTTagCompound display = tag.getCompound("display");
				if(!display.hasKey("Lore"))display.set("Lore", new NBTTagList());
				NBTTagList lore = display.getList("Lore");
				String price = config.getString("shop.price.text", Options.DEFAULT_PRICE_TEXT);
				String pprice;
				double rprice = -1;
				if(tag.hasKey("scs_price"))
				{
					rprice = tag.getDouble("scs_price");
					if(rprice < 0)pprice = config.getString("shop.price.display", Options.DEFAULT_PRICE_DISPLAY);
					if(rprice == 0)pprice = config.getString("shop.price.free", Options.DEFAULT_PRICE_FREE);
					else
					{
						if(cis.getAmount() == 1)
							pprice = config.getString("shop.price.cost", Options.DEFAULT_PRICE_COST).replaceAll("<rawprice>", placePadding(rprice));
						else pprice = config.getString("shop.price.costmulti", Options.DEFAULT_PRICE_COSTMULTI)
							.replaceAll("<rawprice>", placePadding(rprice)).replaceAll("<totalprice>", placePadding(rprice*cis.getAmount()));
					}
				}
				else
				{
					pprice = config.getString("shop.price.display", Options.DEFAULT_PRICE_DISPLAY);
				}
				lore.add(new NBTTagString("", varCur(price.replaceAll("<price>", pprice), rprice)));
			}
			ilist.add(cis);
		}
		Inventory i = this.getServer().createInventory(null, 27, title);
		for(int a = 0; a < ilist.size(); a ++)
		{
			ItemStack item = ilist.get(a);
			if(item == null)continue;
			i.setItem(a, ilist.get(a));
		}
		return i;
	}
	
	private String placePadding(double price)
	{
		String pstring = Double.toString(price);
		int places = pstring.length() - pstring.indexOf('.') - 1;
		int min = config.getInt("shop.mindecplaces", Options.DEFAULT_SHOP_MINDECPLACES);
		if(places < min)
		{
			StringBuffer sb = new StringBuffer().append(pstring);
			for(int j = places; j < min; j ++)
				sb.append(0);
			pstring = sb.toString();
		}
		return pstring;
	}
	
	protected Inventory getShop(NBTTagCompound shop, boolean buy)
	{
		return getShop(shop, buy, "Shop");
	}
	
	protected Shop getShopObject(NBTTagCompound data)
	{
		for(Shop s: shops)
		{
			if(s.data == data)return s;
		}
		return null;
	}
	
	private class DKey<V, O>
	{
		private V a;
		private O b;
		public DKey(V a, O b)
		{
			this.a = a;
			this.b = b;
		}
		public V getKey()
		{
			return a;
		}
		public O getValue()
		{
			return b;
		}
	}
	private String color(String s)
	{
		return ChatColor.translateAlternateColorCodes('&', s);
	}
	private String varTrans(String s, Player player, Shop shop, int amount, String price, double rawprice)
	{
		String a = varCur(varPlayer(s, player), rawprice);
		a = a.replaceAll("<amount>", "" + amount);
		a = a.replaceAll("<price>", "" + price);
		a = a.replaceAll("<rawprice>", "" + rawprice);
		a = a.replaceAll("<itemcorrectl>", (amount == 1 ? "item" : "items"));
		a = a.replaceAll("<itemcorrectu>", (amount == 1 ? "Item" : "Items"));
		return varShop(a, shop);
	}
	private String varShop(String s, Shop shop)
	{
		String a = var(s);
		a.replaceAll("<owner>", shop.getOwner() == null ? "" : shop.getOwner());
		a.replaceAll("<title>", shop.getTitle() == null ? "" : shop.getTitle());
		return a;
	}
	private String varPlayer(String s, Player player)
	{
		return var(s).replaceAll("<player>", player.getName());
	}
	private String var(String s)
	{
		String a = color(s);
		a = a.replaceAll("<curnameplur>", econ.currencyNamePlural());
		a = a.replaceAll("<curnamesing>", econ.currencyNameSingular());
		return a;
	}
	private String varCur(String s, double amount)
	{
		return var(s).replaceAll("<curname>", amount == 1 ? econ.currencyNameSingular() : econ.currencyNamePlural());
	}
	private void integCheck()
	{
		NBTTagList shops = data.getList("Shops");
		for(int i = 0; i < shops.size(); i ++)
		{
			NBTTagCompound a = (NBTTagCompound)shops.get(i);
			if(!a.hasKey("limited"))a.setBoolean("limited", false);
			if(!a.hasKey("mode"))a.setInt("mode", ShopMode.BUY.ID);
			if(!a.hasKey("storage"))a.set("storage", new NBTTagList());
		}
	}
	
	private void loadOld(File dat)throws Exception
	{
		DataInputStream dis = new DataInputStream(new FileInputStream(dat));
		Method m = NBTTagCompound.class.getDeclaredMethod("load", DataInput.class);
		m.setAccessible(true);
		m.invoke(data, dis);
	}
	
	private void buildShops()
	{
		NBTTagList shops = data.getList("Shops");
		for(int i = 0; i < shops.size(); i ++)
		{
			NBTTagCompound a = (NBTTagCompound)shops.get(i);
			Shop s = new Shop(a);
			s.loadData();
			this.shops.add(s);
		}
	}
}
