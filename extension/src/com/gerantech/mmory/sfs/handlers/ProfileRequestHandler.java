package com.gerantech.mmory.sfs.handlers;

import java.sql.SQLException;

import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.data.LobbySFS;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.LobbyUtils;
import com.gerantech.mmory.sfs.utils.PasswordGenerator;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class ProfileRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		int playerId = params.getInt("id");

		//  -=-=-=-=-=-=-=-=-  add resources data  -=-=-=-=-=-=-=-=-
		String query = "SELECT type, count, level FROM " + DBUtils.getInstance().liveDB + ".resources WHERE player_id = " + playerId + (params.containsKey("am") ? ";" : " AND (type<13);") ;
		try {
			params.putSFSArray("resources", dbManager.executeQuery(query, new Object[]{}));
		} catch (SQLException e) { e.printStackTrace(); }

		//  -=-=-=-=-=-=-=-=-  add player data  -=-=-=-=-=-=-=-=-
		if( params.containsKey("pd") )
		{
			query = "SELECT * FROM players WHERE id=" + playerId + " Limit 1;";
			ISFSArray dataArray = null;
			try {
				dataArray = dbManager.executeQuery(query, new Object[]{});
			} catch (SQLException e) { e.printStackTrace(); }
			params.putSFSObject("pd", dataArray.getSFSObject(0));
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
		try {
			params.putSFSArray("decks", dbManager.executeQuery("SELECT " + DBUtils.getInstance().liveDB + ".decks.`type`, " + DBUtils.getInstance().liveDB + ".resources.`level` FROM " + DBUtils.getInstance().liveDB + ".decks INNER JOIN " + DBUtils.getInstance().liveDB + ".resources ON " + DBUtils.getInstance().liveDB + ".decks.player_id = " + DBUtils.getInstance().liveDB + ".resources.player_id AND decks.`type` = " + DBUtils.getInstance().liveDB + ".resources.`type` WHERE " + DBUtils.getInstance().liveDB + ".decks.player_id = "+ playerId +" AND " + DBUtils.getInstance().liveDB + ".decks.deck_index = 0", new Object[]{}));
		} catch (SQLException e) { trace(e.getMessage()); }




		//trace(params.getDump());
		send(Commands.PROFILE, params, sender);
    }
}