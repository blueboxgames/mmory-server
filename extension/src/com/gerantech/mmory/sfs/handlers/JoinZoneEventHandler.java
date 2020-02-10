package com.gerantech.mmory.sfs.handlers;

import java.sql.Timestamp;
import java.time.Instant;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.LobbyUtils;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

/**
 * @author ManJav
 */
public class JoinZoneEventHandler extends BaseServerEventHandler
{
	public void handleServerEvent(ISFSEvent event) throws SFSException
	{
		User user = (User) event.getParameter(SFSEventParam.USER);
		Game game = ((Game) user.getSession().getProperty("core"));
		if( game == null )
			return;
			
			// Update player data
			String query = "UPDATE " + DBUtils.getInstance().liveDB + ".`players` SET `app_version`='" + game.appVersion + "', `sessions_count`='" + (game.sessionsCount+1) + "', `last_login`='" + Timestamp.from(Instant.now()) + "' WHERE `id`=" + game.player.id + ";";
		try {
		getParentExtension().getParentZone().getDBManager().executeUpdate(query, new Object[] {});
	} catch (Exception | Error e) { e.printStackTrace(); }

		// Find last joined lobby room
		Room lobby = LobbyUtils.getInstance().getLobby(game.player.id);
		try {
			if( lobby != null )
				getApi().joinRoom(user, lobby);
		} catch (SFSJoinRoomException e) { e.printStackTrace(); }
	}
}