package com.gerantech.mmory.sfs.handlers;

import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.InitData;
import com.gerantech.mmory.core.LoginData;
import com.gerantech.mmory.core.Player;
import com.gerantech.mmory.core.battle.BattleField;
import com.gerantech.mmory.core.constants.ExchangeType;
import com.gerantech.mmory.core.constants.ResourceType;
import com.gerantech.mmory.core.exchanges.ExchangeItem;
import com.gerantech.mmory.core.exchanges.ExchangeUpdater;
import com.gerantech.mmory.core.scripts.ScriptEngine;
import com.gerantech.mmory.core.utils.maps.IntIntMap;
import com.gerantech.mmory.libs.BBGRoom;
import com.gerantech.mmory.libs.data.RankData;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.gerantech.mmory.libs.utils.BattleUtils;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.ExchangeUtils;
import com.gerantech.mmory.libs.utils.HttpUtils;
import com.gerantech.mmory.libs.utils.QuestsUtils;
import com.gerantech.mmory.libs.utils.RankingUtils;
import com.gerantech.mmory.sfs.utils.LoginErrors;
import com.gerantech.mmory.sfs.utils.PasswordGenerator;
import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSConstants;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import org.apache.http.HttpStatus;

/**
 * @author ManJav
 */
public class LoginEventHandler extends BaseServerEventHandler 
{
	public static int UNTIL_MAINTENANCE = 1555744503;

	public void handleServerEvent(ISFSEvent event)
	{
try {
		String name = (String) event.getParameter(SFSEventParam.LOGIN_NAME);
		String password = (String) event.getParameter(SFSEventParam.LOGIN_PASSWORD);
		ISFSObject inData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_IN_DATA);
		ISFSObject outData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_OUT_DATA);
		ISession session = (ISession)event.getParameter(SFSEventParam.SESSION);

		int now = (int)Instant.now().getEpochSecond();
		//trace("now", now, "UNTIL_MAINTENANCE", UNTIL_MAINTENANCE);
		if( now < UNTIL_MAINTENANCE && !Player.isAdmin(inData.getInt("id")) )
		{
			outData.putInt("umt", UNTIL_MAINTENANCE - now );
			return;
		}

		// check ban
		ISFSObject banData = BanUtils.getInstance().checkBan(inData.getInt("id"), inData.getText("udid"), inData.containsKey("imei")?inData.getText("imei"):null, now);
		if( banData != null )
		{
			outData.putSFSObject("ban", banData);
			if( banData.getInt("mode") > 2 )
				return;
		}

		// check force update
		LoginData loginData = new LoginData();
		if( inData.getInt("appver") < loginData.forceVersion )
		{
			outData.putInt("forceVersion", loginData.forceVersion);
			return;
		}

		if( getParentExtension().getParentZone().containsProperty("startTime") )
		{
			outData.putInt("umt", 15);
			return;
		}

