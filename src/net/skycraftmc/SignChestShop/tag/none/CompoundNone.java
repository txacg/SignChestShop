package net.skycraftmc.SignChestShop.tag.none;

import net.skycraftmc.SignChestShop.tag.TagCompound;
import net.skycraftmc.SignChestShop.util.SafeMethod;

public class CompoundNone extends TagCompound
{
	public CompoundNone(Object tag)
	{
		super(tag, "b", "a", "load", "write");
	}
	protected void initCompound() 
	{
		mgetdouble = SafeMethod.get(tag.getClass(), "getDouble", String.class);
		mgetstring = SafeMethod.get(tag.getClass(), "getString", String.class);
		mgetint = SafeMethod.get(tag.getClass(), "getInt", String.class);
		msetdouble = SafeMethod.get(tag.getClass(), "setDouble", String.class, Double.class);
		msetstring = SafeMethod.get(tag.getClass(), "setString", String.class, String.class);
		msetint = SafeMethod.get(tag.getClass(), "setInt", String.class, Integer.class);
		mremove = SafeMethod.get(tag.getClass(), "remove", String.class);
	}
}
