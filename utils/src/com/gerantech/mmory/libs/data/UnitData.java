package com.gerantech.mmory.libs.data;

import com.gerantech.mmory.core.battle.units.Unit;
import com.gerantech.mmory.core.utils.maps.IntUnitMap;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

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

    public static SFSArray toSFSArray(IntUnitMap unitMap)
    {
        SFSArray ret = new SFSArray();
        int[] keys = unitMap.keys();
        for (int i = 0; i < keys.length; i++)
            ret.addSFSObject(toSFS(unitMap.get(keys[i])));
        return ret;
    }
}
