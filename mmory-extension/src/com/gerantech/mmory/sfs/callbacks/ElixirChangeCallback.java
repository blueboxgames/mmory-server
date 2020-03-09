package com.gerantech.mmory.sfs.callbacks;

import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.gerantech.mmory.libs.callbacks.MapChangeCallback;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.smartfoxserver.v2.entities.data.SFSObject;

public class ElixirChangeCallback extends MapChangeCallback
{
    private final BattleRoom room;

    public ElixirChangeCallback(BattleRoom room)
    {
        super();
        this.room = room;
    }

    @Override
    public void update(int side, int oldValue, int newValue)
    {
        SFSObject params = new SFSObject();
        params.putInt(side + "", newValue);
        room.getZone().getExtension().send(SFSCommands.BATTLE_ELIXIR_UPDATE, params, room.getUserList());
    }
}
