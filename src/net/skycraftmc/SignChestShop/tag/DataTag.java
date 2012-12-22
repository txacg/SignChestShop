package net.skycraftmc.SignChestShop.tag;

import net.skycraftmc.SignChestShop.util.SafeMethod;

public abstract class DataTag<T> extends TagBase
{
	protected SafeMethod mset;
	protected SafeMethod mget;
	public DataTag(Object o, String load, String save, String out, String in)
	{
		super(o, load, save, out, in);
		initData();
	}
	public abstract void set(T t);
	public abstract T get();
	public abstract void initData();
}
