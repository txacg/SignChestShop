package co.technius.SignChestShop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class StringConfig
{
	private File f;
	private PrintWriter pw;
	private HashMap<String, String>map = new HashMap<String, String>();
	public StringConfig(String file)
	{
		f = new File(file);
	}
	public StringConfig(File file)
	{
		f = file;
	}
	public File getFile()
	{
		return f;
	}
	public void start() throws IOException
	{
		if(pw != null)return;
		if(f == null)
		{
			FileOutputStream fos = new FileOutputStream(f);
			fos.flush();
			fos.close();
		}
		pw = new PrintWriter(f);
	}
	public void insertComment(String s)
	{
		if(pw == null)return;
		pw.println("#" + s);
	}
	public void write(String k, String v)
	{
		if(pw == null)return;
		pw.println(k + ": " + v);
	}
	public void writeKey(String k, String def)
	{
		if(pw == null)return;
		if(!map.containsKey(k))pw.println(k + ": " + def);
		else pw.println(k + ": " + map.get(k));
	}
	public void write(String s)
	{
		if(pw == null)return;
		pw.println(s);
	}
	public void writeLine()
	{
		if(pw == null)return;
		pw.println();
	}
	public void set(String k, String v)
	{
		map.put(k, v);
	}
	public String getString(String k, String def)
	{
		if(!map.containsKey(k))return def;
		return map.get(k);
	}
	public int getInt(String k, int def)
	{
		if(!map.containsKey(k))return def;
		try
		{
			return Integer.parseInt(map.get(k));
		}
		catch(Exception e)
		{
			return def;
		}
	}
	public double getDouble(String k, double def)
	{
		if(!map.containsKey(k))return def;
		try
		{
			return Double.parseDouble(map.get(k));
		}
		catch(Exception e)
		{
			return def;
		}
	}
	public boolean getBoolean(String k, boolean def)
	{
		if(!map.containsKey(k))return def;
		return Boolean.parseBoolean(map.get(k));
	}
	public String[] getStringList(String k, String[] def)
	{
		if(!map.containsKey(k))return def;
		return map.get(k).split(",");
	}
	public int[] getIntList(String k, int[] def)
	{
		if(!map.containsKey(k))return def;
		String[] s = map.get(k).split(",");
		int[] a = new int[s.length];
		for(int i = 0; i < s.length; i ++)
		{
			String b = s[i];
			try
			{
				a[i] = Integer.parseInt(b.trim());
			}
			catch(NumberFormatException nfe)
			{
				return def;
			}
		}
		return a;
	}
	public double[] getDoubleList(String k, double[] def)
	{
		if(!map.containsKey(k))return def;
		String[] s = map.get(k).split(",");
		double[] a = new double[s.length];
		for(int i = 0; i < s.length; i ++)
		{
			String b = s[i];
			try
			{
				a[i] = Double.parseDouble(b.trim());
			}
			catch(NumberFormatException nfe)
			{
				return def;
			}
		}
		return a;
	}
	public void close() throws IOException
	{
		if(pw == null)return;
		pw.flush();
		pw.close();
		pw = null;
	}
	public void load() throws FileNotFoundException, IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(f));
		String l;
		map.clear();
		while((l = br.readLine()) != null)
		{
			if(l.isEmpty() || l.startsWith("#"))continue;
			String[] t = l.split(":", 2);
			if(t.length != 2)continue;
			map.put(t[0], t[1].trim());
		}
		br.close();
	}
	public void writeKey(String k, boolean def) 
	{
		writeKey(k, Boolean.toString(def));
	}
	
	public void writeKey(String k, int def)
	{
		writeKey(k, Integer.toString(def));
	}
}
