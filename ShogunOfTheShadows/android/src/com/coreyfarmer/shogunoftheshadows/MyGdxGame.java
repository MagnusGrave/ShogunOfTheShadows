package com.coreyfarmer.shogunoftheshadows;

import android.os.Handler;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

//import org.apache.http.conn.util.*;


public class MyGdxGame implements ApplicationListener
{
	// <- General Scene Stuff ->
	private OrthographicCamera camera;
	private SpriteBatch batch;
	
	
	// <- Title Screen ->
	private Texture titleBgTex;
	private TextButton playButton;
	private TextButton topScoreButton;
	private Texture youtubeTex;
	private Button githubLinkButton;
	private Button youtubeLinkButton;
	private Button bandcampLinkButton;
	
	
	// <- Game Graphics ->
	private Texture bgTex;
	//Holds the texture for each card image mapped by Name.
	//Multiple instances of the same card will all use one single
	//instance of the texture for rendering.
	private Map<String,Texture> cardTextures;
	private Texture faceDownOverlayTex;
	private final String cardBackName = "cardBack";
	private final String cardBackImagePath = "cardBack.png";
	private static final int cardWidth = 260;
	private static final float cardSizeRatio = 64f/44;
	private static int cardHeight = Math.round( cardWidth * cardSizeRatio );
	private static final float cardIngameScale = 352f/cardWidth;
	public static final Size cardSize_handZone = new Size(cardWidth, Math.round(cardWidth * cardSizeRatio));
	public static final Size cardSize_duelZone = cardSize_handZone;
	public static final Size cardSize_shadowZone = cardSize_handZone;
	
	
	// <- Game Properties ->
	public static final int handSize_starting = 6;
	//Players manage their own decks and hands until a CardObject is played.
	//It is then moved to the duelZoneCards list. The CardObject remembers its owner.
	private java.util.List<Player> players = new ArrayList<Player>();
	//turnTaker is characteristic of drawing cards and starting duels with other players
	private Player turnTaker;
	//Theres an important distinction between this and turnTaker.
	//actionTaker is the player responding to the turnTaker's latest action.
	//During a turn priority changes from player to player as each performs an action.
	private Player actionTaker;
	private int currentRoundIndex;
	public static final int maxRoundCount = 3;
	private boolean actionTakerResponseRequired;
	private boolean hasOutcomeBeenDetermined;
	private static final String topScoreFileName = "topScore.txt";
	private GameScoreInfo currentGameScore;
	private GameScoreInfo topScoreInfo;
	private enum WaitState { Outcome }
	private WaitState waitState;
	
	
	// <- Duel Mechanic ->
	//These are the cards that have been played and are still unresolved.
	private java.util.List<CardObject> duelZoneCards_localPlayer = new ArrayList<CardObject>();
	public int GetLocalDuelZoneCardCount() {
		return duelZoneCards_localPlayer.size();
	}
	private java.util.List<CardObject> duelZoneCards_nonLocalPlayer = new ArrayList<CardObject>();
	private Rectangle duelZoneRect;
	private Texture duelZoneTex;
	// Duel Subset - Shadow Mechanics
	private java.util.List<CardObject> shadowZoneCards_localPlayer = new ArrayList<CardObject>();
	private java.util.List<CardObject> shadowZoneCards_nonLocalPlayer = new ArrayList<CardObject>();
	public java.util.List<CardObject> GetShadowZoneCards_nonLocalPlayer() {
		return shadowZoneCards_nonLocalPlayer;
	}
	private java.util.List<CardObject> cardsPlayedFromShadowZoneThisTurn = new ArrayList<CardObject>();
	private Rectangle shadowZoneRect;
	private Rectangle shadowZoneRect_nonlocal;
	private Texture shadowZoneTex;
	private float zoneIngameScale;
	
	
	// <- Card Drag Behavior ->
	private boolean disableCardInput;
	private CardObject draggedCard;
	private CardObject returningCard;
	private final int FPS = 60;
	private final long intervalDuration = 1000l/FPS;
	private final int maxTimerIntervals = 26;
	private final int minTimerIntervals = 2;
	private Handler cardReturnHandler = new Handler();
	private ReturnRunnable returnRunTask = new ReturnRunnable();
	class ReturnRunnable implements Runnable {
		private Vector2 startPos;
		private Vector2 targetPos;
		private int frameCount;
		private int duration;
		public Runnable Init(Vector2 startPos, Vector2 targetPos) {
			System.out.println("Timer Constructor() - returningCard: " + returningCard.cardName());
			this.startPos = startPos;
			this.targetPos = targetPos;
			this.frameCount = 0;
			this.duration = Math.max(minTimerIntervals,  Math.round( maxTimerIntervals * (Vector2.dst(startPos.x, startPos.y, targetPos.x, targetPos.y) / Gdx.graphics.getWidth()) )  );
			return this;
		}
		
		public void run() {
			float t = (float)frameCount / duration;
			int newX = Math.round( MathUtils.lerp((float)startPos.x, (float)targetPos.x, t) );
			int newY = Math.round( MathUtils.lerp((float)startPos.y, (float)targetPos.y, t) );
			if(newX == 0 && newY == 0)
				System.err.println("Return pos is zero, handPos: " + targetPos + ", t: " + t + ", frameCount: " + frameCount + " / duration: " + duration);
			returningCard.SetPosition(newX, newY);
			frameCount++;
			if(frameCount > duration) {
				System.out.println("ReturnRunnable Done - targetPos: " + targetPos);
				returningCard = null;
			} else
			cardReturnHandler.postDelayed(this, intervalDuration);
		}
	}
	
	
	// <- Player Timer ->
	private Handler aiThinkHandler = new Handler();
	private Runnable aiThinkTask = new Runnable() {
		public void run() {
			FireAIThinkTask();
		}
	};

	public void PostAIThinkTask(int milli) {
	aiThinkHandler.postDelayed(aiThinkTask, milli);
	}
	
	private void FireAIThinkTask() {
		players.get(1).AIMakePlay();
	}
	
	
	// <- HUD ->
	private Stage stage;
	private BitmapFont pixelFont;
	private Point namePos_localPlayer;
	private Point namePos_otherPlayer;
	private Point winCountPos_localPlayer;
	private Point winCountPos_otherPlayer;
	private Skin skin;
	private TextArea textArea;
	private Texture infoPanelTex;
	private final float infoPanelHeightRatio = 1f/10f;
	private Rectangle infoPanelRect;
	private TextButton okButton;
	//Help Overlay
	private Button helpButton;
	private Image helpBgImage;
	private ScrollPane helpScrollPane;
	private boolean helpMenuOpen;
	private Label helpLabel;
	private Table outerTable;
	private Texture helpSymbolTex;
	
	// <- Score Menu ->
	enum ViewType { Title, Game }
	private ViewType currentViewType = ViewType.Title;
	private boolean showingScoreOverlay;
	private Texture scoreBgTex;
	private TextArea scoreTextArea;
	private TextButton backButton;
	private TextButton playAgainButton;
	private TextButton nextRoundButton;
	
	
	// <- Audio ->
	private Sound sound_shuffle;
	private Sound sound_pullCard;
	private Sound sound_flipCard;
	private Sound sound_playCard;
	private Sound sound_swordClang;
	private Sound sound_affirmation;
	private Sound sound_slideCard;
	private Music music_emblazonedSong;
	
	
	
