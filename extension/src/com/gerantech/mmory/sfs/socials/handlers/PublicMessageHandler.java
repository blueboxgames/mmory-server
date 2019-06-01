package com.gerantech.mmory.sfs.socials.handlers;

import com.gerantech.mmory.libs.Commands;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 8/24/2017.
 */
public class PublicMessageHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        send(Commands.LOBBY_PUBLIC_MESSAGE, params, getParentExtension().getParentRoom().getUserList());
    }
}