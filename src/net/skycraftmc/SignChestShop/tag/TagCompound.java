package net.skycraftmc.SignChestShop.tag;

import net.skycraftmc.SignChestShop.util.SafeMethod;
import net.skycraftmc.SignChestShop.util.SafeReflectFailException;

public abstract class TagCompound extends TagBase
{
	protected SafeMethod mgetdouble;
	protected SafeMethod mgetstring;
	protected SafeMethod msetdouble;
	protected SafeMethod msetstring;
	protected SafeMethod msetint;
	protected SafeMethod mgetint;
	protected SafeMethod mremove;
	public TagCompound(Object tag, String load, String save, String out, String in) 
	{
		super(tag, load, save, out, in);
		initCompound();
	}
	protected abstract void initCompound();
	public double getDouble(String key)
	{
		Double d = null;
		try {
			d = (Double)mgetdouble.invoke(tag, key);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
		return d;
	}
	public int getInt(String key)
	{
		Integer d = null;
		try {
			d = (Integer)mgetdouble.invoke(tag, key);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
		return d;
	}
	public String getString(String key)
	{
		String d = null;
		try {
			d = (String)mgetdouble.invoke(tag, key);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
		return d;
	}
	public void setDouble(String key, double v)
	{
		try {
			msetdouble.invoke(tag, key, v);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
	}
	public void setInt(String key, int v)
	{
		try {
			msetdouble.invoke(tag, key, v);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
	}
	public void setString(String key, String v)
	{
		try {
			msetdouble.invoke(tag, key, v);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
	}
	public void remove(String key)
	{
		try {
			mremove.invoke(tag, key);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
	}
}
