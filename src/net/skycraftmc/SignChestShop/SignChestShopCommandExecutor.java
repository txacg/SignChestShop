package net.skycraftmc.SignChestShop;

import java.io.IOException;
import java.util.logging.Level;

import net.minecraft.server.v1_7_R1.NBTTagCompound;
import net.skycraftmc.SignChestShop.Shop.ShopMode;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SignChestShopCommandExecutor implements CommandExecutor
{
	private SignChestShopPlugin plugin;
	ConfigManager cm;
	StringConfig config;
	public SignChestShopCommandExecutor(SignChestShopPlugin plugin)
	{
		this.plugin = plugin;
		cm = plugin.cm;
		config = plugin.cm.config;
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
					return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				if(b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN)
					return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				Sign s = (Sign)b.getState();
				boolean e = false;
				for(String x:s.getLines())
				{
					if(!x.isEmpty())e = true;
				}
				if(e && config.getBoolean("shop.forceempty", Options.DEFAULT_SHOP_FORCEEMPTY))
					return msg(sender, ChatColor.RED + "This sign must be empty!");
				NBTTagCompound sh = plugin.getShopData(b);
				if(sh != null)return msg(sender, ChatColor.RED + "There is already a shop here!");
				Inventory i = plugin.getServer().createInventory(null, 27, "Shop");
				plugin.create.put(player.openInventory(i), b);
				player.sendMessage(ChatColor.YELLOW + "Put all the items you want to " +
						"sell in the shop's inventory.");
			}
			else if(args[0].equalsIgnoreCase("break"))
			{
				if(noPerm(sender, "scs.create"))return true;
				if(noConsole(sender))return true;
				Player player = (Player)sender;
				Block b = player.getTargetBlock(null, 5);
				if(b == null)return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				Shop s = plugin.api.getShop(b);
				if(s == null)
					return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				if(checkOwner(player, s, "scs.bypass.break"))return true;
				Sign sign = (Sign)b.getState();
				sign.setLine(0, "");
				sign.setLine(1, "");
				sign.setLine(2, "");
				sign.setLine(3, "");
				sign.update();
				plugin.removeShop(s.data);
				player.sendMessage(ChatColor.YELLOW + "SignChestShop broken.");
			}
			else if(args[0].equalsIgnoreCase("price"))
			{
				if(noPerm(sender, "scs.create"))return true;
				if(noConsole(sender))return true;
				if(args.length != 2)return msg(sender, ChatColor.RED + "Usage: /scs price <price>");
				Player player = (Player)sender;
				Block b = player.getTargetBlock(null, 5);
				if(b == null)return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				NBTTagCompound s = plugin.getShopData(b);
				if(s == null)
					return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				if(checkOwner(player, plugin.getShopObject(s), "scs.bypass.price"))return true;
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
					}
					catch(NumberFormatException nfe)
					{
						return msg(sender, ChatColor.RED + 
								"\"" + args[1] + "\" is not a valid number.");
					}
				}
				Inventory i = plugin.getShop(s, true, "Price");
				plugin.getShop(b).price.put(player.openInventory(i), price);
			}
			else if(args[0].equalsIgnoreCase("edit"))
			{
				if(noPerm(sender, "scs.create"))return true;
				if(noConsole(sender))return true;
				Player player = (Player)sender;
				Block b = player.getTargetBlock(null, 5);
				if(b == null)return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				NBTTagCompound s = plugin.getShopData(b);
				if(s == null)
					return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
				if(checkOwner(player, plugin.getShopObject(s), "scs.bypass.edit"))return true;
				Inventory i = plugin.getShop(s, false, "Edit");
				plugin.getShop(b).edit.add(player.openInventory(i));
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
							(sender != plugin.getServer().getConsoleSender() ? ", check the console for details" : "") + "!");
					plugin.log.log(Level.SEVERE, "Could not load config, reverting to defaults", ioe);
				}
			}
			else if(args[0].equalsIgnoreCase("refresh"))
			{
				if(noPerm(sender, "scs.refresh"))return true;
				boolean s = cm.writeConfig();
				if(s)sender.sendMessage(ChatColor.GREEN + "Config file updated.");
				else sender.sendMessage(ChatColor.RED + "An error occured while updating the config" + 
						(sender != plugin.getServer().getConsoleSender() ? ", check the console for details" : "") + "!");
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
					return msg(sender, cm.varPlayer(config.getString("message.settitle.remove", Messages.DEFAULT_SETTITLE_REMOVE), player));
				}
				StringBuffer sb = new StringBuffer();
				sb.append(args[1]);
				for(int i = 2; i < args.length; i ++)
					sb.append(" ").append(args[i]);
				String n = sb.toString();
				if(n.length() > 32)
					return msg(sender, cm.varPlayer(config.getString("message.settitle.fail", Messages.DEFAULT_SETTITLE_FAIL), player).replaceAll("<title>", n));
				s.setTitle(n);
				msg(sender, cm.varPlayer(config.getString("message.settitle", Messages.DEFAULT_SETTITLE_SUCCESS), player).replaceAll("<title>", n));
			}
			else if(args[0].equalsIgnoreCase("help"))helpCmd(sender, args);
			else sender.sendMessage(ChatColor.GOLD + "Command unrecognized.  " +
					"Type " + ChatColor.AQUA + "/scs help" + ChatColor.GOLD + " for help");
		}
		else
		{
			sender.sendMessage(ChatColor.GOLD + "SignChestShop version " + 
					plugin.getDescription().getVersion());
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
			msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
			return null;
		}
		Shop s = plugin.getShop(b);
		if(s == null)
			msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
		return s;
	}

	private boolean checkOwner(Player player, Shop shop, String perm)
	{
		if(!player.hasPermission(perm) && !player.getName().equals(shop.getOwner()))
			return msg(player, cm.varPlayer(config.getString("message.cmd.notowned", Messages.DEFAULT_CMD_NOTOWNED), player));
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
		sender.sendMessage(cm.color(config.getString("message.cmd.noperm", Messages.DEFAULT_CMD_NOPERM)));
		return true;
	}
}
