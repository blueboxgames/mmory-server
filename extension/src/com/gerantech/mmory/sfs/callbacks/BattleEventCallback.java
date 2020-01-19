package com.gerantech.mmory.sfs.callbacks;

import java.util.Objects;

import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.battle.GameObject;
import com.gerantech.mmory.core.battle.units.Unit;
import com.gerantech.mmory.core.events.BattleEvent;
import com.gerantech.mmory.core.events.EventCallback;
import com.gerantech.mmory.core.socials.Challenge;
import com.gerantech.mmory.sfs.battle.BattleRoom;

public class BattleEventCallback implements EventCallback
{
    private final BattleRoom battleRoom;
//    private boolean waitForConquest = true;

    public BattleEventCallback(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        for( int i = 0 ; i < this.battleRoom.battleField.units.length ; i++ )
        {
            if(this.battleRoom.battleField.units.__get(i).disposed())
                continue;
            this.battleRoom.battleField.units.__get(i).eventCallback = this;
        }
    }

    @Override
    public void dispatch(int id, String type, Object data)
    {
        // only headquarter battle is alive
        if( battleRoom.battleField.state >= BattleField.STATE_4_ENDED || battleRoom.battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
            return;

        // when units disposed
        if( Objects.equals(type, BattleEvent.STATE_CHANGE) && (int) data == GameObject.STATE_8_DIPOSED )
        {
            Unit unit = battleRoom.battleField.getUnit(id);
            if( unit.card.type >= 201 )
            {
                int other = unit.side == 0 ? 1 : 0;
                if( id < 6 )
                {
                    unit.hit(100);
                    battleRoom.updateReservesData();
                    if( id < 2 )
                        battleRoom.endCalculator.scores[other] = Math.min(3, battleRoom.endCalculator.scores[other] + 3);
                    else
                        battleRoom.endCalculator.scores[other] ++;
                }

                battleRoom.sendNewRoundResponse(other, 0);
            }
        }
    }
}
