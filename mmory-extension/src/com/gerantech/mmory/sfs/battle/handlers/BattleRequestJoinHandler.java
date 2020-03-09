package com.gerantech.mmory.sfs.battle.handlers;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.core.scripts.ScriptEngine;
import com.gerantech.mmory.core.utils.maps.IntIntMap;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.gerantech.mmory.sfs.handlers.LoginEventHandler;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

public class BattleRequestJoinHandler extends BBGClientRequestHandler
{
    private int type;
    private int mode;
    private int index;
    private int league;
    private boolean debugMode;
    private int friendlyMode;

    public void handleClientRequest(User sender, ISFSObject params)
    {
try {
        int now = (int)Instant.now().getEpochSecond();
        if( now < LoginEventHandler.UNTIL_MAINTENANCE )
        {
            params.putInt("umt", LoginEventHandler.UNTIL_MAINTENANCE - now);
            send(SFSCommands.BATTLE_JOIN, MessageTypes.RESPONSE_MUST_WAIT, params, sender);
            return;
        }

        this.index = params.getInt("index");
        if( this.index > 1 )
        {
            BBGRoom room = BattleUtils.getInstance().rooms.get(this.index);
            if (room == null || room.getState() >= BattleField.STATE_4_ENDED )
            {
                send(SFSCommands.BATTLE_JOIN, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
				return;
            }
            BattleUtils.getInstance().join(room, sender, params.containsKey("spectatedUser") ? params.getInt("spectatedUser") : -1);
            return;
        }

        Game game = (Game)sender.getSession().getProperty("core");
        this.debugMode = params.containsKey("debugMode");
        this.friendlyMode = params.containsKey("friendlyMode") ? params.getInt("friendlyMode") : 0;
        this.league = game.player.get_arena(0);
        this.mode = ScriptEngine.getInt(ScriptEngine.T41_CHALLENGE_MODE, this.index, game.player.id, null, null);
        this.type = ScriptEngine.getInt(ScriptEngine.T42_CHALLENGE_TYPE, this.index, null, null, null);
        IntIntMap cost = new IntIntMap((String)ScriptEngine.get(ScriptEngine.T52_CHALLENGE_RUN_REQS, this.index, null, null, null));
        if( !game.player.has(cost) )
        {
            send(SFSCommands.BATTLE_JOIN, MessageTypes.RESPONSE_NOT_ENOUGH_REQS, params, sender);
            return;
        }
        this.joinUser(sender);
} catch (Exception | Error e) { e.printStackTrace(); }
    }
 
	private void joinUser(User user)
    {
        BBGRoom room;
        room = findWaitingBattleRoom(user);

        if( room == null )
            room = BattleUtils.getInstance().make((Class<?>) getParentExtension().getParentZone().getProperty("battleClass"), user, this.index, this.mode, this.type, this.friendlyMode);
        BattleUtils.getInstance().join(room, user);
        ((BattleRoom)room).battleField.debugMode = debugMode;
    }

    private BBGRoom findWaitingBattleRoom(User user)
    {
        AbstractMap<Integer, BBGRoom> battles = BattleUtils.getInstance().rooms;
        trace("alive battles " + battles.size());
        BattleRoom room = null;
        for(Map.Entry<Integer, BBGRoom> entry : battles.entrySet())
        {
            room = (BattleRoom)entry.getValue();
            trace(room.toString());
            if( room.isFull() )
                continue;
            if( room.battleField.friendlyMode > 0 )
                continue;
            if( room.getPropertyAsInt("league") != league )
                continue;
            if( room.getState() != BattleField.STATE_0_WAITING )
                continue;
            if( room.getPropertyAsInt("mode") != mode )
                continue;
            return room;
        }
        return null;
    }
}