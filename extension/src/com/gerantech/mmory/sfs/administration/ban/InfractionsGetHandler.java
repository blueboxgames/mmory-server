package com.gerantech.mmory.sfs.administration.ban;

import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.gerantech.mmory.core.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class InfractionsGetHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
			return;
		params.putSFSArray("data", BanUtils.getInstance().getInfractions(params.containsKey("id") ? params.getInt("id") : 0, -1, 200, null));
		send(Commands.INFRACTIONS_GET, params, sender);
    }
}