		if( inData.getInt("id") < 0 )
			createPlayer(session, name, password, inData, outData, loginData);
		else
			loadPlayer(session, name, password, inData, outData, loginData);
		outData.putText("forbidenApps", "parallel,clone,dualspace,multiapp,multiaccounts,ludashi,mochat,trendmicro");
		// Check for initial assets md5
		if( this.getParentExtension().getParentZone().getProperty("assets") != null )
			outData.putSFSObject("assets", (ISFSObject) this.getParentExtension().getParentZone().getProperty("assets"));
	
	
} catch (Exception | Error e) { e.printStackTrace();}
	}

	private void createPlayer(ISession session, String name, String password, ISFSObject inData, ISFSObject outData, LoginData loginData) throws SFSException
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String deviceUDID = inData.getText("udid");
		String deviceIMEI = inData.containsKey("imei") && !inData.getText("imei").isEmpty() ? "' OR imei='" + inData.getText("imei") : "";
		String deviceModel = inData.getText("device");
		if( inData.getInt("id") == -1 )
		{
			// retrieve user that saved account before
			try {
				ISFSArray res = dbManager.executeQuery("SELECT player_id FROM devices WHERE udid='" + deviceUDID + "' AND model='" + deviceModel + deviceIMEI + "'", new Object[]{});
				if ( res.size() > 0 )
				{
					ISFSArray res2 = dbManager.executeQuery("SELECT id, name, password FROM players WHERE id=" + res.getSFSObject(0).getInt("player_id"), new Object[]{});
					if ( res2.size() > 0 )
					{
						outData.putBool("exists", true);
						outData.putInt("id", res2.getSFSObject(0).getInt("id"));
						outData.putText("name", res2.getSFSObject(0).getText("name"));
						outData.putText("password", res2.getSFSObject(0).getText("password"));
						return;
					}
				}
			} catch (SQLException e) { e.printStackTrace(); }
		}

		password = PasswordGenerator.generate().toString();

		// Insert to DataBase
		int playerId = 0;
		try {
			playerId = Math.toIntExact((Long)dbManager.executeInsert("INSERT INTO players (name, password) VALUES ('guest', '"+password+"');", new Object[] {}));
			outData.putUtfString(SFSConstants.NEW_LOGIN_NAME, playerId + "");
		} catch (SQLException e) { e.printStackTrace(); }

		// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_- INSERT INITIAL RESOURCES -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
		// get initial user resources
		SFSArray resources = new SFSArray();
		for (int i : loginData.resources.keys())
		{
			SFSObject so = new SFSObject();

			so.putInt("type", i);
			so.putInt("count", loginData.resources.get(i));
			so.putInt("level", ResourceType.isCard(i) ? 1 : 0);

			resources.addSFSObject( so );
		}

		String query = "INSERT INTO resources (`player_id`, `type`, `count`, `level`) VALUES ";
		for(int i=0; i<resources.size(); i++)
		{
			query += "('" + playerId + "', '" + resources.getSFSObject(i).getInt("type") + "', '" + resources.getSFSObject(i).getInt("count") + "', '" + resources.getSFSObject(i).getInt("level") + "')" ;
			query += i<resources.size()-1 ? ", " : ";";
		}
		try {
			dbManager.executeInsert(query, new Object[] {});
		} catch (SQLException e) { e.printStackTrace(); }

		session.setProperty("joinedRoomId", -1);

		// send data to user
		outData.putInt("id", playerId);
		outData.putInt("sessionsCount", 0);
		outData.putText("name", "guest");
		outData.putText("password", password);
		outData.putSFSArray("resources", resources);
		outData.putSFSArray("operations", new SFSArray());
		outData.putSFSArray("exchanges", new SFSArray());
		outData.putSFSArray("prefs", new SFSArray());
		outData.putSFSArray("decks", DBUtils.getInstance().createDeck(loginData, playerId));

		// add udid and device as account id for restore players
		try {
			if( deviceUDID != null )
			{
				if( deviceIMEI == "" )
					dbManager.executeInsert("INSERT INTO devices (`player_id`, `model`, `udid`) VALUES ('" + playerId + "', '" + deviceModel + "', '" + deviceUDID + "');", new Object[] {});
				else
					dbManager.executeInsert("INSERT INTO devices (`player_id`, `model`, `udid`, `imei`) VALUES ('" + playerId + "', '" + deviceModel + "', '" + deviceUDID + "', '" + inData.getText("imei") + "');", new Object[] {});
			}
		} catch (SQLException e) { e.printStackTrace(); }
		initiateCore(session, inData, outData, loginData);
	}

	private void loadPlayer(ISession session, String name, String password, ISFSObject inData, ISFSObject outData, LoginData loginData) throws SFSException
	{
		DBUtils dbUtils = DBUtils.getInstance();
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		int id = Integer.parseInt(name);
		ISFSArray res = null;
		try { res = dbManager.executeQuery("SELECT name, password, sessions_count FROM players WHERE id=" + id, new Object[]{});
			if( res.size() == 0 )
			{
				DBUtils.getInstance().recoverFromInactives(id);
				res = dbManager.executeQuery("SELECT name, password, sessions_count FROM players WHERE id=" + id, new Object[]{});
			}
		} catch(SQLException e) { e.printStackTrace(); }

		if( res == null || res.size() != 1 )
		{
			LoginErrors.dispatch(LoginErrors.LOGIN_BAD_USERNAME, "Login error! id=" + id + " name=" + name, new String[]{"user id not found."});
			return;
		}

		ISFSObject userData = res.getSFSObject(0);
		if( !getApi().checkSecurePassword(session, userData.getText("password"), password) )
		{
			LoginErrors.dispatch(LoginErrors.LOGIN_BAD_PASSWORD, "Login error! id" + id + " inpass " + password + " dbpass:" + userData.getText("password"), new String[]{name});
			return;
		}

		// Retrieve player data from db
		outData.putInt("id", id);
		outData.putText("name", userData.getText("name"));
		// outData.putInt("createAt", Math.toIntExact(userData.getLong("create_at") / 1000));
		// outData.putInt("lastLogin", Math.toIntExact(userData.getLong("last_login")/ 1000));
		outData.putInt("sessionsCount", userData.getInt("sessions_count"));
		outData.putSFSArray("resources", dbUtils.getResources(id));
		outData.putSFSArray("operations", dbUtils.getOperations(id));
		outData.putSFSArray("exchanges", dbUtils.getExchanges(id, (int)Instant.now().getEpochSecond()));
		outData.putSFSArray("quests", new SFSArray());
		outData.putSFSArray("prefs", dbUtils.getPrefs(id, inData.getInt("appver")));
		outData.putSFSArray("decks", dbUtils.getDecks(id));

		// Find active battle room
		BBGRoom room = BattleUtils.getInstance().find(id, BattleField.STATE_2_STARTED, BattleField.STATE_2_STARTED);
		int joinedRoomId = room == null ? -1 : room.getId();
		session.setProperty("joinedRoomId", joinedRoomId);
		outData.putBool("inBattle", joinedRoomId > -1 );
		//outData.putSFSArray("challenges", ChallengeUtils.getInstance().getChallengesOfAttendee(-1, game.player, false));
		initiateCore(session, inData, outData, loginData);
	}

	private void initiateCore(ISession session, ISFSObject inData, ISFSObject outData, LoginData loginData)
	{
		int now = (int)Instant.now().getEpochSecond();
		outData.putInt("serverTime", now);
		outData.putInt("tutorialMode", 1);
		outData.putInt("noticeVersion", loginData.noticeVersion);
		outData.putInt("forceVersion", loginData.forceVersion);
		outData.putText("coreVersion", loginData.coreVersion);
		outData.putText("invitationCode", PasswordGenerator.getInvitationCode(outData.getInt("id")));
		outData.putBool("hasQuests", true);
		outData.putBool("hasOperations", true);

		InitData initData = new InitData();
		initData.nickName = outData.getText("name");
		initData.id = outData.getInt("id");
		initData.appVersion = inData.getInt("appver");
		initData.market = inData.getText("market");

		// initData.createAt = outData.getInt("createAt");
		// initData.lastLogin = outData.getInt("lastLogin");
		initData.sessionsCount = outData.getInt("sessionsCount");

		// create resources init data
		ISFSObject element;
		ISFSArray resources = outData.getSFSArray("resources");
		for(int i = 0; i < resources.size(); i++)
		{
			element = resources.getSFSObject(i);
			initData.resources.set(element.getInt("type"), element.getInt("count"));
			if( ResourceType.isCard(element.getInt("type")) )
				initData.cardsLevel.set(element.getInt("type"), element.getInt("level"));
		}

		// create decks init data
		for(int i = 0; i < outData.getSFSArray("decks").size(); i++)
		{
			element = outData.getSFSArray("decks").getSFSObject(i);
			if( !initData.decks.exists(element.getInt("deck_index")) )
				initData.decks.set(element.getInt("deck_index"), new IntIntMap());
			initData.decks.get(element.getInt("deck_index")).set(element.getInt("index"), element.getInt("type"));
		}

		// create operations init data
		ISFSArray operations = outData.getSFSArray("operations");
		for(int i = 0; i < operations.size(); i++)
		{
			element = operations.getSFSObject(i);
			initData.operations.set(element.getInt("index"), element.getInt("score"));
		}

		// create exchanges init data
		ISFSArray exchanges = outData.getSFSArray("exchanges");
		IntIntMap dbItems = new IntIntMap();
		long startTime = System.nanoTime();
		for(int i = 0; i < exchanges.size(); i++)
		{
			dbItems.set(exchanges.getSFSObject(i).getInt("type"), exchanges.getSFSObject(i).getInt("id"));
			System.out.println(exchanges.getSFSObject(i).getInt("id"));
		}
		long endTime = System.nanoTime();
		System.out.println(endTime - startTime);

		for(int i : dbItems.keys())
		{
			System.out.println(dbItems.get(i));
		}
		
		// load script
		if( ScriptEngine.script == null )
		{
			HttpUtils.Data _data = HttpUtils.post("http://localhost:8080/assets/script-data.cs", null, false);
			if( _data.statusCode != HttpStatus.SC_OK )
			{
				outData.putInt("umt", 15);
				return;
			}
			else
			{
				ScriptEngine.initialize(_data.text, inData.getInt("appver"));
				trace("http://localhost:8080/assets/script-data.cs loaded.");
			}
		}

		if( inData.getInt("appver") < 2500 )
			outData.putText("script", ScriptEngine.script);

		// initial exchanges
		boolean contained;
		for (int l=0; l<loginData.exchanges.size(); l++)
		{
			contained = false;
			for(int i=0; i<exchanges.size(); i++)
			{
				element = exchanges.getSFSObject(i);
				if( element.getInt("type") == loginData.exchanges.get(l) )
				{
					contained = true;
					break;
				}
			}
			if( !contained && ( initData.appVersion < 3200 || ExchangeType.getCategory(loginData.exchanges.get(l)) != ExchangeType.C20_SPECIALS || initData.resources.get(ResourceType.R2_POINT) > 100 ))// add special after arena 2
				addSFSExchange(loginData.exchanges.get(l), exchanges);
		}

		// initial core
		Game game = new Game();
		game.init(initData);
		game.player.tutorialMode = outData.getInt("tutorialMode");
		game.player.hasOperations = outData.getBool("hasOperations");
		game.exchanger.updater = new ExchangeUpdater(game, now);

		game.player.resourceIds = new ConcurrentHashMap<>();
		for(int i = 0; i < resources.size(); i++)
		{
			game.player.resourceIds.put(resources.getSFSObject(i).getInt("type"), resources.getSFSObject(i).getInt("id"));
			resources.getSFSObject(i).removeElement("id");
		}

		for(int i = 0; i < exchanges.size(); i++)
		{
			element = exchanges.getSFSObject(i);
			game.exchanger.updater.add(element.getInt("type"), element.getInt("num_exchanges"), element.getInt("expired_at"), element.getText("reqs"), element.getText("outcome"));
		}

		game.exchanger.updater.addItems();

		for( ExchangeItem item : game.exchanger.updater.changes )
			DBUtils.getInstance().updateExchange(item.type, game.player.id, item.expiredAt, item.numExchanges, item.outcomes.toString(), item.requirements.toString());

		// create exchange data
		SFSArray _exchanges = new SFSArray();
		int[] keys = game.exchanger.items.keys();
		for( int t : keys )
			_exchanges.addSFSObject(ExchangeUtils.toSFS(game.exchanger.items.get(t)));
		outData.putSFSArray("exchanges", _exchanges);

		// init and update in memory data base
		ConcurrentHashMap<Integer, RankData> users = RankingUtils.getInstance().getUsers();
		RankData rd = new RankData(game.player.nickName,  game.player.get_point());
		if( users.containsKey(game.player.id))
			users.replace(game.player.id, rd);
		else
			users.put(game.player.id, rd);

		// insert quests in registration or get in next time
		ISFSArray quests = QuestsUtils.getInstance().getAll(game.player.id);
		if( quests.size() > 0 )
			QuestsUtils.getInstance().updateAll(game.player, quests);
		else
			QuestsUtils.getInstance().insertNewQuests(game.player);
		outData.putSFSArray("quests", QuestsUtils.toSFS(game.player.quests));

		session.setProperty("core", game);
	}

	private void addSFSExchange(int type, ISFSArray exchanges)
	{
		SFSObject element = new SFSObject();
		element.putInt("type", type);
		element.putInt("num_exchanges", 0);
		element.putInt("expired_at", 1);
		element.putText("outcome", "");
		exchanges.addSFSObject(element);
	}
}