package com.gerantech.mmory.sfs.socials.handlers;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.utils.FriendsUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * FriendDataHandler Created by ManJav on 2/6/2020.
 */
public class FriendDataHandler extends BBGClientRequestHandler {
  public void handleClientRequest(User sender, ISFSObject params) {
    params.putSFSArray("friends", FriendsUtils.getInstance().getFriendList(((Game)sender.getSession().getProperty("core")).player.id));
    send(Commands.BUDDY_DATA, params, sender);
  }
}