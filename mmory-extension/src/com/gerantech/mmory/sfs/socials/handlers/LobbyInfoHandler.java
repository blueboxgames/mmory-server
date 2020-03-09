package com.gerantech.mmory.sfs.socials.handlers;

import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.utils.LobbyDataUtils;
import com.gerantech.mmory.libs.utils.RankingUtils;
import com.gerantech.mmory.libs.data.LobbySFS;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyInfoHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        LobbySFS data = (LobbySFS) getParentExtension().getParentRoom().getProperty("data");
        LobbyDataUtils.getInstance().fillRoomData(data, params, RankingUtils.getInstance().getUsers(), true, true);
        if( params.containsKey("broadcast") )
            send(SFSCommands.LOBBY_INFO, params, getParentExtension().getParentRoom().getUserList());
        else
            send(SFSCommands.LOBBY_INFO, params, sender);
    }
}