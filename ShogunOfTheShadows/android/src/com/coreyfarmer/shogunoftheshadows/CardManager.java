package com.coreyfarmer.shogunoftheshadows;

import java.util.*;

public class CardManager
{
	private static CardManager instance;
	public static CardManager instance() {
		if(instance == null)
			instance = new CardManager();
		return instance;
	}

	private List<Card> cards = new ArrayList<Card>();
	public int cardCount() { return cards.size(); }

	public CardManager() {
		cards.add(new Card("Melee", "card_melee.png", UsageType.Combat, 90));
		cards.add(new Card("Ranged", "card_ranged.png", UsageType.Combat, 180));
		cards.add(new Card("Magic", "card_magic.png", UsageType.Combat, 270));
	}

	public Card GetCard(int index) {
		return cards.get(index);
	}
	public Card GetCard(String name) {
		Card matchingCard = null;
		for(Card card : cards) {
			if(card.name().equals(name)) {
				matchingCard = card;
				break;
			}
		}
		return matchingCard;
	}
	
	public static CardObject DecideWinner(CardObject cardObA, CardObject cardObB) {
		Card cardA = CardManager.instance().GetCard(cardObA.cardName());
		Card cardB = CardManager.instance().GetCard(cardObB.cardName());
		return DecideWinner(cardA, cardB) == cardA ? cardObA : cardObB;
	}
	
	public static Card DecideWinner(Card cardA, Card cardB) {
		int a = cardA.combatDegree();
		int b = cardB.combatDegree();
		int diff = Math.abs(a - b);
		int aResult = (a - diff + 360) % 360;
		int bResult = (b - diff + 360) % 360;
		
		//Return null for ties
		if(aResult == bResult) {
			System.out.println("Card A: " + cardA.name() + ", aResult: " + aResult + "  -TIED-  Card B: " + cardB.name() + ", bResult: " + bResult);
			return null;
		} else {
			boolean aWins = aResult > bResult;
			System.out.println("Card A: " + cardA.name() + ", aResult: " + aResult + (aWins ? " >" : " <") + " Card B: " + cardB.name() + ", bResult: " + bResult);
			return aWins ? cardA : cardB;
		}
	}
}
