package com.gerantech.mmory.sfs.battle.handlers;

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
            this.room.setProperty("readyPlayers", 1);
            if( this.room.getUsersByType(BBGRoom.USER_TYPE_PLAYER).size() > 1 )
                send(Commands.BATTLE_READY, MessageTypes.RESPONSE_MUST_WAIT, params, user);
        }
        else
        {
            boolean shouldIncrement = true;
            List<User> players = room.getPlayersList();
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).equals(user)) {
					shouldIncrement = false;
				}
            }
            
            if( shouldIncrement )
                this.room.setProperty("readyPlayers", this.room.getPropertyAsInt("readyPlayers")+1 );
        }

        if( this.room.getUsersByType(BBGRoom.USER_TYPE_PLAYER).size() == this.room.getPropertyAsInt("readyPlayers") )
        {
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