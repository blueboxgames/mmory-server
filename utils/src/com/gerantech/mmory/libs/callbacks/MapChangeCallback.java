package com.gerantech.mmory.libs.callbacks;

import com.gerantech.mmory.core.interfaces.IValueChangeCallback;
import com.gerantech.mmory.core.utils.maps.IntIntMap;
import haxe.root.Array;

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