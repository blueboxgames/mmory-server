package com.gerantech.mmory.sfs.administration.ban;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;

/**
 * @author ManJav
 */
public class BanHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
			return;
		// get name
		ISFSArray players = null;
		String query = "SELECT name FROM players WHERE id=" + params.getInt("id");
		try {
			players = getDBManager().executeQuery(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }
		if( players == null || players.size() == 0 )
		{
			send(Commands.BAN, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
			return;
		}

		ISFSObject device = DBUtils.getInstance().getDevice(params.getInt("id"));
		BanUtils.getInstance().warnOrBan(params.getInt("id"), device.getText("udid"), device.containsKey("imei") ? device.getText("imei") : null, params.getInt("mode"), (int)Instant.now().getEpochSecond(), params.getInt("len"), params.getText("msg"));
		send(Commands.BAN, MessageTypes.RESPONSE_SUCCEED, params, sender);

		if( params.getInt("mode") >= 2 )
		{
			User u = getParentExtension().getParentZone().getUserByName(params.getInt("id")+"");
			if( u != null )
			{
				List<Room> publics = getParentExtension().getParentZone().getRoomListFromGroup("publics");
				for (Room p : publics)
				{
					SFSObject msg = new SFSObject();
					msg.putInt("m",  MessageTypes.M18_COMMENT_BAN);
					msg.putUtfString("s", "ادمین");
					msg.putUtfString("o", players.getSFSObject(0).getUtfString("name"));
					msg.putInt("p",  -1);
					getApi().sendExtensionResponse(Commands.LOBBY_PUBLIC_MESSAGE, msg, p.getUserList(), p, false);
					p.getVariable("msg").getSFSArrayValue().addSFSObject(msg);

				}
				getApi().disconnectUser(u);
			}
		}
	}
}