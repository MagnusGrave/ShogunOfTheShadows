package com.coreyfarmer.shogunoftheshadows;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Player {
	// <- General ->
	private MyGdxGame game;
	private Random shuffleRandom;
	// <- Player Data ->
	private int playerIndex;
	public int playerIndex() { return playerIndex; }
	private List<String> deck = new ArrayList<String>();
	private List<CardObject> hand = new ArrayList<CardObject>();
	public List<CardObject> hand() { return hand; }
	private PlayerControlType controlType;
	public PlayerControlType controlType() { return controlType; }
	public boolean IsLocalPlayer() {
		return controlType == PlayerControlType.LocalPlayer;
	}
	private List<Integer> roundPointsList = new ArrayList<Integer>();
	// <- AI Behavior ->
	private final float AIPerfectPlayRate = 0.75f;
	
	
	public Player(MyGdxGame game, int playerIndex, PlayerControlType controlType) {
		this.game = game;
		this.playerIndex = playerIndex;
		this.controlType = controlType;
		shuffleRandom = new Random();
		
		//Generate deck - 10 of each of the three currently existing cards
		for(int i = 0; i < CardManager.instance().cardCount(); i++) {
			Card card = CardManager.instance().GetCard(i);
			for(int c = 0; c < 10; c++) {
				deck.add(card.name());
			}
		}
		ShuffleDeck(3);
	}
	
	
	private void DrawCards() {
		//Pull hand from top of deck
		for(int h = 1; h <= MyGdxGame.handSize_starting; h++) {
			String cardName = deck.remove(deck.size()-h);
			Card card = CardManager.instance().GetCard(cardName);
			Vector2 handPos = GetCardHandPosition(h-1);
			Rectangle rect = new Rectangle(handPos.x, handPos.y, MyGdxGame.cardSize_handZone.getWidth(), MyGdxGame.cardSize_handZone.getHeight());
			hand.add(new CardObject(card, rect, ZoneType.Hand, playerIndex));
		}
	}
	
	public void CondenseHand() {
		for(int h = 0; h < hand.size(); h++) {
			Vector2 pos = GetCardHandPosition(h);
			hand.get(h).SetPosition((int)pos.x, (int)pos.y);
		}
	}
	
	
	public int GetPointsByRound(int roundIndex) {
		if(roundIndex >= roundPointsList.size()) {
			System.err.println("roundIndex exceeds roundPointsList size. returning -1.");
			return -1;
		}
		return roundPointsList.get(roundIndex);
	}
	
	public void TrackPointsForRound(int roundIndex, int pointsEarned) {
		int pointTotal = roundPointsList.remove(roundIndex) + pointsEarned;
		roundPointsList.add(roundIndex, pointTotal);
	}
	
	
	public Vector2 GetCardHandPosition(CardObject card) {
		int index = hand.indexOf(card);
		System.out.println("GetHandPos() - index: " + index);
		return GetCardHandPosition(index);
	}
	
	public Vector2 GetCardHandPosition(int handIndex) {
		int x = 0;
		int y = 0;
		if(controlType == PlayerControlType.LocalPlayer) {
			int widthSection = (int)Math.round(Gdx.graphics.getWidth() / (double)MyGdxGame.handSize_starting);
			int overflowAmount = (int)Math.round((MyGdxGame.cardSize_handZone.getWidth() - widthSection) / (double)MyGdxGame.handSize_starting);
			System.out.println("widthSection: " + widthSection + ", overflowAmount: " + overflowAmount);
			x = (widthSection - overflowAmount) * handIndex;
		} else {
			int widthSection = Math.round(Gdx.graphics.getWidth() * 4f/5f) / MyGdxGame.handSize_starting;
			x = Math.round(1f/5f/2f * Gdx.graphics.getWidth())/2 + (widthSection * handIndex);
			y = Gdx.graphics.getHeight() - MyGdxGame.cardSize_handZone.getHeight();
		}
		return new Vector2(x, y);
	}
	
	public void ShuffleDeck(int shuffleCount) {
		for(int s = 0; s < shuffleCount; s++) {
			int splitOffset = shuffleRandom.nextInt(deck.size()/3) - (deck.size()/3/2);
			int splitIndex = deck.size()/2 + splitOffset;
			List<String> firstHalf = new ArrayList<String>();
			for(int f = 0; f < splitIndex; f++)
				firstHalf.add(deck.get(f));
			List<String> secondHalf = new ArrayList<String>();
			for(int g = splitIndex; g < deck.size(); g++)
				secondHalf.add(deck.get(g));
			int deckSize = deck.size();
			deck.clear();
			//Merge the two halfs randomly to simulate the shuffle
			do {
				String card = "UNKNOWN";
				if(shuffleRandom.nextBoolean()) {
					if(firstHalf.size() > 0)
						card = firstHalf.remove(0);
					else
						card = secondHalf.remove(0);
				} else {
					if(secondHalf.size() > 0)
						card = secondHalf.remove(0);
					else
						card = firstHalf.remove(0);
				}
				deck.add(card);
			} while(deck.size() < deckSize);
		}
	}
	
	//Turn State Logic - Start
	
	public void StartRound() {
		//In case the player has any cards left in hand
		hand.clear();
		
		DrawCards();
		
		roundPointsList.add(0);
	}
	
	public void StartTurn() {
		System.out.println("Player.StartTurn()");
		
		if(controlType == PlayerControlType.AI)
			game.PostAIThinkTask(2000);
	}
	
	public void StartAction() {
		System.out.println("Player.StartAction()");

		if(controlType == PlayerControlType.AI)
			game.PostAIThinkTask(2000);
	}
	
	
	public void AIMakePlay() {
		List<CardObject> shadowCards = game.GetShadowZoneCards_nonLocalPlayer();
		
		if(hand().size() == 0 && shadowCards.size() == 0) {
			game.PassAction();
			return;
		}
		
		//Apply semi-intelligent choices to AI logic
		CardObject bestPlay = null;
		//Most the time the AI makes the best possible decision, other times it miss-plays
		boolean makePerfectPlay = (shuffleRandom.nextInt(100)/100f) <= AIPerfectPlayRate;
		if(makePerfectPlay) {
			List<CardObject> cardsToBeat = game.GetDuelCardsOfOtherPlayer(playerIndex);
			for(CardObject cardToBeat : cardsToBeat) {
				List<CardObject> trumpingCards = new ArrayList<CardObject>();
				for(CardObject shadowCard : game.GetShadowZoneCards_nonLocalPlayer()) {
					if(CardManager.DecideWinner(shadowCard, cardToBeat) == shadowCard)
						trumpingCards.add(shadowCard);
				}
				for(CardObject handCard : hand) {
					if(CardManager.DecideWinner(handCard, cardToBeat) == handCard)
						trumpingCards.add(handCard);
				}
				if(trumpingCards.size() > 0)
					bestPlay = trumpingCards.get(0);
			}
		}
		
		boolean isResponseRequired = game.GetLocalDuelZoneCardCount() > 0;
		ZoneType targetZone = isResponseRequired ? ZoneType.Duel : (shuffleRandom.nextBoolean() ? ZoneType.Duel : ZoneType.Shadow);
		
		ZoneType sourceZone = ZoneType.Hand;
		if(shadowCards.size() > 0 && (hand.size() == 0 || shuffleRandom.nextBoolean()))
			sourceZone = ZoneType.Shadow;
			
		//if its coming from the shadow zone the only option is to play it
		if(sourceZone == ZoneType.Shadow)
			targetZone = ZoneType.Duel;
			
		CardObject finalChoiceCard = null;
		//Inject winning play, if one exists
		if(bestPlay != null) {
			finalChoiceCard = bestPlay;
			targetZone = ZoneType.Duel;
		} else {
			if(sourceZone == ZoneType.Hand)
				finalChoiceCard = hand.get(shuffleRandom.nextInt(hand.size()));
			else if(sourceZone == ZoneType.Shadow)
				finalChoiceCard = shadowCards.get(shuffleRandom.nextInt(shadowCards.size()));
			else
				System.err.println("Player.AIMakePlay() - Add support for ZoneType: " + sourceZone);
		}
			
		game.PlayCard(finalChoiceCard, targetZone);
	}
	
	//Turn State Logic - End
	
	public void UseCard(CardObject playedCard) {
		hand.remove(playedCard);
	}
}
