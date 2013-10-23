package net.skycraftmc.SignChestShop.util;

public class UpdateInformation 
{
	private String version;
	private String type;
	public UpdateInformation(String version, String type)
	{
		this.type = type;
		this.version = version.split("[ ]+", 2)[1];
	}
	public String getVersion()
	{
		return version;
	}
	public String getType()
	{
		return type;
	}
}
