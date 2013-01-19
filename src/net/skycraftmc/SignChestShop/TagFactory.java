package net.skycraftmc.SignChestShop;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.skycraftmc.SignChestShop.tag.TagCompound;
import net.skycraftmc.SignChestShop.tag.TagString;
import net.skycraftmc.SignChestShop.tag.none.CompoundNone;
import net.skycraftmc.SignChestShop.tag.none.StringNone;
import net.skycraftmc.SignChestShop.tag.v1_4_6.Compound1_4_6;
import net.skycraftmc.SignChestShop.tag.v1_4_6.String1_4_6;
import net.skycraftmc.SignChestShop.util.SafeField;
import net.skycraftmc.SignChestShop.util.SafeReflectFailException;

import org.bukkit.inventory.ItemStack;

public class TagFactory
{
	private String ver;
	public TagFactory(SignChestShopPlugin plugin)
	{
		String[] t = plugin.getServer().getClass().getPackage().getName().split("[.]", 5);
		ver = t[3];
	}
	private Object createNMSTag(String type, String name)
	{
		Class<?>clazz = null;
		try
		{
			clazz = Class.forName("net.minecraft.server.NBTTag" + type);
		}
		catch(ClassNotFoundException e)
		{
			try
			{
				clazz = Class.forName("net.minecraft.server." + ver + ".NBTTag" + type);
			}
			catch(ClassNotFoundException e2)
			{
				
			}
		}
		if(clazz == null)return null;
		Object o = null;
		try {
			if(name == null)o = clazz.newInstance();
			else
			{
				try {
					Constructor<?> con = clazz.getDeclaredConstructor(String.class);
					con.setAccessible(true);
					o = con.newInstance(name);
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return o;
	}
	public TagCompound createCompound(String name)
	{
		if(ver.equals("v1_4_6"))return new Compound1_4_6(createNMSTag("Compound", name));
		else if(ver.equals("v1_4_5"))return new CompoundNone(createNMSTag("Compound", name));
		else return new CompoundNone(createNMSTag("Compound", name));
	}
	public TagString createString(String name)
	{
		if(ver.equals("v1_4_6"))return new String1_4_6(createNMSTag("String", name));
		else if(ver.equals("v1_4_5"))return new StringNone(createNMSTag("String", name));
		else return new StringNone(createNMSTag("String", name));
	}
	public TagCompound getCompound(ItemStack i)
	{
		try
		{
			if(ver.equals("v1_4_6"))
				return new Compound1_4_6(SafeField.get(i.getClass(), "handle").get(i));
			else if(ver.equals("v1_4_5"))
				return new CompoundNone(SafeField.get(i.getClass(), "handle").get(i));
			else return new CompoundNone(SafeField.get(i.getClass(), "handle").get(i));
		}
		catch(SafeReflectFailException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	public boolean isCompatible()
	{
		if(ver.equals("v1_4_6"))return true;
		else if(ver.equals("v1_4_5"))return true;
		else if(ver.equals("v1_4_R1"))return true;
		else return false;
	}
}
