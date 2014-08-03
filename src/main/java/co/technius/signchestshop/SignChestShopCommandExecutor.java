package co.technius.signchestshop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import net.minecraft.server.v1_7_R4.NBTTagCompound;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import co.technius.signchestshop.Shop.ShopMode;
import co.technius.signchestshop.util.UUIDUtil;

public class SignChestShopCommandExecutor implements CommandExecutor
{

    private class CmdDesc {

        private final String cmd;
        private final String desc;
        private final String perm;

        private CmdDesc(final String cmd, final String desc, final String perm) {
            this.cmd = cmd;
            this.desc = desc;
            this.perm = perm;
        }

        public String asDef() {
            return def(cmd, desc);
        }

        public String getPerm() {
            return perm;
        }
    }

    private final SignChestShopPlugin plugin;
    ConfigManager cm;
    StringConfig config;

    private final CmdDesc[] help = {
            new CmdDesc("scs help", "Displays this menu", null),
            new CmdDesc("scs create", "Creates a shop", "scs.create"),
            new CmdDesc("scs break", "Deletes a shop", "scs.create"),
            new CmdDesc("scs price", "Prices items in a shop", "scs.create"),
            new CmdDesc("scs edit", "Edits a shop", "scs.create"),
            new CmdDesc("scs info", "Shows shop information", null),
            new CmdDesc("scs setmode <mode>", "Sets the mode of a shop", "scs.create"),
            new CmdDesc("scs storage", "Accesses a shop's storage", "scs.create"),
            new CmdDesc("scs settitle <name>", "Sets the title of a shop", "scs.create"),
            new CmdDesc("scs setowner <name>", "Sets the owner of a shop", "scs.admin"),
            new CmdDesc("scs setlimited <true/false>", "Sets shop item availibility", "scs.admin"),
            new CmdDesc("scs reload", "Reloads the config", "scs.reload"),
            new CmdDesc("scs refresh", "Updates the config", "scs.update"),
    };

    public SignChestShopCommandExecutor(final SignChestShopPlugin plugin) {
        this.plugin = plugin;
        cm = plugin.cm;
        config = plugin.cm.config;
    }

