package com.coreyfarmer.shogunoftheshadows;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameScoreInfo implements Serializable {
	private final static long serialVersionUID = 1;
	public List<RoundScoreInfo> roundScores = new ArrayList<RoundScoreInfo>();

	public void RecordRoundForPlayer(int roundIndex, String playerName, int points) {
		if(roundScores.size() <= roundIndex)
			roundScores.add(new RoundScoreInfo());
		roundScores.get(roundIndex).playerScores.add(new PlayerScoreInfo(playerName, points));
	}

	//Returns null when the game is a tie
	public String GetWinningPlayerName() {
		int[] playersWinCount = new int[roundScores.get(0).playerScores.size()];
		for(int r = 0; r < roundScores.size(); r++) {
			int highestPoints = 0;
			List<Integer> winningPlayerIndices = new ArrayList<Integer>();
			int playerIndex = 0;
			for(int p = 0; p < roundScores.get(r).playerScores.size(); p++) {
				int points = roundScores.get(r).playerScores.get(p).points;
				System.out.println(roundScores.get(r).playerScores.get(p).playerName + " Points: " + points);
				if(points >= highestPoints) {
					System.out.println("New Highscore for round: " + points + " by " + roundScores.get(r).playerScores.get(p).playerName);
					highestPoints = points;
					playerIndex = p;
				}
			}
			int prevValue = playersWinCount[playerIndex];
			playersWinCount[playerIndex] = prevValue + 1;
		}
		int winningPlayerIndex = -1;
		int mostWonGames = 0;
		for(int p = 0; p < playersWinCount.length; p++) {
			System.out.println("Indexed Player win count: " + playersWinCount[p] + " by " + roundScores.get(0).playerScores.get(p).playerName);
			if(playersWinCount[p] > mostWonGames) {
				winningPlayerIndex = p;
				mostWonGames = playersWinCount[p];
			}
		}
		
		boolean arePlayersTied = true;
		int lastScore = -1;
		for(int p = 0; p < playersWinCount.length; p++) {
			if(lastScore > -1 && playersWinCount[p] != lastScore) {
				arePlayersTied = false;
				break;
			}
			lastScore = playersWinCount[p];
		}
		System.out.println("arePlayersTied: " + arePlayersTied);
		
		if(arePlayersTied)
			return null;
		else
			return roundScores.get(0).playerScores.get(winningPlayerIndex).playerName;
	}

	public boolean IsBetterThan(GameScoreInfo comparingScoreInfo, String usersPlayerName) {
		//if the user lost then reject it immediately
		if(comparingScoreInfo.roundScores.size() < MyGdxGame.maxRoundCount || (comparingScoreInfo.GetWinningPlayerName() != null && !comparingScoreInfo.GetWinningPlayerName().equals(usersPlayerName)))
			return true;
		else {
			int thisUsersTotalScore = 0;
			for(RoundScoreInfo roundInfo : roundScores) {
				thisUsersTotalScore += roundInfo.playerScores.get(0).points;
			}
			int comparingUsersTotalScore = 0;
			for(RoundScoreInfo roundInfo : comparingScoreInfo.roundScores) {
				comparingUsersTotalScore += roundInfo.playerScores.get(0).points;
			}
			if(thisUsersTotalScore > comparingUsersTotalScore)
				return true;
			else
				return false;
		}
	}
}
