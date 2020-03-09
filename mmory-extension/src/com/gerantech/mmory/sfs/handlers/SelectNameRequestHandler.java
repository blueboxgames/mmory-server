package com.gerantech.mmory.sfs.handlers;

import java.sql.SQLException;

import com.gerantech.mmory.core.Game;
import com.gerantech.mmory.core.constants.ExchangeType;
import com.gerantech.mmory.core.constants.MessageTypes;
import com.gerantech.mmory.libs.BBGClientRequestHandler;
import com.gerantech.mmory.core.constants.SFSCommands;
import com.gerantech.mmory.libs.utils.BanUtils;
import com.gerantech.mmory.libs.utils.DBUtils;
import com.gerantech.mmory.libs.utils.ExchangeUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.util.filters.FilteredMessage;

/**
 * @author ManJav
 *
 */
public class SelectNameRequestHandler extends BBGClientRequestHandler 
{
	public SelectNameRequestHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));

		String name = params.getUtfString("name");
		trace(name, game.player.id, game.player.nickName);
		// forbidden size ...
		if( name.length() < game.loginData.nameMinLen || name.length() > game.loginData.nameMaxLen )
		{
			params.putInt("response", MessageTypes.RESPONSE_UNKNOWN_ERROR);
			send(SFSCommands.SELECT_NAME, params, sender);
			return;
		}

		// forbidden things ...
		FilteredMessage fm = BanUtils.getInstance().filterBadWords(name, false);
		if( (fm != null && fm.getOccurrences() > 0) || name.indexOf("'") > -1 )
		{
			params.putInt("response", MessageTypes.RESPONSE_NOT_ALLOWED);
			send(SFSCommands.SELECT_NAME, params, sender);
			return;
		}

		if( !game.player.nickName.equals("guest") )
		{
			int res = ExchangeUtils.getInstance().process(game, game.exchanger.items.get(ExchangeType.C42_RENAME), 0, 0);
			if( res != MessageTypes.RESPONSE_SUCCEED )
			{
				params.putInt("response", MessageTypes.RESPONSE_NOT_ENOUGH_REQS);
				send(SFSCommands.SELECT_NAME, params, sender);
				return;
			}
		}

		try {
			getDBManager().executeUpdate("UPDATE " + DBUtils.getInstance().liveDB + ".`players` SET `name`='" + name + "' WHERE `id`=" + game.player.id + ";", new Object[] {});
		} catch (SQLException e) {
			params.putText("errorCode", e.getErrorCode() + "");
			trace(e.getMessage());
		}
		game.player.nickName = name;
		params.putInt("response", MessageTypes.RESPONSE_SUCCEED);
		send(SFSCommands.SELECT_NAME, params, sender);
	}
}