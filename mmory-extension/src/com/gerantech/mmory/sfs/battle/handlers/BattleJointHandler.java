package com.gerantech.mmory.sfs.battle.handlers;

import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.core.exchanges.ExchangeItem;
import com.gerantech.mmory.core.scripts.ScriptEngine;
import com.gerantech.mmory.core.socials.Challenge;
import com.gerantech.mmory.core.utils.maps.IntIntMap;
import com.gerantech.mmory.libs.utils.ExchangeUtils;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class BattleJointHandler extends BaseServerEventHandler {
	private User user;
	private BattleRoom room;

	public void handleServerEvent(ISFSEvent arg) {
		try {
			user = (User) arg.getParameter(SFSEventParam.USER);
			if (!arg.getParameter(SFSEventParam.ROOM).getClass().getSimpleName().equals("BattleRoom"))
				return;

			room = (BattleRoom) arg.getParameter(SFSEventParam.ROOM);
			if (!arg.getType().equals(SFSEventType.USER_JOIN_ROOM) || room == null)
				return;

			// Rejoin to previous room
			if (room.getState() > BattleField.STATE_0_WAITING) {
				this.sendBattleData(user);
				return;
			}

			join();
		} catch (Error | Exception e) { e.printStackTrace(); }
	}

	private void join() {

		if (room.isFull()) {
			sendStartBattleResponse(false);
			return;
		}

		// Wait to match making ( complete battle-room`s players )
		if (room.battleField.friendlyMode == 0) {
			int delay = 5000;// Math.max(12000, player.get_arena(0) * 400 + 7000);
			// trace(room.getName(), waitingPeak, room.getPlayersList().size(),

			room.autoJoinTimer = SmartFoxServer.getInstance().getTaskScheduler().schedule(new TimerTask() {
				@Override
				public void run() {
					cancel();
					room.autoJoinTimer.cancel(true);
					room.setMaxUsers(1);
					try {
						sendStartBattleResponse(true);
					} catch (Error | Exception e) { e.printStackTrace(); }
				}
			}, delay, TimeUnit.MILLISECONDS);
		}
	}

	private void sendStartBattleResponse(Boolean opponentNotFound) {
		
		this.room.create(opponentNotFound);
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

		// reduce battle cost
		if (room.battleField.friendlyMode == 0 && game.player.get_battleswins() > 4) {
			IntIntMap cost = new IntIntMap((String) ScriptEngine.get(ScriptEngine.T52_CHALLENGE_RUN_REQS, room.getPropertyAsInt("index"), null, null, null));
			ExchangeItem exItem = Challenge.getExchangeItem(room.getPropertyAsInt("mode"), cost, game.player.get_arena(0));
			int response = ExchangeUtils.getInstance().process(game, exItem, 0, 0);
			if (response != MessageTypes.RESPONSE_SUCCEED) {
				params.putInt("response", response);
				send(SFSCommands.BATTLE_JOIN, params, user);
				return;
			}
		}

		params.putInt("id", room.getId());
		params.putInt("side", room.getPlayerGroup(user));
		params.putInt("userType", room.getUserType(user));
		params.putInt("index", room.getPropertyAsInt("index"));
		params.putInt("type", room.getPropertyAsInt("type"));
		params.putInt("mode", room.getPropertyAsInt("mode"));
		params.putInt("createAt", room.battleField.createAt);
		params.putInt("friendlyMode", room.battleField.friendlyMode);
		params.putBool("singleMode", room.battleField.singleMode);
		params.putBool("debugMode", room.battleField.debugMode);

		for ( int i = 0; i < room.battleField.games.length; i++ )
		{
			Player player = room.battleField.games.__get(i).player;
			SFSObject p = new SFSObject();
			p.putInt("id", player.id);
			p.putInt("xp", player.get_xp());
			p.putInt("point", player.get_point());
			p.putUtfString("name", player.nickName);

			String deck = "";
			int qlen = room.battleField.decks.get(i)._queue.length;
			for (int k = 0; k < qlen; k++) {
				int t = room.battleField.decks.get(i)._queue[k];
				deck += (t + ":" + room.battleField.decks.get(i).get(t).level + (k < qlen - 1 ? "," : ""));
			}
			p.putText("deck", deck);
			p.putInt("score", room.endCalculator.scores[i]);
			params.putSFSObject(i == 0 ? "p0" : "p1", p);
		}

		send(SFSCommands.BATTLE_JOIN, params, user);
	}
}
