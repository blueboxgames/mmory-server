package com.gerantech.mmory.sfs.callbacks;

import java.util.ArrayList;
import java.util.List;

import com.gerantech.mmory.core.battle.units.Unit;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.core.interfaces.IUnitHitCallback;
import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;

public class HitUnitCallback implements IUnitHitCallback {
    private final BattleRoom room;
    private final List<Integer> disposeds;

    public HitUnitCallback(final BattleRoom room) {
        super();
        this.room = room;
        this.disposeds = new ArrayList<>();
    }

    @Override
    public void hit(int arg0, List hitUnits) {
        for (Object o : hitUnits) {
            final Unit u = this.room.battleField.getUnit((int) o);
            if (u != null && u.disposed())
                this.disposeds.add(u.id);
        }
        if (this.disposeds.isEmpty())
            return;

        final ISFSObject units = new SFSObject();
        units.putIntArray("k", disposeds);
        this.room.getExtension().send(SFSCommands.BATTLE_UNIT_CHANGE, units, this.room.getUserList());
        this.disposeds.clear();
    }
}