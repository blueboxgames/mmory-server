package com.gerantech.mmory.sfs.socials;

import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.sfs.socials.handlers.LobbyReportHandler;
import com.gerantech.mmory.sfs.socials.handlers.PublicMessageHandler;
import com.gerantech.mmory.libs.data.LobbySFS;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.smartfoxserver.v2.util.filters.FilteredMessage;

import java.time.Instant;

/**
 * Created by ManJav on 2/1/2018.
 */
public class BaseLobbyRoom extends SFSExtension
{
    protected Game game;
    protected int mode;
    protected Room lobby;
    protected LobbySFS data;
    protected ISFSArray messages;

    public void init()
    {
        lobby = getParentRoom();
        data = (LobbySFS) lobby.getProperty("data");
        addRequestHandler(SFSCommands.LOBBY_PUBLIC_MESSAGE, PublicMessageHandler.class);
        addRequestHandler(SFSCommands.LOBBY_REPORT, LobbyReportHandler.class);
    }

    public void handleClientRequest(String requestId, User sender, ISFSObject params)
    {
        try {
            if( requestId.equals(SFSCommands.LOBBY_PUBLIC_MESSAGE) )
                organizeMessage(sender, params, true);
            super.handleClientRequest(requestId, sender, params);
        } catch (Exception | Error e) { e.printStackTrace(); }
    }

    protected void organizeMessage(User sender, ISFSObject params, boolean alreadyAdd)
    {
        // Add time and user-id to message
        if( sender != null )
        {
            game = ((Game) sender.getSession().getProperty("core"));

            params.putInt("i", game.player.id);
            params.putUtfString("s", game.player.nickName);
        }

        params.putInt("u", (int) Instant.now().getEpochSecond());

        if( !params.containsKey("m") )
            params.putInt("m", MessageTypes.M0_TEXT);
        mode = params.getInt("m");

        // filter bad words
        if( mode == MessageTypes.M0_TEXT && lobby.getGroupId().equals("publics") )
        {
            boolean isAdmin = Player.isAdmin(game.player.id);
            FilteredMessage fm = BanUtils.getInstance().filterBadWords(params.getUtfString("t"), isAdmin);
            if( fm != null && fm.getOccurrences() > 0 )
            {
                if( !isAdmin )
                {
                    BanUtils.getInstance().report(params, lobby.getName(), 10000);
                    params.putBool("x", false);
                    return;
                }
                params.putUtfString("t", fm.getMessage());
            }
        }
       
        messages = messageQueue();

        // Max 30 len message queue
        while (messages.size() > 30)
            messages.removeElementAt(0);

        if( mode == MessageTypes.M0_TEXT )
        {
            if( params.getUtfString("t").length() > 160 )
                params.putUtfString("t", params.getUtfString("t").substring(0, 160) + " ...");
            // Merge messages from a sender
            ISFSObject last = messages.size() > 0 ? messages.getSFSObject(messages.size() - 1) : null;
            if( last != null && last.getInt("m") == MessageTypes.M0_TEXT && last.getInt("i") == game.player.id )
            {
                params.putUtfString("t", last.getUtfString("t") + "\n" + params.getUtfString("t"));
                messages.removeElementAt(messages.size() - 1);
            }
        }
        if( alreadyAdd )
            messages.addSFSObject(params);
    }
    protected ISFSArray messageQueue ()
    {
        return lobby.getVariable("msg").getSFSArrayValue();
    }
}