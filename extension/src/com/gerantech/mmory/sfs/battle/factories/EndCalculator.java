package com.gerantech.mmory.sfs.battle.factories;

import com.gerantech.mmory.sfs.battle.BattleRoom;

public class EndCalculator
{
	protected final BattleRoom room;
	public int[] scores = new int[2];

	public EndCalculator(BattleRoom roomClass)
	{
		this.room = roomClass;
	}

	public boolean check()
	{
		for (int s : scores)
			if (s > 2)
				return true;
		return false;
	}

	public float ratio()
	{
		if( scores[0] == 0 && scores[1] == 0 )
			return 1;
		return (float) scores[0] / (float) scores[1];
	}
}
