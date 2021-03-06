package com.gerantech.mmory.libs.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.LoginData;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.constants.ExchangeType;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.core.constants.ResourceType;
import com.gerantech.mmory.core.utils.maps.IntIntMap;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.data.RankData;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

/**
 * Created by ManJav on 12/4/2017.
 */
public class DBUtils extends UtilBase
{
    private final IDBManager db;
    private final Boolean DEBUG_MODE = true;
    private final String _DB = ext.getParentZone().getDBManager().getConfig().connectionString.split("/")[3].split("\\?")[0];
    public final String liveDB = _DB + "_live";
    public DBUtils()
    {
        super();
        db = ext.getParentZone().getDBManager();
    }
    public static DBUtils getInstance()
    {
        return (DBUtils)UtilBase.get(DBUtils.class);
    }

    public String cleanInactiveUsers(String pastHours) {
        String query = "";
        String ret = "";
        Instant start = Instant.now();
        Instant time = start.minusMillis(Long.parseUnsignedLong(pastHours)*3600000);
        Timestamp timestamp = Timestamp.from(time);
        try {
            Connection con = db.getConnection();
            try {
                con.setAutoCommit(false);
                Statement statement = con.createStatement();
                // ---------- Table Backups ------------
                backupTable(statement, "players",  new String[]{"name","password","create_at","app_version","last_login","sessions_count"});
                backupTable(statement, "decks",    new String[]{"player_id","type","index","deck_index"});
                backupTable(statement, "exchanges",new String[]{"player_id","type","num_exchanges","expired_at","outcome","reqs"});
                backupTable(statement, "quests",   new String[]{"player_id","type","key","step","timestamp"});
                backupTable(statement, "resources",new String[]{"player_id","type","count","level"});
                backupTable(statement, "userprefs",new String[]{"player_id","k","v"});

                query = "DELETE FROM " + liveDB + ".players WHERE last_login < \"" + timestamp.toString() + "\"";
                traceQuery(query);
                statement.execute(query);
                con.commit();
                con.setAutoCommit(true);
                ret = "Cleaned inactive useres before " + timestamp.toString() + " ";
                trace(ret);
            } catch(SQLException e) {
                con.rollback();
                e.printStackTrace();
                ret = "Failed to clean users ";
            } finally {
                Instant fin = Instant.now();
                con.close();
                ret += "in " + Duration.between(start, fin).toMillis() + " milisecounds.\n";
            }
        } catch( SQLException e ) { e.printStackTrace(); }
        return ret;
    }

    private String getOnDuplicateKeyChanges(String[] columns) {
        String ret = " ON DUPLICATE KEY UPDATE ";
        List<String> valueColumn = new ArrayList<String>();
        for ( String column : columns ) {
            valueColumn.add("`"+column+"`=VALUES(`"+column+"`)");
        }
        ret += String.join(",", valueColumn);
        return ret;
    }

    public void restore(int playerId) {
        restorePlayer(playerId, "id",          "players",      new String[]{"name","password","create_at","app_version","last_login","sessions_count"});
        restorePlayer(playerId, "player_id",   "decks",        new String[]{"player_id","type","index","deck_index"});
        restorePlayer(playerId, "player_id",   "exchanges",    new String[]{"player_id","type","num_exchanges","expired_at","outcome","reqs"});
        restorePlayer(playerId, "player_id",   "quests",       new String[]{"player_id","type","key","step","timestamp"});
        restorePlayer(playerId, "player_id",   "resources",    new String[]{"player_id","type","count","level"});
        restorePlayer(playerId, "player_id",   "userprefs",    new String[]{"player_id","k","v"});
    }

    private void backupTable(Statement statement, String table, String[] columns) throws SQLException {
        String query = "INSERT INTO " + table + " SELECT * FROM " + liveDB + "." + table + getOnDuplicateKeyChanges(columns);
        traceQuery(query);
        statement.execute(query);
    }

