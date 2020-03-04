package com.gerantech.mmory.sfs.battle.handlers;

import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleLeaveRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		try {
			BattleRoom room = (BattleRoom) BattleUtils.getInstance().rooms.get(params.getInt("r"));
			if( room.getState() <= BattleField.STATE_5_DISPOSED )
				room.leave(sender, params.containsKey("retryMode"));
		} catch (Exception | Error e) { e.printStackTrace(); };
	}
}