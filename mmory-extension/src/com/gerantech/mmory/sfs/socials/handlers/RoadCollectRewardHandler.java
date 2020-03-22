package com.gerantech.mmory.sfs.socials.handlers;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.others.TrophyReward;
import com.gerantech.mmory.core.socials.Friends;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.callbacks.MapChangeCallback;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.ExchangeUtils;
import com.gerantech.mmory.libs.utils.FriendsUtils;
import com.gerantech.mmory.libs.utils.RankingUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 */
public class RoadCollectRewardHandler extends BBGClientRequestHandler {
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = (Game) sender.getSession().getProperty("core");
		MapChangeCallback mapChangeCallback = new MapChangeCallback();
		game.player.resources.changeCallback = mapChangeCallback;
		int response = -10;
		if( params.getInt("l") > 10000 )
		{
			int left = game.player.id;
			int right = params.getInt("l");
			Friends friendship = FriendsUtils.getInstance().getFriendship(left, right, Friends.STATE_NORMAL);
			if( friendship == null )
			{
				send(SFSCommands.COLLECT_ROAD_REWARD, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
				return;
			}

			int step = friendship.inviter == left ? friendship.inviterStep : friendship.inviteeStep;
			TrophyReward reward = game.friendRoad.rewards.__get(params.getInt("i"));

			response = reward.achievable(RankingUtils.getInstance().getPoint(right), step, false);
			if( response != MessageTypes.RESPONSE_SUCCEED )
			{
				send(SFSCommands.COLLECT_ROAD_REWARD, response, params, sender);
				return;
			}
			
			game.player.resources.increase(reward.key, reward.value);
			if( friendship.inviter == left )
				friendship.inviterStep = step + 1;
			else
				friendship.inviteeStep = step + 1;
			FriendsUtils.getInstance().update(friendship);
		}
		else
		{
			response = game.player.achieveReward(params.getInt("l"), params.getInt("i"));
		}
		game.player.resources.changeCallback = null;
		if( response != MessageTypes.RESPONSE_SUCCEED )
		{
			send(SFSCommands.COLLECT_ROAD_REWARD, response, params, sender);
			return;
		}
		
		DBUtils.getInstance().updateResources(game.player, mapChangeCallback.updates);
		DBUtils.getInstance().insertResources(game.player, mapChangeCallback.inserts);
		ISFSArray outcomes = ExchangeUtils.getInstance().getRewards(mapChangeCallback);
		if( outcomes.size() > 0 )
			params.putSFSArray("outcomes", outcomes);
		send(SFSCommands.COLLECT_ROAD_REWARD, response, params, sender);
	}
}