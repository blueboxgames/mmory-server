package com.gerantech.mmory.sfs.handlers;

import java.sql.SQLException;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 *
 */
public class OauthHandler extends BBGClientRequestHandler 
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		int playerId = ((Game)sender.getSession().getProperty("core")).player.id;
		try
		{
			// retrieve user that saved account before
			ISFSArray accounts = getDBManager().executeQuery("SELECT type, player_id FROM accounts WHERE social_id='" + params.getText("accountId") + "'", new Object[] {});
			boolean needInsert = accounts.size() == 0;
			if( !needInsert && playerId != accounts.getSFSObject(0).getInt("player_id") )// if exists player and his id equals player id
	        {
				playerId = accounts.getSFSObject(0).getInt("player_id");
				ISFSArray players = getDBManager().executeQuery("SELECT name, password FROM players WHERE id=" + playerId, new Object[] {});
				if( players.size() > 0 )
				{
					params.putText("playerName",		players.getSFSObject(0).getText("name"));
					params.putText("playerPassword",	players.getSFSObject(0).getText("password"));
				}
				needInsert = !linkExists(accounts, params.getInt("accountType")); // already stored
			}

			if( needInsert )
				getDBManager().executeInsert("INSERT INTO accounts (`player_id`, `type`, `social_id`, `name`, `image_url`) VALUES ('" + playerId + "', '" + params.getInt("accountType") + "', '" + params.getText("accountId") + "', '" + params.getText("accountName") + "', '" + params.getText("accountImageURL") + "');", new Object[] {});

		} catch (SQLException e) { e.printStackTrace(); }

		params.putInt("playerId", playerId);
		send(SFSCommands.OAUTH, params, sender);
	}

	private boolean linkExists(ISFSArray accounts, int accountType )
	{
		boolean ret = false;
		for(int i=0; i<accounts.size(); i++)
			if( accounts.getSFSObject(i).getInt("type") == accountType )
				ret = true;
		return ret;
	}
}