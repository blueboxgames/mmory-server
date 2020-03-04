package com.gerantech.mmory.sfs.socials.handlers;

import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.data.LobbySFS;
import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.utils.LobbyUtils;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyRemoveHandler extends BBGClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = ((Game)sender.getSession().getProperty("core"));
        if( !game.player.admin )
        {
            send(SFSCommands.LOBBY_REMOVE, MessageTypes.RESPONSE_NOT_ALLOWED, params, sender);
            return;
        }

        //Map<Integer, LobbySFS> all = LobbyUtils.getInstance().getAllData();
        if( !LobbyUtils.getInstance().getAllData().containsKey(params.getInt("id")) )
        {
            send(SFSCommands.LOBBY_REMOVE, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
            return;
        }

        LobbySFS data = LobbyUtils.getInstance().getDataById(params.getInt("id"));
        Room lobby = getParentExtension().getParentZone().getRoomByName(data.getName());
        if( lobby != null )
            for( User u : lobby.getUserList() )
                getApi().disconnectUser(u);

        LobbyUtils.getInstance().remove(params.getInt("id"));
        send(SFSCommands.LOBBY_REMOVE, MessageTypes.RESPONSE_SUCCEED, params, sender);
    }
}