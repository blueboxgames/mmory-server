package com.gerantech.mmory.sfs.administration;

import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.utils.RoomsUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

public class SpectateRequestLeaveHandler extends BBGClientRequestHandler {
    public void handleClientRequest(User sender, ISFSObject params) {
        BBGRoom spectatingRoom = RoomsUtils.getInstance().findByName(params.getText("t"));
        if (spectatingRoom == null)
            return;
        RoomsUtils.getInstance().leave(spectatingRoom, sender);
    }
}