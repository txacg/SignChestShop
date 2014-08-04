package net.obnoxint.mcdev.signchestshop;

public final class R {

    /* ***************** *
     * Message templates *
     * ***************** */
    public static final String MSG_BUY_SUCCESS = "&e<itemcorrectu> bought for <price>!";
    public static final String MSG_BUY_SUCCESS_OWNED = "&e<itemcorrectu> bought from <owner> for <price>!";
    public static final String MSG_BUY_SUCCESS_TITLED = "&e<itemcorrectu> bought at <title> for <price>!";
    public static final String MSG_BUY_SUCCESS_TITLED_OWNED = "&e<itemcorrectu> bought at <title> from <owner> for <price>!";

    public static final String MSG_BUY_FAIL = "&eYou need at least <price> to buy this item!";
    public static final String MSG_BUY_FREE = "&e<itemcorrectu> bought for free!";
    public static final String MSG_BUY_INVALID = "&cYou can't do that!";
    public static final String MSG_BUY_NOPERM = "&cYou are not allowed to buy from shops.";
    public static final String MSG_BUY_NOPERMID = "&cYou are not allowed to buy this item!";

    public static final String MSG_BUY_NOTICE = "&e<player> bought <price> worth of items from one of your shops!";
    public static final String MSG_BUY_NOTICE_TITLED = "&e<player> bought <price> worth of items from your <title> shop!";

    public static final String MSG_SELL_SUCCESS = "&e<itemcorrectu> sold for <price>!";
    public static final String MSG_SELL_SUCCESS_OWNED = "&e<itemcorrectu> sold to <owner> <price>!";
    public static final String MSG_SELL_SUCCESS_TITLED = "&e<itemcorrectu> sold at <title> for <price>!";
    public static final String MSG_SELL_SUCCESS_TITLED_OWNED = "&e<itemcorrectu> sold at <title> to <owner> for <price>!";

    public static final String MSG_SELL_NOPERM = "&cYou are not allowed to sell to shops.";
    public static final String MSG_SELL_NOPERMID = "&cYou are not allowed to sell this item!";
    public static final String MSG_SELL_INVALID = "&cYou can't do that!";
    public static final String MSG_SELL_FAIL = "&cThe owner of this shop doesn't have enough money to buy this item!";
    public static final String MSG_SELL_NOSPACE = "&cThis shop doesn't have space for your item!";
    public static final String MSG_SELL_NOTICE = "&e<player> sold <price> worth of items to one of your shops!";
    public static final String MSG_SELL_NOTICE_TITLED = "&e<player> sold <price> worth of items to your <title> shop!";

    public static final String MSG_EDIT = "&eSignChestShop edited!";

    public static final String MSG_PRICE_CANCEL = "&eItem pricing cancelled.";
    public static final String MSG_PRICE_SET = "&ePrice set!";

    public static final String MSG_CREATE_CANCEL = "&eShop creation cancelled.";
    public static final String MSG_CREATE_SUCCESS = "&eShop created!";

    public static final String MSG_BREAK_NOPERM = "&cYou are not allowed to break shops!";
    public static final String MSG_BREAK_PERM = "&ePlease use &a/scs break &eto break this shop.";

    public static final String MSG_SETTITLE_SUCCESS = "&eShop title set to \"<title>\"";
    public static final String MSG_SETTITLE_REMOVE = "&eShop title removed!";
    public static final String MSG_SETTITLE_FAIL = "&cThe shop title can only have a maximum of 32 characters!";

    public static final String MSG_CMD_NOTARGET = "&cYou must target a SignChestShop!";
    public static final String MSG_CMD_NOPERM = "&cYou are not allowed to use this command!";
    public static final String MSG_CMD_NOTOWNED = "&cYou do not own this shop!";

    /* ****************** *
     * Configuration keys *
     * ****************** */
    public static final String CFG_BUY_MODE = "amount";

    public static final boolean CFG_BUY_SHIFTCLICK = true;
    public static final boolean CFG_BUY_PERMS = false;
    public static final boolean CFG_BUY_PERMSID = false;

    public static final String CFG_BUY_MODENAME = "Buy";
    public static final String CFG_BUY_MODEEXP = "from";

    public static final boolean CFG_SELL_PERMS = false;
    public static final boolean CFG_SELL_PERMSID = false;

    public static final String CFG_SELL_MODENAME = "Sell";
    public static final String CFG_SELL_MODEEXP = "to";

    public static final String CFG_PRICE_TEXT = "&bPrice: &6<price>";
    public static final String CFG_PRICE_FREE = "Free";
    public static final String CFG_PRICE_DISPLAY = "Display Only";
    public static final String CFG_PRICE_COST = "<rawprice> <curname>";
    public static final String CFG_PRICE_COSTMULTI = "<totalprice> <curname> total (<rawprice> <curname> each)";

    public static final boolean CFG_SHOP_NOTIFICATIONS = true;
    public static final boolean CFG_SHOP_AUTO_LIMIT = true;
    public static final boolean CFG_SHOP_AUTO_OWNER = true;
    public static final boolean CFG_SHOP_FORCEEMPTY = true;
    public static final int CFG_SHOP_MINDECPLACES = 2;

    public static final String CFG_SHOP_TITLE_DEFAULT = "<mode>";
    public static final String CFG_SHOP_TITLE_OWNED = "<mode> <modeexp> <owner>";
    public static final String CFG_SHOP_TITLE_TITLED = "<mode> <modeexp> <title>";
    public static final String CFG_SHOP_TITLE_OWNED_TITLED = "<mode>: <title>";

    public static final boolean CFG_LOG_SHOP_CREATION = false;

    private R() {}

}
