package com.gerantech.mmory.libs.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfigUtils
 */
public class ConfigUtils extends UtilBase {
	static String propertyFileName = "extensions/MMOry/mmory.properties";
	static public Properties loadProps() {
		Properties props = new Properties();
	
		try {
			props.load(new FileInputStream(propertyFileName));
		}
		catch (IOException e) {
			// TODO: requires to log as error after 2203-logs branch merge.
			// getLogger().error("Could not load config: " + propertyFileName);
		}
	
		return props;
	}
	
	public void saveProps(Properties props)
	{
		try {
			props.store(new FileOutputStream(propertyFileName), "");
		}
		catch (IOException e) {
			// TODO: requires to log as error after 2203-logs branch merge.
			// getLogger().error("Could not save config in: " + propertyFileName);
		}
	}
}