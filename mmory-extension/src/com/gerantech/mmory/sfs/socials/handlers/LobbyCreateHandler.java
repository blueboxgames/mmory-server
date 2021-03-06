package com.gerantech.mmory.sfs.socials.handlers;

import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.callbacks.MapChangeCallback;
import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.LobbyUtils;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyCreateHandler extends BBGClientRequestHandler
{

    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = (Game)(sender.getSession().getProperty("core"));

        MapChangeCallback mapChangeCallback = new MapChangeCallback();
        game.player.resources.changeCallback = mapChangeCallback;
        Boolean succeed = game.lobby.create();
        game.player.resources.changeCallback = null;

        if( !succeed )
        {
            send(SFSCommands.LOBBY_CREATE, MessageTypes.RESPONSE_NOT_ENOUGH_REQS, params, sender);
            return;
        }
        String roomName = params.getUtfString("name");
        String bio = params.getUtfString("bio");
        int capacity = params.getInt("max");
        int minPoint = params.getInt("min");
        int emblem = params.getInt("pic");
        int privacy = params.getInt("pri");

        if( getParentExtension().getParentZone().getRoomByName(roomName) != null )
        {
            send(SFSCommands.LOBBY_CREATE, MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
            return;
        }

        DBUtils.getInstance().updateResources(game.player, mapChangeCallback.updates);
        Room room = LobbyUtils.getInstance().create(sender, roomName, bio, emblem, capacity, minPoint, privacy);
        if( room == null )
        {
            send(SFSCommands.LOBBY_CREATE, MessageTypes.RESPONSE_UNKNOWN_ERROR, params, sender);
            return;
        }

        send(SFSCommands.LOBBY_CREATE, MessageTypes.RESPONSE_SUCCEED, params, sender);
    }
}