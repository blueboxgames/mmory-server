package com.gerantech.mmory.sfs.callbacks;

import java.util.List;

import com.gerantech.mmory.core.interfaces.IUnitHitCallback;
import com.gerantech.mmory.sfs.battle.BattleRoom;

/**
 * Created by ManJav on 4/1/2018.
 */
public class HitUnitCallback implements IUnitHitCallback
{
    private final BattleRoom battleRoom;
    public HitUnitCallback(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
    }

    @Override
    public void hit(int bulletId, List list)
    {
        /*String log = "hit bulletId:" + bulletId;
        for (int i = 0; i <units.size() ; i++)
            log += "[ troopId:" + units.get(i).id + " troopHealth:" + units.get(i).card.health  +(i == units.size()-1 ? " ]":" ,  ");*/
        battleRoom.hitUnit(bulletId, list);
    }
}
