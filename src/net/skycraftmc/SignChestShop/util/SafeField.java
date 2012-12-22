package net.skycraftmc.SignChestShop.util;

import java.lang.reflect.Field;

public class SafeField 
{
	private Field f;
	private SafeField(Class<?> c, String name) throws SecurityException, NoSuchFieldException
	{
		f = c.getDeclaredField(name);
		f.setAccessible(true);
	}
	public static SafeField get(Class<?> c, String name)
	{
		SafeField f = null;
		try {
			f = new SafeField(c, name);
		} catch (SecurityException e) {
		} catch (NoSuchFieldException e) {
		}
		return f;
	}
	public boolean set(Object o, Object value)
	{
		try {
			f.set(o, value);
			return true;
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		return false;
	}
	public Object get(Object o)throws SafeReflectFailException
	{
		try {
			return f.get(o);
		} catch (Exception e){
			throw new SafeReflectFailException(e);
		}
	}
}
