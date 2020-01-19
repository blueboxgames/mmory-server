package com.gerantech.mmory.sfs.battle.factories;

import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.battle.units.Unit;
import com.gerantech.mmory.sfs.battle.BattleRoom;

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
        int threshold = 4;
        Unit u;
        for( int i = 0; i < room.battleField.units.length ; i++ )
        {
            u = room.battleField.units.__get(i);
            if( u.disposed() )
                continue;
            if( (u.side == 0 && u.y <= threshold) || (u.side == 1 && u.y >= BattleField.HEIGHT - threshold) )
                return u;
        }
        return null;
    }
}