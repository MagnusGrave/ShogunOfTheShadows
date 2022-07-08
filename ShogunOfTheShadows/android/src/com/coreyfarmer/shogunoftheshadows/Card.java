package com.coreyfarmer.shogunoftheshadows;

public class Card
{
	public Card(String name, String imagePath, UsageType usageType, int combatDegree) {
		this.name = name;
		this.imagePath = imagePath;
		this.usageType = usageType;
		this.combatDegree = combatDegree;
	}

	private String name;
	public String name() { return name; }
	private String imagePath;
	public String imagePath() { return imagePath; }
	//Im unsure about this enum and its necessity
	private UsageType usageType;
	public UsageType usageType() { return usageType; }
	//The card's combat trump situation, this is a cyclical value in degrees
	//Combat outcome, simply put, is an angle comparison
	//Must be a value from 0-360
	//outcome for "a" winning = (a - diff + 360 % 360) > (b - diff + 360 % 360) with diff = |a - b|
	private int combatDegree;
	public int combatDegree() { return combatDegree; }
}
