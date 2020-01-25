package com.gerantech.mmory.sfs.socials.handlers;

import java.sql.SQLException;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.constants.PrefsTypes;
import com.gerantech.mmory.core.constants.ResourceType;
import com.gerantech.mmory.core.scripts.ScriptEngine;
import com.gerantech.mmory.core.socials.Lobby;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.sfs.utils.PasswordGenerator;
import com.smartfoxserver.v2.api.ISFSBuddyApi;
import com.smartfoxserver.v2.buddylist.BuddyList;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;

/**
 * BuddyRequestHandler
 */
public class BuddyRequestHandler extends BBGClientRequestHandler
{
    private IDBManager dM;
    private User reqSender;
    private int playerID;
    private User reqReciver;
    private int friendID;
    
    private ISFSObject params;

    /**
     * <pre>
     *sender:
     * request sender
     *params:
     * -> 1
     * refCode: ReferralCode,
     * -> 2
     * friend: friend id
     * status: { -1: block, 0: unfriend, 1: friendRequest 2: friendAccept },
     * </pre>
     */
    public void handleClientRequest(User sender, ISFSObject params)
    {
        this.dM = getParentExtension().getParentZone().getDBManager();
        this.reqSender = sender;
        this.params = params;
        Game game = ((Game)sender.getSession().getProperty("core"));
        /**----------------------------------------------------------------
        Case 1: Referral Request
            Adds as friend.
            Invitee resource increase.
        -------------------------------------------------------------------*/
        if( params.containsKey("refCode") )
        {
            String refCode = params.getText("refCode");
            // invalid referral code
            if( !PasswordGenerator.checkReferralCode(refCode) )
            {
                send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_NOT_ALLOWED, this.params, sender);
                return;
            }
            int fid = -1;
            int invitee = game.player.id;
            int inviter = PasswordGenerator.recoverPlayerId(params.getText("refCode"));
            
            playerID = invitee;
            friendID = inviter;
            reqReciver = getParentExtension().getParentZone().getUserManager().getUserByName(inviter+"");
            
            if( invitee == inviter )
            {
                send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
                return;
            }

            if( game.player.getTutorStep() == PrefsTypes.T_71_SELECT_NAME_FOCUS )
            {
                try {
                    game.player.resources.increase(ResourceType.R4_CURRENCY_HARD, Lobby.buddyInviteeReward);
                    dM.executeUpdate("UPDATE " + DBUtils.getInstance().liveDB + ".resources SET count=" + game.player.get_hards() + " WHERE type=" + ResourceType.R4_CURRENCY_HARD + " AND player_id=" + invitee, new Object[]{});
                    params.putInt("rewardType", ResourceType.R4_CURRENCY_HARD);
                    params.putInt("rewardCount", Lobby.buddyInviteeReward);
                } catch( SQLException e ) {
                    send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_UNKNOWN_ERROR, this.params, sender);
                }
            }

            fid = getFriendship(invitee, inviter, true);
            if( fid < 0 )
                return;
            
