package com.gerantech.mmory.sfs.inbox;

import com.gerantech.mmory.libs.utils.InboxUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class InboxOpenHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
    	if( params.getInt("receiverId") > 1000 )
			InboxUtils.getInstance().open(params.getInt("id"));
	}
}