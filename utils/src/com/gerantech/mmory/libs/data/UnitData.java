package com.gerantech.mmory.libs.data;

import com.gerantech.mmory.core.battle.units.Unit;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

import haxe.root.Array;
public class UnitData {
    public double x;
    public double y;
    public double health;

    public UnitData(double x, double y, double health) {
        this.x = x;
        this.y = y;
        this.health = health;
    }

    public static SFSObject toSFS(Unit unit) {
        SFSObject ret = new SFSObject();
        ret.putInt("i", unit.id);
        ret.putInt("s", unit.side);
        ret.putInt("t", unit.card.type);
        ret.putInt("l", unit.card.level);
        ret.putDouble("h", unit.health);
        ret.putDouble("x", unit.x);
        ret.putDouble("y", unit.y);
        return ret;
    }

    public static SFSArray toSFSArray(Array<Unit> unitMap)
    {
        SFSArray ret = new SFSArray();
        for(int i = 0; i < unitMap.length ; i++ )
        {
            ret.addSFSObject(toSFS((Unit) unitMap.__get(i)));
            System.out.println(unitMap.__get(i));
        }
        return ret;
    }
}
