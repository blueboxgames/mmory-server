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
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.data.LobbySFS;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.libs.utils.LobbyUtils;
import com.gerantech.mmory.sfs.battle.handlers.BattleLeaveRequestHandler;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class SpectateRoom extends SFSExtension
{
	private Room room;
	private ScheduledFuture<?> timer;

	public void init() 
	{
		room = getParentRoom();
		addRequestHandler(SFSCommands.BATTLE_LEAVE, BattleLeaveRequestHandler.class);
		List<RoomVariable> listOfVars = new ArrayList<>();
		listOfVars.add( new SFSRoomVariable("rooms", SFSArray.newInstance()) );
		sfsApi.setRoomVariables(null, room, listOfVars);
		LobbyUtils lobbyUtils = LobbyUtils.getInstance();

		timer = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new TimerTask() {
		@Override
		public void run() {
			ISFSArray reservedRooms = room.getVariable("rooms").getSFSArrayValue();
			SFSArray battles = SFSArray.newInstance();
			SFSObject battle;
			int numRooms = 0;
			Set<Map.Entry<Integer, BBGRoom>> entries = BattleUtils.getInstance().rooms.entrySet();
			for( Map.Entry<Integer, BBGRoom> entry : entries )
			{
				BBGRoom r = entry.getValue();
				if( r.getPropertyAsInt("state") != BattleField.STATE_2_STARTED )
					continue;
				battle = new SFSObject();
				battle.putInt("id", r.getId());
				battle.putText("name", r.getName());
				battle.putInt("startAt", r.getPropertyAsInt("startAt"));
				ISFSArray players = new SFSArray();

				ArrayList<?> registeredPlayers = (ArrayList<?>)r.getProperty("registeredPlayers");
				for ( Object obj : registeredPlayers )
				{
					Player player = ((Game)obj).player;
					SFSObject p = new SFSObject();
					p.putText("n", player.nickName);
					LobbySFS lobby = lobbyUtils.getDataByMember(player.id);
					if( lobby != null )
					{
						p.putText("ln", lobby.getName());
						p.putInt("lp", lobby.getEmblem());
					}
					players.addSFSObject(p);
				}
				battle.putSFSArray("players", players);

				battles.addSFSObject(battle);
				numRooms ++;
				if( numRooms > 20 )
					break;
			}

			if( isChanged(reservedRooms, battles) )
			{
				List<RoomVariable> listOfVars = new ArrayList<>();
				listOfVars.add(new SFSRoomVariable("rooms", battles));
				sfsApi.setRoomVariables(null, room, listOfVars);
			}
		}


		}, 0, 1, TimeUnit.SECONDS );
		trace(room.getName(), "created.");
	}

	private boolean isChanged(ISFSArray reservedRooms, SFSArray newRooms)
	{
		if( reservedRooms.size() != newRooms.size() )
			return true;
		//trace(reservedRooms.size(), newRooms.size());

		/*for (int i = 0; i < reservedRooms.size(); i++)
			if( !reservedRooms.getSFSObject(i).getText("users").equals(newRooms.getSFSObject(i).getText("users")) )
				return true;*/
		return false;
	}

	public void destroy()
	{
		clearAllHandlers();
		if( timer != null )
			timer.cancel(true);
		timer = null;
		trace(room.getName(), "destroyed.");
		super.destroy();
	}
}