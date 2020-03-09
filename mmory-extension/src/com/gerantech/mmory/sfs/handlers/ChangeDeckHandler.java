package com.gerantech.mmory.sfs.handlers;

import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * Created by ManJav on 17/11/29.
 */
public class ChangeDeckHandler extends BBGClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        trace(params.getDump());
        Player player = ((Game) sender.getSession().getProperty("core")).player;

        if( !player.cards.exists(params.getInt("type")) )
        {
            send(SFSCommands.CHANGE_DECK, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
            return;
        }

        if( player.decks.get(params.getInt("deckIndex")).existsValue(params.getInt("type")))
        {
            send(SFSCommands.CHANGE_DECK, MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
            return;
        }
        send(SFSCommands.CHANGE_DECK, DBUtils.getInstance().updateDeck(player, params.getInt("deckIndex"), params.getInt("index"), params.getInt("type")), params, sender);
    }
}
