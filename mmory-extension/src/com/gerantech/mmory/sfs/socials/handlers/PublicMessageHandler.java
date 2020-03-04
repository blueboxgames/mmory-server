package com.gerantech.mmory.sfs.socials.handlers;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.gerantech.mmory.libs.Commands;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.util.ClientDisconnectionReason;

/**
 * Created by ManJav on 8/24/2017.
 */
public class PublicMessageHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        if( params.containsKey("x") )
        {
            send(Commands.LOBBY_PUBLIC_MESSAGE, params, sender);
            SmartFoxServer.getInstance().getTaskScheduler().schedule(new TimerTask() {
                @Override
                public void run() {
                cancel();
                try {
                    getApi().disconnectUser(sender, ClientDisconnectionReason.KICK);
                } catch (Error | Exception e) { e.printStackTrace(); }
                }
            }, 10, TimeUnit.MILLISECONDS);
            return;
        }
        send(Commands.LOBBY_PUBLIC_MESSAGE, params, getParentExtension().getParentRoom().getUserList());
    }
}