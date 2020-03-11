package com.gerantech.mmory.sfs.battle.handlers;

import java.util.List;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.data.UnitData;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.libs.utils.RankingUtils;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleRequestStartHandler extends BaseClientRequestHandler {
	BattleRoom room;

	public void handleClientRequest(User sender, ISFSObject params) {
		this.room = (BattleRoom) BattleUtils.getInstance().getRoom(params.getInt("r"));
		
		sender.setProperty("lastBattleStared", room.getId());
		for( User u : room.getPlayersList() )
			if( !u.containsProperty("lastBattleStared") || (int)u.getProperty("lastBattleStared") != room.getId() )
				return;
		if( this.room.getState() > BattleField.STATE_1_CREATED )
			this.sendBattleData(sender);
		else
			this.sendStartBattleResponse();
	}

	private void sendStartBattleResponse() {
		this.room.start();
		List<User> players = this.room.getPlayersList();
		for (int i = 0; i < players.size(); i++)
			this.sendBattleData(players.get(i));
	}

	private void sendBattleData(User user) {
		if (user.isNpc())
			return;

		Game game = (Game) user.getSession().getProperty("core");
		if (game.player.isBot())
			return;

		SFSObject params = new SFSObject();
		params.putDouble("now", room.battleField.now);
		params.putDouble("startAt", (double)room.battleField.startAt * 1000);
		params.putSFSArray("units", UnitData.toSFSArray(room.battleField.units));
		if (!room.isSpectator(user))
			RankingUtils.getInstance().update(game.player.id, game.player.nickName, game.player.get_point(), this.room.getId());
		send(SFSCommands.BATTLE_START, params, user);
	}
}