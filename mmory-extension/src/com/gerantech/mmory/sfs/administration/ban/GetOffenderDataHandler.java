package com.gerantech.mmory.sfs.administration.ban;

import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 */
public class GetOffenderDataHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
		{
			send(Commands.OFFENDER_DATA_GET, MessageTypes.RESPONSE_NOT_ALLOWED, params, sender);
			return;
		}

		// get ban count
		ISFSArray bannes = BanUtils.getInstance().getBannedUsers(params.getInt("id"), null, null, 2, 0, "time");
		params.putInt("time", bannes.size() > 0 ? bannes.getSFSObject(0).getInt("time") : 0);

		// get all opened infractions
		params.putSFSArray("infractions", BanUtils.getInstance().getInfractions(params.getInt("id"), 0, 5, "infractions.content, infractions.offend_at"));

		send(Commands.OFFENDER_DATA_GET, MessageTypes.RESPONSE_SUCCEED, params, sender);
	}
}