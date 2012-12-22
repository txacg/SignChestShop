package net.skycraftmc.SignChestShop.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class SafeMethod 
{
	private Method m;
	private SafeMethod(Class<?> c, String name, Class<?>... args) 
			throws SecurityException, NoSuchMethodException
	{
		m = c.getDeclaredMethod(name, args);
		m.setAccessible(true);
	}
	public static SafeMethod get(Class<?> c, String name, Class<?>...args)
	{
		SafeMethod m = null;
		try {
			m = new SafeMethod(c, name, args);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
		return m;
	}
	public boolean isVoid()
	{
		return m.getReturnType() == Void.TYPE;
	}
	public Object invoke(Object obj, Object... args)throws SafeReflectFailException
	{
		Object o = null;
		try
		{
			o = m.invoke(obj, args);
		}catch(SecurityException e){
			throw new SafeReflectFailException(e);
		} catch (IllegalArgumentException e) {
			throw new SafeReflectFailException(e);
		} catch (IllegalAccessException e) {
			throw new SafeReflectFailException(e);
		} catch (InvocationTargetException e) {
			throw new SafeReflectFailException(e);
		}
		return o;
	}
}
