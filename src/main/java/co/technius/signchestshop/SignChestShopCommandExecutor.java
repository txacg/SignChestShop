package co.technius.signchestshop;

import cat.nyaa.signchestshop.I18n;
import cat.nyaa.utils.Internationalization;
import co.technius.signchestshop.Shop.ShopMode;
import co.technius.signchestshop.util.UUIDUtil;
import net.minecraft.server.v1_11_R1.MovingObjectPosition;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.Vec3D;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import static net.obnoxint.mcdev.signchestshop.R.CFG_SHOP_FORCEEMPTY;

public class SignChestShopCommandExecutor implements CommandExecutor {

    private final SignChestShopPlugin plugin;
    private final CmdDesc[] help = {
            new CmdDesc("help", null),
            new CmdDesc("create", "scs.create"),
            new CmdDesc("break", "scs.create"),
            new CmdDesc("price", "scs.create"),
            new CmdDesc("edit", "scs.create"),
            new CmdDesc("info", null),
            new CmdDesc("setmode", "scs.create"),
            new CmdDesc("storage", "scs.create"),
            new CmdDesc("settitle", "scs.create"),
            new CmdDesc("setowner", "scs.admin"),
            new CmdDesc("setlimited", "scs.admin"),
            new CmdDesc("reload", "scs.reload"),
            new CmdDesc("refresh", "scs.update"),
    };
    ConfigManager cm;
    StringConfig config;
    private Internationalization i18n;

    public SignChestShopCommandExecutor(final SignChestShopPlugin plugin) {
        this.plugin = plugin;
        cm = plugin.cm;
        config = plugin.cm.config;
        i18n = plugin.i18n;
    }

