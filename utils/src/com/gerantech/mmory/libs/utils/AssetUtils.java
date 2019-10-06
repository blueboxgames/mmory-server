package com.gerantech.mmory.libs.utils;

import java.io.IOException;
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
    
    public void loadAll()
    {
        // Initial MD5's
        if( ext.getParentZone().getProperty("checksum") != null )
            return;

        ISFSObject ret = null;
        try {
            ret = SFSObject.newFromJsonData(Files.readAllBytes(Paths.get("./assets.json")).toString());
        } catch (IOException e) { e.printStackTrace(); }

        Iterator<Entry<String, SFSDataWrapper>> iterator = ret.iterator();
        while( iterator.hasNext() )
            addMd5(ret.getSFSObject(iterator.next().getKey()));

        ext.getParentZone().setProperty("checksum", ret);
    }

    private void addMd5(ISFSObject item)
    {
        String result = null;
        try {
            result = DigestUtils.md5Hex(Files.newInputStream(Paths.get("www/" + item.getUtfString("url"))));
        } catch(IOException e) { e.printStackTrace(); }
        item.putUtfString("md5", result);
    }
}