package net.skycraftmc.SignChestShop.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class Updater
{
	public static UpdateInformation findUpdate()throws IOException, XMLStreamException
	{
		URL result = null;
		URL filerss = new URL("http://dev.bukkit.org/server-mods/signchestshop/files.rss");
		InputStream in = filerss.openStream();
		XMLEventReader reader = 
				XMLInputFactory.newInstance().createXMLEventReader(in);
		String title = null;
		String link = null;
		String desc = null;
		boolean i = false;
		while(reader.hasNext())
		{
			XMLEvent event = reader.nextEvent();
			if(!event.isStartElement())continue;
			String l = event.asStartElement().getName().getLocalPart();
			if(l.equals("item"))i = true;
			if(!i)continue;
			if(l.equals("title"))title = reader.nextEvent().asCharacters().getData();
			else if(l.equals("link"))
				link = reader.nextEvent().asCharacters().getData();
			else if(l.equals("description"))
			{
				desc = "";
				XMLEvent ev;
				for(;;)
				{
					ev = reader.nextEvent();
					if(ev.isEndElement())
					{
						if(ev.asEndElement().getName().getLocalPart().equals("description"))
							break;
					}
					desc = desc + ev.asCharacters().getData();
				}
			}
			if(title != null && link != null && desc != null)break;
		}
		in.close();
		result = new URL(link);
		return new UpdateInformation(title, result, desc);
	}
}
