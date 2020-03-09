package com.gerantech.mmory.sfs.handlers;

import java.sql.SQLException;

import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.data.LobbySFS;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.LobbyUtils;
import com.gerantech.mmory.sfs.utils.PasswordGenerator;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 *
 */
public class ProfileRequestHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		int playerId = params.getInt("id");

		//  -=-=-=-=-=-=-=-=-  add resources data  -=-=-=-=-=-=-=-=-
		String query = "SELECT type, count, level FROM " + DBUtils.getInstance().liveDB + ".resources WHERE player_id = " + playerId + (params.containsKey("am") ? ";" : " AND (type<13);");
		ISFSArray resources = null;
		ISFSArray supData = null;
		boolean isOld = false;
		try {
			resources = getDBManager().executeQuery(query, new Object[]{});
			isOld = resources.size() < 1;
			if( isOld )
			{
				query = "SELECT type, count, level FROM resources WHERE player_id = " + playerId + (params.containsKey("am") ? ";" : " AND (type<13);");
				resources = getDBManager().executeQuery(query, new Object[]{});
			}
			if( params.containsKey("am") )
			{
				query = "SELECT id, create_at, model FROM " + DBUtils.getInstance().liveDB + ".players INNER JOIN devices ON players.id=devices.player_id WHERE id=" + playerId;
				supData = getDBManager().executeQuery(query, new Object[]{});
				isOld = supData.size() < 1;
				if( isOld )
				{
					query = "SELECT id, create_at, model FROM players INNER JOIN devices ON players.id=devices.player_id WHERE id=" + playerId;
					supData = getDBManager().executeQuery(query, new Object[]{});
				}
			}
		} catch (SQLException e) { e.printStackTrace(); }
		params.putSFSArray("resources", resources);
		if( supData != null )
			params.putSFSArray("supData", supData);
		String liveDB = isOld ? "" : (DBUtils.getInstance().liveDB + ".");

		//  -=-=-=-=-=-=-=-=-  add player data  -=-=-=-=-=-=-=-=-
		if( params.containsKey("pd") )
		{
			query = "SELECT * FROM " + liveDB + "players WHERE id=" + playerId + " Limit 1;";
			ISFSArray players = null;
			try {
				players = getDBManager().executeQuery(query, new Object[]{});
			} catch (SQLException e) { e.printStackTrace(); }
			params.putSFSObject("pd", players.getSFSObject(0));
		}

		//  -=-=-=-=-=-=-=-=-  add lobby data  -=-=-=-=-=-=-=-=-
		LobbySFS lobbyData = null;
		if( params.containsKey("lp") )
			lobbyData = LobbyUtils.getInstance().getDataByMember(playerId);
		if( lobbyData != null )
		{
			params.putText("ln", lobbyData.getName());
			params.putInt("lp", lobbyData.getEmblem());
		}

		//  -=-=-=-=-=-=-=-=-  add player tag  -=-=-=-=-=-=-=-=-
		params.putText("tag", PasswordGenerator.getInvitationCode(playerId));

		//  -=-=-=-=-=-=-=-=-  add player deck  -=-=-=-=-=-=-=-=-
		query = "SELECT " + liveDB + "decks.`type`, " + liveDB + "resources.`level` FROM " + liveDB + "decks INNER JOIN " + 
		liveDB + "resources ON " + liveDB + "decks.player_id = " + liveDB + "resources.player_id AND decks.`type` = " + liveDB + "resources.`type` WHERE " + 
		liveDB + "decks.player_id = " + playerId + " AND " + liveDB + "decks.deck_index = 0";
		try {
			params.putSFSArray("decks", getDBManager().executeQuery(query, new Object[]{}));
		} catch (SQLException e) { trace(e.getMessage()); }

		//trace(params.getDump());
		send(SFSCommands.PROFILE, params, sender);
	}
}