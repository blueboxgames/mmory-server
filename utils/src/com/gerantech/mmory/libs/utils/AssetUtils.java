package com.gerantech.mmory.libs.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map.Entry;

import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSDataWrapper;
import com.smartfoxserver.v2.entities.data.SFSObject;

import org.apache.commons.codec.digest.DigestUtils;
/**
 * HashUtils
 */
public class AssetUtils extends UtilBase
{
    static public AssetUtils getInstance()
    {
		return (AssetUtils) UtilBase.get(AssetUtils.class);
    }

    public String baseURL;
    
    public void loadAll()
    {
        // Initial MD5's
        if( ext.getParentZone().containsProperty("assets") )
            return;

        baseURL = ConfigUtils.getInstance().load(ConfigUtils.DEFAULT).getProperty("assetsBaseURL");

        ISFSObject ret = new SFSObject();
        try {
            ret = SFSObject.newFromJsonData(new String(Files.readAllBytes(Paths.get("config/assets.json"))));
        } catch (IOException e) { e.printStackTrace(); }
        
        Iterator<Entry<String, SFSDataWrapper>> iterator = ret.iterator();
        while( iterator.hasNext() )
            this.addMd5(iterator.next().getKey(), ret);
        ext.getParentZone().setProperty("assets", ret);
        trace("loaded assets in " + (System.currentTimeMillis() - (long) ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }

    private void addMd5(String key, ISFSObject assets)
    {
        String md5 = null;
        ISFSObject item = assets.getSFSObject(key);
        try {
            md5 = DigestUtils.md5Hex(Files.newInputStream(Paths.get("www/" + new URI(baseURL + key).getPath())));
        } catch(Exception e) { e.printStackTrace(); }
        item.putText("md5", md5);
        item.putText("url", baseURL + key);
    }
}