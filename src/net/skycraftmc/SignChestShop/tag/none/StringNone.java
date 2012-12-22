package net.skycraftmc.SignChestShop.tag.none;

import net.skycraftmc.SignChestShop.tag.TagString;
import net.skycraftmc.SignChestShop.util.SafeField;

public class StringNone extends TagString 
{
	protected SafeField fset;
	public StringNone(Object o, String load, String save, String out, String in) 
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
