package com.coreyfarmer.shogunoftheshadows;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();

		//trying to make fullscreen
		//cfg.hideStatusBar = true;
		cfg.useImmersiveMode = true;

		initialize(new MyGdxGame(), cfg);
	}
}
