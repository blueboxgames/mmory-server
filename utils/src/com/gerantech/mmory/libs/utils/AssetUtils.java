package com.gerantech.mmory.libs.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
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
		if( ext.getParentZone().getProperty("checksum") == null )
		{

            // long latestModified = 0;
            ISFSObject fromJson = SFSObject.newFromJsonData(getAssetsJsonString());
            ISFSObject ret = new SFSObject();
            long startTime = System.nanoTime();
            for (Iterator<Entry<String, SFSDataWrapper>> iterator = fromJson.iterator(); iterator.hasNext(); ) {
                Entry<String, SFSDataWrapper> next = iterator.next();
                ISFSObject fileData = new SFSObject();
                fileData.putUtfString("url", fromJson.getSFSObject(next.getKey()).getUtfString("url"));
                fileData.putBool("first", fromJson.getSFSObject(next.getKey()).getBool("first"));
                fileData.putBool("pre", fromJson.getSFSObject(next.getKey()).getBool("pre"));
                fileData.putBool("post", fromJson.getSFSObject(next.getKey()).getBool("post"));
                fileData.putUtfString("md5", hashMd5File("www/" + fromJson.getSFSObject(next.getKey()).getUtfString("url")));
                ret.putSFSObject(next.getKey(), fileData);
            }
            long endTime   = System.nanoTime();
            long totalTime = endTime - startTime;
            System.out.println(totalTime);
            ext.getParentZone().setProperty("checksum", ret);
		}
    }
    /**
     * Opens a url stream and read it to memory and return it's md5 digest.
     * @return MD5 Checksum
     */
    public static String hashMd5Url(String url)
    {
        String result = null;
        try
        {
            InputStream is;
            try
            {
                is = new URL(url).openStream();
                result = getInputDigest(is);
            }
            catch (java.io.FileNotFoundException e)
            {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String hashMd5File(String path)
    {
        String result = null;
        try {
            result = DigestUtils.md5Hex(Files.newInputStream(Paths.get(path)));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getModifiedDate(String path)
    {
        String result = null;
        try
        {
            File file = new File(path);
            result = String.valueOf(file.lastModified());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String getInputDigest(InputStream is)
    {
        StringBuffer sb = new StringBuffer();
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try {
                is = new DigestInputStream(is, md);
                while (is.read() != -1) {}
            } finally {
                is.close();
            }
            byte[] digest = md.digest();

            for (int i = 0; i < digest.length; i++) {
                sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    private String getAssetsJsonString()
    {
        try {
            String content = new String(Files.readAllBytes(Paths.get("./assets.json")));
            return content;
		} catch (IOException e) {
			e.printStackTrace();
		}
        return "{}";
    }
}