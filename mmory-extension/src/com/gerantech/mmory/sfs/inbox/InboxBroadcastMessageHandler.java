package com.gerantech.mmory.sfs.inbox;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.utils.InboxUtils;
import com.gerantech.mmory.libs.utils.FCMUtils;
import com.gerantech.mmory.libs.utils.OneSignalUtils;
import com.gerantech.mmory.sfs.utils.PushProvider;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.Collection;

/**
 * Created by ManJav on 17/11/08.
 */
public class InboxBroadcastMessageHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Collection<Integer> receivers = params.getIntArray("receiverIds");
        Integer[] receiverIds = receivers.toArray(new Integer[receivers.size()]);
        String data = params.containsKey("data") ? params.getText("data") : null;
        PushProvider pushProvider = PushProvider.FCM;

        // check is admin and 10000 usage
        if( params.getInt("senderId") == 10000 && !((Game)sender.getSession().getProperty("core")).player.admin )
            return;
        
        for( int i = 0; i < receiverIds.length; i++ )
            InboxUtils.getInstance().send(params.getInt("type"), params.getUtfString("text"), params.getInt("senderId"), receiverIds[i], data);
            
        int delivered = 0;
        if( params.containsKey("isPush") && params.getBool("isPush") )
        {
            switch (pushProvider)
            {
                case FCM:
                    delivered += FCMUtils.getInstance().send(params.getUtfString("text"), data, receiverIds);
                    break;
                case ONESIGNAL:
                    OneSignalUtils.getInstance().send(params.getUtfString("text"), data, receiverIds);
                default:
                    delivered += FCMUtils.getInstance().send(params.getUtfString("text"), data, receiverIds);
                    break;
            }
        }

        params.putInt("delivered", delivered);
        send(SFSCommands.INBOX_BROADCAST, params, sender);
    }
}