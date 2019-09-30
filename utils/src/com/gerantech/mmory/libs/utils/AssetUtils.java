package com.gerantech.mmory.libs.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;

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
            long latestModified = 0;
            ISFSObject checksum = new SFSObject();
            ISFSObject addressList = new SFSObject();
            ISFSObject timeList = new SFSObject();
            // ISFSObject md5List = new SFSObject();
            List<String> fileListing = getDirectoryListing("./www/ext");
            for (String v : fileListing)
            {
                String fileAddress = v.replace('\\', '/');
                String fileName = "";
                if(fileAddress.split("./www/ext/inits").length == 2)
                    fileName = fileAddress.split("./www/ext/inits")[1];
                else if( fileAddress.split("./www/ext/others").length == 2 )
                    fileName = fileAddress.split("./www/ext/others")[1];
                else
                    fileName = fileAddress.split("./www/ext")[1];
                
                long modifyTime = Long.parseLong(getModifiedDate(v));
                if(modifyTime > latestModified)
                    latestModified = modifyTime;
                
                addressList.putUtfString(fileName, fileAddress.split("./www/")[1]);
                // md5List.putUtfString(fileName, hashMd5File(v));
                timeList.putUtfString(fileName, getModifiedDate(v));
            }
            checksum.putLong("lastMod", latestModified);
            checksum.putSFSObject("address", addressList);
            checksum.putSFSObject("time", timeList);
            // checksum.putSFSObject("md5", md5List);
			ext.getParentZone().setProperty("checksum", checksum);
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
        try
        {
            InputStream is;
            try 
            {
                is = Files.newInputStream(Paths.get(path));
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
    private List<String> getDirectoryListing(String path)
	{
		try (Stream<Path> walk = Files.walk(Paths.get(path))) {

			List<String> result = walk.filter(Files::isRegularFile)
					.map(x -> x.toString()).collect(Collectors.toList());
	
			return result;
	
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
}