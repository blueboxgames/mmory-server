package com.gerantech.mmory.libs.callbacks;

import com.gerantech.mmory.core.interfaces.IValueChangeCallback;
import com.gerantech.mmory.core.utils.maps.IntIntMap;

/**
 * Created by ManJav on 8/14/2017.
 */

public class MapChangeCallback implements IValueChangeCallback
{
    public IntIntMap inserts = new IntIntMap();
    public IntIntMap updates = new IntIntMap();
    public IntIntMap all = new IntIntMap();

    @Override
    public void insert(int key, int oldValue, int newValue)
    {
       // if( !inserts.exists(key) )
            inserts.increase(key, newValue - oldValue);
        all.increase(key, newValue - oldValue);
    }

    @Override
    public void update(int key, int oldValue, int newValue)
    {
       // if( !updates.exists(key) )
            updates.increase(key, newValue - oldValue);
        all.increase(key, newValue - oldValue);
    }
}