package com.gerantech.mmory.sfs.battle.factories;

import com.gerantech.mmory.core.battle.units.Unit;
import com.gerantech.mmory.sfs.battle.BattleRoom;

public class ZoneEndCalculator extends EndCalculator
{
    public ZoneEndCalculator(BattleRoom room)
    {
        super(room);
    }

    @Override
    public boolean check()
    {
        if( super.check() )
            return true;
        Unit flag = room.battleField.units.__get(0);
        if( flag.health < flag.card.health )
            return false;
        
        int side = flag.side + 3;
        this.room.trace("zone captured by " + side);
        this.round ++;
        this.scores[side] ++;
        this.room.sendNewRoundResponse(side, -1);

        if( this.scores[side] > 2 )
            return true;

        this.room.battleField.requestKillPioneers(-1);
        return false;
    }
}