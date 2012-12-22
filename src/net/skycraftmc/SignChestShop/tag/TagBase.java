package net.skycraftmc.SignChestShop.tag;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.skycraftmc.SignChestShop.util.SafeMethod;
import net.skycraftmc.SignChestShop.util.SafeReflectFailException;

public abstract class TagBase
{
	protected Object tag;
	protected SafeMethod mload;
	protected SafeMethod msave;
	protected SafeMethod mout;
	protected SafeMethod min;
	public TagBase(Object tag, String load, String save, String out, String in)
	{
		this.tag = tag;
		init(load, save, out, in);
	}
	private void init(String load, String save, String out, String in)
	{
		mload = SafeMethod.get(tag.getClass(), load);
		msave = SafeMethod.get(tag.getClass(), load);
		mout = SafeMethod.get(tag.getClass(), load);
		min = SafeMethod.get(tag.getClass(), load);
	}
	public void loadTag(DataInput in)throws IOException
	{
		try {
			mload.invoke(this, in);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
	}
	public void saveTag(DataOutput out)throws IOException
	{
		try {
			mload.invoke(this, out);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
	}
	public void load(DataInput in)throws IOException
	{
		try {
			mout.invoke(this, in);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
	}
	public void write(DataOutput out)throws IOException
	{
		try {
			min.invoke(this, out);
		} catch (SafeReflectFailException e) {
			e.printStackTrace();
		}
	}
	public Object getHandle()
	{
		return tag;
	}
}
