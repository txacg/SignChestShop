package co.technius.SignChestShop.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.stream.XMLStreamException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Updater
{
	public static UpdateInformation findUpdate(String version) 
			throws IOException, XMLStreamException
	{
		URL link = new URL("https://api.curseforge.com/servermods/files?projectIds=47097");
		URLConnection c = link.openConnection();
		c.addRequestProperty("User-Agent", "SignChestShop version " + version + " (by Technius)");
		BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
		String s = br.readLine();
		JSONArray jsa = (JSONArray) JSONValue.parse(s);
		if(jsa.size() > 0)
		{
			JSONObject jso = (JSONObject) jsa.get(jsa.size() - 1);
			return new UpdateInformation((String) jso.get("name"), (String) jso.get("releaseType"));
		}
		return null;
	}
}
