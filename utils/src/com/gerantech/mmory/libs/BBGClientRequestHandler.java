package com.gerantech.mmory.libs;

import java.util.List;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BBGClientRequestHandler extends BaseClientRequestHandler
{
    protected IDBManager dbManager() {
        return getParentExtension().getParentZone().getDBManager();
    }

    protected void send(String cmdName, int response, ISFSObject params, User recipient)
    {
        params.putInt("response", response);
        super.send(cmdName, params, recipient);
    }

    protected void send(String cmdName, int response, ISFSObject params, List<User> recipients)
    {
        params.putInt("response", response);
        super.send(cmdName, params, recipients);
    }

    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
    }
}