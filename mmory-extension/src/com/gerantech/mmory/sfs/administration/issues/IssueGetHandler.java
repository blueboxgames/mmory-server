package com.gerantech.mmory.sfs.administration.issues;

import java.sql.SQLException;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 *
 */
public class IssueGetHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
			return;

    	String query = "SELECT bugs.id, bugs.player_id, bugs.description, bugs.status, UNIX_TIMESTAMP(bugs.report_at) as date, players.name as sender FROM bugs INNER JOIN players ON bugs.player_id = players.id";
    	if( params.containsKey("id") )
			query += " WHERE bugs.player_id=" + params.getInt("id");
		query += " ORDER BY bugs.id DESC LIMIT 100;";

 		try {
			params.putSFSArray("issues", getDBManager().executeQuery(query, new Object[] {}));
		} catch (SQLException e) { e.printStackTrace(); }
		send(Commands.ISSUE_GET, params, sender);
	}
}