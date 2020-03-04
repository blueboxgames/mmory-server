package com.gerantech.mmory.libs.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

/**
 * ConfigUtils
 */
public class ConfigUtils extends UtilBase
{
	
	public static String DEFAULT = "extensions/MMOry/default.properties";
	static public ConfigUtils getInstance()
	{
		return (ConfigUtils)UtilBase.get(ConfigUtils.class);
	}
	private Map<String, Properties> propertyList;
	public Properties load(String name)
	{
		if( this.propertyList == null )
			this.propertyList = new HashMap<>();
			
		if( this.propertyList.containsKey(name) )
			return this.propertyList.get(name);

		// load and cache properties
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(name));
		}
		catch (IOException e) { e.printStackTrace(); }
		this.propertyList.put(name, properties);
		return properties;
	}

	public void save(String name) {
		if( !this.propertyList.containsKey(name) )
		{
			trace(ExtensionLogLevel.ERROR, "Config " + name + " not found.");
			return;
		}
		try (OutputStream output = new FileOutputStream(name))
		{
			Properties properties = this.propertyList.get(name);
			// save properties to project root folder
			properties.store(output, null);
			System.out.println(properties);

		} catch (IOException e) { e.printStackTrace(); }
	}
}