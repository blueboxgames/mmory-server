package com.gerantech.mmory.sfs.battle.handlers;

import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleSummonRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		try {
			BattleRoom room = (BattleRoom) BattleUtils.getInstance().rooms.get(params.getInt("r"));
			if( room.getState() < BattleField.STATE_1_CREATED || room.getState() > BattleField.STATE_3_PAUSED )
				return;
			int side = room.getPlayerGroup(sender);
			double time = params.containsKey("time") ? params.getDouble("time") : System.currentTimeMillis() - 200;
			room.summonUnit(side, params.getInt("t"), params.getDouble("x"), params.getDouble("y"), time);
	} catch (Exception | Error e) { e.printStackTrace(); };
}
}