    public boolean helpCmd(final CommandSender sender, final String[] args, final String title, final CmdDesc[] help) {
        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (final NumberFormatException nfe) {
                return msg(sender, ChatColor.RED + "\"" + args[1] + "\" is not a valid number");
            }
        }
        final ArrayList<String> d = new ArrayList<String>();
        int max = 1;
        int cmda = 0;
        for (int i = 0; i < help.length; i++) {
            final CmdDesc c = help[i];
            if (c.getPerm() != null) {
                if (!sender.hasPermission(c.getPerm())) {
                    continue;
                }
            }
            if (d.size() < 10) {
                if (i >= (page - 1) * 10 && i <= ((page - 1) * 10) + 9) {
                    d.add(c.asDef());
                }
            }
            if (cmda > 10 && cmda % 10 == 1) {
                max++;
            }
            cmda++;
        }
        sender.sendMessage(ChatColor.GOLD + title + " Help (" + ChatColor.AQUA + page + ChatColor.GOLD + "/" +
                ChatColor.AQUA + max + ChatColor.GOLD + "), " + ChatColor.AQUA + cmda + ChatColor.GOLD + " total");
        for (final String s : d) {
            sender.sendMessage(s);
        }
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("create")) {
                if (noPerm(sender, "scs.create"))
                    return true;
                if (noConsole(sender))
                    return true;
                final Player player = (Player) sender;
                final Block b = player.getTargetBlock(null, 5);
                if (b == null)
                    return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
                if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN)
                    return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
                final Sign s = (Sign) b.getState();
                boolean e = false;
                for (final String x : s.getLines()) {
                    if (!x.isEmpty()) {
                        e = true;
                    }
                }
                if (e && config.getBoolean("shop.forceempty", Options.DEFAULT_SHOP_FORCEEMPTY))
                    return msg(sender, ChatColor.RED + "This sign must be empty!");
                final NBTTagCompound sh = plugin.getShopData(b);
                if (sh != null)
                    return msg(sender, ChatColor.RED + "There is already a shop here!");
                final Inventory i = plugin.getServer().createInventory(null, 27, "Shop");
                plugin.create.put(player.openInventory(i), b);
                player.sendMessage(ChatColor.YELLOW + "Put all the items you want to " +
                        "sell in the shop's inventory.");
            }
            else if (args[0].equalsIgnoreCase("break")) {
                if (noPerm(sender, "scs.create"))
                    return true;
                if (noConsole(sender))
                    return true;
                final Player player = (Player) sender;
                final Block b = player.getTargetBlock(null, 5);
                if (b == null)
                    return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
                final Shop s = plugin.api.getShop(b);
                if (s == null)
                    return msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
                if (checkOwner(player, s, "scs.bypass.break"))
                    return true;
                final Sign sign = (Sign) b.getState();
                sign.setLine(0, "");
                sign.setLine(1, "");
                sign.setLine(2, "");
                sign.setLine(3, "");
                sign.update();
                plugin.removeShop(s.data);
                player.sendMessage(ChatColor.YELLOW + "SignChestShop broken.");
            } else if (args[0].equalsIgnoreCase("price")) {
                final Shop s = checkTarget(sender, "scs.create", 2, 2, args.length, "scs price <price>");
                if (s == null)
                    return true;
                final Player player = (Player) sender;
                if (checkOwner(player, s, "scs.bypass.price"))
                    return true;
                double price;
                if (args[1].equalsIgnoreCase("free")) {
                    price = 0;
                } else if (args[1].equalsIgnoreCase("display")) {
                    price = -1;
                } else {
                    try {
                        price = Double.parseDouble(args[1]);
                        if (price <= 0)
                            return msg(sender, ChatColor.RED +
                                    "Price must be a positive number!");
                    } catch (final NumberFormatException nfe) {
                        return msg(sender, ChatColor.RED + "\"" + args[1] + "\" is not a valid number.");
                    }
                }
                final Inventory i = plugin.getShop(s.data, true, "Price");
                s.price.put(player.openInventory(i), price);
            } else if (args[0].equalsIgnoreCase("edit")) {
                final Shop s = checkTarget(sender, "scs.create", 1, 1, args.length, "scs edit");
                if (s == null)
                    return true;
                final Player player = (Player) sender;
                if (checkOwner(player, s, "scs.bypass.edit"))
                    return true;
                final Inventory i = plugin.getShop(s.data, false, "Edit");
                s.edit.add(player.openInventory(i));
            } else if (args[0].equalsIgnoreCase("info")) {
                final Shop s = checkTarget(sender, null, 1, 1, args.length, "scs info");
                if (s != null) {
                    final String o = s.getOwnerName();
                    msg(sender, ChatColor.AQUA + "Shop Information" + (s.getTitle() != null ? ": " + ChatColor.GOLD + s.getTitle() : ""));
                    if (o != null) {
                        msg(sender, ChatColor.AQUA + "Owner: " + ChatColor.GOLD + o);
                    }
                    msg(sender, ChatColor.AQUA + "Mode: " + ChatColor.GOLD + s.getMode().toString().toLowerCase());
                    msg(sender, ChatColor.AQUA + "Runs out of stock: " + ChatColor.GOLD + (s.isLimited() ? "Yes" : "No"));
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (noPerm(sender, "scs.reload"))
                    return true;
                try {
                    config.load();
                    sender.sendMessage(ChatColor.GREEN + "Config reloaded successfully.");
                } catch (final IOException ioe) {
                    sender.sendMessage(ChatColor.RED + "An error occured while reloading the config" +
                            (sender != plugin.getServer().getConsoleSender() ? ", check the console for details" : "") + "!");
                    plugin.log.log(Level.SEVERE, "Could not load config, reverting to defaults", ioe);
                }
            } else if (args[0].equalsIgnoreCase("refresh")) {
                if (noPerm(sender, "scs.refresh"))
                    return true;
                final boolean s = cm.writeConfig();
                if (s) {
                    sender.sendMessage(ChatColor.GREEN + "Config file updated.");
                } else {
                    sender.sendMessage(ChatColor.RED + "An error occured while updating the config" +
                            (sender != plugin.getServer().getConsoleSender() ? ", check the console for details" : "") + "!");
                }
            } else if (args[0].equalsIgnoreCase("setmode")) {
                final Shop s = checkTarget(sender, "scs.create", 2, 2, args.length, "scs setmode <mode>");
                if (s == null)
                    return true;
                if (checkOwner((Player) sender, s, "scs.bypass.setmode"))
                    return true;
                if (args[1].equalsIgnoreCase("buy")) {
                    s.setMode(ShopMode.BUY);
                } else if (args[1].equalsIgnoreCase("sell")) {
                    s.setMode(ShopMode.SELL);
                } else
                    return msg(sender, ChatColor.RED + "Acceptable modes: buy, sell");
                sender.sendMessage(ChatColor.GREEN + "Mode set to " + args[1]);
            } else if (args[0].equalsIgnoreCase("storage")) {
                final Shop s = checkTarget(sender, "scs.create", 1, 1, args.length, "scs storage");
                if (s == null)
                    return true;
                final Player player = (Player) sender;
                if (checkOwner(player, s, "scs.bypass.storage"))
                    return true;
                s.storage.add(player.openInventory(s.getStorage()));
            } else if (args[0].equalsIgnoreCase("setowner")) {
                final Shop s = checkTarget(sender, "scs.admin", 2, 2, args.length, "scs setowner <name>");
                if (s == null)
                    return true;
                if (args[1].equalsIgnoreCase("none"))
                {
                    s.setOwner((UUID) null);
                    return msg(sender, ChatColor.GREEN + "This shop no longer has an owner.");
                }
                final Future<UUID> f = UUIDUtil.getUUID(args[1]);
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        if (f.isDone()) {
                            cancel();
                            try {
                                final UUID id = f.get();
                                if (id == null) {
                                    msg(sender, ChatColor.RED + "\"" + args[1] + "\" is not an actual player.");
                                } else {
                                    s.setOwner(id);
                                    msg(sender, ChatColor.GREEN + "The owner of this shop has been set to \"" + args[1] + "\"");
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                                msg(sender, ChatColor.RED + "The player \"" + args[1] + "\" could not be found.");
                                return;
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                return true;
            } else if (args[0].equalsIgnoreCase("setlimited")) {
                final Shop s = checkTarget(sender, "scs.admin", 2, 2, args.length, "scs setlimited <true/false>");
                if (s == null)
                    return true;
                final boolean l = args[1].equalsIgnoreCase("true");
                s.setLimited(l);
                return msg(sender, ChatColor.GREEN + "This shop is now " + (l ? "" : "un") + "limited.");
            } else if (args[0].equalsIgnoreCase("settitle")) {
                final Shop s = checkTarget(sender, "scs.create", 1, Integer.MAX_VALUE, args.length, "scs settitle <name>");
                if (s == null)
                    return true;
                final Player player = (Player) sender;
                if (checkOwner(player, s, "scs.bypass.settitle"))
                    return true;
                if (args[0].equalsIgnoreCase("none"))
                {
                    s.setTitle(null);
                    return msg(sender, cm.varPlayer(config.getString("message.settitle.remove", Messages.DEFAULT_SETTITLE_REMOVE), player));
                }
                final StringBuffer sb = new StringBuffer();
                sb.append(args[1]);
                for (int i = 2; i < args.length; i++) {
                    sb.append(" ").append(args[i]);
                }
                final String n = sb.toString();
                if (n.length() > 32)
                    return msg(sender, cm.varPlayer(config.getString("message.settitle.fail", Messages.DEFAULT_SETTITLE_FAIL), player).replaceAll("<title>", n));
                s.setTitle(n);
                msg(sender, cm.varPlayer(config.getString("message.settitle", Messages.DEFAULT_SETTITLE_SUCCESS), player).replaceAll("<title>", n));
            } else if (args[0].equalsIgnoreCase("help")) {
                helpCmd(sender, args, "SignChestShop", help);
            } else {
                sender.sendMessage(ChatColor.GOLD + "Command unrecognized.  " +
                        "Type " + ChatColor.AQUA + "/scs help" + ChatColor.GOLD + " for help");
            }
        } else {
            sender.sendMessage(ChatColor.GOLD + "SignChestShop version " +
                    plugin.getDescription().getVersion());
            sender.sendMessage("Type " + ChatColor.AQUA + "/scs help" + ChatColor.GOLD + " for help");
        }
        return true;
    }

    private boolean checkOwner(final Player player, final Shop shop, final String perm) {
        if (!player.hasPermission(perm) && !player.getUniqueId().equals(shop.getOwner()))
            return msg(player, cm.varPlayer(config.getString("message.cmd.notowned", Messages.DEFAULT_CMD_NOTOWNED), player));
        return false;
    }

    private Shop checkTarget(final CommandSender sender, final String perm, final int argmin, final int argmax, final int argc, final String usage) {
        // TODO Find alternative to getTargetBlock
        if (noPerm(sender, perm))
            return null;
        if (noConsole(sender))
            return null;
        if (argc < argmin || argc > argmax) {
            msg(sender, ChatColor.RED + "Usage: /" + usage);
            return null;
        }
        final Player player = (Player) sender;
        @SuppressWarnings("deprecation")
        final Block b = player.getTargetBlock(null, 5);
        if (b == null) {
            msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
            return null;
        }
        final Shop s = plugin.getShop(b);
        if (s == null) {
            msg(sender, cm.varPlayer(config.getString("message.cmd.notarget", Messages.DEFAULT_CMD_NOTARGET), player));
        }
        return s;
    }

    private String def(final String a, final String b) {
        return ChatColor.AQUA + a + ChatColor.DARK_RED + " - " + ChatColor.GOLD + b;
    }

    private boolean msg(final CommandSender sender, final String msg) {
        sender.sendMessage(msg);
        return true;
    }

    private boolean noConsole(final CommandSender sender) {
        if (!(sender instanceof Player))
            return true;
        return false;
    }

    private boolean noPerm(final CommandSender sender, final String perm) {
        if (perm == null || sender.hasPermission(perm))
            return false;
        sender.sendMessage(cm.color(config.getString("message.cmd.noperm", Messages.DEFAULT_CMD_NOPERM)));
        return true;
    }
}
