package com.gerantech.mmory.sfs.battle.factories;

import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.battle.units.Unit;

import java.util.Iterator;
import java.util.Map;

public class TouchDownEndCalculator extends EndCalculator
{
    public int round = 1;
    public TouchDownEndCalculator(BattleRoom room)
    {
        super(room);
    }

    @Override
    public boolean check()
    {
        if( super.check() )
            return true;
        
        Unit unit = checkUnitPassed();
        if( unit == null )
            return false;
        
        room.trace("unit passed " + unit.id);
        round ++;
        scores[unit.side] ++;
        room.sendNewRoundResponse(unit.side, unit.id);
        if( scores[unit.side] > 2 )
            return true;

        room.battleField.requestKillPioneers(unit.side);
        return false;
    }

    Unit checkUnitPassed()
    {
        int threshold = 24;
        Unit u;
        int[] keys = room.battleField.units.keys();
        for (int k : keys)
        {
           u = room.battleField.units.get(k);
           if( (u.side == 0 && u.y <= threshold) || (u.side == 1 && u.y >= BattleField.HEIGHT - threshold) )
               return u;
        }
        return null;
    }
}