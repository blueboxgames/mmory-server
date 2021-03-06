package com.gerantech.mmory.sfs.socials.handlers;

import java.util.Arrays;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.libs.utils.FCMUtils;
import com.gerantech.mmory.libs.utils.InboxUtils;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 9/4/2017.
 */
public class BuddyBattleRequestHandler extends BaseClientRequestHandler
{
    private static final int STATE_REQUEST = 0;
    private static final int STATE_BATTLE_STARTED = 1;

    public void handleClientRequest(User sender, ISFSObject params)
    {
        Player player = ((Game) sender.getSession().getProperty("core")).player;
        int battleState = params.getInt("bs");
        if ( battleState == STATE_REQUEST )
        {
            Integer objectUserId = params.getInt("o");
            User objectUser = getParentExtension().getParentZone().getUserManager().getUserByName(objectUserId + "");
            if( objectUser != null )
            {
                BBGRoom room = BattleUtils.getInstance().make((Class<?>) getParentExtension().getParentZone().getProperty("battleClass"), sender, 0, params.getInt("m"), 0, 2);
                BattleUtils.getInstance().join(room, sender);
                params.putInt("bid", room.getId());
                params.putInt("s", player.id);
                params.putUtfString("sn", player.nickName);
                sendResponse(params);
            }
            else
            {
                FCMUtils.getInstance().send(player.nickName + " تو رو به رقابت دوستانه دعوت می کنه ", null, objectUserId);
                params.putInt("bs", 4);
                sendResponse(params);
            }
        }
        else if( battleState == STATE_BATTLE_STARTED )
        {
            User subjectUser = getParentExtension().getParentZone().getUserManager().getUserByName(params.getInt("s") + "");
            BBGRoom room = BattleUtils.getInstance().getRoom(params.getInt("bid"));
            BattleUtils.getInstance().join(room, sender);
            if( subjectUser != null )
                send(SFSCommands.BUDDY_BATTLE, params, Arrays.asList(sender, subjectUser));
        }
        else
        {
            Room room = getParentExtension().getParentZone().getRoomById(params.getInt("bid"));
            if( room != null )
            {
                params.putInt("c", player.id);
                getApi().removeRoom(room);
            }
            sendResponse(params);

            // Send requested battle to subjectUser's inbox
            if( player.id != params.getInt("o") )
                InboxUtils.getInstance().send(0, player.nickName + " تو رو به مبارزه دعوت کرد. ", BanUtils.SYSTEM_ID, params.getInt("o"),"");
        }
    }

    private void sendResponse(ISFSObject params)
    {
        User subjectUser = getParentExtension().getParentZone().getUserManager().getUserByName(params.getInt("s") + "");
        if (subjectUser != null)
            send(SFSCommands.BUDDY_BATTLE, params, subjectUser);

        User objectUser = getParentExtension().getParentZone().getUserManager().getUserByName(params.getInt("o") + "");
        if (objectUser != null)
            send(SFSCommands.BUDDY_BATTLE, params, objectUser);
    }
}