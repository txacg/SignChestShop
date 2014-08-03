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
import net.minecraft.server.v1_7_R4.NBTBase;
import net.minecraft.server.v1_7_R4.NBTCompressedStreamTools;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NBTTagList;
import net.minecraft.server.v1_7_R4.NBTTagString;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
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

    static class DKey<V, O> {

        V a;
        O b;

        public DKey(final V a, final O b) {
            this.a = a;
            this.b = b;
        }
    }

    private StringConfig config;
    ConfigManager cm;
    protected NBTTagCompound data;
    protected ArrayList<Shop> shops = new ArrayList<Shop>();
    HashMap<InventoryView, Block> create = new HashMap<InventoryView, Block>();
    Economy econ;
    Logger log;
    SignChestShopAPI api;
    protected static SignChestShopPlugin inst;
    private boolean initsuccess = false;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void breakBlock(final BlockBreakEvent event) {
        final Block b = event.getBlock();
        final DKey<Block, NBTTagCompound> k = getAttachedShop(b);
        final Block sb = k.a;
        final NBTTagCompound c = k.b;
        if (c != null) {
            final Player player = event.getPlayer();
            final boolean a = player.hasPermission("signchestshop.create");
            if (a) {
                player.sendMessage(cm.color(config.getString("message.break.perm",
                        Messages.DEFAULT_BREAK_PERM)));
            } else {
                player.sendMessage(cm.varPlayer(config.getString(
                        "message.break.noperm", Messages.DEFAULT_BREAK_NOPERM), player));
            }
            event.setCancelled(true);
            sb.getState().update();
            return;
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void click(final InventoryClickEvent event) {
        Player player = null;
        if (event.getWhoClicked() instanceof Player) {
            player = (Player) event.getWhoClicked();
        }
        if (player == null)
            return;
        final boolean top = event.getRawSlot() < event.getView().getTopInventory().getSize();
        Shop shop = null;
        boolean transaction = false;
        for (final Shop s : shops) {
            if (shop != null) {
                break;
            }
            if (s.transactions.containsKey(event.getView())) {
                shop = s;
                transaction = true;
            }
            else if (s.edit.contains(event.getView())) {
                shop = s;
            } else if (s.price.containsKey(event.getView())) {
                shop = s;
            }
        }
        if (shop == null)
            return;
        if (transaction) {
            final ShopMode mode = shop.getMode();
            final String str = mode == ShopMode.BUY ? "buy" : "sell";
            if (top) {
                event.setResult(Result.DENY);
                event.setCancelled(true);
                player.updateInventory();
                final ItemStack current = event.getCurrentItem();
                final ItemStack cursor = player.getItemOnCursor();
                if (current == null || current.getType() == Material.AIR) {
                    if (mode == ShopMode.SELL) {
                        player.sendMessage(cm.varPlayer(config.getString("message.sell.invalid", Messages.DEFAULT_SELL_INVALID), player));
                    }
                    return;
                }
                final net.minecraft.server.v1_7_R4.ItemStack currentNMS = nmsStack(current.clone());
                if (!currentNMS.getTag().hasKey("scs_price"))
                    return;
                double price = currentNMS.getTag().getDouble("scs_price");
                if (config.getBoolean(str + ".permsid", mode == ShopMode.BUY ? Options.DEFAULT_BUY_PERMSID : Options.DEFAULT_SELL_PERMSID)) {
                    if (!player.hasPermission("scs." + str + "." + current.getTypeId()) && !player.hasPermission("scs." + str + ".*")) {
                        player.sendMessage(cm.varPlayer(config.getString("message." + str + ".noperm", mode == ShopMode.BUY ?
                                Messages.DEFAULT_BUY_NOPERM : Messages.DEFAULT_SELL_NOPERM), player));
                        return;
                    }
                }

                if (cursor.getType() != Material.AIR) {
                    if (!isSimilarUnstripped(currentNMS, nmsStack(cursor)))
                        return;
                }

                String curname = (price == 1 ? econ.currencyNameSingular() : econ.currencyNamePlural());
                if (!curname.isEmpty()) {
                    curname = " " + curname;
                }

                if (shop.getMode() == ShopMode.BUY) {
                    final int a = current.getType().getMaxStackSize();
                    int amount = 1;
                    final String buymode = config.getString("buy.mode", "single");
                    if (buymode.equalsIgnoreCase("stack") ||
                            (event.isShiftClick() && config.getBoolean("buy.shiftclick", true))) {
                        amount = shop.isLimited() ? current.getAmount() : a;
                    } else if (buymode.equalsIgnoreCase("amount")) {
                        amount = current.getAmount();
                    } else {
                        amount = 1;
                    }
                    if (event.getAction() == InventoryAction.PICKUP_HALF || event.getAction() == InventoryAction.PICKUP_ONE) {
                        amount = 1;
                    }
                    final int iamount = event.getCursor().getAmount();
                    if (amount + iamount > a) {
                        amount = a - iamount;
                    }
                    price = price * amount;
                    if (!econ.has(player, price)) {
                        player.sendMessage(cm.varTrans(config.getString("message.buy.fail", Messages.DEFAULT_BUY_FAIL), player, shop,
                                amount, price + curname, price));
                        return;
                    }
                    if (price != 0) {
                        shop.transactions.put(event.getView(), shop.transactions.get(event.getView()) + price);
                        econ.withdrawPlayer(player, price);
                        player.sendMessage(cm.varTrans(config.getString("message.buy.success",
                                Messages.DEFAULT_BUY_SUCCESS), player, shop, amount, price + curname, price));
                    } else {
                        player.sendMessage(cm.varTrans(config.getString("message.buy.free",
                                Messages.DEFAULT_BUY_FREE), player, shop, amount, price + curname, price));
                    }
                    final ItemStack n = iamount == 0 ? shop.getItem(event.getRawSlot()) : player.getItemOnCursor();
                    n.setAmount(amount + iamount);
                    player.setItemOnCursor(n);
                    if (shop.getOwner() != null) {
                        econ.depositPlayer(getServer().getOfflinePlayer(shop.getOwner()), price);
                    }
                    if (shop.isLimited()) {
                        if (current.getAmount() == amount) {
                            shop.setItem(event.getRawSlot(), null);
                            event.getView().getTopInventory().setItem(event.getRawSlot(), null);
                        } else {
                            updateShopItems(shop, current.getAmount(), amount, event.getRawSlot(),
                                    event.getView().getTopInventory());
                        }
                    }
                }
                else if (shop.getMode() == ShopMode.SELL) {
                    if (cursor.getType() == Material.AIR)
                        return;
                    int amount = cursor.getAmount();
                    if (event.isRightClick()) {
                        amount = 1;
                    }
                    final boolean limited = shop.isLimited();
                    if (limited && amount > current.getAmount()) {
                        amount = current.getAmount();
                    }
                    price *= amount;
                    final OfflinePlayer owner = getServer().getPlayer(shop.getOwner());
                    if (owner != null) {
                        if (!econ.has(owner, price)) {
                            player.sendMessage(cm.varTrans(config.getString("message.sell.fail",
                                    Messages.DEFAULT_SELL_FAIL), player, shop, amount, price + curname, price));
                            return;
                        }
                    }
                    if (limited) {
                        final Inventory storage = shop.getStorage();
                        int freespace = 0;
                        for (final ItemStack i : storage.getContents()) {
                            if (i == null || i.getType() == Material.AIR) {
                                freespace += cursor.getType().getMaxStackSize();
                            }
                            else if (i.isSimilar(cursor)) {
                                freespace += i.getType().getMaxStackSize() - i.getAmount();
                            }
                        }
                        if (freespace < amount * 2) {
                            player.sendMessage(cm.varTrans(config.getString("messages.sell.nospace", Messages.DEFAULT_SELL_NOSPACE),
                                    player, shop, amount, price + curname, price));
                            return;
                        }
                        final int ms = cursor.getType().getMaxStackSize();
                        ItemStack o = cursor.clone();
                        int ac = amount * 2;
                        o.setAmount(ac > ms ? ms : ac);
                        int as = 0;
                        for (int i = 0; i < ac; i += ms) {
                            as++;
                        }
                        final ItemStack[] add = new ItemStack[as];
                        for (final int i = 0; ac > ms; ac -= ms) {
                            add[i] = o;
                        }
                        if (ac > 0) {
                            o = o.clone();
                            o.setAmount(ac);
                            add[add.length - 1] = o;
                        }
                        storage.addItem(add);
                        if (amount == current.getAmount()) {
                            shop.setItem(event.getRawSlot(), null);
                            event.getView().getTopInventory().setItem(event.getRawSlot(), null);
                        } else {
                            updateShopItems(shop, current.getAmount(), amount, event.getRawSlot(),
                                    event.getView().getTopInventory());
                        }
                    }
                    if (price != 0) {
                        shop.transactions.put(event.getView(), shop.transactions.get(event.getView()) + price);
                        econ.depositPlayer(player, price);
                        if (owner != null) {
                            econ.withdrawPlayer(owner, price);
                        }
                    }
                    player.sendMessage(cm.varTrans(config.getString("message.sell.success",
                            Messages.DEFAULT_SELL_SUCCESS), player, shop, amount, price + curname, price));
                    final ItemStack n = cursor.clone();
                    n.setAmount(n.getAmount() - amount);
                    player.setItemOnCursor(n.getAmount() == 0 ? null : n);
                }
                player.updateInventory();
            } else if ((top && player.getItemOnCursor().getType() != Material.AIR &&
                    event.getSlot() != -999) || (!top && event.getCurrentItem().getType() != Material.AIR
                    && event.isShiftClick())) {
                player.sendMessage(cm.varPlayer(config.getString("message." + str + ".invalid", Messages.DEFAULT_SELL_INVALID), player));
                player.updateInventory();
                event.setResult(Result.DENY);
                event.setCancelled(true);
            }
        } else if (shop.price.containsKey(event.getView()) && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            final double p = shop.price.get(event.getView());
            event.setResult(Result.DENY);
            event.setCancelled(true);
            if (!top) {
                player.sendMessage(cm.varPlayer(config.getString("message.price.cancel", Messages.DEFAULT_PRICE_CANCEL), player));
                event.getView().close();
                return;
            }
            shop.setPrice(event.getSlot(), p);
            if (event.getAction() == InventoryAction.PICKUP_ALL) {
                shop.price.remove(event.getView());
                event.getView().close();
            } else {
                final net.minecraft.server.v1_7_R4.ItemStack nms = shop.getRawItem(event.getRawSlot());
                addPrice(nms);
                event.getInventory().setItem(event.getRawSlot(), CraftItemStack.asCraftMirror(nms));
            }
            player.sendMessage(cm.varPlayer(config.getString("message.price.set", Messages.DEFAULT_PRICE_SET), player));
            player.updateInventory();
        } else if (shop.edit.contains(event.getView())) {
            final boolean sclick = top && event.isShiftClick();
            ItemStack i = player.getItemOnCursor().clone();
            if (i.getType() == Material.AIR && sclick) {
                i = event.getCurrentItem();
            }
            if (i.getType() != Material.AIR) {
                if (!top || sclick) {
                    final net.minecraft.server.v1_7_R4.ItemStack nms = nmsStack(i);
                    stripSCSData(nms);
                    final ItemStack ui = CraftItemStack.asCraftMirror(nms);
                    if (sclick) {
                        event.setCurrentItem(ui);
                    } else {
                        player.setItemOnCursor(ui);
                    }
                    final Player runnablePlayer = player;
                    getServer().getScheduler().scheduleSyncDelayedTask(this,
                            new Runnable() {

                        @Override
                        public void run()
                        {
                            runnablePlayer.updateInventory();
                        }

                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void close(final InventoryCloseEvent event) {
        Shop shp = null;
        for (final Shop s : shops) {
            if (s.transactions.containsKey(event.getView())) {
                if (config.getBoolean("shop.notifications", Options.DEFAULT_SHOP_NOTIFICATIONS) && s.getOwner() != null) {
                    final Player p = getServer().getPlayer(s.getOwner());
                    final String title = s.getTitle();
                    final double amount = s.transactions.get(event.getView());
                    String msg;
                    String def;
                    if (s.getMode() == ShopMode.BUY && title == null) {
                        msg = "buy.notice.default";
                        def = Messages.DEFAULT_BUY_NOTICE;
                    } else if (s.getMode() == ShopMode.BUY && title != null) {
                        msg = "buy.notice.titled";
                        def = Messages.DEFAULT_BUY_NOTICE_TITLED;
                    } else if (s.getMode() == ShopMode.SELL && title == null) {
                        msg = "sell.notice.default";
                        def = Messages.DEFAULT_SELL_NOTICE;
                    } else {
                        msg = "sell.notice.titled";
                        def = Messages.DEFAULT_SELL_NOTICE_TITLED;
                    }

                    if (p != null) {
                        p.sendMessage(cm.varNotice(config.getString(msg, def), s, p, amount));
                    }
                }
                shp = s;
                s.transactions.remove(event.getView());
            }
            else if (s.transactions.containsKey(event.getView())) {
                shp = s;
            } else if (s.price.containsKey(event.getView())) {
                shp = s;
            } else if (s.edit.contains(event.getView())) {
                shp = s;
            } else if (s.storage.contains(event.getView())) {
                shp = s;
            }
        }
        if (!(event.getPlayer() instanceof Player))
            return;
        final Player player = (Player) event.getPlayer();
        if (create.containsKey(event.getView())) {
            final ArrayList<ItemStack> c = new ArrayList<ItemStack>();
            for (final ItemStack i : event.getInventory().getContents()) {
                c.add(i);
            }
            if (c.isEmpty()) {
                player.sendMessage(cm.varPlayer(config.getString("message.create.cancel", Messages.DEFAULT_CREATE_CANCEL), player));
                return;
            }
            final Block b = create.get(event.getView());
            final Sign s = (Sign) b.getState();
            boolean e = true;
            for (final String x : s.getLines()) {
                if (!x.trim().isEmpty()) {
                    e = false;
                }
            }
            final Location bloc = b.getLocation();
            final NBTTagCompound shop = new NBTTagCompound();
            shop.setDouble("x", bloc.getX());
            shop.setDouble("y", bloc.getY());
            shop.setDouble("z", bloc.getZ());
            shop.setString("world", bloc.getWorld().getName());
            final NBTTagList items = new NBTTagList();
            for (final ItemStack i : c) {
                final NBTTagCompound copy = new NBTTagCompound();
                if (i != null) {
                    nmsStack(i).save(copy);
                }
                items.add(copy);
            }
            shop.set("items", items);
            final Shop shopvar = new Shop(shop);
            if (config.getBoolean("shop.auto.owner", Options.DEFAULT_SHOP_AUTO_OWNER)) {
                shopvar.setOwner(player.getUniqueId());
            }
            shopvar.setLimited(config.getBoolean("shop.auto.limit", Options.DEFAULT_SHOP_AUTO_LIMIT));
            shops.add(shopvar);
            data.getList("Shops", 10).add(shop);
            player.sendMessage(cm.varPlayer(config.getString("message.create.success", Messages.DEFAULT_CREATE_SUCCESS), player));
            if (e) {
                s.setLine(0, ChatColor.AQUA + "Shop");
                s.setLine(1, "Right click to");
                s.setLine(2, "open!");
            }
            s.update();
            if (config.getBoolean("log.create", Options.DEFAULT_LOG_SHOP_CREATION)) {
                log.info(player.getName() + " created a SignChestShop at " + bloc.getX() + ", " +
                        bloc.getY() + ", " + bloc.getZ() + " at world " + bloc.getWorld().getName());
            }
            create.remove(event.getView());
        } else if (shp == null)
            return;
        else if (shp.price.containsKey(event.getView())) {
            player.sendMessage(cm.varPlayer(config.getString("message.price.cancel", Messages.DEFAULT_PRICE_CANCEL), player));
            shp.price.remove(event.getView());
            shp.update();
            return;
        } else if (shp.edit.contains(event.getView())) {
            final NBTTagCompound shop = shp.data;
            final NBTTagList items = new NBTTagList();
            final Inventory inv = event.getView().getTopInventory();
            for (final ItemStack i : inv.getContents()) {
                if (i != null) {
                    final NBTTagCompound copy = new NBTTagCompound();
                    nmsStack(i).save(copy);
                    items.add(copy);
                } else {
                    items.add(new NBTTagCompound());
                }
            }
            shop.set("items", items);
            player.sendMessage(cm.varPlayer(config.getString("message.edit", Messages.DEFAULT_EDIT), player));
            shp.edit.remove(event.getView());
            shp.update();
        } else if (shp.storage.contains(event.getView())) {
            shp.storage.remove(event.getView());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(final EntityExplodeEvent event) {
        final Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            final Block b = it.next();
            final NBTTagCompound c = getShopData(b);
            if (c != null) {
                it.remove();
            } else if (getAttachedShop(b).b != null) {
                it.remove();
            }
        }
    }

    /**
     * Returns the plugin's API
     *
     * @return The API
     */
    public SignChestShopAPI getAPI() {
        return api;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void interact(final PlayerInteractEvent event) {
        if (event.isCancelled())
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        final Block b = event.getClickedBlock();
        if (b == null)
            return;
        if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN)
            return;
        final Shop shop = getShop(b);
        if (shop == null)
            return;
        final ShopMode mode = shop.getMode();
        if (mode == ShopMode.BUY) {
            if (!event.getPlayer().hasPermission("scs.buy") && config.getBoolean("buy.perms", Options.DEFAULT_BUY_PERMS)) {
                event.getPlayer().sendMessage(cm.color(config.getString("messages.buy.noperm", Messages.DEFAULT_BUY_NOPERM)));
                return;
            }
        } else if (mode == ShopMode.SELL) {
            if (!event.getPlayer().hasPermission("scs.sell") && config.getBoolean("sell.perms", Options.DEFAULT_SELL_PERMS)) {
                event.getPlayer().sendMessage(cm.color(config.getString("messages.sell.noperm", Messages.DEFAULT_SELL_NOPERM)));
                return;
            }
        }
        shop.open(event.getPlayer());
    }

    @Override
    public void onDisable() {
        final File f = getDataFolder();
        if (!f.exists()) {
            f.mkdir();
        }
        for (final Shop s : shops) {
            s.finishEverything();
        }
        for (final Map.Entry<InventoryView, Block> k : create.entrySet()) {
            k.getKey().close();
        }
        create.clear();
        shops.clear();
        if (initsuccess) {
            final File dat = new File(f, "data.dat");
            try {
                final DataOutputStream dos = new DataOutputStream(new FileOutputStream(dat));
                try {
                    save(dos);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                dos.flush();
                dos.close();
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            }
        }
        initsuccess = false;
    }

    @Override
    public void onEnable() {
        inst = this;
        log = getLogger();
        final RegisteredServiceProvider<Economy> ecoprov =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (ecoprov == null) {
            log.warning("No economy plugin detected.  Disabling.");
            setEnabled(false);
            return;
        }
        econ = ecoprov.getProvider();
        final String[] vercheck = getServer().getClass().getPackage().getName().split("[.]", 4);
        if (vercheck.length == 4) {
            if (!vercheck[3].equals("v1_7_R4")) {
                getLogger().warning(
                        "This version of SignChestShop may not be compatible with this version of CraftBukkit.");
            }
        } else {
            getLogger().warning(
                    "This version of SignChestShop may not be compatible with this version of CraftBukkit.");
        }
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        cm = new ConfigManager(this);
        final File cfile = new File(getDataFolder(), "config.txt");
        config = new StringConfig(cfile);
        cm.config = config;
        if (!cfile.exists()) {
            cm.writeConfig();
        }
        cm.loadConfig();
        final File dat = new File(getDataFolder(), "data.dat");
        data = new NBTTagCompound();
        if (dat.exists()) {
            try {
                final DataInputStream dis = new DataInputStream(new FileInputStream(dat));
                load(dis);
                dis.close();
            } catch (final Exception e) {
                log.log(Level.WARNING, "Failed to load data", e);
            }
        } else {
            data.set("Shops", new NBTTagList());
        }
        integCheck();
        buildShops();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("signchestshop").setExecutor(new SignChestShopCommandExecutor(this));
        api = new SignChestShopAPI(this);
        initsuccess = true;
        if (config.getBoolean("updater.check", true)) {
            getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {

                @Override
                public void run() {
                    try {
                        final UpdateInformation info = Updater.findUpdate(getDescription().getVersion());
                        if (info != null) {
                            if (!("v" + getDescription().getVersion()).equals(info.getVersion())) {
                                getLogger().info("A new update is available: " + info.getVersion() + " (" + info.getType() + ")");
                            }
                        }
                    } catch (final IOException e) {
                        getLogger().warning("Failed to find update: " + e);
                    }
                }

            });
        }
    }

    protected Shop getShop(final Block block) {
        for (final Shop shop : shops) {
            final World w = getServer().getWorld(shop.getWorld());
            if (w == null) {
                continue;
            }
            final Location loc = new Location(w, shop.getX(), shop.getY(), shop.getZ()).getBlock().getLocation();
            if (loc.equals(block.getLocation()))
                return shop;
        }
        return null;
    }

    protected Inventory getShop(final NBTTagCompound shop, final boolean buy) {
        return getShop(shop, buy, "Shop");
    }

    protected Inventory getShop(final NBTTagCompound shop, final boolean displayprice, final String title) {
        final NBTTagList items = shop.getList("items", 10);
        final ArrayList<ItemStack> ilist = new ArrayList<ItemStack>();
        for (int i = 0; i < items.size(); i++) {
            final NBTTagCompound c = (NBTTagCompound) items.get(i).clone();
            if (c.c().size() == 0) {
                ilist.add(null);
                continue;
            }
            final net.minecraft.server.v1_7_R4.ItemStack item = net.minecraft.server.v1_7_R4.ItemStack.createStack(c);
            final CraftItemStack cis = CraftItemStack.asCraftMirror(item);
            if (displayprice) {
                addPrice(item);
            }
            ilist.add(cis);
        }
        final Inventory i = getServer().createInventory(null, 27, title);
        for (int a = 0; a < ilist.size(); a++) {
            final ItemStack item = ilist.get(a);
            if (item == null) {
                continue;
            }
            i.setItem(a, ilist.get(a));
        }
        return i;
    }

    protected NBTTagCompound getShopData(final Block b) {
        final Location bloc = b.getLocation();
        final NBTTagList shops = data.getList("Shops", 10);
        for (int i = 0; i < shops.size(); i++) {
            final NBTTagCompound d = shops.get(i);
            final double x = d.getDouble("x");
            final double y = d.getDouble("y");
            final double z = d.getDouble("z");
            final String world = d.getString("world");
            if (bloc.getX() != x || bloc.getY() != y || bloc.getZ() != z) {
                continue;
            }
            if (!bloc.getWorld().getName().equals(world)) {
                continue;
            }
            if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN)
            {
                removeShop(d);
                return null;
            }
            return d;
        }
        return null;
    }

    protected Shop getShopObject(final NBTTagCompound data) {
        for (final Shop s : shops) {
            if (s.data == data)
                return s;
        }
        return null;
    }

    protected net.minecraft.server.v1_7_R4.ItemStack nmsStack(final ItemStack i) {
        return CraftItemStack.asNMSCopy(i);
    }

    protected void removeShop(final NBTTagCompound s) {
        final NBTTagList shops = data.getList("Shops", 10);
        final NBTTagList newshops = new NBTTagList();
        for (int i = 0; i < shops.size(); i++) {
            final NBTTagCompound c = shops.get(i);
            if (c != s) {
                newshops.add(c);
            } else {
                final Iterator<Shop> it = this.shops.iterator();
                while (it.hasNext()) {
                    final Shop sh = it.next();
                    if (sh.data == c) {
                        it.remove();
                    }
                }
            }
        }
        data.set("Shops", newshops);
    }

    void addPrice(final net.minecraft.server.v1_7_R4.ItemStack item) {
        NBTTagCompound tag = item.tag;
        if (tag == null) {
            item.setTag((tag = new NBTTagCompound()));
        }
        if (!tag.hasKey("display")) {
            tag.set("display", new NBTTagCompound());
        }
        final NBTTagCompound display = tag.getCompound("display");
        if (!display.hasKey("Lore")) {
            display.set("Lore", new NBTTagList());
        }
        final NBTTagList lore = display.getList("Lore", 8);
        lore.add(new NBTTagString(price(tag, item.count, null)));
    }

    String price(final NBTTagCompound tag, final int amount, final Double gprice) {
        final String price = config.getString("shop.price.text", Options.DEFAULT_PRICE_TEXT);
        String pprice;
        double rprice = -1;
        if (tag.hasKey("scs_price")) {
            rprice = gprice == null ? tag.getDouble("scs_price") : gprice.doubleValue();
            if (rprice < 0) {
                pprice = config.getString("shop.price.display", Options.DEFAULT_PRICE_DISPLAY);
            }
            if (rprice == 0) {
                pprice = config.getString("shop.price.free", Options.DEFAULT_PRICE_FREE);
            } else {
                if (amount == 1) {
                    pprice = config.getString("shop.price.cost", Options.DEFAULT_PRICE_COST).replaceAll("<rawprice>", cm.placePadding(rprice));
                } else {
                    pprice = config.getString("shop.price.costmulti", Options.DEFAULT_PRICE_COSTMULTI)
                            .replaceAll("<rawprice>", cm.placePadding(rprice)).replaceAll("<totalprice>", cm.placePadding(rprice * amount));
                }
            }
        } else {
            pprice = config.getString("shop.price.display", Options.DEFAULT_PRICE_DISPLAY);
        }
        return cm.varCur(price.replaceAll("<price>", pprice), rprice);
    }

    void stripSCSData(final net.minecraft.server.v1_7_R4.ItemStack nms) {
        stripSCSData(nms, true);
    }

    void stripSCSData(final net.minecraft.server.v1_7_R4.ItemStack nms, final boolean lastlore) {
        if (nms.tag == null)
            return;
        nms.tag.remove("scs_price");
        if (lastlore) {
            removeLastLore(nms);
        }
        if (nms.tag.c().size() == 0) {
            nms.setTag(null);
        }
    }

    private void buildShops() {
        final NBTTagList shops = data.getList("Shops", 10);
        for (int i = 0; i < shops.size(); i++)
        {
            final NBTTagCompound a = shops.get(i);
            final Shop s = new Shop(a);
            s.loadData();
            this.shops.add(s);
        }
    }

    private DKey<Block, NBTTagCompound> getAttachedShop(final Block b) {
        Block sb = b;
        NBTTagCompound c = getShopData(b);
        if (c == null) {
            final Block[] d = { b, b.getRelative(BlockFace.EAST), b.getRelative(BlockFace.WEST),
                    b.getRelative(BlockFace.NORTH), b.getRelative(BlockFace.SOUTH),
                    b.getRelative(BlockFace.UP) };
            for (final Block s : d) {
                final MaterialData md = s.getState().getData();
                if (!(md instanceof org.bukkit.material.Sign)) {
                    continue;
                }
                final org.bukkit.material.Sign e = (org.bukkit.material.Sign) md;
                if (e.getAttachedFace() == s.getFace(b)) {
                    c = getShopData(s);
                    sb = s;
                }
                if (c != null) {
                    break;
                }
            }
        }
        return new DKey<Block, NBTTagCompound>(sb, c);
    }

    private void integCheck() {
        final HashMap<String, ArrayList<NBTTagCompound>> requireConversion =
                new HashMap<String, ArrayList<NBTTagCompound>>();
        final NBTTagList shops = data.getList("Shops", 10);
        for (int i = 0; i < shops.size(); i++) {
            final NBTTagCompound a = shops.get(i);
            if (!a.hasKey("limited")) {
                a.setBoolean("limited", false);
            }
            if (!a.hasKey("mode")) {
                a.setInt("mode", ShopMode.BUY.ID);
            }
            if (!a.hasKey("storage")) {
                a.set("storage", new NBTTagList());
            }
            if (a.hasKey("owner") && (!a.hasKey("ownerUUIDMost") || !a.hasKey("ownerUUIDLeast"))) {
                final String o = a.getString("owner");
                ArrayList<NBTTagCompound> shopList = requireConversion.get("owner");
                if (shopList == null) {
                    requireConversion.put(o, (shopList = new ArrayList<NBTTagCompound>()));
                }
                shopList.add(a);
            }
        }

        if (!data.hasKey("usingUUID")) {
            getLogger().info("Old file detected; converting names to UUIDs");
            final ExecutorService es = Executors.newFixedThreadPool(1);
            final Future<Integer> f = es.submit(new Callable<Integer>() {

                @Override
                public Integer call()
                {
                    final ArrayList<String> names = new ArrayList<String>();
                    names.addAll(requireConversion.keySet());
                    final UUIDFetcher f = new UUIDFetcher(names);
                    try {
                        getLogger().info("Fetching names from the Internet...");
                        final Map<String, UUID> results = f.call();
                        getLogger().info("Fetch complete. Converting names...");
                        int total = 0;
                        for (final String s : names) {
                            final UUID id = results.get(s);
                            final long mid = id.getMostSignificantBits();
                            final long lid = id.getLeastSignificantBits();
                            int c = 0;
                            for (final NBTTagCompound t : requireConversion.get(s)) {
                                t.remove("owner");
                                t.setLong("ownerUUIDMost", mid);
                                t.setLong("ownerUUIDLeast", lid);
                                ++c;
                            }
                            getLogger().info("Converted " + c + " shop(s) belonging to " + s);
                            total += c;
                        }

                        return Integer.valueOf(total);
                    } catch (final Exception e) {
                        getLogger().log(Level.WARNING, "Failed to convert names to UUID", e);
                        return null;
                    }
                }

            });

            Integer i = null;
            try {
                i = f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            es.shutdownNow();
            data.setBoolean("usingUUID", true);
            getLogger().info("Converted " + (i != null ? i.intValue() : 0) + " shops");
        }
    }

    @SuppressWarnings("unused")
    private boolean isSimilar(final net.minecraft.server.v1_7_R4.ItemStack stack1, final net.minecraft.server.v1_7_R4.ItemStack stack2) {
        final net.minecraft.server.v1_7_R4.ItemStack s1c = stack1.cloneItemStack();
        final net.minecraft.server.v1_7_R4.ItemStack s2c = stack2.cloneItemStack();
        stripSCSData(s1c);
        stripSCSData(s2c);
        return CraftItemStack.asCraftMirror(s1c).isSimilar(CraftItemStack.asCraftMirror(s2c));
    }

    private boolean isSimilarUnstripped(final net.minecraft.server.v1_7_R4.ItemStack display, final net.minecraft.server.v1_7_R4.ItemStack unstr) {
        final net.minecraft.server.v1_7_R4.ItemStack s1c = display.cloneItemStack();
        final net.minecraft.server.v1_7_R4.ItemStack s2c = unstr.cloneItemStack();
        stripSCSData(s1c);
        return CraftItemStack.asCraftMirror(s1c).isSimilar(CraftItemStack.asCraftMirror(s2c));
    }

    private void load(final DataInputStream s) throws Exception {
        data = NBTCompressedStreamTools.a(s);
    }

    private NBTTagList removeLastLore(final NBTTagList lore) {
        final NBTTagList newlore = new NBTTagList();
        for (int x = 0; x < lore.size() - 1; x++) {
            newlore.add(new NBTTagString(lore.getString(x)));
        }
        return newlore;
    }

    private void removeLastLore(final net.minecraft.server.v1_7_R4.ItemStack item) {
        final NBTTagCompound display = item.tag.getCompound("display");
        final NBTTagList lore = display.getList("Lore", 8);
        if (lore.size() == 1) {
            display.remove("Lore");
        } else {
            display.set("Lore", removeLastLore(lore));
        }
        if (display.c().size() == 0) {
            item.tag.remove("display");
        }
    }

    private void save(final DataOutputStream s) throws Exception {
        final Method m = NBTTagCompound.class.getDeclaredMethod("a", String.class, NBTBase.class, DataOutput.class);
        m.setAccessible(true);
        m.invoke(null, "", data, s);
    }

    private void updateShopItems(final Shop shop, final int ca, final int amount, final int slot, final Inventory inv) {
        updateShopItems(shop, ca - amount, slot, inv);
    }

    private void updateShopItems(final Shop shop, final int namount, final int slot, final Inventory inv) {
        net.minecraft.server.v1_7_R4.ItemStack sn = shop.getRawItem(slot);
        sn.count = namount;
        shop.setItem(slot, sn, true);
        sn = sn.cloneItemStack();
        addPrice(sn);
        inv.setItem(slot, CraftItemStack.asCraftMirror(sn));
    }
}