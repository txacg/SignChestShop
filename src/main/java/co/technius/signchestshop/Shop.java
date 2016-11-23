package co.technius.signchestshop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.minecraft.server.v1_11_R1.NBTBase;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.NBTTagList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import co.technius.signchestshop.util.UUIDUtil;

/**
 * Represents a SignChestShop.
 */
public class Shop {

    /**
     * Represents the modes of a shop.
     */
    public enum ShopMode {
        /**
         * Represents a shop that players buy from.
         */
        BUY,

        /**
         * Represents a shop that players sell to.
         */
        SELL;

        /**
         * Finds and returns a {@link ShopMode} with the specified ID, or null if not found.
         *
         * @param id - The ID of the shop mode
         * @return The ShopMode with the specified ID, or null if not found.
         */
        public static ShopMode getByID(final int id) {
            if (id < 0 || id >= values().length)
                return null;
            return values()[id];
        }

        /**
         * Finds and returns a {@link ShopMode} with the specified name, or null if not found.
         *
         * @param name - The ID of the shop mode
         * @return The ShopMode with the specified name, or null if not found.
         */
        public static ShopMode getByName(final String name) {
            for (final ShopMode m : values()) {
                if (m.name().equalsIgnoreCase(name))
                    return m;
            }
            return null;
        }

        /**
         * The ID of this shop mode.
         *
         * @see #getID()
         */
        public final int ID = ordinal();

        /**
         * @return The ID of this shop mode. Equal to this {@link ShopMode}'s {@link #ID} variable.
         * @see #ID
         */
        public int getID() {
            return ID;
        }
    }

    NBTTagCompound data;

    HashMap<InventoryView, Double> transactions = new HashMap<InventoryView, Double>();
    HashMap<InventoryView, Double> price = new HashMap<InventoryView, Double>();
    HashSet<InventoryView> edit = new HashSet<InventoryView>();
    HashSet<InventoryView> storage = new HashSet<InventoryView>();

    Inventory storageinv;
    UUID owner;

    private final SignChestShopPlugin scs;

    Shop(final NBTTagCompound data) {
        if (!data.hasKey("x") || !data.hasKey("y") || !data.hasKey("z")
                || !data.hasKey("world") || !data.hasKey("items"))
            throw new IllegalArgumentException("Invalid shop data");
        this.data = data;
        storageinv = new CraftInventoryCustom(null, 27, "Storage");
        scs = SignChestShopPlugin.inst;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Shop other = (Shop) obj;
        if (other.getX() != getX() || other.getY() != getY() || other.getZ() != getZ()
                || !other.getWorld().equals(getWorld()))
            return false;
        return true;
    }

    /**
     * Returns the block of this shop. This is the sign itself, not the block it is attached to.
     *
     * @return The block of this shop.
     */
    public Block getBlock() {
        return getLocation().getBlock();
    }

    /**
     * @return An array of all {@link org.bukkit.inventory.InventoryView}s currently open.
     */
    public InventoryView[] getBrowsing() {
        return transactions.keySet().toArray(new InventoryView[transactions.size()]);
    }

    /**
     * Returns the contents of this shop. The slots in the shop that
     * contain no items will be null.
     *
     * @return The contents of this shop.
     */
    public ItemStack[] getContents() {
        final NBTTagList ilist = data.getList("items", 10);
        final ItemStack[] i = new ItemStack[ilist.size()];
        for (int a = 0; a < ilist.size(); a++) {
            final NBTTagCompound c = ilist.get(a);
            if (c.c().size() == 0) {
                i[a] = null;
            } else {
                i[a] = CraftItemStack.asCraftMirror((new net.minecraft.server.v1_11_R1.ItemStack(c)));
            }
        }
        return i;
    }

    /**
     * Returns the item at the specified slot. Has the same
     * effect as calling {@link #getItem(int, boolean)} with
     * withPriceText set to false.
     *
     * @param index - The index of the item
     * @return The item, or null if the specified index does not contain an item.
     */
    public ItemStack getItem(final int index) {
        return getItem(index, false);
    }

    /**
     * Returns the item at the specified slot.
     *
     * @param index         - The index of the item
     * @param withPriceText - If set to true, then the item will have the price lore attached
     * @return The item, or null if the specified index does not contain an item.
     */
    public ItemStack getItem(final int index, final boolean withPriceText) {
        final NBTTagList ilist = data.getList("items", 10);
        if (index >= ilist.size() || index < 0)
            return null;
        final NBTTagCompound tag = ilist.get(index);
        final net.minecraft.server.v1_11_R1.ItemStack item = new net.minecraft.server.v1_11_R1.ItemStack(tag);
        if (withPriceText) {
            scs.addPrice(item);
        } else if (item.getTag() != null) {
            scs.stripSCSData(item, false);
        }
        return CraftItemStack.asCraftMirror(item);
    }

