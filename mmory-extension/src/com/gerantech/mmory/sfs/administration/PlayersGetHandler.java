package com.gerantech.mmory.sfs.administration;

import java.sql.SQLException;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.sfs.utils.PasswordGenerator;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 *
 */
public class PlayersGetHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = (Game) sender.getSession().getProperty("core");
		if( !game.player.admin )
			return;
		String query = "SELECT id, name, password, app_version, sessions_count, DATE_FORMAT(create_at, '%y-%m-%d  %h:%i:%s') create_at, DATE_FORMAT(last_login, '%y-%m-%d  %h:%i:%s') last_login FROM players WHERE ";
		if( params.containsKey("id") )
			query += "id=" + params.getInt("id");
		else if( params.containsKey("tag") )
			query += "id=" + PasswordGenerator.recoverPlayerId(params.getText("tag"));
		else if( params.containsKey("name") )
			query += "name LIKE '%" + params.getUtfString("name") + "%'";
		else
			return;

		query += " ORDER BY id DESC LIMIT 500;";
		trace(query);
		try {
			params.putSFSArray("players", getDBManager().executeQuery(query , new Object[] {}));
		} catch (SQLException e) { e.printStackTrace(); }
		send(SFSCommands.PLAYERS_GET, params, sender);
	}
}