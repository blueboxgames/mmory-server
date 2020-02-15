package com.gerantech.mmory.sfs.battle.handlers;

import java.util.ArrayList;
import java.util.List;

import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * BattleReadyHandler
 */
public class BattleReadyHandler extends BBGClientRequestHandler {

    private BattleRoom room;
    @Override
    public void handleClientRequest(User user, ISFSObject params) {
        if( !params.containsKey("roomId") )
        {
            send(Commands.BATTLE_READY, MessageTypes.RESPONSE_NOT_ALLOWED, params, user);
            return;
        }

        if( BattleUtils.getInstance().rooms.get(params.getInt("roomId")) != null )
        {
            this.room = (BattleRoom) BattleUtils.getInstance().rooms.get(params.getInt("roomId"));
        }
        else
        {
            send(Commands.BATTLE_READY, MessageTypes.RESPONSE_NOT_FOUND, params, user);
            return;
        }

        if( !this.room.containsProperty("readyPlayers") )
        {
            List<User> roomPlayers = new ArrayList<User>();
            roomPlayers.add(user);
            this.room.setProperty("readyPlayers", roomPlayers);
            if( this.room.getMaxUsers() > 1 )
                send(Commands.BATTLE_READY, MessageTypes.RESPONSE_MUST_WAIT, params, user);
        }
        else
        {
            List<User> players = room.getPlayersList();
            @SuppressWarnings("unchecked")
            List<User> readyPlayers = (List<User>) room.getProperty("readyPlayers");
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).equals(user)) {
                    readyPlayers.add(user);
				}
            }
        }

        List<?> readyList = (List<?>) this.room.getProperty("readyPlayers");
        if( this.room.getMaxUsers() == readyList.size() )
        {
            if( room.getUserType(user) != BBGRoom.USER_TYPE_PLAYER )
            {
                send(Commands.BATTLE_READY, MessageTypes.RESPONSE_SUCCEED, params, user);
                return;
            }
            List<User> players = room.getPlayersList();
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).equals(user)) {
					this.room.start();
				}
			}
            List<User> users = this.room.getPlayersList();
            send(Commands.BATTLE_READY, MessageTypes.RESPONSE_SUCCEED, params, users);
            return;
        }
    }
}