    /**
     * Returns the location of this shop.
     *
     * @return This shop's location
     */
    public Location getLocation() {
        return new Location(Bukkit.getWorld(getWorld()), getX(), getY(), getZ());
    }

    /**
     * @return The {@link ShopMode} of the shop
     */
    public ShopMode getMode() {
        return ShopMode.getByID(data.getInt("mode"));
    }

    /**
     * @return The UUID of the owner of this shop, or null if this shop has no owner.
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * @return The name of the owner of this shop, or null if this shop has no owner.
     * This method is a blocking method.
     */
    public String getOwnerName() {
        if (owner == null)
            return null;
        return Bukkit.getServer().getOfflinePlayer(owner).getName();
    }

    /**
     * @param index - The index of the item
     * @return The price of the item, -1 if it is display only, or -2 if no item exists at the index.
     */
    public double getPrice(final int index) {
        final NBTTagList ilist = data.getList("items", 10);
        if (index >= ilist.size() || index < 0)
            throw new ArrayIndexOutOfBoundsException(index);
        final NBTTagCompound c = ilist.get(index);
        if (c.c().size() == 0)
            return -2;
        if (!c.hasKey("tag"))
            return -1;
        final NBTTagCompound tag = c.getCompound("tag");
        if (!tag.hasKey("scs_price"))
            return -1;
        return tag.getDouble("scs_price");
    }

    /**
     * @return An {@link Inventory} representing the contents of the shop's storage.
     */
    public Inventory getStorage() {
        return storageinv;
    }

    /**
     * @return The title of this shop, or null if this shop has no title.
     */
    public String getTitle() {
        if (!data.hasKey("title"))
            return null;
        return data.getString("title");
    }

    /**
     * Returns the name of this shop's world
     *
     * @return This shop's world
     */
    public String getWorld() {
        return data.getString("world");
    }

    /**
     * Returns the x-coordinate of this shop
     *
     * @return This shop's x-coordinate
     */
    public double getX() {
        return data.getDouble("x");
    }

    /**
     * Returns the y-coordinate of this shop
     *
     * @return This shop's y-coordinate
     */
    public double getY() {
        return data.getDouble("y");
    }

    /**
     * Returns the z-coordinate of this shop
     *
     * @return This shop's z-coordinate
     */
    public double getZ() {
        return data.getDouble("z");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final String world = getWorld();
        result = prime * result + new Double(getX()).hashCode();
        result = prime * result + new Double(getY()).hashCode();
        result = prime * result + new Double(getZ()).hashCode();
        result = prime * result + world == null ? 0 : world.hashCode();
        return result;
    }

    /**
     * @return True if the shop can run out of stock, otherwise false.
     */
    public boolean isLimited() {
        return data.getBoolean("limited");
    }

    /**
     * Opens the shop for the specified player.
     *
     * @param player - The player
     * @return The {@link org.bukkit.inventory.InventoryView} associated with this transaction.
     */
    public InventoryView open(final Player player) {
        final String title = scs.cm.doShopTitle(this);
        final Inventory i = scs.getShop(data, true, title.length() > 32 ? title.substring(0, 32) : title);
        final InventoryView iv = player.openInventory(i);
        transactions.put(iv, 0.0);
        return iv;
    }

    /**
     * Sets the item at the specified index.
     * This has the same effect as {@link #setItem(int, ItemStack, boolean)} with retainPrice as false.
     *
     * @param index - The index of the item.
     * @param item  - The {@link ItemStack}.
     */
    public void setItem(final int index, final ItemStack item) {
        setItem(index, item, false);
    }

    /**
     * Sets the item at the specified index.
     *
     * @param index       - The index of the item.
     * @param item        - The {@link ItemStack}.
     * @param retainPrice - If this is true, then the price will be kept.
     */
    public void setItem(final int index, final ItemStack item, final boolean retainPrice) {
        setItem(index, CraftItemStack.asNMSCopy(item), retainPrice);
    }

    /**
     * Sets the shop to be limited or not.
     *
     * @param value - If the shop can run out of stock
     */
    public void setLimited(final boolean value) {
        data.setBoolean("limited", value);
    }

    /**
     * Sets the {@link ShopMode} of this shop.
     *
     * @param mode The mode of the shop
     */
    public void setMode(final ShopMode mode) {
        if (mode == null)
            throw new IllegalArgumentException("The shop mode is null.");
        data.setInt("mode", mode.ID);
    }

