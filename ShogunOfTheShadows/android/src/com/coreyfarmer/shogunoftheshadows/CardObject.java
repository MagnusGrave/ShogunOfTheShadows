package com.coreyfarmer.shogunoftheshadows;

import com.badlogic.gdx.math.Rectangle;

/**
 * Game state info of card instances.
 */
public class CardObject {
	public CardObject(Card card, Rectangle rect, ZoneType startZone, int owner_playerIndex) {
		this.cardName = card.name();
		this.rect = rect;
		this.currentZone = startZone;
		this.owner_playerIndex = owner_playerIndex;
	}
	
	private String cardName;
	public String cardName(){ return cardName; }
	
	private Rectangle rect;
	public Rectangle rect() { return rect; }
	public void SetPosition(int x, int y) {
		System.out.println("CardManager: " + cardName + ", pos: " + x + ", " + y);
		rect.setPosition(x, y);
	}
	
	private ZoneType currentZone;
	public ZoneType currentZone() { return currentZone; }
	public void SetZone(ZoneType newZone) {
		currentZone = newZone;
		if(currentZone == ZoneType.Shadow)
			isFaceDown = true;
	}
	
	private int owner_playerIndex;
	public int owner_playerIndex() { return owner_playerIndex; }
	
	//This will be used by card effects that can reveal an opponent's shadow cards.
	private boolean isFaceDown;
	public boolean IsFaceDown() { return isFaceDown; }
	public void TurnFaceUp() {
		isFaceDown = false;
	}
}
