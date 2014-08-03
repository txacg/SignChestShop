package co.technius.signchestshop.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Updater
{

    public static UpdateInformation findUpdate(final String version)
            throws IOException
    {
        final URL link = new URL("https://api.curseforge.com/servermods/files?projectIds=47097");
        final URLConnection c = link.openConnection();
        c.addRequestProperty("User-Agent", "SignChestShop version " + version + " (by Technius)");
        final BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        final String s = br.readLine();
        final JSONArray jsa = (JSONArray) JSONValue.parse(s);
        if (jsa.size() > 0)
        {
            final JSONObject jso = (JSONObject) jsa.get(jsa.size() - 1);
            return new UpdateInformation((String) jso.get("name"), (String) jso.get("releaseType"));
        }
        return null;
    }
}
