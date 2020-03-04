package com.gerantech.mmory.sfs.administration.issues;

import java.sql.SQLException;

import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 *
 */
public class IssueTrackHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		try {
			getDBManager().executeUpdate("UPDATE `bugs` SET `status`=" + params.getInt("status") + " WHERE id=" + params.getInt("id"), new Object[]{});
		} catch (SQLException e) {  e.printStackTrace(); }
    }
}