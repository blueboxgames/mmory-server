package com.gerantech.mmory.sfs.administration;

import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.sfs.utils.PasswordGenerator;
import com.gerantech.mmory.core.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class RestoreHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));

		if( params.containsKey("code") )
		{
			int playerId = PasswordGenerator.validateAndRecoverPId(params.getText("code"));
			if( playerId > -1 ) {
				try {
					ISFSArray res = getParentExtension().getParentZone().getDBManager().executeQuery("SELECT password FROM players WHERE id=" + playerId + "", new Object[]{});
					if( res.size() == 1 )
						params.putText("password", res.getSFSObject(0).getText("password"));
					else
						playerId = -1;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			params.putInt( "id", playerId);
			send(Commands.RESTORE, params, sender);
			return;
		}

		params.putText("restoreCode", PasswordGenerator.getRestoreCode(game.player.id));
		send(Commands.RESTORE, params, sender);
    }
}