    /**
     * Sets the owner of this shop.
     *
     * @param owner - The new owner of this shop, or null to remove the owner.
     * @deprecated This operation will call a blocking UUID conversion.
     * Use {@link #setOwner(UUID)} and {@link UUIDUtil#getUUID(String)} instead.
     */
    @Deprecated
    public void setOwner(final String owner) {
        UUID id = null;
        if (owner != null) {
            final Future<UUID> f = UUIDUtil.getUUID(owner);
            try {
                id = f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        setOwner(id);
    }

    /**
     * Sets the owner of this shop.
     *
     * @param owner - The new owner of this shop, or null to remove the owner.
     */
    public void setOwner(final UUID owner) {
        this.owner = owner;
        if (owner == null) {
            data.remove("ownerUUIDMost");
            data.remove("ownerUUIDLeast");
        } else {
            data.setLong("ownerUUIDMost", owner.getMostSignificantBits());
            data.setLong("ownerUUIDLeast", owner.getLeastSignificantBits());
        }
        update();
    }

    /**
     * Sets the price of the item at the specified index.
     * Use -1 for display only and 0 for free.
     *
     * @param index - The index of the item
     * @param price - The price of the item
     */
    public void setPrice(final int index, final double price) {
        final NBTTagList ilist = data.getList("items", 10);
        if (index >= ilist.size() || index < 0)
            throw new ArrayIndexOutOfBoundsException(index);
        final NBTTagCompound c = ilist.get(index);
        if (c.c().size() == 0)
            throw new IllegalArgumentException("index " + index + " contains no item");
        if (!c.hasKey("tag")) {
            c.set("tag", new NBTTagCompound());
        }
        final NBTTagCompound tag = c.getCompound("tag");
        if (price >= 0) {
            tag.setDouble("scs_price", price);
        } else {
            tag.remove("scs_price");
        }
    }

    /**
     * Sets the title of this shop.
     *
     * @param title - The new title of this shop, or null to remove the title.
     */
    public void setTitle(final String title) {
        if (title == null) {
            data.remove("title");
        } else {
            data.setString("title", title);
        }
        update();
    }

    /**
     * Reopens all currently open transactions.
     */
    public void update() {
        final ArrayList<Player> p = new ArrayList<Player>();
        for (final InventoryView iv : transactions.keySet()) {
            p.add((Player) iv.getPlayer());
            iv.close();
        }
        for (final Player pl : p) {
            open(pl);
        }
    }

    protected void finishData() {
        final NBTTagList list = new NBTTagList();
        for (final ItemStack i : storageinv.getContents()) {
            list.add(i == null ? new NBTTagCompound() : CraftItemStack.asNMSCopy(i).save(new NBTTagCompound()));
        }
        data.set("storage", list);
    }

    protected void finishEverything() {
        finishData();
        final ArrayList<Set<InventoryView>> lists =
                new ArrayList<Set<InventoryView>>();
        lists.addAll(Arrays.asList(transactions.keySet(), edit, storage, price.keySet()));
        Iterator<InventoryView> it;
        for (final Set<InventoryView> l : lists) {
            for (it = l.iterator(); it.hasNext(); ) {
                it.next().close();
                it.remove();
            }
        }
    }

    protected NBTTagCompound getData() {
        return data;
    }

    protected void loadData() {
        final NBTTagList l = data.getList("storage", 10);
        for (int i = 0; i < l.size(); i++) {
            storageinv.setItem(i, CraftItemStack.asCraftMirror(
                    new  net.minecraft.server.v1_11_R1.ItemStack(l.get(i))));
        }
        if (data.hasKey("ownerUUIDMost") && data.hasKey("ownerUUIDLeast")) {
            owner = new UUID(data.getLong("ownerUUIDMost"), data.getLong("ownerUUIDLeast"));
        }
    }

    protected void setItem(final int index, final net.minecraft.server.v1_11_R1.ItemStack item, final boolean retainPrice) {
        final NBTTagList ilist = data.getList("items", 10);
        if (index >= ilist.size() || index < 0)
            throw new ArrayIndexOutOfBoundsException(index);
        final NBTTagCompound old = ilist.get(index);
        if (item == null || item.isEmpty()) {
            old.c().clear();
            return;
        }
        setItem(old, item, retainPrice);
    }

    protected void setItem(final NBTTagCompound old, final net.minecraft.server.v1_11_R1.ItemStack nms, final boolean retainPrice) {
        final NBTTagCompound c = new NBTTagCompound();
        nms.save(c);
        for (final Object o : c.c()) {
            final String name = (String) o;
            final NBTBase b = c.get(name);
            if (name.equals("scs_price")) {
                if (!retainPrice) {
                    old.remove("scs_price");
                }
            } else if (old.hasKey(name)) {
                old.set(name, b);
            } else {
                old.remove(name);
            }
        }
    }

    net.minecraft.server.v1_11_R1.ItemStack getRawItem(final int index) {
        final NBTTagList ilist = data.getList("items", 10);
        if (index >= ilist.size() || index < 0)
            return null;
        return new net.minecraft.server.v1_11_R1.ItemStack(ilist.get(index));
    }
}
