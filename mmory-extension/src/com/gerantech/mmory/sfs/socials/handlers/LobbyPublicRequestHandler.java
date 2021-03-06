package com.gerantech.mmory.sfs.socials.handlers;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;

public class LobbyPublicRequestHandler extends BBGClientRequestHandler
{
    private static AtomicInteger roomId = new AtomicInteger();

	public void handleClientRequest(User sender, ISFSObject params)
    {
        Player player = ((Game)sender.getSession().getProperty("core")).player;
        if( params.containsKey("imei") || !params.getText("imei").isEmpty() )
        {
            String queryStr = "INSERT INTO devices(`player_id`, `model`, `udid`, `imei`) VALUES (" + player.id + ", '', '', '" + params.getText("imei") + "') ON DUPLICATE KEY UPDATE imei='" + params.getText("imei") + "'";
            // trace(queryStr);
            try {
                getDBManager().executeUpdate(queryStr, new Object[]{});
            } catch (SQLException e) { e.printStackTrace(); }
        }

        ISFSObject banParams = BanUtils.getInstance().checkBan(player.id, null, params.getText("imei"), (int)Instant.now().getEpochSecond());
        Room theRoom = findReady(sender);
        if( theRoom == null )
        {
            theRoom = make(sender);
            /*LobbySFS lobbyData = new.LobbySFS(theRoom.getId(), theRoom.getName(), "", 0,0,0,0,null, null);
            lobbyData.setMessages(new SFSArray());
            theRoom.setProperty("data", lobbyData);*/
        }
        try {
            getApi().joinRoom(sender, theRoom, null, false, null);
        } catch (SFSJoinRoomException e) { e.printStackTrace(); }
        send(SFSCommands.LOBBY_PUBLIC, banParams, sender);
    }

    private Room findReady(User user)
    {
        // search public lobby with geo ip
        //MatchExpression exp = new MatchExpression('rank', NumberMatch.GREATER_THAN, 5).and('country', StringMatch.EQUALS, 'Italy')
        //List<User> matchingUsers = sfsApi.findUsers(zone.getUserList(), exp, 50);
        List<Room> rList = getParentExtension().getParentZone().getRoomListFromGroup("publics");
        for (Room r : rList)
            if ( !r.isFull() )
                return r;
        return null;
    }

    private Room make(User owner)
    {
        CreateRoomSettings.RoomExtensionSettings res = new CreateRoomSettings.RoomExtensionSettings("MMOry", "com.gerantech.mmory.sfs.socials.BaseLobbyRoom");

        trace("---------=========<<<<  MAKE public lobby by ", owner.getName(), " >>>>==========---------");

        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(false);
        rs.setDynamic(true);
        rs.setAutoRemoveMode(SFSRoomRemoveMode.NEVER_REMOVE);
        //rs.setRoomProperties( roomProperties );
        rs.setName( "public_lobby_" + roomId.getAndIncrement() );
        rs.setMaxUsers(30);
        rs.setGroupId("publics");
        rs.setExtension(res);

        List<RoomVariable> listOfVars = new ArrayList<>();
        listOfVars.add( new SFSRoomVariable("msg", new SFSArray(),  false, true, false) );
        rs.setRoomVariables(listOfVars);

        try {
            return getApi().createRoom(getParentExtension().getParentZone(), rs, owner);
        } catch (SFSCreateRoomException e) {
            e.printStackTrace();
        }
        return null;
    }
}