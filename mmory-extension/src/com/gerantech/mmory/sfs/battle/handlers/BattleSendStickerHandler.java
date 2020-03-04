package com.gerantech.mmory.sfs.battle.handlers;

import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 7/28/2017.
 */
public class BattleSendStickerHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
			try {
				((BattleRoom) BattleUtils.getInstance().rooms.get(params.getInt("r"))).sendSticker(sender, params);
			} catch (Exception | Error e) { e.printStackTrace(); };
    }
}