    private void restorePlayer(int playerId, String idName, String table, String[] columns) {
        String query =  "INSERT INTO " + liveDB + "." + table + " SELECT * FROM " + table + " WHERE " + idName + "=" + playerId + getOnDuplicateKeyChanges(columns);
        traceQuery(query);
        try {
            db.executeInsert(query, new Object[]{});
        } catch(SQLException e) { e.printStackTrace(); }
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   RESOURCES  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public SFSArray getResources(int playerId)
    {
        SFSArray ret = new SFSArray();
        try {
            ret = (SFSArray) db.executeQuery("SELECT id, type, count, level FROM " + liveDB + ".resources WHERE player_id = " + playerId, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }

    public void updateResources(Player player, IntIntMap resources)
    {
        boolean hasRankFields = resources.exists(ResourceType.R2_POINT);
        String query = "UPDATE " + liveDB + ".resources SET count= CASE";
        int[] keys = resources.keys();
        int keyLen = keys.length;
        if( keyLen <= 0 )
            return;

        for (int i = 0; i < keyLen; i++)
        {
            if( resources.get(keys[i]) == 0 || ResourceType.isBook(keys[i]) )
                continue;

            query += " WHEN type= " + keys[i] + " THEN " + player.resources.get(keys[i]);
        }
        query += " ELSE count END WHERE player_id=" + player.id;
        try {
            db.executeUpdate(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }

        // update ranking table
        if( hasRankFields )
            RankingUtils.getInstance().update(player.id, null, player.get_point(), RankData.STATUS_NAN);;
    }

    public void insertResources(Player player, IntIntMap resources)
    {
        int[] keys = resources.keys();
        int keyLen = keys.length;
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < keyLen; i++)
            if( !ResourceType.isBook(keys[i]) )
                res.add(keys[i]);

        keyLen = res.size();
        if( keyLen == 0 )
            return;

        String query = "INSERT INTO " + liveDB + ".resources (`player_id`, `type`, `count`, `level`) VALUES ";
        for (int i = 0; i < keyLen; i++)
        {
            if( ResourceType.isBook(res.get(i)) )
                continue;
            query += "('" + player.id + "', '" + res.get(i) + "', '" + player.resources.get(res.get(i)) + "', '" + (ResourceType.isCard(res.get(i))?player.cards.get(res.get(i)).level:0) + "')";
            query += i < keyLen - 1 ? ", " : ";";
        }
        if( query == "INSERT INTO " + liveDB + ".resources (`player_id`, `type`, `count`, `level`) VALUES " )
            return;
        try{
        db.executeInsert(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
        //trace(query);
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   EXCHANGES  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public ISFSArray getExchanges(int playerId, int now)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT type, num_exchanges, expired_at, outcome, reqs FROM " + liveDB + ".exchanges WHERE player_id=" + playerId + " OR player_id=10000", new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }
    public void updateExchange(int type, int playerId, int expireAt, int numExchanges, String outcomesStr, String reqsStr)
    {
        String query = "SELECT " + liveDB + "._func_exchanges(" + type + "," + playerId + "," + numExchanges + "," + expireAt + ",'" + outcomesStr + "', '" + reqsStr + "')";
        try {
            db.executeQuery(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   OPERATIONS  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public ISFSArray getOperations(int playerId)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT `index`,`score` FROM " + liveDB + ".operations WHERE player_id=" + playerId, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }
    public void setOperationScore(Player player, int index, int score)
    {
        try {
            if( player.operations.exists( index ) )
                db.executeUpdate("UPDATE " + liveDB + ".`operations` SET `score`='" + score + "' WHERE `index`=" + index + " AND `player_id`=" + player.id + ";", new Object[] {});
            else
                db.executeInsert("INSERT INTO " + liveDB + ".operations (`index`, `player_id`, `score`) VALUES ('" + index + "', '" + player.id + "', '" + score + "');", new Object[] {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   DECKS  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public ISFSArray createDeck(LoginData loginData, int playerId)
    {
        SFSArray decks = new SFSArray();
        //for (int di=0; di<loginData.deckSize; di++)
        for (int i=0; i<loginData.deck.length; i++)
        {
            ISFSObject so = new SFSObject();
            so.putInt("index", i);
            so.putInt("deck_index", 0);
            so.putInt("type", (int) loginData.deck.__get(i));
            decks.addSFSObject(so);
        }


        String query = "INSERT INTO " + liveDB + ".decks (`player_id`, `deck_index`, `index`, `type`) VALUES ";
        for(int i=0; i<decks.size(); i++)
        {
            query += "(" + playerId + ", " + decks.getSFSObject(i).getInt("deck_index") + ", " + decks.getSFSObject(i).getInt("index") + ",  " + decks.getSFSObject(i).getInt("type") + ")" ;
            query += i<decks.size()-1 ? ", " : ";";
        }
        trace(query);
        try {
            db.executeInsert(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
        return decks;
    }

    public ISFSArray getDecks(int playerId)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT * FROM " + liveDB + ".decks WHERE player_id = " + playerId, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }

    public int updateDeck(Player player, int deckIndex, int index, int type)
    {
        player.decks.get(deckIndex).set(index, type);
        try {
            String query = "UPDATE " + liveDB + ".decks SET `type` = "+ type +" WHERE " +
                    "NOT EXISTS (SELECT 1 FROM ( SELECT 1 FROM " + liveDB + ".decks WHERE " + liveDB + ".decks.player_id = "+ player.id +" AND " + liveDB + ".decks.deck_index = "+ deckIndex +" AND " + liveDB + ".decks.`type` = "+ type +") as c1)" + " AND " +
                    "player_id = "+ player.id +" AND deck_index = "+ deckIndex +" AND `index` = " + index;

            trace(query);
            db.executeUpdate(query, new Object[]{});
            return MessageTypes.RESPONSE_SUCCEED;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   OTHERS  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public void upgradeBuilding(Player player, int type, int level)
    {
        String query = "UPDATE " + liveDB + ".`resources` SET `level`='" + level + "' WHERE `type`=" + type + " AND `player_id`=" + player.id + ";";
        try {
        db.executeUpdate(query, new Object[] {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    public ISFSArray getFriends(int playerId)
    {
        String query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN friendship ON players.id=friendship.invitee_id OR players.id=friendship.inviter_id INNER JOIN resources ON resources.type=1001 AND players.id=resources.player_id WHERE players.id!=" + playerId + " AND friendship.inviter_id=" + playerId + " OR friendship.invitee_id=" + playerId + " ORDER BY resources.count DESC LIMIT 0,100";
        ISFSArray result = null;
        try {
            result = db.executeQuery(query, new Object[]{});
        } catch (Exception e) { e.printStackTrace(); }
        return result;
    }

    public ISFSArray getFriendships(int playerId, String coloumns)
    {
        if( coloumns == null )
            coloumns = "*";
        String query = "SELECT " + coloumns + " FROM friendship WHERE invitee_id=" + playerId + " OR inviter_id="+ playerId;
        ISFSArray result = null;
        try {
            result = db.executeQuery(query, new Object[]{});
        } catch (Exception e) { e.printStackTrace(); }
        return result;
    }


    public ISFSArray getPrefs(int id, int appVersion)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT k,v FROM " + liveDB + ".userprefs WHERE player_id=" + id, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }

        for( int i=0; i < ret.size(); i ++ )
        {
            if( ret.getSFSObject(i).getText("k").equals("101") )
            {
                if( ret.getSFSObject(i).getUtfString("v").equals("111") )
                    ret.getSFSObject(i).putUtfString("v", "141");
                else if( ret.getSFSObject(i).getUtfString("v").equals("113") )
                    ret.getSFSObject(i).putUtfString("v", "151");
                else if( ret.getSFSObject(i).getUtfString("v").equals("115") || ret.getSFSObject(i).getUtfString("v").equals("116") || ret.getSFSObject(i).getUtfString("v").equals("118") )
                    ret.getSFSObject(i).putUtfString("v", "182");
            }
        }

        return ret;
    }

    public String resetDailyBattles()
    {
        String result = "";
        try {
            db.executeUpdate("UPDATE " + liveDB + ".`exchanges` SET `num_exchanges`= 0 WHERE `type`=29 AND `num_exchanges` != 0;", new Object[] {});
        } catch (SQLException e) { return "Query failed"; }

        // reset disconnected in-battle players
        Set<Map.Entry<Integer, BBGRoom>> entries = BattleUtils.getInstance().rooms.entrySet();
        for( Map.Entry<Integer, BBGRoom> entry : entries )
        {
            List<?> games = (List<?>) entry.getValue().getProperty("games");
            for( Object game : games )
                result += resetDailyBattlesOfUsers((Game)game,  " in game " + entry.getValue().getName());
        }

        // reset connected players
        Collection<User> users = ext.getParentZone().getUserList();
        for( User u : users )
            result += resetDailyBattlesOfUsers((Game)u.getSession().getProperty("core"), "");

        return "Query succeeded.\n" + result;
    }

    private String resetDailyBattlesOfUsers(Game game, String comment)
    {
        if( game.exchanger.items.exists(ExchangeType.C29_DAILY_BATTLES) && game.exchanger.items.get(ExchangeType.C29_DAILY_BATTLES).numExchanges > 0 )
        {
            game.exchanger.items.get(ExchangeType.C29_DAILY_BATTLES).numExchanges = 0;
            return game.player.id + " daily battles reset to '0'" + comment + ".\n";
        }
        return "";
    }

    public String getPlayerNameById(int id)
    {
        try {
            String querystr = "SELECT name from players WHERE id = "+ id +" LIMIT 1";
            ISFSArray sfsArray = db.executeQuery( querystr, new Object[]{} );
            return sfsArray.getSFSObject(0).getUtfString("name");
        } catch (SQLException e) {
            e.printStackTrace();
            return "Player";
        }
    }

    public ISFSObject getDevice(int id)
    {
        String query = "SELECT * FROM devices WHERE player_id=" + id;
        ISFSArray udids = null;
        try {
            udids = db.executeQuery(query, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        if( udids != null && udids.size() > 0 )
            return udids.getSFSObject(0);
        return null;
    }

    public void traceQuery(String query)
    {
        if( DEBUG_MODE )
            System.out.println("SQLQuery: " + query);
    }
}