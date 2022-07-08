package com.coreyfarmer.shogunoftheshadows;

import java.io.*;
import java.util.*;

public class PlayerScoreInfo implements Serializable {
	private final static long serialVersionUID = 2;
	public String playerName;
	public int points;
	
	public PlayerScoreInfo(String playerName, int points) {
		this.playerName = playerName;
		this.points = points;
	}
}