            if( acceptFriendRequest(fid, true) == 0 )
                send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_SUCCEED, this.params, sender);
            else
                send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_NOT_ALLOWED, this.params, sender);
        }
        /**----------------------------------------------------------------
        Case 2: Friendship status change request.
            Change friendship based on request status.
            -1: block , 0: unfriend, 1: pending, 2: friend
        -------------------------------------------------------------------*/
        else if( params.containsKey("friend") && params.containsKey("status") )
        {
            int fid = -1;
            int handleStatus = -1;
            playerID = game.player.id;
            friendID = params.getInt("friend");
            int status = params.getInt("status");
            reqReciver = getParentExtension().getParentZone().getUserManager().getUserByName(friendID+"");

            if( playerID == friendID )
            {
                send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
                return;
            }

            fid = getFriendship(playerID, friendID, false);
            
            if( fid < 0 )
                return;

            switch (status)
            {
                case -1:
                    handleStatus = blockRequest(fid);
                    if( handleStatus == 0 || handleStatus == 1 )
                        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_SUCCEED, this.params, sender);
                    else
                        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_UNKNOWN_ERROR, this.params, sender);
                    break;
                case 0:
                    if( unfriendRequest(fid) > 0 )
                        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_SUCCEED, this.params, sender);
                    else
                        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_UNKNOWN_ERROR, this.params, sender);
                    break;
                case 2:
                    handleStatus = acceptFriendRequest(fid, false);
                    if( handleStatus == 0 )
                        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_SUCCEED, this.params, sender);
                    else
                        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_NOT_ALLOWED, this.params, sender);
                    break;
                default:
                    handleStatus = sendFriendRequest(fid);
                    if( handleStatus == 0 )
                        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_SENT, this.params, sender);
                    else if( handleStatus == 1 || handleStatus == 2 )
                        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_ALREADY_SENT, this.params, sender);
                    else
                        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_NOT_ALLOWED, this.params, sender);
                    break;
            }
        }
        /**----------------------------------------------------------------
        Case 3: Any other cases
            Missing parameter
        -------------------------------------------------------------------*/
        else
        {
            send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_UNKNOWN_ERROR, this.params, sender);
        }
    }

    /**
     * Given friendship id (column 1 database)
     * If already block: unblocks, if not blocked: blocks.
     * prevent further message send and friend request send.
     * @return 0: block success 1: unblock success -1: unsuccessful
     */
    private int blockRequest(int fid)
    {
        int status = -1;
        try {
            ISFSArray friendshipStatusArray = dM.executeQuery("SELECT status FROM friendship WHERE id=" + fid, new Object[]{});
            if( friendshipStatusArray.size() > 0 )
            {
                // Already blocked: Unblock
                if( friendshipStatusArray.getSFSObject(0).getInt("status") == -1 )
                {
                    dM.executeUpdate("UPDATE friendship SET status=0 WHERE id=" + fid, new Object[]{});
                    status = 1;
                }
                // Block:
                else
                {
                    dM.executeUpdate("UPDATE friendship SET status=-1 WHERE id=" + fid, new Object[]{});
                    // Remove from buddy list if already in it.
                    BuddyList buddies = getParentExtension().getParentZone().getBuddyListManager().getBuddyList(reqSender.getName());
                    if( buddies.containsBuddy(friendID+"") )
                    {
                        getParentExtension().getBuddyApi().removeBuddy(reqSender, reqReciver.getName(), true, true);
                        if( reqReciver != null )
                            getParentExtension().getBuddyApi().removeBuddy(reqReciver, reqSender.getName(), true, true);
                    }
                    status = 0;
                }
            }
        } catch( SQLException e ) {
            e.printStackTrace();
        }
        return status;
    }

    /**
     * Given a friend id, sends request to add friend
     * 
     * @return 0: successful 1: pending, 2: already friend, -1: blocked, -2: unsucessful
     */
    private int sendFriendRequest(int fid)
    {
        int status = -2;
        try {
            ISFSArray friendshipStatusArray = dM.executeQuery("SELECT status FROM friendship WHERE id=" + fid, new Object[]{});
            if( friendshipStatusArray.size() > 0 )
            {
                int friendshipStatus = friendshipStatusArray.getSFSObject(0).getInt("status");
                if( friendshipStatus == 0 )
                {
                    // Set status to pending in friendship table.
                    dM.executeUpdate("UPDATE friendship SET status=1 WHERE id=" + fid, new Object[]{});
                    status = 0;
                }
                else
                {
                    // Do nothing and set status correspondingly.
                    status = friendshipStatus;
                }
            }
        } catch( SQLException e ) {
            e.printStackTrace();
        }
        return status;
    }

    /**
     * Given an fid if status of database is (1) pending will set status to
     * (2) friend. if force is set will automaticly set status 2 regardless
     * of currenct status value, unless status is block.
     * 
     * @return 0: successful, -1: unsucessful, -2: invalid -3: block
     */
    private int acceptFriendRequest(int fid, boolean force)
    {
        int status = -1;
        try {
            ISFSArray friendshipStatusArray = dM.executeQuery("SELECT status FROM friendship WHERE id=" + fid, new Object[]{});
            if( friendshipStatusArray.size() > 0 )
            {
                int friendshipStatus = friendshipStatusArray.getSFSObject(0).getInt("status");
                
                if( friendshipStatus == -1 )
                {
                    status = -3;
                }
                else if( force )
                {
                    // Set status to friend in friendship table.
                    dM.executeUpdate("UPDATE friendship SET status=2 WHERE id=" + fid, new Object[]{});
                    status = 0;
                }
                else if( friendshipStatus != 1 )
                {
                    // invalid
                    status = -2;
                }
                else
                {
                    try {
                        ISFSBuddyApi buddyApi = getParentExtension().getBuddyApi();
                        buddyApi.addBuddy(reqSender, friendID+"", false, true, false);
                        if( reqReciver != null )
                            buddyApi.addBuddy(reqReciver, playerID+"", false, true, false);
                            
                        dM.executeUpdate("UPDATE friendship SET status=2 WHERE id=" + fid, new Object[]{});
                        status = 0;
                    } catch (SFSBuddyListException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch( SQLException e ) {
            e.printStackTrace();
        }
        return status;
    }

    /**
     * Given a friendship id, sets friendship status to unfriend between them.
     * @return 0: successful -1: unsuccessful
     */
    private int unfriendRequest(int fid)
    {
        int status = -1;
        try {
            // Remove from buddy list if already in it.
            BuddyList buddies = getParentExtension().getParentZone().getBuddyListManager().getBuddyList(reqSender.getName());
            if( buddies.containsBuddy(friendID+"") )
            {
                getParentExtension().getBuddyApi().removeBuddy(reqSender, reqReciver.getName(), true, true);
                if( reqReciver != null )
                    getParentExtension().getBuddyApi().removeBuddy(reqReciver, reqSender.getName(), true, true);
            }
            dM.executeUpdate("UPDATE friendship SET status=0 WHERE id=" + fid, new Object[]{});
            status = 0;
        } catch( SQLException e ) {
            e.printStackTrace();
        }
        return status;
    }

    private int getFriendship(int f1, int f2, boolean isReferral)
    {
        int fid = -1;
        try {
            ISFSArray rowID = dM.executeQuery("SELECT id FROM friendship WHERE f1=" + f1 + " AND f2=" + f2 + " OR f2=" + f1 + " AND f1=" + f2, new Object[]{});
            // Exists: get id.
            if( rowID.size() > 0 )
            {
                fid = rowID.getSFSObject(0).getInt("id");
            }
            // Not Exists: create and get id.
            else
            {
                if( isReferral )
                {
                    int p1 = DBUtils.getInstance().getResources(f1).getInt(2);
                    int p2 = DBUtils.getInstance().getResources(f2).getInt(2);
                    
                    // Passing __arg1 to T82 returns step of that point.
                    int f1_step = (int) ScriptEngine.get(ScriptEngine.T82_BUDDY_ROAD, p2, null, null, null);
                    int f2_step = (int) ScriptEngine.get(ScriptEngine.T82_BUDDY_ROAD, p1, null, null, null);
                    
                    dM.executeInsert("INSERT INTO friendship (f1, f2, f1_step, f2_step) VALUES(?,?,?,?)", new Object[]{f1, f2, f1_step, f2_step});
                }
                else
                {
                    dM.executeInsert("INSERT INTO friendship (f1, f2) VALUES(?,?)", new Object[]{f1, f2});
                }
                rowID = dM.executeQuery("SELECT id FROM friendship WHERE f1=" + f1 + " AND f2=" + f2 + " OR f2=" + f1 + " AND f1=" + f2, new Object[]{});
                if( rowID.size() > 0 )
                    fid = rowID.getSFSObject(0).getInt("id");
                else
                    send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_UNKNOWN_ERROR, this.params, reqSender);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Database Error.
            send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_UNKNOWN_ERROR, this.params, reqSender);
            return fid;
        }
        if( fid < 0 )
            send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_NOT_FOUND, this.params, reqSender);
        return fid;
    }
}