package com.gerantech.mmory.sfs.administration;

import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.utils.RoomsUtils;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

public class SpectateRequestJoinHandler extends BBGClientRequestHandler {
    public void handleClientRequest(User sender, ISFSObject params) {
        BBGRoom spectatingRoom = RoomsUtils.getInstance().findByName(params.getText("t"));
        if (spectatingRoom == null)
            spectatingRoom = make(sender, params.getText("t"));
        RoomsUtils.getInstance().join(spectatingRoom, sender);
        send(SFSCommands.SPECTATE_JOIN, params, sender);
    }

    public BBGRoom make(User owner, String type) {
        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(true);
        rs.setDynamic(true);
        rs.setMaxSpectators(10);
        rs.setMaxUsers(10);
        rs.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
        rs.setName(type);
        rs.setGroupId("spectates");

        return RoomsUtils.getInstance().make(SpectateRoom.class, owner, rs);
    }
}