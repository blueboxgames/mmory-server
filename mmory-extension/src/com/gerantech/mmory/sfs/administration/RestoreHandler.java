package com.gerantech.mmory.sfs.administration;

import java.sql.SQLException;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.sfs.utils.PasswordGenerator;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 *
 */
public class RestoreHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( params.containsKey("code") )
		{
			int playerId = PasswordGenerator.validateAndRecoverPId(params.getText("code"));
			if( playerId > -1 ) {
				try {
					ISFSArray res = getDBManager().executeQuery("SELECT password FROM players WHERE id=" + playerId, new Object[]{});
					if( res.size() == 1 )
						params.putText("password", res.getSFSObject(0).getText("password"));
					else
						playerId = -1;
				} catch (SQLException e) { e.printStackTrace(); }
			}
			params.putInt( "id", playerId);
			send(Commands.RESTORE, params, sender);
			return;
		}

		params.putText("restoreCode", PasswordGenerator.getRestoreCode(game.player.id));
		send(Commands.RESTORE, params, sender);
	}
}