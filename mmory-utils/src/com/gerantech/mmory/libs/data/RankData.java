package com.gerantech.mmory.libs.data;

import java.io.Serializable;
public class RankData implements Serializable
{
	static public final int STATUS_NAN = -1;
	static public final int STATUS_OFF = 0;
	static public final int STATUS_ON = 1;
	static public final int STATUS_BUSY = 2;
	
	private static final long serialVersionUID = 1L;
	public String name;
	public int point;
	public int status = 0;
	public RankData(String name, int point, int status)
	{
		super();
		this.name = name;
		this.point = point;
		this.status = status;
	}
}