    public boolean helpCmd(final CommandSender sender, final String[] args, final String title, final CmdDesc[] help) {
        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (final NumberFormatException nfe) {
                msg(sender, "message.error.not_int");
                return true;
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
        //sender.sendMessage(ChatColor.GOLD + title + " Help (" + ChatColor.AQUA + page + ChatColor.GOLD + "/" +
        //        ChatColor.AQUA + max + ChatColor.GOLD + "), " + ChatColor.AQUA + cmda + ChatColor.GOLD + " total");
        msg(sender, "message.help.page", title, page, max, cmda);
        for (final String s : d) {
            sender.sendMessage(s);
        }
        return true;
    }

    @Override
    //@SuppressWarnings("deprecation")
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("create")) {
                if (noPerm(sender, "scs.create")) {
                    return true;
                }
                if (noConsole(sender)) {
                    return true;
                }
                final Player player = (Player) sender;
                final Block block = rayTrace(player);
                if (block == null) {
                    msg(sender, "message.cmd.notarget");
                    return true;
                }
                if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) {
                    msg(sender, "message.cmd.notarget");
                    return true;
                }
                final Sign s = (Sign) block.getState();
                boolean isEmpty = true;
                for (final String x : s.getLines()) {
                    if (!x.isEmpty()) {
                        isEmpty = false;
                    }
                }
                if (!isEmpty && config.getBoolean("shop.forceempty", CFG_SHOP_FORCEEMPTY)) {
                    msg(sender, "message.create.must_empty");
                    return true;
                }
                final NBTTagCompound sh = plugin.getShopData(block);
                if (sh != null) {
                    msg(sender, "message.create.already_exists");
                    return true;
                }
                final Inventory inv = plugin.getServer().createInventory(null, 27, I18n._("message.create.title"));
                plugin.create.put(player.openInventory(inv), block);
                msg(sender, "message.create.notify");
            } else if (args[0].equalsIgnoreCase("break")) {
                if (noPerm(sender, "scs.create")) {
                    return true;
                }
                if (noConsole(sender)) {
                    return true;
                }
                final Player player = (Player) sender;
                final Block block = rayTrace(player);
                if (block == null) {
                    msg(sender, "message.cmd.notarget");
                    return true;
                }
                final Shop s = plugin.getShop(block);
                if (s == null) {
                    msg(sender, "message.cmd.notarget");
                    return true;
                }
                if (!isOwner(player, s) && !player.hasPermission("scs.bypass.break")) {
                    msg(player, "message.cmd.notowned");
                    return true;
                }
                final Sign sign = (Sign) block.getState();
                sign.setLine(0, "");
                sign.setLine(1, "");
                sign.setLine(2, "");
                sign.setLine(3, "");
                sign.update();
                plugin.removeShop(s.data);
                msg(sender, "message.break.success");
                return true;
            } else if (args[0].equalsIgnoreCase("price")) {
                final Shop s = checkTarget(sender, "scs.create", 2, 2, args.length, "price");
                if (s == null) {
                    return true;
                }
                final Player player = (Player) sender;
                if (!isOwner(player, s) && !player.hasPermission("scs.bypass.price")) {
                    msg(player, "message.cmd.notowned");
                    return true;
                }
                double price;
                if (args[1].equalsIgnoreCase("free")) {
                    price = 0;
                } else if (args[1].equalsIgnoreCase("display")) {
                    price = -1;
                } else {
                    try {
                        price = Double.parseDouble(args[1]);
                        if (price < 0.01) {
                            msg(sender, "message.error.not_double");
                            return true;
                        }
                    } catch (final NumberFormatException nfe) {
                        msg(sender, "message.error.not_double");
                        return true;
                    }
                }
                final Inventory i = plugin.getShop(s.data, true, I18n._("message.price.title"));
                s.price.put(player.openInventory(i), price);
                return true;
            } else if (args[0].equalsIgnoreCase("edit")) {
                final Shop s = checkTarget(sender, "scs.create", 1, 1, args.length, "edit");
                if (s == null) {
                    return true;
                }
                final Player player = (Player) sender;
                if (!isOwner(player, s) && !player.hasPermission("scs.bypass.edit")) {
                    msg(player, "message.cmd.notowned");
                    return true;
                }
                final Inventory i = plugin.getShop(s.data, false, I18n._("message.edit.title"));
                s.edit.add(player.openInventory(i));
            } else if (args[0].equalsIgnoreCase("info")) {
                final Shop s = checkTarget(sender, null, 1, 1, args.length, "info");
                if (s != null) {
                    final String o = s.getOwnerName();
                    msg(sender, "message.info.info_0", (s.getTitle() != null ? " : " + ChatColor.GOLD + s.getTitle() : ""));
                    if (o != null) {
                        msg(sender, "message.info.info_1", o);
                    }
                    msg(sender, "message.info.info_2", s.getMode().toString().toLowerCase());
                    msg(sender, "message.info.info_3", (s.isLimited() ? "Yes" : "No"));
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (noPerm(sender, "scs.reload")) {
                    return true;
                }
                try {
                    config.load();
                    sender.sendMessage(ChatColor.GREEN + "Config reloaded successfully.");
                } catch (final IOException ioe) {
                    sender.sendMessage(ChatColor.RED + "An error occured while reloading the config" +
                            (sender != plugin.getServer().getConsoleSender() ? ", check the console for details" : "") + "!");
                    plugin.log.log(Level.SEVERE, "Could not load config, reverting to defaults", ioe);
                }
            } else if (args[0].equalsIgnoreCase("refresh")) {
                if (noPerm(sender, "scs.refresh")) {
                    return true;
                }
                final boolean s = cm.writeConfig();
                if (s) {
                    sender.sendMessage(ChatColor.GREEN + "Config file updated.");
                } else {
                    sender.sendMessage(ChatColor.RED + "An error occured while updating the config" +
                            (sender != plugin.getServer().getConsoleSender() ? ", check the console for details" : "") + "!");
                }
            } else if (args[0].equalsIgnoreCase("setmode")) {
                final Shop s = checkTarget(sender, "scs.create", 2, 2, args.length, "setmode");
                if (s == null) {
                    return true;
                }
                Player player = (Player) sender;
                if (!isOwner(player, s) && !player.hasPermission("scs.bypass.setmode")) {
                    msg(player, "message.cmd.notowned");
                    return true;
                }
                if (args[1].equalsIgnoreCase("buy")) {
                    s.setMode(ShopMode.BUY);
                } else if (args[1].equalsIgnoreCase("sell")) {
                    s.setMode(ShopMode.SELL);
                } else {
                    msg(sender, "message.setmode.mode");
                    return true;
                }
                msg(sender, "message.setmode.success", args[1].toUpperCase());
                return true;
            } else if (args[0].equalsIgnoreCase("storage")) {
                final Shop s = checkTarget(sender, "scs.create", 1, 1, args.length, "storage");
                if (s == null) {
                    return true;
                }
                final Player player = (Player) sender;
                if (!isOwner(player, s) && !player.hasPermission("scs.bypass.storage")) {
                    msg(player, "message.cmd.notowned");
                    return true;
                }
                s.storage.add(player.openInventory(s.getStorage()));
                return true;
            } else if (args[0].equalsIgnoreCase("setowner")) {
                final Shop s = checkTarget(sender, "scs.admin", 2, 2, args.length, "setowner");
                if (s == null) {
                    return true;
                }
                if (args[1].equalsIgnoreCase("none")) {
                    s.setOwner((UUID) null);
                    msg(sender, "message.setowner.none");
                    return true;
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
                                    msg(sender, "message.setowner.invalid", args[1]);
                                } else {
                                    s.setOwner(id);
                                    msg(sender, "message.setowner.success", args[1]);
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                                msg(sender, "message.setowner.not_found", args[1]);
                                return;
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                return true;
            } else if (args[0].equalsIgnoreCase("setlimited")) {
                final Shop s = checkTarget(sender, "scs.admin", 2, 2, args.length, "setlimited");
                if (s == null) {
                    return true;
                }
                final boolean l = args[1].equalsIgnoreCase("true");
                s.setLimited(l);
                if (l) {
                    msg(sender, "message.setlimited.success", I18n._("message.setlimited.limited"));
                } else {
                    msg(sender, "message.setlimited.success", I18n._("message.setlimited.unlimited"));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("settitle")) {
                final Shop s = checkTarget(sender, "scs.create", 2, Integer.MAX_VALUE, args.length, "settitle");
                if (s == null)
                    return true;
                final Player player = (Player) sender;
                if (!isOwner(player, s) && !player.hasPermission("scs.bypass.settitle")) {
                    msg(player, "message.cmd.notowned");
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("none")) {
                    s.setTitle(null);
                    msg(sender, "message.settitle.remove");
                    return true;
                }
                if (!(args.length > 1)) {
                    return true;
                }
                final StringBuffer sb = new StringBuffer();
                sb.append(args[1]);
                for (int i = 2; i < args.length; i++) {
                    sb.append(" ").append(args[i]);
                }
                final String title = sb.toString();
                if (title.length() > 32) {
                    msg(sender, "message.settitle.fail");
                    return true;
                }
                s.setTitle(title);
                msg(sender, "message.settitle.success", title);
                return true;
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

    private boolean isOwner(final Player player, final Shop shop) {
        if (player.getUniqueId().equals(shop.getOwner())) {
            return true;
        }
        return false;
    }

    private Shop checkTarget(final CommandSender sender, final String perm, final int argmin, final int argmax, final int argc, final String command) {
        if (noPerm(sender, perm)) {
            return null;
        }
        if (noConsole(sender)) {
            return null;
        }
        if (argc < argmin || argc > argmax) {
            msg(sender, "command." + command + ".description");
            msg(sender, "command." + command + ".usage");
            return null;
        }
        final Player player = (Player) sender;
        //@SuppressWarnings("deprecation")
        final Block block = rayTrace(player);
        if (block == null) {
            msg(sender, "message.cmd.notarget");
            return null;
        }
        final Shop s = plugin.getShop(block);
        if (s == null) {
            msg(sender, "message.cmd.notarget");
            return null;
        }
        return s;
    }

    private String def(final String a, final String b) {
        return ChatColor.AQUA + a + ChatColor.DARK_RED + " - " + ChatColor.GOLD + b;
    }

    private boolean msg(final CommandSender sender, String key, Object... args) {
        sender.sendMessage(I18n._(key, args));
        return true;
    }

    private boolean noConsole(final CommandSender sender) {
        if (!(sender instanceof Player))
            return true;
        return false;
    }

    private boolean noPerm(final CommandSender sender, final String perm) {
        if (perm == null || sender.hasPermission(perm)) {
            return false;
        }
        msg(sender, "message.cmd.noperm");
        return true;
    }

    private Block rayTrace(Player player) {
        CraftWorld world = (CraftWorld) player.getWorld();
        Location eye = player.getEyeLocation();
        Vector target = eye.toVector().add(eye.getDirection().multiply(5));
        MovingObjectPosition mop = world.getHandle().rayTrace(new Vec3D(eye.getX(), eye.getY(), eye.getZ()),
                new Vec3D(target.getX(), target.getY(), target.getZ()));
        if (mop != null) {
            double x = mop.pos.x;
            double y = mop.pos.y;
            double z = mop.pos.z;
            if (x < 1.0) {
                x = x - 1.0;
            }
            if (z < 1.0) {
                z = z - 1.0;
            }
            return player.getWorld().getBlockAt((int) x, (int) y, (int) z);
        }
        return null;
    }

    private class CmdDesc {

        private final String cmd;
        private final String desc;
        private final String perm;

        private CmdDesc(final String cmd, final String perm) {
            this.cmd = I18n._("command." + cmd + ".usage");
            this.desc = I18n._("command." + cmd + ".description");
            this.perm = perm;
        }

        public String asDef() {
            return def(cmd, desc);
        }

        public String getPerm() {
            return perm;
        }
    }
}
