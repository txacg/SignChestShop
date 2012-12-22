package net.skycraftmc.SignChestShop.tag.none;

import net.skycraftmc.SignChestShop.tag.DataTag;
import net.skycraftmc.SignChestShop.util.SafeField;

public class BooleanNone extends DataTag<Boolean>
{
	protected SafeField fset;
	public BooleanNone(Object o, String load, String save, String out, String in) {
		super(o, load, save, out, in);
	}
	public void set(Boolean t) 
	{
		fset.set(tag, t.booleanValue());
	}
	public void initData() 
	{
		fset = SafeField.get(tag.getClass(), "data");
	}
	public Boolean get() {
		return null;
	}
}
