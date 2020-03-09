package com.gerantech.mmory.sfs.socials.handlers;

import java.sql.SQLException;
import java.time.Instant;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * Created by ManJav on 2/7/2018.
 */
public class LobbyReportHandler extends BBGClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Player reporter = ((Game) sender.getSession().getProperty("core")).player;
        String lobby = getParentExtension().getParentRoom().getName();
        ISFSArray infractions = null;

        // check report exists on db
        String query = "SELECT * From infractions WHERE content = '" + params.getUtfString("t") + "' AND lobby = '" + lobby + "' AND offender = " + params.getInt("i");
        // trace(query);
        try {
            infractions = getDBManager().executeQuery(query, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        if( infractions != null &&  infractions.size() > 0 )
        {
            send(sender, params, infractions.getSFSObject(0).getInt("reporter") == reporter.id ? 2 : 1);
            return;
        }

        // check reporter is verbose
        //trace(query);
        query = "SELECT * From infractions WHERE report_at > FROM_UNIXTIME(" + (Instant.now().getEpochSecond() - 86400)+ ") AND reporter = " + reporter.id;
        try {
            infractions = getDBManager().executeQuery(query, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        if( infractions != null &&  infractions.size() > 4 )
        {
            send(sender, params, 3);
            return;
        }

        // insert report
        BanUtils.getInstance().report(params, lobby, reporter.id);
        send(sender, params, MessageTypes.RESPONSE_SUCCEED);
    }

    protected void send(User sender, ISFSObject params, int response)
    {
        params.putInt("response", response);
        params.removeElement("t");
        params.removeElement("u");
        super.send(SFSCommands.LOBBY_REPORT, params, sender);
    }
}