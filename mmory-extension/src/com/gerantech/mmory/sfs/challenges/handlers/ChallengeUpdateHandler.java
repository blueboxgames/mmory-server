package com.gerantech.mmory.sfs.challenges.handlers;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.utils.ChallengeUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class ChallengeUpdateHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        params.putSFSArray("attendees", ChallengeUtils.getInstance().get(params.getInt("id")).getAttendees());
        send(Commands.CHALLENGE_UPDATE, params, sender);
    }
}