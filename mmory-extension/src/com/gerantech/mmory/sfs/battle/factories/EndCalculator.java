package com.gerantech.mmory.sfs.battle.factories;

import com.gerantech.mmory.sfs.battle.BattleRoom;

public class EndCalculator
{
	protected final BattleRoom room;
	public int round = 1;
	public int[] scores = new int[2];

	public EndCalculator(BattleRoom room)
	{
		this.room = room;
	}

	public boolean check()
	{
		for (int s : this.scores)
			if (s > 2)
				return true;
		return false;
	}

	public float ratio()
	{
		if( this.scores[0] == 0 && this.scores[1] == 0 )
			return 1;
		return (float) this.scores[0] / (float) this.scores[1];
	}
}
