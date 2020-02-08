package com.gerantech.mmory.libs.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.gerantech.mmory.core.socials.Friends;
import com.gerantech.mmory.libs.data.RankData;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

/**
 * Created by ManJav on 2/6/2020.
 */
public class FriendsUtils extends UtilBase {
    public static FriendsUtils getInstance() {
        return (FriendsUtils) UtilBase.get(FriendsUtils.class);
    }

    public void loadAll() {
        // TODOL lazy Friendship loading
        if (ext.getParentZone().containsProperty("Friends"))
            return;

        ISFSArray rows = new SFSArray();
        try {
            rows = ext.getParentZone().getDBManager().executeQuery("SELECT * FROM friendship;", new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }

        Map<Integer, Friends> friends = new ConcurrentHashMap<>();
        ISFSObject f;
        Friends fSFS;
        for (int i = 0; i < rows.size(); i++) {
            f = rows.getSFSObject(i);
            //trace(f.getDump());
            fSFS = new Friends(f.getInt("id"), f.getInt("inviter_id"), f.getInt("invitee_id"), f.getInt("inviter_step"), f.getInt("invitee_step"), f.getInt("state"));
            friends.put(fSFS.id, fSFS);
        }
        ext.getParentZone().setProperty("friends", friends);
        trace("loaded Friends data in " + (System.currentTimeMillis() - (long) ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Integer, Friends> getAll() {
        return (ConcurrentHashMap<Integer, Friends>) ext.getParentZone().getProperty("friends");
    }

    public Friends get(int id) {
        ConcurrentHashMap<Integer, Friends> all = this.getAll();
        if (all.containsKey(id))
            return all.get(id);
        return null;
    }

    public List<Friends> getFriends(int playerId, int state) {
        List<Friends> ret = new ArrayList<>();
        Set<Map.Entry<Integer, Friends>> entries = this.getAll().entrySet();
        for (Map.Entry<Integer, Friends> entry : entries)
            if( (state == -1 || state == entry.getValue().state) && (entry.getValue().inviter == playerId || entry.getValue().invitee == playerId) )
                ret.add(entry.getValue());
        return ret;
    }

    public SFSArray getFriendList(int playerId, int state) {
        List<Friends> friends = getFriends(playerId, state);
        int fid;
        RankData rank = null;
        SFSArray data = new SFSArray();
        for (Friends f : friends) {
            fid = f.inviter == playerId ? f.invitee : f.inviter;
            // trace("ss", playerId, fid, f.state);
            rank = RankingUtils.getInstance().getUsers().get(fid);
            SFSObject item = new SFSObject();
            item.putInt("id", fid);
            item.putInt("step", f.inviter == playerId ? f.inviterStep : f.inviteeStep);
            item.putInt("point", rank.point);
            item.putInt("status", rank.status);
            item.putUtfString("name", rank.name);
            data.addSFSObject(item);
        }
        return data;
    }

    public Friends getFriendship(int left, int right, int state)
    {
        Set<Map.Entry<Integer, Friends>> entries = this.getAll().entrySet();
        for (Map.Entry<Integer, Friends> entry : entries)
            if( (state == -1 || state == entry.getValue().state) && (entry.getValue().inviter == left && entry.getValue().invitee == right) || (entry.getValue().invitee == left && entry.getValue().inviter == right) )
                return entry.getValue();
        return null;
    }

    public Friends create(int inviter, int invitee, int inviterStep, int inviteeStep)
    {
        Friends friendship = getFriendship(inviter, invitee, -1);
        if( friendship != null )
        {
            friendship.inviterStep = friendship.inviter == inviter ? inviterStep : inviteeStep;
            friendship.inviteeStep = friendship.inviter == inviter ? inviteeStep : inviterStep;
            this.update(friendship);
            return friendship;
        }

        String query = "INSERT INTO friendship (inviter_id, invitee_id, inviter_step, invitee_step) VALUES (" + inviter + ", " + invitee + ", " + inviterStep + ", " + inviteeStep + ")";
        int id;
        Friends ret = null;
        try {
            id = Math.toIntExact((Long)ext.getParentZone().getDBManager().executeInsert(query, new Object[] {}));
            ret = new Friends(id, inviter, invitee, inviterStep, inviteeStep, 0);
            this.getAll().put(id, ret);
        } catch (SQLException e) {  e.printStackTrace(); }
        return ret;
    }

    public void update(Friends friendship)
    {
        // ConcurrentHashMap<Integer, Friends> friends = this.getAll();
        // friends.replace(friendship.id, friendship);
        // ext.getParentZone().setProperty("friends", friends);

        String query = "UPDATE friendship SET " +
        "inviter_id=" + friendship.inviter + ", invitee_id=" + friendship.invitee + 
        ", inviter_step=" + friendship.inviterStep + ", invitee_step=" + friendship.inviteeStep + 
        ", state=" + friendship.state + " WHERE id = " + friendship.id;
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
    }
}