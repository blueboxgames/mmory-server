package com.gerantech.mmory.sfs.challenges.handlers;

import java.time.Instant;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.ExchangeType;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.constants.ResourceType;
import com.gerantech.mmory.core.exchanges.ExchangeItem;
import com.gerantech.mmory.core.utils.maps.IntIntMap;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.callbacks.MapChangeCallback;
import com.gerantech.mmory.libs.data.ChallengeSFS;
import com.gerantech.mmory.libs.utils.ChallengeUtils;
import com.gerantech.mmory.libs.utils.ExchangeUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class ChallengeCollectRewardHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = ((Game)sender.getSession().getProperty("core"));
        int now = (int) Instant.now().getEpochSecond();
        int response = ChallengeUtils.getInstance().collectReward(params.getInt("id"), game.player.id, now);
        params.putInt("response", response);
        if( response != MessageTypes.RESPONSE_SUCCEED )
        {
            send(SFSCommands.CHALLENGE_COLLECT, params, sender);
            return;
        }

        // add rewards based on rank
        ChallengeSFS challenge = ChallengeUtils.getInstance().get(params.getInt("id"));
        challenge.representAttendees();

        ExchangeUtils manager = ExchangeUtils.getInstance();
        ExchangeItem ei = new ExchangeItem(null);
        ei.type = ExchangeType.C121_MAGIC;
        ei.requirements = new IntIntMap();
        ei.outcomes = challenge.base.getRewardByAttendee(game.player.id);
        int rewardType = ei.outcomes.keys()[0];
        if( ResourceType.isBook( rewardType ) )
            ei.outcomes.set(rewardType, game.player.get_arena(0));


        MapChangeCallback mapChangeCallback = new MapChangeCallback();
        response =  manager.process(game, ei, now, 0, mapChangeCallback);
        if( response != MessageTypes.RESPONSE_SUCCEED )
        {
            send(SFSCommands.CHALLENGE_COLLECT, params, sender);
            return;
        }

        params.putSFSArray("rewards", manager.getRewards(mapChangeCallback));
        send(SFSCommands.CHALLENGE_COLLECT, params, sender);
    }
}