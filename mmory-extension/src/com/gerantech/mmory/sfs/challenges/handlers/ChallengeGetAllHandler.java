package com.gerantech.mmory.sfs.challenges.handlers;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class ChallengeGetAllHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
       // Player player = ((Game)sender.getSession().getProperty("core")).player;
       // params.putSFSArray("challenges", ChallengeUtils.getInstance().getChallengesOfAttendee(-1, player, true));

        send(SFSCommands.CHALLENGE_GET_ALL, params, sender);
    }
}