	@Override
	public void create()
	{
		DisplayMode[] modes = Gdx.graphics.getDisplayModes();
		for (DisplayMode mode : modes) {
			System.out.println("Display Mode: " + mode);
		}
		
		//This needs to be called for the Handlers to work properly
		//HANDLER//Looper.prepare();
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		batch = new SpriteBatch();
		
		bgTex = new Texture(Gdx.files.internal("gameBG.png"));
		
		
		
		cardTextures = new HashMap<String,Texture>();
		cardTextures.put(cardBackName, new Texture(Gdx.files.internal(cardBackImagePath)));
		for(int i = 0; i < CardManager.instance().cardCount(); i++) {
			Card card = CardManager.instance().GetCard(i);
			cardTextures.put(card.name(), new Texture(Gdx.files.internal(card.imagePath())));
		}
		faceDownOverlayTex = new Texture(Gdx.files.internal("faceDownOverlay.png"));
		
		
		int duelZoneWidth = Math.round(Gdx.graphics.getWidth() * 0.9f);
		int duelZoneHeight = Math.round(duelZoneWidth * 96f/160f);
		duelZoneRect = new Rectangle((Gdx.graphics.getWidth() - duelZoneWidth) / 2, (Gdx.graphics.getHeight() - duelZoneHeight) / 2, duelZoneWidth, duelZoneHeight);
		duelZoneTex = new Texture(Gdx.files.internal("duelZoneBG.png"));
		
		int shadowZoneWidth = Math.round(Gdx.graphics.getWidth() * 0.9f);
		int shadowZoneHeight = Math.round(shadowZoneWidth * 24f/160f);
		int shadowZonePadding = 8;
		zoneIngameScale = 2560f / shadowZoneWidth;
		shadowZoneRect = new Rectangle((Gdx.graphics.getWidth() - shadowZoneWidth) / 2, Gdx.graphics.getHeight()/2 - shadowZoneHeight - (duelZoneHeight/2) - shadowZonePadding, shadowZoneWidth, shadowZoneHeight);
		shadowZoneRect_nonlocal = new Rectangle(shadowZoneRect.x, Gdx.graphics.getHeight() - shadowZoneRect.y - shadowZoneRect.height, shadowZoneRect.width, shadowZoneRect.height);
		shadowZoneTex = new Texture(Gdx.files.internal("shadowZoneBG.png"));
		
		infoPanelTex = new Texture(Gdx.files.internal("sizeTest.png"));
		int infoPanelHeight = Math.round(Gdx.graphics.getHeight() * infoPanelHeightRatio);
		infoPanelRect = new Rectangle(0, Gdx.graphics.getHeight() - infoPanelHeight, Gdx.graphics.getWidth(), infoPanelHeight);
		
		
		stage = new Stage();
		
		pixelFont = new BitmapFont(Gdx.files.internal("fonts/Awkward.fnt"), Gdx.files.internal("fonts/Awkward_0.png"), false);
		pixelFont.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		pixelFont.getData().setScale(3f); //.setScale(3f);
		
		namePos_localPlayer = new Point((int)(Gdx.graphics.getWidth() * (1f/16)), (int)(Gdx.graphics.getHeight() * (7f/32)));
		namePos_otherPlayer =  new Point((int)(Gdx.graphics.getWidth() * (1f/16)), (int)(Gdx.graphics.getHeight() * (51f/64)));
		
		winCountPos_localPlayer = new Point((int)(Gdx.graphics.getWidth() * (12f/16)), (int)(Gdx.graphics.getHeight() * (7f/32)));
		winCountPos_otherPlayer =  new Point((int)(Gdx.graphics.getWidth() * (12f/16)), (int)(Gdx.graphics.getHeight() * (51f/64)));
		
		skin = new Skin();
		TextFieldStyle fieldStyle = new TextFieldStyle();
		fieldStyle.font = pixelFont;
		fieldStyle.fontColor = Color.WHITE;
		skin.add("default", fieldStyle);
		
		textArea = new TextArea("CREATED", skin);
		textArea.setX(50);
		textArea.setY(Gdx.graphics.getHeight() - 430);
		textArea.setWidth(980);
		textArea.setHeight(400);
		textArea.setDisabled(true);
		textArea.setVisible(false);
		stage.addActor(textArea);

		TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
		textButtonStyle.font = pixelFont;
		// Start - Button State BGs
		Texture buttonUpBgTex = new Texture(Gdx.files.internal("ButtonState_Up.png"));
		textButtonStyle.up = new Image(buttonUpBgTex).getDrawable();
		textButtonStyle.fontColor = Color.BLUE.mul(Color.GRAY);
		Texture buttonDownBgTex = new Texture(Gdx.files.internal("ButtonState_Down.png"));
		textButtonStyle.down = new Image(buttonDownBgTex).getDrawable();
		textButtonStyle.downFontColor = Color.BLUE.mul(Color.GRAY);
		Texture buttonHoverBgTex = new Texture(Gdx.files.internal("ButtonState_Hover.png"));
		textButtonStyle.over = new Image(buttonHoverBgTex).getDrawable();
		textButtonStyle.overFontColor = Color.BLUE.mul(Color.GRAY);
		Texture buttonDisabledBgTex = new Texture(Gdx.files.internal("ButtonState_Disabled.png"));
		textButtonStyle.disabled = new Image(buttonDisabledBgTex).getDrawable();
		textButtonStyle.disabledFontColor = Color.BLUE.mul(Color.GRAY);
		
		//Score Screen
		scoreBgTex = new Texture(Gdx.files.internal("scoreScreenBG.png"));
		scoreTextArea = new TextArea("Score", skin);
		int scorePadding = 50;
		scoreTextArea.setX(scorePadding);
		int scoreTextHeight = Gdx.graphics.getHeight() - (scorePadding * 2);
		scoreTextArea.setY(Gdx.graphics.getHeight() - scoreTextHeight - scorePadding);
		scoreTextArea.setWidth(980);
		scoreTextArea.setHeight(scoreTextHeight);
		scoreTextArea.setDisabled(true);
		stage.addActor(scoreTextArea);
		
		
		int okButtonWidth = Math.round(Gdx.graphics.getWidth() * (1f/2));
		int okButtonHeight = Math.round(okButtonWidth * (2f/3));
		okButton = new TextButton("OK", textButtonStyle);
		okButton.setBounds(Gdx.graphics.getWidth()/2 - (okButtonWidth/2), Gdx.graphics.getHeight() * 1 / 4 - okButtonHeight, okButtonWidth, okButtonHeight);
		stage.addActor(okButton);
		okButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				//System.out.println("Ok Button");
				playAffirmation();
				Ok();
			}
		});
		okButton.setVisible(false);
		okButton.setDisabled(true);
		

		//Title Screen Setup
		titleBgTex = new Texture(Gdx.files.internal("titleBG.png"));
		playButton = new TextButton("Play", textButtonStyle);
		//playButton.setBounds(Gdx.graphics.getWidth()/2 - (okButtonWidth/2), Gdx.graphics.getHeight() * 3 / 8 - Math.round(okButtonHeight * 0.75f), okButtonWidth, okButtonHeight);
		//Side by side layout test
		int sbsButtonWidth = Math.round(Gdx.graphics.getWidth() * (1f/3));
		int sbsButtonHeight = Math.round(sbsButtonWidth * (2f/3));
		playButton.setBounds(Gdx.graphics.getWidth()/2 - sbsButtonWidth - 75, (Gdx.graphics.getHeight() * 3 / 8 - Math.round(sbsButtonHeight * 0.75f)) / 2, sbsButtonWidth, sbsButtonHeight);
		stage.addActor(playButton);
		playButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				//System.out.println("Play Button");
				playAffirmation();
				StartGame();
			}
		});
		

		int socialButtonWidth = Math.round(Gdx.graphics.getWidth() * (1f/8));
		int socialButtonHeight = Math.round(socialButtonWidth * (1f/1));
		//Hyperlink buttons
		Texture githubTex = new Texture(Gdx.files.internal("githubSocial.png"));
		githubLinkButton = new Button(new SpriteDrawable(new Sprite(githubTex)));
		githubLinkButton.setBounds(Gdx.graphics.getWidth()/3f - (Gdx.graphics.getWidth()/3f/2f) - (socialButtonWidth/2f), Gdx.graphics.getHeight() * (1f/14) - (socialButtonHeight/2f), socialButtonWidth, socialButtonHeight);
		stage.addActor(githubLinkButton);
		githubLinkButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.net.openURI("https://github.com/MagnusGrave");
				event.handle();
			}
		});
		youtubeTex = new Texture(Gdx.files.internal("youtubeSocial.png"));
		youtubeLinkButton = new Button(new SpriteDrawable(new Sprite(youtubeTex)));
		youtubeLinkButton.setBounds(Gdx.graphics.getWidth() * 2/3f - (Gdx.graphics.getWidth()/3f/2f) - (socialButtonWidth/2f), Gdx.graphics.getHeight() * (1f/14) - (socialButtonHeight/2f), socialButtonWidth, socialButtonHeight);
		stage.addActor(youtubeLinkButton);
		youtubeLinkButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.net.openURI("https://m.youtube.com/user/KydenXuD");
				event.handle();
			}
		});
		Texture bandcampTex = new Texture(Gdx.files.internal("bandcampSocial.png"));
		bandcampLinkButton = new Button(new SpriteDrawable(new Sprite(bandcampTex)));
		bandcampLinkButton.setBounds(Gdx.graphics.getWidth() - (Gdx.graphics.getWidth()/3f/2f) - (socialButtonWidth/2f), Gdx.graphics.getHeight() * (1f/14) - (socialButtonHeight/2f), socialButtonWidth, socialButtonHeight);
		stage.addActor(bandcampLinkButton);
		bandcampLinkButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.net.openURI("https://faet.bandcamp.com/");
				event.handle();
			}
		});
		
		
		playAgainButton = new TextButton("Play Again", textButtonStyle);
		//playAgainButton.setBounds(Gdx.graphics.getWidth()/2 - (okButtonWidth/2), Gdx.graphics.getHeight() * 3 / 8 - Math.round(okButtonHeight * 0.75f), okButtonWidth, okButtonHeight);
		playAgainButton.setBounds(Gdx.graphics.getWidth()/2 + 75, (Gdx.graphics.getHeight() * 3 / 8 - Math.round(sbsButtonHeight * 0.75f)) / 2, sbsButtonWidth, sbsButtonHeight);
		stage.addActor(playAgainButton);
		playAgainButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					//System.out.println("Play Button");
					playAffirmation();
					CloseScoreMenu();
					StartGame();
				}
			});
		playAgainButton.setVisible(false);
		playAgainButton.setDisabled(true);
		
		nextRoundButton = new TextButton("Next Round", textButtonStyle);
		//nextRoundButton.setBounds(Gdx.graphics.getWidth()/2 - (okButtonWidth/2), Gdx.graphics.getHeight() * 3 / 8 - Math.round(okButtonHeight * 0.75f), okButtonWidth, okButtonHeight);
		nextRoundButton.setBounds(Gdx.graphics.getWidth()/2 - (okButtonWidth/2), Gdx.graphics.getHeight() * 1 / 8 - (okButtonHeight / 2), okButtonWidth, okButtonHeight);
		stage.addActor(nextRoundButton);
		nextRoundButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					//System.out.println("Play Button");
					playAffirmation();
					CloseScoreMenu();
					StartRound();
				}
			});
		nextRoundButton.setVisible(false);
		nextRoundButton.setDisabled(true);
		
		topScoreButton = new TextButton("Top Score", textButtonStyle);
		//topScoreButton.setBounds(Gdx.graphics.getWidth()/2 - (okButtonWidth/2), Gdx.graphics.getHeight() * 1 / 8 - (okButtonHeight / 2), okButtonWidth, okButtonHeight);
		//Side by side layout
		topScoreButton.setBounds(Gdx.graphics.getWidth()/2 + 75, (Gdx.graphics.getHeight() * 3 / 8 - Math.round(sbsButtonHeight * 0.75f)) / 2, sbsButtonWidth, sbsButtonHeight);
		stage.addActor(topScoreButton);
		topScoreButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				//System.out.println("Top Score Button");
				playAffirmation();
				ShowScoreMenu();
			}
		});
		
		backButton = new TextButton("Main Menu", textButtonStyle);
		//backButton.setBounds(Gdx.graphics.getWidth()/2 - (okButtonWidth/2), Gdx.graphics.getHeight() * 1 / 8 - (okButtonHeight / 2), okButtonWidth, okButtonHeight);
		backButton.setBounds(Gdx.graphics.getWidth()/2 - sbsButtonWidth - 75, (Gdx.graphics.getHeight() * 3 / 8 - Math.round(sbsButtonHeight * 0.75f)) / 2, sbsButtonWidth, sbsButtonHeight);
		stage.addActor(backButton);
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				//System.out.println("Back Button");
				playAffirmation();
				if(currentViewType == ViewType.Title)
					CloseScoreMenu();
				else
					BackToTitle();
			}
		});
		backButton.setVisible(false);
		backButton.setDisabled(true);
		
		
		//Help Overlay
		helpBgImage = new Image(new SpriteDrawable(new Sprite(bgTex)));
		helpBgImage.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		stage.addActor(helpBgImage);
		Table innerTable = new Table();
		outerTable = new Table();
		helpScrollPane = new ScrollPane(innerTable);
		helpScrollPane.layout();
		helpScrollPane.setScrollingDisabled(true, false);
		outerTable.add(helpScrollPane);
		outerTable.row();
		int heightPadding = 20;
		outerTable.setX(0);
		outerTable.setY(heightPadding/2);
		outerTable.setWidth(Gdx.graphics.getWidth());
		outerTable.setHeight(Gdx.graphics.getHeight() - heightPadding);
		stage.addActor(outerTable);
		//Setup actual text holder component
		LabelStyle helpLabelStyle = new LabelStyle(pixelFont, Color.WHITE);
		String contents =
			"-= How To Play =-\n"+
			"\n"+
			"At its core this game is like Rock, Paper, Scissors. When two players play cards to the duel zone those cards are then compared. One card will trump the other. "+
			"This trump card wins the duel and the card's owner gains a number of points. The trumping occurs as such: Ranged trumps Melee, Melee trumps Magic, Magic trumps Ranged.\n"+
			"\n"+
			"Ranged > Melee > Magic > Ranged\n"+
			"\n"+
			"-Game Flow-\n"+
			"The game functions in a series of rounds. Each round is a series of turns. Within a turn the active turn-taker performs an action, which may or may not require a response from their opponent. "+
			"Turns go back and forth until both players are out of cards; at which point the round ends. A full game consists of 3 rounds. Whoever wins the most rounds wins the game.\n"+
			"\n"+
			"-Actions-\n"+
			"As the active turn-taker, there's a choice of two actions: to play a card to the Duel Zone or to play a card to the Shadow Zone. Playing a card from Hand to Duel Zone is referred to as playing a card directly. "+
			"When the turn-taker plays directly to the Duel Zone the card is played face-up. This makes the attack more obvious and easier for the opponent to trump but with this risk comes reward: if the attack wins it yields more points. "+
			"If the turn-taker chooses to play to the Duel Zone then the opponent must respond by playing a card to the Duel Zone. When players play matching cards then the tie is won by the active turn-taker. If, however, the turn-taker chooses to play a card to the Shadow Zone then there "+
			"is no opportunity to respond and the turn ends. Playing a card to the Shadow Zone is a way to prepare an attack that retains the element of surprise. Think of Shadow Zone plays as sneak attacks. "+
			"This is because cards played to Shadow Zone are "+
			"played face-down. During a subsequent turn, a card in the Shadow Zone can be played to the Duel Zone and it will remain face-down until the opponent has responded. The disadvantage of this tactic is "+
			"that winning a duel this way is worth less points, as the turn-taker. In the case of a tie, if one card was played from the Shadow Zone and the other card was played from a hand then the Shadow Zone "+
			"card wins.\n"+
			"\n"+
			"-Duel Points-\n"+
			"Attacking directly = 2 points\n"+
			"Attacking stealthily = 1 point\n"+
			"Defending directly/stealthily = 1 point\n"+
			"\n";
		helpLabel = new Label(contents, helpLabelStyle);
		helpLabel.setWrap(true);
		helpLabel.setWidth(1050);
		innerTable.add(helpLabel).width(1050f);
		innerTable.row();
		helpSymbolTex = new Texture(Gdx.files.internal("helpSymbol.png"));
		helpButton = new Button(new SpriteDrawable(new Sprite(helpSymbolTex)));
		int helpButtonSize = 100;
		helpButton.setBounds(Gdx.graphics.getWidth() - helpButtonSize, Gdx.graphics.getHeight() - helpButtonSize, helpButtonSize, helpButtonSize);
		stage.addActor(helpButton);
		helpButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				//System.out.println("Back Button");
				playAffirmation();
				ToggleHelpMenu();
			}
		});
		helpBgImage.setVisible(false);
		outerTable.setVisible(false);
		helpLabel.setVisible(false);
		
		
		InputProcessor cardInputProcessor = new InputProcessor() {
			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				if(disableCardInput)
					return false;
				
				if(pointer > 0) {
					System.out.println("touchDown: false, pointer > 0");
					return false;
				}
				
				if(draggedCard != null) {
					System.out.println("touchDown: false, draggedCard exists");
					return false;
				}
				
				draggedCard = null;
				Player localPlayer = null;
				for(Player player : players) {
					if(player.controlType() == PlayerControlType.LocalPlayer) {
						localPlayer = player;
						break;
					}
				}
				//need to iterate thru this in reverse so the cards sitting on top get picked
				for(int i = localPlayer.hand().size() - 1; i > -1; i--) {
					CardObject co = localPlayer.hand().get(i);
					if(co != returningCard && co.rect().contains(screenX, Gdx.graphics.getHeight() - screenY)) {
						draggedCard = co;
						break;
					}
				}
				
				if(draggedCard != null && draggedCard == returningCard) {
					System.out.println("touchDown: false, new draggedCard is returningCard");
					return false;
				}
				
				if(draggedCard == null && localPlayer == actionTaker) {
					for(CardObject sz : shadowZoneCards_localPlayer) {
						if(sz != returningCard && sz.rect().contains(screenX, Gdx.graphics.getHeight() - screenY)) {
							draggedCard = sz;
							break;
						}
					}
				}
				
				if(draggedCard != null)
					sound_pullCard.play();
				
				System.out.println("touchDown: true");
				return true;
			}
			@Override
			public boolean touchUp(int screenX, int screenY, int pointer, int button) {
				if(disableCardInput)
					return false;
				
				if(pointer > 0) {
					System.out.println("touchUp: false");
					return false;
				}
				
				System.out.println("touchUp: true");
				
				if(draggedCard != null) {
					boolean inDuelZone = duelZoneRect.contains(screenX, screenY);
					boolean inShadowZone = shadowZoneRect.contains(screenX, Gdx.graphics.getHeight() - screenY);
					if(actionTaker.controlType() == PlayerControlType.LocalPlayer && (inDuelZone || (inShadowZone && !actionTakerResponseRequired))) {
						PlayCard(draggedCard, inDuelZone ? ZoneType.Duel : ZoneType.Shadow);
					} else {
						System.out.println("return timer. is already active: " + (returningCard != null));
						
						returningCard = draggedCard;
						Vector2 targetPos = null;
						if(shadowZoneCards_localPlayer.contains(returningCard))
							targetPos = GetShadowZonePosition(actionTaker, shadowZoneCards_localPlayer.size(), shadowZoneCards_localPlayer.indexOf(returningCard));
						else
							targetPos = players.get(returningCard.owner_playerIndex()).GetCardHandPosition(returningCard);
						cardReturnHandler.post(returnRunTask.Init(new Vector2(returningCard.rect().x, returningCard.rect().y), targetPos));
						
						sound_slideCard.play();
					}
				}
				draggedCard = null;
				
				return true;
			}
			@Override
			public boolean touchDragged(int x, int y, int pointer) {
				if(disableCardInput)
					return false;
				
				if(pointer > 0) {
					System.out.println("touchDragged: false");
					return false;
				}
				
				if(draggedCard != null) {
					System.out.println("touchDragged: " + x + ", " + y);
					int posX = MathUtils.clamp(x - Math.round(draggedCard.rect().width/2f), 0, (int)(Gdx.graphics.getWidth() - draggedCard.rect().width));
					int posY = MathUtils.clamp((int)Gdx.graphics.getHeight() - y - Math.round(draggedCard.rect().height/2f), 0, (int)Gdx.graphics.getHeight() - (int)draggedCard.rect().height);
					draggedCard.SetPosition(posX, posY);
				}
				
				return true;
			}
			@Override
			public boolean keyTyped(char keyChar) {
				//System.out.println("keyTyped");
				return true;
			}
			@Override
			public boolean keyUp(int p1) {
				//System.out.println("keyUp");
				return true;
			}
			@Override
			public boolean keyDown(int p1) {
				//System.out.println("keyDown");
				return true;
			}
			@Override
			public boolean mouseMoved(int p1, int p2) {
				//System.out.println("mouseMoved");
				return true;
			}
			/*@Override
			public boolean scrolled(int p1) {
				//System.out.println("scrolled");
				return true;
			}*/

			@Override
			public boolean scrolled(float p1, float p2) {
				//System.out.println("scrolled");
				return true;
			}

		};
		Gdx.input.setInputProcessor(new InputMultiplexer(stage, cardInputProcessor));
		
		//Audio
		sound_shuffle = Gdx.audio.newSound(Gdx.files.internal("sounds/clip_shuffle.mp3"));
		sound_pullCard = Gdx.audio.newSound(Gdx.files.internal("sounds/clip_pullCard.mp3"));
		sound_playCard = Gdx.audio.newSound(Gdx.files.internal("sounds/clip_playCard.mp3"));
		sound_flipCard = Gdx.audio.newSound(Gdx.files.internal("sounds/clip_flipCard.wav"));
		sound_swordClang = Gdx.audio.newSound(Gdx.files.internal("sounds/clip_swordClang.wav"));
		sound_affirmation = Gdx.audio.newSound(Gdx.files.internal("sounds/clip_affirmation.wav"));
		sound_slideCard = Gdx.audio.newSound(Gdx.files.internal("sounds/clip_slideCard.wav"));
		music_emblazonedSong = Gdx.audio.newMusic(Gdx.files.internal("sounds/song_emblazonedOutcastes.mp3"));
		music_emblazonedSong.play();
		music_emblazonedSong.setLooping(true);
		music_emblazonedSong.setVolume(0.65f);
		
		// <- Load top score ->
		//Load the string from some file
		FileHandle handle = Gdx.files.local(topScoreFileName);
		if(handle.exists()) {
			try {
				topScoreInfo = ( GameScoreInfo ) fromBytes( handle.readBytes() );
			} catch(IOException ioe) {
				System.err.println("create() : Loading topScore - Oops, IOException: " + ioe);
				topScoreInfo = new GameScoreInfo();
			} catch(ClassNotFoundException cne) {
				System.err.println("create() : Loading topScore - Oops, ClassNotFoundException: " + cne);
				topScoreInfo = new GameScoreInfo();
			}
		} else {
			topScoreInfo = new GameScoreInfo();
		}
		
		
		BackToTitle();
	}
	

	//Decode the object from a byte array
	private static Object fromBytes( byte[] bytes) throws IOException , ClassNotFoundException {
		byte[] data = bytes;
		ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( data ) ); 
		Object o = ois.readObject(); 
		ois.close(); 
		System.out.println("fromString() - success, object: " + o.toString());
		return o; 
	}
	
	//Encode the object into a byte array
	private static byte[] toBytes( Serializable o ) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );
		oos.writeObject( o );
		oos.close();
		return baos.toByteArray();
	} 
	
	private static void SaveTopScore(GameScoreInfo scoreInfo) {
		byte[] bytes = null;
		try {
			bytes = toBytes(scoreInfo);
		} catch(IOException ioe) {
			System.err.println("SaveTopScore() - Oops, IOException: " + ioe);
		}
		if(bytes == null)
			return;
		FileHandle file = Gdx.files.local(topScoreFileName);
		file.writeBytes(bytes, false);
	}
	
	
	private void playAffirmation() {
		long soundId = sound_affirmation.play();
		sound_affirmation.setVolume(soundId, 0.3f);
	}
	
	private void StartGame() {
		//Set UI state
		currentViewType = ViewType.Game;
		//Hide buttons
		playButton.setVisible(false);
		playButton.setDisabled(true);
		topScoreButton.setVisible(false);
		topScoreButton.setDisabled(true);
		textArea.setVisible(true);
		githubLinkButton.setVisible(false);
		githubLinkButton.setDisabled(true);
		youtubeLinkButton.setVisible(false);
		youtubeLinkButton.setDisabled(true);
		bandcampLinkButton.setVisible(false);
		bandcampLinkButton.setDisabled(true);
		
		//Reset all game state variables
		currentGameScore = new GameScoreInfo();
		actionTaker = null;
		currentRoundIndex = 0;
		actionTakerResponseRequired = false;
		hasOutcomeBeenDetermined = false;
		duelZoneCards_localPlayer.clear();
		duelZoneCards_nonLocalPlayer.clear();
		shadowZoneCards_localPlayer.clear();
		shadowZoneCards_nonLocalPlayer.clear();
		cardsPlayedFromShadowZoneThisTurn.clear();
		disableCardInput = false;
		draggedCard = null;
		returningCard = null;
		waitState = null;
		players.clear();
		
		//Create all players
		//Always create a player for the local user
		players.add(new Player(this, 0, PlayerControlType.LocalPlayer));
		//Create an AI to duel against
		players.add(new Player(this, 1, PlayerControlType.AI));
		
		Random turnStartRandom = new Random();
		turnTaker = players.get(turnStartRandom.nextInt(players.size()));
		
		
		StartRound();
	}
	
	private void BackToTitle() {
		currentViewType = ViewType.Title;
		
		playButton.setVisible(true);
		playButton.setDisabled(false);
		topScoreButton.setVisible(true);
		topScoreButton.setDisabled(false);
		githubLinkButton.setVisible(true);
		githubLinkButton.setDisabled(false);
		youtubeLinkButton.setVisible(true);
		youtubeLinkButton.setDisabled(false);
		bandcampLinkButton.setVisible(true);
		bandcampLinkButton.setDisabled(false);
		
		textArea.setVisible(false);
		
		
		disableCardInput = true;
		
		
		CloseScoreMenu();
	}
	
	private void ShowScoreMenu() {
		showingScoreOverlay = true;
		
		//Hide title menu UI
		playButton.setVisible(false);
		playButton.setDisabled(true);
		topScoreButton.setVisible(false);
		topScoreButton.setDisabled(true);
		githubLinkButton.setVisible(false);
		githubLinkButton.setDisabled(true);
		youtubeLinkButton.setVisible(false);
		youtubeLinkButton.setDisabled(true);
		bandcampLinkButton.setVisible(false);
		bandcampLinkButton.setDisabled(true);
		
		//Hide Game Stuff
		textArea.setVisible(false);
		
		//Show UI
		scoreTextArea.setVisible(true);
		scoreTextArea.setText(GetGameScoreText(currentViewType == ViewType.Game ? currentGameScore : topScoreInfo));
		if(currentViewType == ViewType.Title || currentRoundIndex == maxRoundCount) {
			backButton.setVisible(true);
			backButton.setDisabled(false);
		}
		if(currentViewType == ViewType.Game) {
			if(currentRoundIndex == maxRoundCount) {
				playAgainButton.setVisible(true);
				playAgainButton.setDisabled(false);
			} else {
				nextRoundButton.setVisible(true);
				nextRoundButton.setDisabled(false);
			}
		}
	}
	
	public void ToggleHelpMenu() {
		helpMenuOpen = ! helpMenuOpen;
		if(helpMenuOpen) {
			helpBgImage.setVisible(true);
			outerTable.setVisible(true);
			helpLabel.setVisible(true);
		} else {
			helpBgImage.setVisible(false);
			outerTable.setVisible(false);
			helpLabel.setVisible(false);
		}
	}
	
	private String GetGameScoreText(GameScoreInfo gameScore) {
		String text = "";
		
		if(gameScore == null || gameScore.roundScores.size() == 0) {
			System.err.println("GetGameScoreText() - gameScore is empty!");
			return "Finish your first game to receive a top score.";
		}
		
		//Score display logic
		for(int r = 0; r < gameScore.roundScores.size(); r++) {
			text += "Round " + (r + 1) + '\n';
			for(int p = 0; p < gameScore.roundScores.get(r).playerScores.size(); p++) {
				PlayerScoreInfo playerScore = gameScore.roundScores.get(r).playerScores.get(p);
				text += "    " + playerScore.playerName + " - Points: " + playerScore.points + '\n';
			}
		}
		
		//if the game is over then announce the winner
		if(gameScore.roundScores.size() == maxRoundCount) {
			String winningPlayerName = gameScore.GetWinningPlayerName();
			if(winningPlayerName != null)
				text += '\n' + winningPlayerName + " wins the game!";
			else
				text += '\n' + "The game is a tie.";
		}
		
		//System.out.println("Score Text: " + text);
		
		return text;
	}
	
	private void CloseScoreMenu() {
		showingScoreOverlay = false;
		scoreTextArea.setVisible(false);
		backButton.setVisible(false);
		backButton.setDisabled(true);
		playAgainButton.setVisible(false);
		playAgainButton.setDisabled(true);
		nextRoundButton.setVisible(false);
		nextRoundButton.setDisabled(true);
		
		if(currentViewType == ViewType.Title) {
			//Hide title menu UI
			playButton.setVisible(true);
			playButton.setDisabled(false);
			topScoreButton.setVisible(true);
			topScoreButton.setDisabled(false);
			githubLinkButton.setVisible(true);
			githubLinkButton.setDisabled(false);
			youtubeLinkButton.setVisible(true);
			youtubeLinkButton.setDisabled(false);
			bandcampLinkButton.setVisible(true);
			bandcampLinkButton.setDisabled(false);
		} else if(currentViewType == ViewType.Game) {
			textArea.setVisible(true);
		} else {
			System.err.println("Add support for ViewType: " + currentViewType);
		}
	}
	
	public List<CardObject> GetDuelCardsOfOtherPlayer(int askersPlayerIndex) {
		List<CardObject> duelCardsOfOtherPlayer = new ArrayList<CardObject>();
		for(int i = 0; i < players.size(); i++) {
			if(askersPlayerIndex == i)
				continue;
			duelCardsOfOtherPlayer = players.get(i).IsLocalPlayer() ? duelZoneCards_localPlayer : duelZoneCards_nonLocalPlayer;
		}
		return duelCardsOfOtherPlayer;
	}
	
	//*Turn-based methods envelope action-based methods
	
	public void StartRound() {
		for(int p = 0; p < players.size(); p++) {
			players.get(p).StartRound();
		}
		
		sound_shuffle.play();
		
		StartTurn();
	}
	
	//Turn-based Method - Entry
	private void StartTurn() {
		actionTaker = turnTaker;
		turnTaker.StartTurn();
		textArea.setText( GetIndicatorMessage() );
	}
	
	//Action-based Method
	private void StartAction() {
		actionTaker.StartAction();
		textArea.setText( GetIndicatorMessage() );
	}
	
	//eventually hook this up as an additional card effect
	public void RevealShadowCard(CardObject cardObjectToReveal) {
		cardObjectToReveal.TurnFaceUp();
		
		sound_flipCard.play();
	}
	
	//Action-based Method - Intermediate
	public void PlayCard(CardObject playedCard, ZoneType newZone) {
		actionTakerResponseRequired = newZone == ZoneType.Duel;
		
		System.out.println("PlayCard() : cards current zone: " + playedCard.currentZone() + ", new zone: " + newZone);
		
		switch(playedCard.currentZone()) {
			case Duel:
				break;
			case Hand:
				actionTaker.UseCard(playedCard);
				actionTaker.CondenseHand();
				break;
			case Shadow:
				java.util.List<CardObject> shadowZoneCards = actionTaker.IsLocalPlayer() ? shadowZoneCards_localPlayer : shadowZoneCards_nonLocalPlayer;
				shadowZoneCards.remove(playedCard);
				cardsPlayedFromShadowZoneThisTurn.add(playedCard);
				OrganizeShadowCards(actionTaker);
				break;
			default:
				System.err.println("Game.PlayCard() - Add support for currentZone: " + playedCard.currentZone());
				break;
		}
		
		playedCard.SetZone(newZone);

		switch(newZone) {
			case Duel:
				java.util.List<CardObject> duelZoneCards = actionTaker.IsLocalPlayer() ? duelZoneCards_localPlayer : duelZoneCards_nonLocalPlayer;
				duelZoneCards.add(playedCard);
				OrganizeDuelCards(actionTaker);
				break;
			case Shadow:
				java.util.List<CardObject> shadowZoneCards = actionTaker.IsLocalPlayer() ? shadowZoneCards_localPlayer : shadowZoneCards_nonLocalPlayer;
				shadowZoneCards.add(playedCard);
				OrganizeShadowCards(actionTaker);
				break;
			default:
				System.err.println("Game.PlayCard() - Add support for newZone: " + newZone);
				break;
		}
		
		sound_playCard.play();
		
		if(newZone == ZoneType.Shadow)
			EndTurn();
		else
			CyclePriority();
	}
	
	private void OrganizeDuelCards(Player player) {
		java.util.List<CardObject> duelZoneCards = actionTaker.IsLocalPlayer() ? duelZoneCards_localPlayer : duelZoneCards_nonLocalPlayer;
		for(int d = 0; d < duelZoneCards.size(); d++) {
			Vector2 pos = GetDuelZonePosition(actionTaker, duelZoneCards.size(), d);
			duelZoneCards.get(d).SetPosition((int)pos.x, (int)pos.y);
			System.out.println("Game.OrganizeDuelCards() - played pos: " + duelZoneCards.get(d).rect().x + ", " + duelZoneCards.get(d).rect().y);
		}
	}
	
	private Vector2 GetDuelZonePosition(Player player, int totalCount, int cardIndex) {
		Vector2 duelCenter = new Vector2(duelZoneRect.x + (duelZoneRect.width / 2), duelZoneRect.y + (duelZoneRect.height / 2));
		
		int totalWidthCenterOffset = cardWidth * totalCount / 2;
		int xStart = (int)duelCenter.x - totalWidthCenterOffset;
		int xPos = xStart + (cardWidth * cardIndex);
		
		float yOffset = actionTaker.IsLocalPlayer() ? -cardHeight : 10;
		
		return new Vector2(xPos, duelCenter.y + yOffset);
	}
	
	private void OrganizeShadowCards(Player owner) {
		java.util.List<CardObject> shadowZoneCards = owner.IsLocalPlayer() ? shadowZoneCards_localPlayer : shadowZoneCards_nonLocalPlayer;
		for(int i = 0; i < shadowZoneCards.size(); i++) {
			Vector2 pos = GetShadowZonePosition(owner, shadowZoneCards.size(), i);
			shadowZoneCards.get(i).SetPosition((int)pos.x, (int)pos.y);
			System.out.println("Game.OrganizeShadowCards() - played pos: " + shadowZoneCards.get(i).rect().x + ", " + shadowZoneCards.get(i).rect().y);
		}
	}
	
	private Vector2 GetShadowZonePosition(Player player, int totalCount, int cardIndex) {
		Rectangle rect = player.IsLocalPlayer() ? shadowZoneRect : shadowZoneRect_nonlocal;
		
		Vector2 shadowCenter = new Vector2(rect.x + (rect.width / 2), rect.y + (rect.height / 2));
		int totalWidthCenterOffset = cardWidth * totalCount / 2;
		int xStart = (int)shadowCenter.x - totalWidthCenterOffset;
		int xPos = xStart + (cardWidth * cardIndex);
		
		int localYOffset = Math.round( -cardSize_shadowZone.getHeight() + rect.getHeight() - 10 );
		int yOffset = player.IsLocalPlayer() ? localYOffset : 10;
		
		return new Vector2(xPos, rect.y + yOffset);
	}
	
	//Action-based Method - Intermediate
	//This is a counterpart to PlayCard(); used by players who're unable to, or choose not to, participate in a duel.
	public void PassAction() {
		CyclePriority();
	}
	
	//As the turnTaker we automatically pass priority to the next player or as the non-turnTaker we pass priority back to the turnTaker
	public void CyclePriority() {
		Player target = actionTaker == turnTaker ? players.get(NextPlayerIndex()) : turnTaker;
		PassPriority(target);
	}

	Handler clangHandler = new Handler();
	Runnable clangRunnable = new Runnable() {
		public void run() {
			sound_swordClang.play();
		}
	};
	Handler outcomeHandler = new Handler();
	Runnable outcomeRunnable = new Runnable() {
		public void run() {
			DetermineOutcome();
		}
	};
	
	//Action-based Method - Intermediate
	//Called by turnTakers who perform an action requiring a response OR by actionTakers who're responding
	public void PassPriority(Player priorityTarget) {
		actionTaker = priorityTarget;
		//If priority has returned to the turnTaker then it's time to resolve the duel
		if(priorityTarget == turnTaker) {
			clangHandler.postDelayed(clangRunnable, 700);
			outcomeHandler.postDelayed(outcomeRunnable, 1500);
		} else
			StartAction();
	}
	
	//Action-based Method - Exit
	private void DetermineOutcome() {
		CardObject localCardObject = null;
		if(duelZoneCards_localPlayer.size() > 0)
			localCardObject = duelZoneCards_localPlayer.get(0);
		CardObject nonlocalCardObject = null;
		if(duelZoneCards_nonLocalPlayer.size() > 0)
			nonlocalCardObject = duelZoneCards_nonLocalPlayer.get(0);
			
		Card winningCard = null;
		boolean didLocalPlayerWin = true;
		if(localCardObject != null && nonlocalCardObject != null) {
			winningCard = CardManager.DecideWinner(CardManager.instance().GetCard(localCardObject.cardName()), CardManager.instance().GetCard(nonlocalCardObject.cardName()));
			if(winningCard != null)
				didLocalPlayerWin = winningCard.name().equals(localCardObject.cardName());
			else {
				//In case of a tie the turnTaker wins, unless the response is from the shadow zone
				if(cardsPlayedFromShadowZoneThisTurn.contains(nonlocalCardObject) && !cardsPlayedFromShadowZoneThisTurn.contains(localCardObject))
					didLocalPlayerWin = false;
				else
					didLocalPlayerWin = cardsPlayedFromShadowZoneThisTurn.contains(localCardObject) ? true : turnTaker.IsLocalPlayer();
			}
		} else {
			if(localCardObject != null) {
				winningCard = CardManager.instance().GetCard(localCardObject.cardName());
			} else if(nonlocalCardObject != null) {
				didLocalPlayerWin = false;
				winningCard = CardManager.instance().GetCard(nonlocalCardObject.cardName());
			} else
				winningCard = null;
		}
		
		Player winningPlayer = players.get( didLocalPlayerWin ? localCardObject.owner_playerIndex() : nonlocalCardObject.owner_playerIndex() );
		
		final int shiftDist = 150;
		final int gap = 50;
		int shift = didLocalPlayerWin ? shiftDist : -shiftDist;
		if(localCardObject != null)
			localCardObject.SetPosition((int)localCardObject.rect().x, (int)localCardObject.rect().y + shift);
		if(nonlocalCardObject != null)
			nonlocalCardObject.SetPosition((int)nonlocalCardObject.rect().x, (int)nonlocalCardObject.rect().y + shift + gap);
		String message = "message";
		if(winningCard != null) {
			if(localCardObject == null || nonlocalCardObject == null) {
				message = " " + GetPlayerName(winningPlayer) + " won by forfeit.";
			} else {
				String losingCardName = winningCard.name().equals(localCardObject.cardName()) ? nonlocalCardObject.cardName() : localCardObject.cardName();
				message = winningCard.name() + " beats " + losingCardName + ".";
			}
		} else {
			if(didLocalPlayerWin && winningPlayer != turnTaker)
				message = " The sneakier one wins a tie.";
			else
				message = " The Turn-taker wins a tie.";
		}
		message += (didLocalPlayerWin ? " You won!" : " You lost.");
		textArea.setText(message);
		
		//Increment the winning owners winCount
		int points = 1;
		CardObject winnersCard = didLocalPlayerWin ? localCardObject : nonlocalCardObject;
		if(!cardsPlayedFromShadowZoneThisTurn.contains(winnersCard) && turnTaker.IsLocalPlayer() == didLocalPlayerWin)
			points += 1;
		winningPlayer.TrackPointsForRound(currentRoundIndex, points);
		hasOutcomeBeenDetermined = true;
		
		WaitForOk(WaitState.Outcome);
	}
	
	private void WaitForOk(WaitState newState) {
		waitState = newState;
		disableCardInput = true;
		okButton.setVisible(true);
		okButton.setDisabled(false);
	}
	
	private void DoneWaitingForOk() {
		disableCardInput = false;
		okButton.setVisible(false);
		okButton.setDisabled(true);
	}
	
	private void Ok() {
		DoneWaitingForOk();
		
		switch(waitState) {
			case Outcome:
				duelZoneCards_localPlayer.clear();
				duelZoneCards_nonLocalPlayer.clear();
				EndTurn();
				break;
			default:
				System.err.println("Add support for WaitState: " + waitState);
				break;
		}
	}
	
	
	//Turn-based Method - Exit
	public void EndTurn() {
		turnTaker = players.get(NextPlayerIndex());
		
		actionTakerResponseRequired = false;
		cardsPlayedFromShadowZoneThisTurn.clear();
		hasOutcomeBeenDetermined = false;
		
		//Check if players have cards remaining
		boolean doAnyPlayersHaveCardsLeft = false;
		for(int p = 0; p < players.size(); p++) {
			if(players.get(p).hand().size() > 0) {
				doAnyPlayersHaveCardsLeft = true;
				break;
			}
		}
		//Don't end the round if players still have Cards sitting in the shadow zone
		if(
			!doAnyPlayersHaveCardsLeft
			&&
			shadowZoneCards_localPlayer.size() > 0
			&&
			shadowZoneCards_nonLocalPlayer.size() > 0
		)
			doAnyPlayersHaveCardsLeft = true;
			
		if(doAnyPlayersHaveCardsLeft)
			StartTurn();
		else
			EndRound();
	}
	
	public void EndRound() {
		System.err.println("Game.EndRound() - currentRoundIndex: " + currentRoundIndex);
		
		for(Player player : players) {
			currentGameScore.RecordRoundForPlayer(currentRoundIndex, GetPlayerName(player), player.GetPointsByRound(currentRoundIndex));
		}
		
		//This is causing problems here, cause the players scores are still being shown in the background
		currentRoundIndex++;
		
		//Game End
		if(currentRoundIndex == maxRoundCount) {
			//if this score is better than the previous topScore then replace and store it locally
			if(currentGameScore.IsBetterThan(topScoreInfo, GetPlayerName(players.get(0)))) {
				topScoreInfo = currentGameScore;
				SaveTopScore(currentGameScore);
			}
		}
		
		ShowScoreMenu();
	}
	
	public String GetPlayerName(Player player) {
		//String playerName = "Player" + player.playerIndex();
		String playerName = null;
		if(player.playerIndex() == 0)
			playerName = "User";
		else
			playerName = "Shadow Shogun";
		return playerName;
	}
	
	private int NextPlayerIndex() {
		int nextPlayerIndex = players.indexOf(turnTaker) + 1;
		if(nextPlayerIndex >= players.size())
			nextPlayerIndex = 0;
		return nextPlayerIndex;
	}
	
	private String GetIndicatorMessage() {
		//Update UI indicator for the turnTaker
		String actionTakerName = GetPlayerName(actionTaker);
		String turnText;
		if(turnTaker.IsLocalPlayer()) {
			if(actionTaker == turnTaker)
				turnText = actionTakerName + ": It's your turn. Choose an action.";
			else
				turnText = actionTakerName + " is choosing a response.";
		} else {
			if(actionTaker == turnTaker)
				turnText = "It's " + actionTakerName + "'s turn. They're choosing an action.";
			else
				turnText = actionTakerName + ": Choose a response.";
		}
		return turnText;
	}
	
	
	@Override
	public void render()
	{       
	    camera.update();
	
	    Gdx.gl.glClearColor(1, 1, 1, 1);
	    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		
		if(showingScoreOverlay) {
			batch.draw(scoreBgTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		} else {
			if(currentViewType == ViewType.Game) {
				batch.draw(bgTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
				batch.draw(duelZoneTex, duelZoneRect.x, duelZoneRect.y, duelZoneRect.width, duelZoneRect.height);
				batch.draw(shadowZoneTex, shadowZoneRect.x, shadowZoneRect.y, shadowZoneRect.width, shadowZoneRect.height);
				batch.draw(shadowZoneTex, shadowZoneRect_nonlocal.x, shadowZoneRect_nonlocal.y, shadowZoneRect_nonlocal.width, shadowZoneRect_nonlocal.height, 0, 0, Math.round(shadowZoneRect_nonlocal.width*zoneIngameScale), Math.round(shadowZoneRect_nonlocal.height*zoneIngameScale), true, true);
		
				for(CardObject col : shadowZoneCards_localPlayer) {
					if(col == draggedCard)
						continue;
					batch.draw(cardTextures.get(col.cardName()), col.rect().x, col.rect().y, col.rect().width, col.rect().height);
			
					batch.draw(faceDownOverlayTex, col.rect().x, col.rect().y, col.rect().width, col.rect().height);
				}
			
				List<CardObject> shadowNonLocal_snapshot = new ArrayList<CardObject>( shadowZoneCards_nonLocalPlayer );
				for(CardObject col : shadowNonLocal_snapshot) {
					//Display the enemy's cards upside down
					if(col == draggedCard)
						continue;
					batch.draw(cardTextures.get(cardBackName), col.rect().x, col.rect().y, col.rect().width, col.rect().height, 0, 0, Math.round(col.rect().width*cardIngameScale), Math.round(col.rect().height*cardIngameScale), true, true);
				}
		
				for(CardObject col : duelZoneCards_localPlayer) {
					batch.draw(cardTextures.get(col.cardName()), col.rect().x, col.rect().y, col.rect().width, col.rect().height);
		
					if(cardsPlayedFromShadowZoneThisTurn.contains(col) && !hasOutcomeBeenDetermined)
						batch.draw(faceDownOverlayTex, col.rect().x, col.rect().y, col.rect().width, col.rect().height);
				}
				for(CardObject col : duelZoneCards_nonLocalPlayer) {
					//Display the enemy's cards upside down
					String cardName = col.cardName();
					if(cardsPlayedFromShadowZoneThisTurn.contains(col) && !hasOutcomeBeenDetermined)
						cardName = cardBackName;
					batch.draw(cardTextures.get(cardName), col.rect().x, col.rect().y, col.rect().width, col.rect().height, 0, 0, Math.round(col.rect().width*cardIngameScale), Math.round(col.rect().height*cardIngameScale), true, true);
				}
		
				for(int p = 0; p < players.size(); p++) {
					boolean isLocalPlayer = players.get(p).IsLocalPlayer();
					java.util.List<CardObject> listSnapshot = new ArrayList<CardObject>( players.get(p).hand() );
					for(CardObject co : listSnapshot) {
						if(co == draggedCard)
							continue;
						String cardName = co.cardName();
						if(!isLocalPlayer)
							cardName = cardBackName;
						batch.draw(cardTextures.get(cardName), co.rect().x, co.rect().y, co.rect().width, co.rect().height);
					}
				}
		
				//Render the dragged card on top of everything else
				if(draggedCard != null) {
					batch.draw(cardTextures.get(draggedCard.cardName()), draggedCard.rect().x, draggedCard.rect().y, draggedCard.rect().width, draggedCard.rect().height);
		
					if(draggedCard.currentZone() == ZoneType.Shadow)
						batch.draw(faceDownOverlayTex, draggedCard.rect().x, draggedCard.rect().y, draggedCard.rect().width, draggedCard.rect().height);
				}
		
				//UI can now be drawn using the spriteBatch
				for(int p = 0; p < players.size(); p++) {
					Point namePos = players.get(p).IsLocalPlayer() ? namePos_localPlayer : namePos_otherPlayer;
					pixelFont.draw(batch, GetPlayerName(players.get(p)), namePos.x, namePos.y);
				
					Point winCountPos = players.get(p).IsLocalPlayer() ? winCountPos_localPlayer : winCountPos_otherPlayer;
					pixelFont.draw(batch, "Score: " + players.get(p).GetPointsByRound(currentRoundIndex), winCountPos.x, winCountPos.y);
				}
		
				batch.draw(infoPanelTex, infoPanelRect.x, infoPanelRect.y, infoPanelRect.width, infoPanelRect.height);
			} else if(currentViewType == ViewType.Title) {
				batch.draw(titleBgTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			
			} else {
				System.err.println("render() - Add support for ViewType: " + currentViewType);
			}
		}
		
		batch.end();
		
		//Draw stage stuff
		stage.act();
		stage.draw();
	}

	@Override
	public void dispose()
	{
		bgTex.dispose();
		for(Texture tex : cardTextures.values())
			tex.dispose();
		pixelFont.dispose();
		batch.dispose();
		titleBgTex.dispose();
		faceDownOverlayTex.dispose();
		duelZoneTex.dispose();
		shadowZoneTex.dispose();
		stage.dispose();
		skin.dispose();
		infoPanelTex.dispose();
		scoreBgTex.dispose();
		helpSymbolTex.dispose();
		sound_shuffle.dispose();
		sound_pullCard.dispose();
		sound_flipCard.dispose();
		sound_playCard.dispose();
		sound_swordClang.dispose();
		sound_affirmation.dispose();
		sound_slideCard.dispose();
		music_emblazonedSong.dispose();
	}

	@Override
	public void resize(int width, int height)
	{
	}

	@Override
	public void pause()
	{
	}

	@Override
	public void resume()
	{
	}
}
