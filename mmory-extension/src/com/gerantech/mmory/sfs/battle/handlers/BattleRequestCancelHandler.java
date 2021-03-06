package com.gerantech.mmory.sfs.battle.handlers;

import java.util.Map;
import java.util.Set;

import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleRequestCancelHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Set<Map.Entry<Integer, BBGRoom>> entries = BattleUtils.getInstance().getRooms().entrySet();
        BBGRoom foundRoom = null;
        for (Map.Entry<Integer, BBGRoom> entry : entries)
        {
            if( entry.getValue().getOwner().equals(sender) )
            {
                if( entry.getValue().getState() < BattleField.STATE_1_CREATED )
                {
                    foundRoom = entry.getValue();
                    break;
                }
            }
        }

        if( foundRoom != null )
            BattleUtils.getInstance().remove(foundRoom);
        send(SFSCommands.BATTLE_CANCEL, null, sender);
    }
}