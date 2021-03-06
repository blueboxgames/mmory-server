package com.gerantech.mmory.sfs.quests;

import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.utils.QuestsUtils;
import com.gerantech.mmory.core.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class QuestInitializeHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
    	Game game = (Game) sender.getSession().getProperty("core");
		QuestsUtils.getInstance().insertNewQuests(game.player);
		params.putSFSArray("quests", QuestsUtils.toSFS(game.player.quests));
		send(SFSCommands.QUEST_INIT, params, sender);
    }
}