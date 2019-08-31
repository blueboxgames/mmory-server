package com.gerantech.mmory.sfs.battle.handlers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.exchanges.ExchangeItem;
import com.gerantech.mmory.core.scripts.ScriptEngine;
import com.gerantech.mmory.core.socials.Challenge;
import com.gerantech.mmory.core.utils.maps.IntIntMap;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.data.UnitData;
import com.gerantech.mmory.libs.utils.ExchangeUtils;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.buddylist.SFSBuddyVariable;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;
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

			if (room.isSpectator(user)) {
				sendBattleData(user);
				return;
			}

			join();
		} catch (Error | Exception e) { e.printStackTrace(); }
	}

	private void join() {
		// Rejoin to previous room
		if (room.getPropertyAsInt("state") == BattleField.STATE_2_STARTED) {
			List<User> players = room.getPlayersList();
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).equals(user)) {
					sendBattleData(players.get(i));
				}
				/*
				 * else if( !players.get(i).isNpc() ) { SFSObject sfsO = new SFSObject();
				 * sfsO.putText("user", ((Game)
				 * user.getSession().getProperty("core")).player.nickName); send("battleRejoin",
				 * sfsO, players.get(i)); }
				 */
			}
			return;
		}

		if (room.isFull()) {
			sendStartBattleResponse(false);
			return;
		}

		// Wait to match making ( complete battle-room`s players )
		if (room.getPropertyAsInt("friendlyMode") == 0) {
			int delay = 5000;// Math.max(12000, player.get_arena(0) * 400 + 7000);
			// trace(room.getName(), waitingPeak, room.getPlayersList().size(),
			// room.getOwner().getName());

			room.autoJoinTimer = SmartFoxServer.getInstance().getTaskScheduler().schedule(new TimerTask() {
				@Override
				public void run() {
					cancel();
					room.autoJoinTimer.cancel(true);
					room.setMaxUsers(1);
					try { sendStartBattleResponse(true); } catch (Error | Exception e) { e.printStackTrace(); }
				}
			}, delay, TimeUnit.MILLISECONDS);
		}
	}

	private void sendStartBattleResponse(Boolean opponentNotFound){
		room.setProperty("startAt", (int) Instant.now().getEpochSecond());
		room.createGame(opponentNotFound);

		List<User> players = room.getPlayersList();
		for (int i = 0; i < players.size(); i++)
			sendBattleData(players.get(i));
	}

	private void sendBattleData(User user) {
		if (user.isNpc())
			return;

		Game game = (Game) user.getSession().getProperty("core");
		if (game.player.isBot())
			return;

		SFSObject params = new SFSObject();

		// reduce battle cost
		if( room.battleField.friendlyMode == 0 && game.player.get_battleswins() > 4 )
		{
			IntIntMap cost = new IntIntMap((String)ScriptEngine.get(ScriptEngine.T52_CHALLENGE_RUN_REQS, room.getPropertyAsInt("type"), null, null, null));
			ExchangeItem exItem = Challenge.getExchangeItem(room.getPropertyAsInt("mode"), cost, game.player.get_arena(0));
			int response = ExchangeUtils.getInstance().process(game, exItem, 0, 0);
			if (response != MessageTypes.RESPONSE_SUCCEED)
			{
				params.putInt("response", response);
				send(Commands.BATTLE_START, params, user);
				return;
			}
		}

		params.putInt("side", room.getPlayerGroup(user));
		params.putInt("startAt", room.battleField.startAt);
		params.putInt("roomId", room.getId());
		params.putInt("userType", room.getUserType(user));
		params.putDouble("now", room.battleField.now);
		params.putText("map", room.battleField.field.mapData);
		params.putInt("index", room.getPropertyAsInt("index"));
		params.putInt("type", room.getPropertyAsInt("type"));
		params.putInt("mode", room.getPropertyAsInt("mode"));
		params.putInt("friendlyMode", room.battleField.friendlyMode);
		params.putBool("singleMode", room.getPropertyAsBool("singleMode"));
		params.putSFSArray("units", UnitData.toSFSArray(room.battleField.units));
		boolean isSpectator = room.isSpectator(user);
		ArrayList<?> registeredPlayers = (ArrayList<?>) room.getProperty("registeredPlayers");
		int i = 0;
		for (Object o : registeredPlayers)
		{
			Player player = ((Game) o).player;
			SFSObject p = new SFSObject();
			p.putInt("id", player.id);
			p.putInt("xp", player.get_xp());
			p.putInt("point", player.get_point());
			p.putUtfString("name", player.nickName);

			String deck = "";
			Iterator<Object> iter = room.battleField.decks.get(i)._queue.iterator();
			while (iter.hasNext()) {
				int type = (int) iter.next();
				deck += (type + ":" + room.battleField.decks.get(i).get(type).level + (iter.hasNext() ? "," : ""));
			}
			p.putText("deck", deck);
			p.putInt("score", room.endCalculator.scores[i]);
			params.putSFSObject(i == 0 ? "p0" : "p1", p);

			i++;
		}

		send(Commands.BATTLE_START, params, user);

		if (!isSpectator) {
			user.getBuddyProperties().setState("Occupied");
			user.getBuddyProperties().setVariable(new SFSBuddyVariable("br", room.getId()));
			user.getBuddyProperties().setVariable(new SFSBuddyVariable("$point", user.getVariable("point").getIntValue()));

			try {
				getParentExtension().getBuddyApi().setBuddyVariables(user, user.getBuddyProperties().getVariables(), true, true);
			} catch (SFSBuddyListException e) {
				e.printStackTrace();
			}
		}
	}
}
