package com.gerantech.mmory.sfs.callbacks;

import com.gerantech.mmory.sfs.battle.BattleRoom;
import com.gerantech.mmory.core.interfaces.IUnitHitCallback;
import haxe.root.Array;
import java.util.List;

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
    public void hit(int bulletId, List<Integer> list)
    {
        /*String log = "hit bulletId:" + bulletId;
        for (int i = 0; i <units.size() ; i++)
            log += "[ troopId:" + units.get(i).id + " troopHealth:" + units.get(i).card.health  +(i == units.size()-1 ? " ]":" ,  ");*/
        battleRoom.hitUnit(bulletId, list);
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
