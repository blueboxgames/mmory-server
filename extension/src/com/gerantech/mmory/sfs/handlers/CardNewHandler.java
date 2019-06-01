package com.gerantech.mmory.sfs.handlers;

import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.Commands;
import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.battle.units.Card;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.utils.maps.IntIntMap;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 */
public class CardNewHandler extends BBGClientRequestHandler
{
	public CardNewHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
    {
    	Game game = (Game)sender.getSession().getProperty("core");
    	int response = Card.addNew(game, params.getInt("c"));
    	if( response == MessageTypes.RESPONSE_SUCCEED )
			DBUtils.getInstance().insertResources(game.player, new IntIntMap(params.getInt("c") + ":1"));
		send(Commands.CARD_NEW, response, params, sender);
    }
}