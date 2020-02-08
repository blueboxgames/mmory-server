package com.gerantech.mmory.libs.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.gerantech.mmory.core.constants.MessageTypes;
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

    public List<Friends> getFriends(int playerId) {
        List<Friends> ret = new ArrayList<>();
        Set<Map.Entry<Integer, Friends>> entries = this.getAll().entrySet();
        for (Map.Entry<Integer, Friends> entry : entries)
            if (entry.getValue().inviter == playerId || entry.getValue().invitee == playerId)
                ret.add(entry.getValue());
        return ret;
    }

    public SFSArray getFriendList(int playerId) {
        List<Friends> friends = getFriends(playerId);
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

    public Friends getFriendship(int left, int right)
    {
        Set<Map.Entry<Integer, Friends>> entries = this.getAll().entrySet();
        for (Map.Entry<Integer, Friends> entry : entries)
            if( (entry.getValue().inviter == left && entry.getValue().invitee == right) || (entry.getValue().invitee == left && entry.getValue().inviter == right) )
                return entry.getValue();
        return null;
    }

    public Friends create(int inviter, int invitee, int inviterStep, int inviteeStep)
    {
        Friends friends = getFriendship(inviter, invitee);
        if( friends != null )
        {
            friends.inviterStep = friends.inviter == inviter ? inviterStep : inviteeStep;
            friends.inviteeStep = friends.inviter == inviter ? inviteeStep : inviterStep;
            this.update(friends);
            return friends;
        }

        String query = "INSERT INTO friends (inviter_id, invitee_id, inviter_step, invitee_step) VALUES (" + inviter + ", " + invitee + ", " + inviterStep + ", " + inviteeStep + ")";
        int id;
        Friends ret = null;
        try {
            id = Math.toIntExact((Long)ext.getParentZone().getDBManager().executeInsert(query, new Object[] {}));
            ret = new Friends(id, inviter, invitee, inviterStep, inviteeStep);
            this.getAll().put(id, ret);
        } catch (SQLException e) {  e.printStackTrace(); }
        return ret;
    }

    public void update(Friends Friends)
    {
        this.getAll().put(Friends.id, Friends);
        String query = "UPDATE friends SET " +
        "inviter_id=" + Friends.inviter + "invitee_id=" + Friends.invitee + 
        "inviter_step=" + Friends.inviterStep + "invitee_step=" + Friends.inviteeStep + 
        " WHERE id = " + Friends.id;
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
    }
    
    public int delete(int id)
    {
        ConcurrentHashMap<Integer, Friends> all = this.getAll();
        if( !all.contains(id) )
            return MessageTypes.RESPONSE_NOT_FOUND;
        
        all.remove(id);
        try {
            ext.getParentZone().getDBManager().executeUpdate("DELETE * FROM friendship WHERE id=" + id, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
        return MessageTypes.RESPONSE_SUCCEED;
    }
}