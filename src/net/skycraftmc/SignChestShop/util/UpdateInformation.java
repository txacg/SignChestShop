package net.skycraftmc.SignChestShop.util;

import java.net.URL;

public class UpdateInformation 
{
	private String version;
	private URL link;
	private String desc;
	public UpdateInformation(String version, URL link, String desc)
	{
		this.link = link;
		this.version = version.split("[ ]+", 2)[1];
		this.desc = desc;
	}
	public URL getLink()
	{
		return link;
	}
	public String getVersion()
	{
		return version;
	}
	public String getDescription()
	{
		return desc;
	}
}
