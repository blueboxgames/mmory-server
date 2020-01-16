package com.gerantech.mmory.sfs.callbacks;

import java.util.Objects;

import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.battle.GameObject;
import com.gerantech.mmory.core.events.BattleEvent;
import com.gerantech.mmory.core.events.EventCallback;
import com.gerantech.mmory.core.socials.Challenge;
import com.gerantech.mmory.sfs.battle.BattleRoom;

import haxe.ds._IntMap.IntMapKeyIterator;

public class BattleEventCallback implements EventCallback
{
    private final BattleRoom battleRoom;
//    private boolean waitForConquest = true;

    public BattleEventCallback(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
		@SuppressWarnings("unchecked")
		IntMapKeyIterator<Integer> iterator = (IntMapKeyIterator<Integer>) battleRoom.battleField.units.keys();
        while (iterator.hasNext())
            battleRoom.battleField.units.get(iterator.next()).eventCallback = this;
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
            if( battleRoom.battleField.units.get(id).card.type >= 201 )
            {
                int other = battleRoom.battleField.units.get(id).side == 0 ? 1 : 0;
                if( id < 6 )
                {
                    battleRoom.battleField.units.get(id).hit(100);
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
