package com.gerantech.mmory.sfs.callbacks;

import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.battle.GameObject;
import com.gerantech.mmory.core.battle.units.Unit;
import com.gerantech.mmory.core.events.BattleEvent;
import com.gerantech.mmory.core.events.EventCallback;
import com.gerantech.mmory.core.socials.Challenge;
import haxe.root.Array;

import java.util.Map;
import java.util.Objects;

public class BattleEventCallback implements EventCallback
{
    private final BattleRoom battleRoom;
//    private boolean waitForConquest = true;

    public BattleEventCallback(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        for (Map.Entry<Object, Unit> unitEntry : battleRoom.battleField.units._map.entrySet())
            unitEntry.getValue().eventCallback = this;
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
                if( id < 2 )
                    battleRoom.endCalculator.scores[other] = Math.min(3, battleRoom.endCalculator.scores[other] + 3);
                else if( id < 6)
                    battleRoom.endCalculator.scores[other] ++;

                battleRoom.sendNewRoundResponse(other, 0);
            }
        }
    }

    @Override
    public Object __hx_getField(String arg0, boolean arg1, boolean arg2, boolean arg3) {
        return null;
    }

    @Override
    public double __hx_getField_f(String arg0, boolean arg1, boolean arg2) {
        return 0;
    }

    @Override
    public void __hx_getFields(Array<String> arg0) {

    }

    @Override
    public Object __hx_invokeField(String arg0, Object[] arg1) {
        return null;
    }

    @Override
    public Object __hx_lookupField(String arg0, boolean arg1, boolean arg2) {
        return null;
    }

    @Override
    public double __hx_lookupField_f(String arg0, boolean arg1) {
        return 0;
    }

    @Override
    public Object __hx_lookupSetField(String arg0, Object arg1) {
        return null;
    }

    @Override
    public double __hx_lookupSetField_f(String arg0, double arg1) {
        return 0;
    }

    @Override
    public Object __hx_setField(String arg0, Object arg1, boolean arg2) {
        return null;
    }

    @Override
    public double __hx_setField_f(String arg0, double arg1, boolean arg2) {
        return 0;
    }
}
