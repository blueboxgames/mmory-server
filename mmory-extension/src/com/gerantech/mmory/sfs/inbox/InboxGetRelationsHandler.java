package com.gerantech.mmory.sfs.inbox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.utils.InboxUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 *
 */
public class InboxGetRelationsHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));
		int me = params.containsKey("me") ?  params.getInt("me") : game.player.id;
		ISFSArray relations = InboxUtils.getInstance().getRelations(me, params.getInt("id"), game.player.admin ? 100 : 40);
		List<Integer> ids = new ArrayList<>();
		for (int i = 0; i < relations.size(); i++)
		{
			ISFSObject rel = relations.getSFSObject(i);
			if( rel.getInt("status") == 0 && rel.containsKey("senderId") && rel.getInt("senderId") != me )
				ids.add(rel.getInt("id"));
			rel.removeElement("id");
		}

		// set read status
		if( ids.size() > 0 )
		{
			String query = "UPDATE messages SET status=1 WHERE";
			for (int i = 0; i < ids.size(); i++) {
				query += " id=" + ids.get(i);
				if (i < ids.size() - 1)
					query += " OR";
			}
			try {
				getDBManager().executeUpdate(query, new Object[]{});
			} catch (SQLException e) { e.printStackTrace(); }
		}

		params.putSFSArray("data", relations);
		send(Commands.INBOX_GET_RELATIONS, params, sender);
	}
}