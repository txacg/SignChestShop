package co.technius.signchestshop;

import java.io.DataInputStream;
import java.io.DataOutput;
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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.v1_7_R3.NBTBase;
import net.minecraft.server.v1_7_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_7_R3.NBTTagCompound;
import net.minecraft.server.v1_7_R3.NBTTagList;
import net.minecraft.server.v1_7_R3.NBTTagString;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
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

import co.technius.signchestshop.Shop.ShopMode;
import co.technius.signchestshop.util.UpdateInformation;
import co.technius.signchestshop.util.Updater;

import com.evilmidget38.UUIDFetcher;

public class SignChestShopPlugin extends JavaPlugin implements Listener
{
	private StringConfig config;
	ConfigManager cm;
	protected NBTTagCompound data;
	protected ArrayList<Shop> shops = new ArrayList<Shop>();
	HashMap<InventoryView, Block>create = new HashMap<InventoryView, Block>();
	Economy econ;
	Logger log;
	SignChestShopAPI api;
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
			if(!vercheck[3].equals("v1_7_R3"))getLogger().warning(
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
			try
			{
				DataInputStream dis = new DataInputStream(new FileInputStream(dat));
				load(dis);
				dis.close();
			}
			catch (Exception e)
			{
				log.log(Level.WARNING, "Failed to load data", e);
			}
		}
		else data.set("Shops", new NBTTagList());
		integCheck();
		buildShops();
		getServer().getPluginManager().registerEvents(this, this);
		getCommand("signchestshop").setExecutor(new SignChestShopCommandExecutor(this));
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
			s.finishEverything();
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
					save(dos);
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
		Shop shop = getShop(b);
		if(shop == null)return;
		ShopMode mode = shop.getMode();
		if(mode == ShopMode.BUY)
		{
			if(!event.getPlayer().hasPermission("scs.buy") && config.getBoolean("buy.perms", Options.DEFAULT_BUY_PERMS))
			{
				event.getPlayer().sendMessage(cm.color(config.getString("messages.buy.noperm", Messages.DEFAULT_BUY_NOPERM)));
				return;
			}
		}
		else if(mode == ShopMode.SELL)
		{
			if(!event.getPlayer().hasPermission("scs.sell") && config.getBoolean("sell.perms", Options.DEFAULT_SELL_PERMS))
			{
				event.getPlayer().sendMessage(cm.color(config.getString("messages.sell.noperm", Messages.DEFAULT_SELL_NOPERM)));
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
			if (s.transactions.containsKey(event.getView()))
			{
				if (config.getBoolean("shop.notifications", Options.DEFAULT_SHOP_NOTIFICATIONS) && s.getOwner() != null)
				{
					Player p = getServer().getPlayer(s.getOwner());
					String title = s.getTitle();
					double amount = s.transactions.get(event.getView());
					String msg;
					String def;
					if (s.getMode() == ShopMode.BUY && title == null)
					{
						msg = "buy.notice.default";
						def = Messages.DEFAULT_BUY_NOTICE;
					}
					else if (s.getMode() == ShopMode.BUY && title != null)
					{
						msg = "buy.notice.titled";
						def = Messages.DEFAULT_BUY_NOTICE_TITLED;
					}
					else if (s.getMode() == ShopMode.SELL && title == null)
					{
						msg = "sell.notice.default";
						def = Messages.DEFAULT_SELL_NOTICE;
					}
					else
					{
						msg = "sell.notice.titled";
						def = Messages.DEFAULT_SELL_NOTICE_TITLED;
					}
					
					if (p != null)
						p.sendMessage(cm.varNotice(config.getString(msg, def), s, p, amount));
				}
				shp = s;
				s.transactions.remove(event.getView());
			}
			else if (s.transactions.containsKey(event.getView()))shp = s;
			else if (s.price.containsKey(event.getView()))shp = s;
			else if (s.edit.contains(event.getView()))shp = s;
			else if (s.storage.contains(event.getView()))shp = s;
		}
		if (!(event.getPlayer() instanceof Player))return;
		Player player = (Player)event.getPlayer();
		if (create.containsKey(event.getView()))
		{
			ArrayList<ItemStack>c = new ArrayList<ItemStack>();
			for(ItemStack i:event.getInventory().getContents())
			{
				c.add(i);
			}
			if(c.isEmpty())
			{
				player.sendMessage(cm.varPlayer(config.getString("message.create.cancel", Messages.DEFAULT_CREATE_CANCEL), player));
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
			if(config.getBoolean("shop.auto.owner", Options.DEFAULT_SHOP_AUTO_OWNER))shopvar.setOwner(player.getUniqueId());
			shopvar.setLimited(config.getBoolean("shop.auto.limit", Options.DEFAULT_SHOP_AUTO_LIMIT));
			shops.add(shopvar);
			data.getList("Shops", 10).add(shop);
			player.sendMessage(cm.varPlayer(config.getString("message.create.success", Messages.DEFAULT_CREATE_SUCCESS), player));
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
			player.sendMessage(cm.varPlayer(config.getString("message.price.cancel", Messages.DEFAULT_PRICE_CANCEL), player));
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
			player.sendMessage(cm.varPlayer(config.getString("message.edit", Messages.DEFAULT_EDIT), player));
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
			if(s.transactions.containsKey(event.getView()))
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
				event.setResult(Result.DENY);
				event.setCancelled(true);
				player.updateInventory();
				ItemStack current = event.getCurrentItem();
				ItemStack cursor = player.getItemOnCursor();
				if(current == null || current.getType() == Material.AIR)
				{
					if(mode == ShopMode.SELL)
						player.sendMessage(cm.varPlayer(config.getString("message.sell.invalid", Messages.DEFAULT_SELL_INVALID), player));
					return;
				}
				net.minecraft.server.v1_7_R3.ItemStack currentNMS = nmsStack(current.clone());
				if(!currentNMS.getTag().hasKey("scs_price"))
					return;
				double price = currentNMS.getTag().getDouble("scs_price");
				if(config.getBoolean(str + ".permsid", mode == ShopMode.BUY ? Options.DEFAULT_BUY_PERMSID : Options.DEFAULT_SELL_PERMSID))
				{
					if(!player.hasPermission("scs." + str + "." + current.getTypeId()) && !player.hasPermission("scs." + str + ".*"))
					{
						player.sendMessage(cm.varPlayer(config.getString("message." + str + ".noperm",  mode == ShopMode.BUY ? 
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
					int iamount = event.getCursor().getAmount();
					if(amount + iamount > a)amount = a - iamount;
					price = price*amount;
					if(!econ.has(player, price))
					{
						player.sendMessage(cm.varTrans(config.getString("message.buy.fail", Messages.DEFAULT_BUY_FAIL), player, shop,
								amount, price + curname, price));
						return;
					}
					if(price != 0)
					{
						shop.transactions.put(event.getView(), shop.transactions.get(event.getView()) + price);
						econ.withdrawPlayer(player, price);
						player.sendMessage(cm.varTrans(config.getString("message.buy.success", 
								Messages.DEFAULT_BUY_SUCCESS), player, shop, amount, price + curname, price));
					}
					else player.sendMessage(cm.varTrans(config.getString("message.buy.free", 
							Messages.DEFAULT_BUY_FREE), player, shop, amount, price + curname, price));
					ItemStack n = iamount == 0 ? shop.getItem(event.getRawSlot()) : player.getItemOnCursor();
					n.setAmount(amount + iamount);
					player.setItemOnCursor(n);
					if(shop.getOwner() != null)
						econ.depositPlayer(getServer().getOfflinePlayer(shop.getOwner()), price);
					if(shop.isLimited())
					{
						if(current.getAmount() == amount)
						{
							shop.setItem(event.getRawSlot(), null);
							event.getView().getTopInventory().setItem(event.getRawSlot(), null);
						}
						else 
						{
							updateShopItems(shop, current.getAmount(), amount, event.getRawSlot(), 
									event.getView().getTopInventory());
						}
					}
				}
				else if(shop.getMode() == ShopMode.SELL)
				{
					if(cursor.getType() == Material.AIR)return;
					int amount = cursor.getAmount();
					if(event.isRightClick())amount = 1;
					boolean limited = shop.isLimited();
					if(limited && amount > current.getAmount())
						amount = current.getAmount();
					price *= amount;
					OfflinePlayer owner = getServer().getPlayer(shop.getOwner());
					if(owner != null)
					{
						if(!econ.has(owner, price))
						{
							player.sendMessage(cm.varTrans(config.getString("message.sell.fail", 
									Messages.DEFAULT_SELL_FAIL), player, shop, amount, price + curname, price));
							return;
						}
					}
					if(limited)
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
						if(freespace < amount*2)
						{
							player.sendMessage(cm.varTrans(config.getString("messages.sell.nospace", Messages.DEFAULT_SELL_NOSPACE), 
									player, shop, amount, price + curname, price));
							return;
						}
						int ms = cursor.getType().getMaxStackSize();
						ItemStack o = cursor.clone();
						int ac = amount*2;
						o.setAmount(ac > ms ? ms : ac);
						int as = 0;
						for(int i = 0; i < ac; i += ms)
							as++;
						ItemStack[] add = new ItemStack[as];
						for(int i = 0; ac > ms; ac -= ms)
							add[i] = o;
						if(ac > 0)
						{
							o = o.clone();
							o.setAmount(ac);
							add[add.length - 1] = o;
						}
						storage.addItem(add);
						if(amount == current.getAmount())
						{
							shop.setItem(event.getRawSlot(), null);
							event.getView().getTopInventory().setItem(event.getRawSlot(), null);
						}
						else
						{
							updateShopItems(shop, current.getAmount(), amount, event.getRawSlot(), 
								event.getView().getTopInventory());
						}
					}
					if(price != 0)
					{
						shop.transactions.put(event.getView(), shop.transactions.get(event.getView()) + price);
						econ.depositPlayer(player, price);
						if(owner != null)econ.withdrawPlayer(owner, price);
					}
					player.sendMessage(cm.varTrans(config.getString("message.sell.success", 
							Messages.DEFAULT_SELL_SUCCESS), player, shop, amount, price + curname, price));
					ItemStack n = cursor.clone();
					n.setAmount(n.getAmount() - amount);
					player.setItemOnCursor(n.getAmount() == 0 ? null : n);
				}
				player.updateInventory();
			}
			else if((top && player.getItemOnCursor().getType() != Material.AIR && 
					event.getSlot() != -999) || (!top && event.getCurrentItem().getType() != Material.AIR
					&& event.isShiftClick()))
			{
				player.sendMessage(cm.varPlayer(config.getString("message." + str + ".invalid", Messages.DEFAULT_SELL_INVALID), player));
				player.updateInventory();
				event.setResult(Result.DENY);
				event.setCancelled(true);
			}
		}
		else if(shop.price.containsKey(event.getView()) && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR)
		{
			double p = shop.price.get(event.getView());
			event.setResult(Result.DENY);
			event.setCancelled(true);
			if(!top)
			{
				player.sendMessage(cm.varPlayer(config.getString("message.price.cancel", Messages.DEFAULT_PRICE_CANCEL), player));
				event.getView().close();
				return;
			}
			shop.setPrice(event.getSlot(), p);
			if(event.getAction() == InventoryAction.PICKUP_ALL)
			{
				shop.price.remove(event.getView());
				event.getView().close();
			}
			else
			{
				net.minecraft.server.v1_7_R3.ItemStack nms = shop.getRawItem(event.getRawSlot());
				addPrice(nms);
				event.getInventory().setItem(event.getRawSlot(), CraftItemStack.asCraftMirror(nms));
			}
			player.sendMessage(cm.varPlayer(config.getString("message.price.set", Messages.DEFAULT_PRICE_SET), player));
			player.updateInventory();
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
					net.minecraft.server.v1_7_R3.ItemStack nms = nmsStack(i);
					stripSCSData(nms);
					final ItemStack ui = CraftItemStack.asCraftMirror(nms);
					if(sclick) event.setCurrentItem(ui);
					else player.setItemOnCursor(ui);
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
	
	private void updateShopItems(Shop shop, int ca, int amount, int slot, Inventory inv)
	{
		updateShopItems(shop, ca - amount, slot, inv);
	}
	
	private void updateShopItems(Shop shop, int namount, int slot, Inventory inv)
	{
		net.minecraft.server.v1_7_R3.ItemStack sn = shop.getRawItem(slot);
		sn.count = namount;
		shop.setItem(slot, sn, true);
		sn = sn.cloneItemStack();
		addPrice(sn);
		inv.setItem(slot, CraftItemStack.asCraftMirror(sn));
	}
	
	private NBTTagList removeLastLore(NBTTagList lore)
	{
		NBTTagList newlore = new NBTTagList();
		for(int x = 0; x < lore.size() - 1; x ++)
		{
			newlore.add(new NBTTagString(lore.f(x)));
		}
		return newlore;
	}
	
	private void removeLastLore(net.minecraft.server.v1_7_R3.ItemStack item)
	{
		NBTTagCompound display = item.tag.getCompound("display");
		NBTTagList lore = display.getList("Lore", 8);
		if(lore.size() == 1)display.remove("Lore");
		else display.set("Lore", removeLastLore(lore));
		if(display.c().size() == 0)item.tag.remove("display");
	}
	
	@SuppressWarnings("unused")
	private boolean isSimilar(net.minecraft.server.v1_7_R3.ItemStack stack1, net.minecraft.server.v1_7_R3.ItemStack stack2)
	{
		net.minecraft.server.v1_7_R3.ItemStack s1c = stack1.cloneItemStack();
		net.minecraft.server.v1_7_R3.ItemStack s2c = stack2.cloneItemStack();
		stripSCSData(s1c);
		stripSCSData(s2c);
		return CraftItemStack.asCraftMirror(s1c).isSimilar(CraftItemStack.asCraftMirror(s2c));
	}
	
	private boolean isSimilarUnstripped(net.minecraft.server.v1_7_R3.ItemStack display, net.minecraft.server.v1_7_R3.ItemStack unstr)
	{
		net.minecraft.server.v1_7_R3.ItemStack s1c = display.cloneItemStack();
		net.minecraft.server.v1_7_R3.ItemStack s2c = unstr.cloneItemStack();
		stripSCSData(s1c);
		return CraftItemStack.asCraftMirror(s1c).isSimilar(CraftItemStack.asCraftMirror(s2c));
	}
	
	void stripSCSData(net.minecraft.server.v1_7_R3.ItemStack nms)
	{
		stripSCSData(nms, true);
	}
	
	void stripSCSData(net.minecraft.server.v1_7_R3.ItemStack nms, boolean lastlore)
	{
		if(nms.tag == null)return;
		nms.tag.remove("scs_price");
		if(lastlore)removeLastLore(nms);
		if(nms.tag.c().size() == 0)nms.setTag(null);
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
			else if(getAttachedShop(b).b != null)it.remove();
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void breakBlock(BlockBreakEvent event)
	{
		Block b = event.getBlock();
		DKey<Block, NBTTagCompound> k = getAttachedShop(b);
		Block sb = k.a;
		NBTTagCompound c = k.b;
		if(c != null)
		{
			Player player = event.getPlayer();
			boolean a = player.hasPermission("signchestshop.create");
			if(a)player.sendMessage(cm.color(config.getString("message.break.perm",
					Messages.DEFAULT_BREAK_PERM)));
			else player.sendMessage(cm.varPlayer(config.getString(
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
	
	protected net.minecraft.server.v1_7_R3.ItemStack nmsStack(ItemStack i)
	{
		return CraftItemStack.asNMSCopy(i);
	}
	
	protected NBTTagCompound getShopData(Block b)
	{
		Location bloc = b.getLocation();
		NBTTagList shops = data.getList("Shops", 10);
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
		NBTTagList shops = data.getList("Shops", 10);
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
		NBTTagList items = shop.getList("items", 10);
		ArrayList<ItemStack>ilist = new ArrayList<ItemStack>();
		for(int i = 0; i <items.size(); i ++)
		{
			NBTTagCompound c = (NBTTagCompound) items.get(i).clone();
			if(c.c().size() == 0)
			{
				ilist.add(null);
				continue;
			}
			net.minecraft.server.v1_7_R3.ItemStack item = net.minecraft.server.v1_7_R3.ItemStack.createStack(c);
			CraftItemStack cis = CraftItemStack.asCraftMirror(item);
			if(displayprice)addPrice(item);
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
	
	void addPrice(net.minecraft.server.v1_7_R3.ItemStack item)
	{
		NBTTagCompound tag = item.tag;
		if(tag == null)item.setTag((tag = new NBTTagCompound()));
		if(!tag.hasKey("display"))tag.set("display", new NBTTagCompound());
		NBTTagCompound display = tag.getCompound("display");
		if(!display.hasKey("Lore"))display.set("Lore", new NBTTagList());
		NBTTagList lore = display.getList("Lore", 8);
		lore.add(new NBTTagString(price(tag, item.count, null)));
	}
	
	String price(NBTTagCompound tag, int amount, Double gprice)
	{
		String price = config.getString("shop.price.text", Options.DEFAULT_PRICE_TEXT);
		String pprice;
		double rprice = -1;
		if(tag.hasKey("scs_price"))
		{
			rprice = gprice == null ? tag.getDouble("scs_price") : gprice.doubleValue();
			if(rprice < 0)pprice = config.getString("shop.price.display", Options.DEFAULT_PRICE_DISPLAY);
			if(rprice == 0)pprice = config.getString("shop.price.free", Options.DEFAULT_PRICE_FREE);
			else
			{
				if(amount == 1)
					pprice = config.getString("shop.price.cost", Options.DEFAULT_PRICE_COST).replaceAll("<rawprice>", cm.placePadding(rprice));
				else pprice = config.getString("shop.price.costmulti", Options.DEFAULT_PRICE_COSTMULTI)
					.replaceAll("<rawprice>", cm.placePadding(rprice)).replaceAll("<totalprice>", cm.placePadding(rprice*amount));
			}
		}
		else
		{
			pprice = config.getString("shop.price.display", Options.DEFAULT_PRICE_DISPLAY);
		}
		return cm.varCur(price.replaceAll("<price>", pprice), rprice);
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
	
	static class DKey<V, O>
	{
		V a;
		O b;
		public DKey(V a, O b)
		{
			this.a = a;
			this.b = b;
		}
	}
	
	private void integCheck()
	{	
		final HashMap<String, ArrayList<NBTTagCompound>> requireConversion = 
			new HashMap<String, ArrayList<NBTTagCompound>>();
		NBTTagList shops = data.getList("Shops", 10);
		for(int i = 0; i < shops.size(); i ++)
		{
			NBTTagCompound a = (NBTTagCompound)shops.get(i);
			if(!a.hasKey("limited"))a.setBoolean("limited", false);
			if(!a.hasKey("mode"))a.setInt("mode", ShopMode.BUY.ID);
			if(!a.hasKey("storage"))a.set("storage", new NBTTagList());
			if(a.hasKey("owner") && (!a.hasKey("ownerUUIDMost") || !a.hasKey("ownerUUIDLeast")))
			{
				String o = a.getString("owner");
				ArrayList<NBTTagCompound> shopList = requireConversion.get("owner");
				if(shopList == null)
					requireConversion.put(o, (shopList = new ArrayList<NBTTagCompound>()));
				shopList.add(a);
			}
		}
		
		if(!data.hasKey("usingUUID"))
		{
			getLogger().info("Old file detected; converting names to UUIDs");
			ExecutorService es = Executors.newFixedThreadPool(1);
			Future<Integer> f = es.submit(new Callable<Integer>(){
				public Integer call()
				{
					ArrayList<String> names = new ArrayList<String>();
					names.addAll(requireConversion.keySet());
					UUIDFetcher f = new UUIDFetcher(names);
					try
					{
						getLogger().info("Fetching names from the Internet...");
						Map<String, UUID> results = f.call();
						getLogger().info("Fetch complete. Converting names...");
						int total = 0;
						for(String s: names)
						{
							UUID id = results.get(s);
							long mid = id.getMostSignificantBits();
							long lid = id.getLeastSignificantBits();
							int c = 0;
							for(NBTTagCompound t: requireConversion.get(s))
							{
								t.remove("owner");
								t.setLong("ownerUUIDMost", mid);
								t.setLong("ownerUUIDLeast", lid);
								++c;
							}
							getLogger().info("Converted " + c + " shop(s) belonging to " + s);
							total += c;
						}
						
						return Integer.valueOf(total);
					}
					catch (Exception e)
					{
						getLogger().log(Level.WARNING, "Failed to convert names to UUID", e);
						return null;
					}
				}
			});
			
			Integer i = null;
			try
			{
				i = f.get();
			}
			catch (InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
			
			es.shutdownNow();
			data.setBoolean("usingUUID", true);
			getLogger().info("Converted " + (i != null ? i.intValue() : 0) + " shops");
		}
	}
	
	private void save(DataOutputStream s) throws Exception
	{
		Method m = NBTTagCompound.class.getDeclaredMethod("a", String.class, NBTBase.class, DataOutput.class);
		m.setAccessible(true);
		m.invoke(null, "", data, s);
	}
	
	private void load(DataInputStream s) throws Exception
	{
		data = NBTCompressedStreamTools.a(s);
	}
	
	private void buildShops()
	{
		NBTTagList shops = data.getList("Shops", 10);
		for(int i = 0; i < shops.size(); i ++)
		{
			NBTTagCompound a = (NBTTagCompound)shops.get(i);
			Shop s = new Shop(a);
			s.loadData();
			this.shops.add(s);
		}
	}
}