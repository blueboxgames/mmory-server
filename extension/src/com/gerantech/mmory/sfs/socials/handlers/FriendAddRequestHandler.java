package com.gerantech.mmory.sfs.socials.handlers;

import java.sql.SQLException;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.socials.Friends;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.data.RankData;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.gerantech.mmory.libs.utils.FCMUtils;
import com.gerantech.mmory.libs.utils.FriendsUtils;
import com.gerantech.mmory.libs.utils.InboxUtils;
import com.gerantech.mmory.libs.utils.RankingUtils;
import com.gerantech.mmory.sfs.utils.PasswordGenerator;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * Created by Babak on 8/19/2017.
 */
public class FriendAddRequestHandler extends BBGClientRequestHandler {

    public void handleClientRequest(final User sender, final ISFSObject params) {
        Game game = ((Game) sender.getSession().getProperty("core"));

        String invitationCode = params.getText("ic");

        int inviteeId = game.player.id;
        int inviterId;
        try {
            inviterId = PasswordGenerator.recoverPlayerId(invitationCode);
        } catch (Exception e) {
            send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
            return;
        }

        // Case 1:
        // User wants to add himself to get rewards
        if (inviterId == inviteeId) {
            send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_NOT_ALLOWED, params, sender);
            return;
        }

        // Case 2:
        // Invalid invitation code
        RankData inviterRank = RankingUtils.getInstance().getUsers().get(inviterId);
        if( inviterRank == null )
        {
            send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
            return;
        }
        params.putText("inviter", inviterRank.name);
        trace(inviterId, inviteeId, inviterRank.name);

        // Case 3:
        // Invitee player has been already added to inviter's friend list
        Friends friendship = FriendsUtils.getInstance().getFriendship(inviterId, inviteeId, Friends.STATE_ANY);
        if( friendship != null && friendship.state == Friends.STATE_NORMAL )
        {
            send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
            return;
        }

        // Case 4:
        // Users can't get rewards when was friends with another user with the same UDID
        final String inviteeUDID = params.containsKey("udid") ? params.getText("udid") : null;
        if( inviteeUDID != null )
        {
            boolean existsUDID = false;
            try {
                existsUDID = getDBManager().executeQuery("SELECT COUNT(*) FROM devices WHERE udid='" + inviteeUDID + "'", new Object[]{}).size() > 1;
            } catch (final SQLException e) { e.printStackTrace(); }
            if( existsUDID )
            {
                send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_NOT_ALLOWED, params, sender);
                return;
            }
        }

        // Finaly create friendship
        int inviterStep = game.friendRoad.calculateStep(RankingUtils.getInstance().getPoint(inviteeId));
        int inviteeStep = game.friendRoad.calculateStep(RankingUtils.getInstance().getPoint(inviterId));
        if( friendship == null )
        {
            FriendsUtils.getInstance().create(inviterId, inviteeId, -1, -1);
        }
        else
        {
            // reach to one step before current
            if( friendship.inviterStep < inviterStep )
                friendship.inviterStep = inviterStep;
            if( friendship.inviteeStep < inviteeStep )
                friendship.inviteeStep = inviteeStep;
            friendship.state = Friends.STATE_NORMAL;
            FriendsUtils.getInstance().update(friendship);
        }
    
        // Send friendship notification to inviter inbox
        String msg = game.player.nickName + " باهات رفیق شد. ";
        InboxUtils.getInstance().send(MessageTypes.M50_URL, msg, BanUtils.SYSTEM_ID, inviterId, "k2k://open?controls=tabs&dashTab=3&socialTab=2" );
        FCMUtils.getInstance().send(msg, "k2k://open?controls=tabs&dashTab=3&socialTab=2", inviterId);
        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_SUCCEED, params, sender);
    }
}