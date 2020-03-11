package com.gerantech.mmory.sfs.battle.handlers;

import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleRequestLeaveHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		try {
			BattleRoom room = (BattleRoom) BattleUtils.getInstance().getRoom(params.getInt("r"));
			room.leave(sender, params.containsKey("retryMode"));
		} catch (Exception | Error e) { e.printStackTrace(); };
	}
}