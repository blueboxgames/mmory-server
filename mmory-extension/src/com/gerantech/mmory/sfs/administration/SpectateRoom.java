package com.gerantech.mmory.sfs.administration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.data.LobbySFS;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.libs.utils.LobbyUtils;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

import haxe.root.Array;

public class SpectateRoom extends BBGRoom {
	private ScheduledFuture<?> timer;
	List<Integer> roomIds = new ArrayList<>();

	public void init(int id, CreateRoomSettings settings) {
		super.init(id, settings);
		this.roomIds = new ArrayList<>();

		LobbyUtils lobbyUtils = LobbyUtils.getInstance();
		List<Integer> newIds = new ArrayList<>();

		timer = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				trace("run");
				newIds.clear();
				Set<Map.Entry<Integer, BBGRoom>> entries = BattleUtils.getInstance().getRooms().entrySet();
				for (Map.Entry<Integer, BBGRoom> entry : entries)
					if (entry.getValue().getState() == BattleField.STATE_2_STARTED)
						newIds.add(entry.getValue().getId());

				if (roomIds.equals(newIds))
					return;

				SFSObject battle;
				SFSArray battles = new SFSArray();
				SFSObject params = new SFSObject();
				int end = Math.max(0, newIds.size() - 20);
				for (int i = newIds.size() - 1; i >= end; i--) {
					BattleRoom r = (BattleRoom) BattleUtils.getInstance().getRoom(newIds.get(i));
					trace(r);
					battle = new SFSObject();
					battle.putInt("id", r.getId());
					battle.putText("name", r.getName());
					battle.putInt("startAt", r.battleField.startAt);
					ISFSArray players = new SFSArray();

					Array<Game> games = r.battleField.games;
					for (int g = 0; g < games.length; g++) {
						Player player = games.__get(g).player;
						SFSObject p = new SFSObject();
						p.putInt("i", player.id);
						p.putText("n", player.nickName);
						LobbySFS lobby = lobbyUtils.getDataByMember(player.id);
						if (lobby != null) {
							p.putText("ln", lobby.getName());
							p.putInt("lp", lobby.getEmblem());
						}
						players.addSFSObject(p);
					}
					battle.putSFSArray("players", players);
					battles.addSFSObject(battle);
				}
				params.putSFSArray("rooms", battles);
				send(SFSCommands.SPECTATE_UPDATE, params, getUserList());
				roomIds = newIds;
			}
		}, 0, 1, TimeUnit.SECONDS);
	}

	private boolean isChanged(List<Integer> newRooms) {
		if (roomIds.equals(newRooms))
			return true;
		// trace(reservedRooms.size(), newRooms.size());

		/*
		 * for (int i = 0; i < reservedRooms.size(); i++) if(
		 * !reservedgetRoomSFSObject(i).getText("users").equals(newgetRoomSFSObject(i).
		 * getText("users")) ) return true;
		 */
		return false;
	}

	public void destroy() {
		// clearAllHandlers();
		if (timer != null)
			timer.cancel(true);
		timer = null;
		trace(getName(), "destroyed.");
		super.destroy();
	}
}