package com.gerantech.mmory.sfs.administration.ban;

import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

import java.sql.SQLException;

/**
 * @author ManJav
 */
public class GetBannedDataHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
		{
			send(SFSCommands.BANNED_DATA_GET, MessageTypes.RESPONSE_NOT_ALLOWED, params, sender);
			return;
		}

		// get device
		ISFSObject device = DBUtils.getInstance().getDevice(params.getInt("id"));

		// create query
		String query = "SELECT players.id, players.name, banneds.message, banneds.mode, banneds.expire_at, banneds.timestamp, banneds.time FROM players INNER JOIN banneds ON players.id = banneds.player_id WHERE players.id = " + params.getInt("id");
		if( device != null && device.getText("udid") != null )
			query += " OR banneds.udid = '" + device.getText("udid") + "'";
		trace(query);
		ISFSArray bannes = null;
		try {
			bannes = getDBManager().executeQuery(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }

		// get all ban messages
		params.putSFSArray("data", bannes);

		send(SFSCommands.BANNED_DATA_GET, MessageTypes.RESPONSE_SUCCEED, params, sender);
	}
}