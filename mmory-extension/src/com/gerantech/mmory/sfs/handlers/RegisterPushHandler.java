package com.gerantech.mmory.sfs.handlers;

import java.sql.SQLException;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 *
 */
public class RegisterPushHandler extends BBGClientRequestHandler
{

	public RegisterPushHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
	{
		IDBManager dbManager = getDBManager();

		try{

		String queryStr = "INSERT INTO pushtokens (player_id";

		if( params.containsKey("oneSignalUserId") )
			queryStr += ", `os_pid`";
		if( params.containsKey("oneSignalPushToken") )
			queryStr += ", `os_token`";
		if( params.containsKey("fcmToken") )
			queryStr += ", `fcm_token`";

		queryStr += ") VALUES (" + ((Game)sender.getSession().getProperty("core")).player.id;

		if( params.containsKey("oneSignalUserId") )
			queryStr += ", '" + params.getText("oneSignalUserId") + "'";
		if( params.containsKey("oneSignalPushToken") )
			queryStr += ", '" + params.getText("oneSignalPushToken") + "'";
		if( params.containsKey("fcmToken") )
			queryStr += ", '" + params.getText("fcmToken") + "'";

		queryStr += ") ON DUPLICATE KEY UPDATE os_pid=VALUES(os_pid), os_token=VALUES(os_token), fcm_token=VALUES(fcm_token)";

		dbManager.executeUpdate(queryStr, new Object[]{});

		} catch (SQLException e) { e.printStackTrace(); }
	}
}