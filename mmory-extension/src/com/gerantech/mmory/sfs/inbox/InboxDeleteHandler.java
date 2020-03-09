package com.gerantech.mmory.sfs.inbox;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class InboxDeleteHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
  {
		// Game game = ((Game)sender.getSession().getProperty("core"));
		// params.putSFSArray("data", InboxUtils.getInstance().getAll(game.player.id, 30));
		// send(SFSCommands.INBOX_GET, params, sender);
	}
}