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
public class IssueReportHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		params.putBool("succeed", true);
		Game game = ((Game)sender.getSession().getProperty("core"));

		String email = params.getText("email");
		String description = params.getUtfString("description");
		//trace(game.player.id, email, description);

		try {
			getDBManager().executeInsert("INSERT INTO bugs (`player_id`, `email`, `description`) VALUES ('" + game.player.id + "', '" + email + "', '" + description + "');", new Object[] {});
		} catch (SQLException e) {
			params.putBool("succeed", false);
			params.putText("errorCode", e.getErrorCode()+"");
			trace(e.getMessage());
		}
		send(Commands.ISSUE_REPORT, params, sender);
	}
}