package net.skycraftmc.SignChestShop.tag.v1_4_6;

import net.skycraftmc.SignChestShop.tag.TagString;
import net.skycraftmc.SignChestShop.util.SafeField;

public class String1_4_6 extends TagString
{
	protected SafeField fset;
	public String1_4_6(Object o) 
	{
		super(o, "b", "a", "load", "write");
	}
	public void set(String t) 
	{
		fset.set(tag, t);
	}
	public String get() 
	{
		return tag.toString();
	}
	public void initData() 
	{
		fset = SafeField.get(tag.getClass(), "data");
	}
}
