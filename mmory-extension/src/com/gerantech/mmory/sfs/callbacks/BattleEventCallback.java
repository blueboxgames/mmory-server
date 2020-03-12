package com.gerantech.mmory.sfs.callbacks;

import java.util.Objects;

import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.battle.GameObject;
import com.gerantech.mmory.core.battle.units.Unit;
import com.gerantech.mmory.core.events.BattleEvent;
import com.gerantech.mmory.core.events.EventCallback;
import com.gerantech.mmory.core.socials.Challenge;
import com.gerantech.mmory.sfs.battle.BattleRoom;

public class BattleEventCallback implements EventCallback {
    private final BattleRoom room;
    // private boolean waitForConquest = true;

    public BattleEventCallback(BattleRoom room) {
        this.room = room;
        for (int i = 0; i < this.room.battleField.units.length; i++)
            if (!this.room.battleField.units.__get(i).disposed())
                this.room.battleField.units.__get(i).eventCallback = this;
    }

    @Override
    public void dispatch(int id, String type, Object data) {
        // only headquarter battle is alive
        if (room.battleField.state >= BattleField.STATE_4_ENDED || room.battleField.field.mode == Challenge.MODE_1_TOUCHDOWN || room.battleField.field.mode == Challenge.MODE_3_ZONE)
            return;

        // when units disposed
        if (Objects.equals(type, BattleEvent.STATE_CHANGE) && (int) data == GameObject.STATE_8_DIPOSED) {

            if (id < 6) {
                Unit unit = room.battleField.getUnit(id);
                int other = unit.side == 0 ? 1 : 0;
                this.room.endCalculator.scores[other] = id < 2 ? 3 : room.endCalculator.scores[other] + 1;
                this.room.sendNewRoundResponse(other, 0);
            }
        }
    }
}
