package com.gerantech.mmory.libs.utils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.core.scripts.ScriptEngine;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.data.LobbySFS;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

/**
 * Created by ManJav on 9/23/2017.
 */
public class BattleUtils extends UtilBase
{
    public static BattleUtils getInstance()
    {
        return (BattleUtils)UtilBase.get(BattleUtils.class);
    }
    public ConcurrentHashMap<Integer, String> maps = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Integer, BBGRoom> rooms = new ConcurrentHashMap<>();

    public BBGRoom make(Class<?> roomClass, User owner, int index, int mode, int type, int friendlyMode)
    {
        // temp solution
        long now = Instant.now().getEpochSecond();
        Set<Map.Entry<Integer, BBGRoom>> entries = rooms.entrySet();
        for( Map.Entry<Integer, BBGRoom> entry : entries )
        {
            if ( entry.getValue().containsProperty("startAt") && now - entry.getValue().getPropertyAsInt("startAt") > 500 )
            {
                trace("WAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAY!!!    BATTLE KHARAB SHOOOOOD!!!!");
                remove(entry.getValue());
            }
        }

        Player player = ((Game)owner.getSession().getProperty("core")).player;
        int league = player.get_arena(0);
        boolean tutorial = player.get_battleswins() < ScriptEngine.getInt(ScriptEngine.T61_BATTLE_NUM_TUTORS, player.id, null, null, null);
        Map<Object, Object> roomProperties = new HashMap<>();
        roomProperties.put("index", index);
        roomProperties.put("mode", mode);
        roomProperties.put("type", type);
        roomProperties.put("league", league);// F===> is temp
        roomProperties.put("friendlyMode", friendlyMode);
        roomProperties.put("state", BattleField.STATE_0_WAITING);

        int id = (int)Math.floor(System.currentTimeMillis()/100%100000000);
        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setName( "m" + mode + "_t" + type + "_f" + friendlyMode + "_" + id );
        rs.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
        rs.setRoomProperties(roomProperties);
        rs.setMaxUsers(tutorial ? 1 : 2);
        rs.setGroupId("battles");
        rs.setMaxSpectators(50);
        rs.setDynamic(true);
        rs.setGame(true);

        BBGRoom newRoom = null;
        try {
            newRoom = (BBGRoom)roomClass.newInstance();
        } catch (Exception e) { e.printStackTrace(); }
        newRoom.init(id, rs);
        newRoom.setOwner(owner);
        this.rooms.put(id, newRoom);
        ext.getLogger().info(String.format("Battle created: %s, %s, type = %s", new Object[] { newRoom.getZone().toString(), newRoom.toString(), newRoom.getClass().getSimpleName() }));
        return newRoom;
    }

    public void join(BBGRoom room, User user)
    {
        this.join(room, user, -1);
    }
    /**
     * join users in  battle room
     * @param room
     * @param user
     * @param spectatingUser
     */
    public void join(BBGRoom room, User user, int spectatingUser)
    {
        Player player = ((Game)user.getSession().getProperty("core")).player;
        if( spectatingUser == -1 && room.getState() > BattleField.STATE_0_WAITING && !isPlayer(room, player.id) )
        {
            trace(ExtensionLogLevel.ERROR, "Battle " + room.getName() + " is full.");
            return;
        }

        user.getSession().setProperty("spectatingUser", spectatingUser);
        room.addUser(user, spectatingUser > -1 ? BBGRoom.USER_TYPE_SPECTATOR : BBGRoom.USER_TYPE_PLAYER);
        ext.getLogger().info(String.format("Battle joined: %s, %s, spectatingUser = %s", new Object[] { room.toString(), user.toString(), spectatingUser }));
    }

    public void leave(BBGRoom room, User user)
    {
        room.leave(user);
        ext.getLogger().info(String.format("User %s exit from %s", new Object[] { user.toString(), room.getName() }));
        if( room.getUserList().size() == 0 && room.getAutoRemoveMode() == SFSRoomRemoveMode.WHEN_EMPTY )
            remove(room);
    }

    /**
     * Kick all users and remove room
     * @param room
     */
    public void remove(BBGRoom room)
    {
        if( room.getUserList().size() > 0 )
        {
            room.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
            List<User> users = room.getUsersByType(-1);
            for( User u : users )
                leave(room, u);
        }
        else
        {
            room.destroy();
            removeReferences(room);
            rooms.remove(room.getId());
            ext.getLogger().info(String.format("Battle removed: %s, %s, num remaining battles = %s", new Object[] { room.getZone().toString(), room.toString(), rooms.size() }));
        }
    }

    public BBGRoom findByPlayer(int playerId, int minState, int maxState)
    {
        Set<Map.Entry<Integer, BBGRoom>> entries = rooms.entrySet();
        for( Map.Entry<Integer, BBGRoom> entry : entries )
            if( entry.getValue().getState() >= minState && entry.getValue().getState() <= maxState )
                if( isPlayer(entry.getValue(), playerId) )
                    return entry.getValue();
        return null;
    }

    public BBGRoom findByUser(User user, int minState, int maxState)
    {
        Set<Map.Entry<Integer, BBGRoom>> entries = rooms.entrySet();
        for( Map.Entry<Integer, BBGRoom> entry : entries )
            if( entry.getValue().getState() >= minState && entry.getValue().getState() <= maxState )
                if( entry.getValue().containsUser(user) )
                    return entry.getValue();
        return null;
    }

    public Boolean isPlayer(BBGRoom room, int playerId)
    {
        if( !room.containsProperty("games") )
            return false;
        for( Object g : (List<?>)room.getProperty("games") )
            if( ((Game)g).player.id == playerId )
                return true;

        return false;
    }

    private void removeReferences(BBGRoom room)
    {
        List<Room> lobbies = ext.getParentZone().getRoomListFromGroup("lobbies");
        int msgIndex = -1;
        for (int i = 0; i < lobbies.size(); i++)
        {
            Room lobby = lobbies.get(i);
            if( lobby.containsProperty(room.getName()) )
            {
                ISFSArray messageQueue = ((LobbySFS)lobby.getProperty("data")).getMessages();
                int msgSize = messageQueue.size();
                for (int j = msgSize-1; j >= 0; j--)
                {
                    ISFSObject msg = messageQueue.getSFSObject(j);
                    if( !msg.containsKey("bid") )
                        continue;
                    int battleState = msg.containsKey("st") ? msg.getInt("st") : 0;
                    if( msg.getInt("bid") == room.getId() && battleState < 2 )
                    {
                        msg.putInt("st", battleState == 0 ? 3 : 2);
                        if( battleState == 0 )
                            msgIndex = j;
                        if( lobby.getUserList().size() > 0 )
                            ext.getApi().sendExtensionResponse(SFSCommands.LOBBY_PUBLIC_MESSAGE, msg, lobby.getUserList(), lobby, false);
                    }
                }
                //trace(room.getName(), lobby.getName());
                if( msgIndex > -1 )
                    messageQueue.removeElementAt(msgIndex);
            }
        }
    }
}