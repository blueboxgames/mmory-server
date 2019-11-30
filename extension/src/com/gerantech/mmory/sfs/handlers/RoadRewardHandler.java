package com.gerantech.mmory.sfs.handlers;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.callbacks.MapChangeCallback;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.ExchangeUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 */
public class RoadRewardHandler extends BBGClientRequestHandler
{
	public RoadRewardHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = (Game) sender.getSession().getProperty("core");
		MapChangeCallback mapChangeCallback = new MapChangeCallback();
		game.player.resources.changeCallback = mapChangeCallback;
		int response = -10;
		try {
			response = game.player.achieveReward(params.getInt("l"), params.getInt("i"));
		} catch (Exception | Error e) { e.printStackTrace(); }
		game.player.resources.changeCallback = null;
		if( response != MessageTypes.RESPONSE_SUCCEED )
		{
			send(Commands.COLLECT_ROAD_REWARD, response, params, sender);
			return;
		}
		DBUtils dbUtils = DBUtils.getInstance();
		dbUtils.updateResources(game.player, mapChangeCallback.updates);
		dbUtils.insertResources(game.player, mapChangeCallback.inserts);
		ISFSArray outcomes = ExchangeUtils.getInstance().getRewards(mapChangeCallback);
		if( outcomes.size() > 0 )
			params.putSFSArray("outcomes", outcomes);
		send(Commands.COLLECT_ROAD_REWARD, response, params, sender);
	}
}