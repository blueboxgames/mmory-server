package com.gerantech.mmory.sfs.socials.handlers;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.socials.Friends;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.utils.FriendsUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * Created by ManJav on 8/24/2017.
 */
public class FriendRemoveRequestHandler extends BBGClientRequestHandler {
	public void handleClientRequest(User sender, ISFSObject params) {
		int senderId = ((Game) sender.getSession().getProperty("core")).player.id;
		Friends friendship = FriendsUtils.getInstance().getFriendship(senderId, params.getInt("id"), Friends.STATE_NORMAL);
		if( friendship == null )
		{
			send(Commands.BUDDY_REMOVE, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
			return;
		}
		friendship.state = Friends.STATE_REMOVE;
		FriendsUtils.getInstance().update(friendship);
		send(Commands.BUDDY_REMOVE, MessageTypes.RESPONSE_SUCCEED, params, sender);
	}
}