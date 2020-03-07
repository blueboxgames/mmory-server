package com.gerantech.mmory.sfs.battle.handlers;

import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.data.RankData;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.libs.utils.RankingUtils;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

/**
 * Created by ManJav on 9/2/2017.
 */
public class BattleUsersExitHandler extends BaseServerEventHandler
{
    public void handleServerEvent(ISFSEvent arg) throws SFSException
    {
        User user = (User) arg.getParameter(SFSEventParam.USER);
        Object core = user.getSession().getProperty("core");
        if( core == null)
            return;

        BattleUtils bu = BattleUtils.getInstance();
        Player p = bu.getGame(user).player;
        BBGRoom room = bu.find(p.id, BattleField.STATE_0_WAITING, BattleField.STATE_5_DISPOSED);
        if( room == null )
            return;
        if( room.getState() < BattleField.STATE_1_CREATED )
            BattleUtils.getInstance().remove(room);
        else
            BattleUtils.getInstance().leave(room, user);

        // user status update
        if (!room.isSpectator(user))
            RankingUtils.getInstance().update(p.id, p.nickName, p.get_point(), arg.getType().equals(SFSEventType.USER_DISCONNECT) ? RankData.STATUS_OFF : RankData.STATUS_ON);

    }
}