package net.skycraftmc.SignChestShop.util;

@SuppressWarnings("serial")
public class SafeReflectFailException extends Exception
{
	public SafeReflectFailException(Throwable e) {
		super(e);
	}
	public SafeReflectFailException(){
		
	}
	public SafeReflectFailException(String s){
		
	}
	public SafeReflectFailException(Throwable e, String s) {
		super(s, e);
	}
	
}
