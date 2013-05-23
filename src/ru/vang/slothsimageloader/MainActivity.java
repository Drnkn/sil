package ru.vang.slothsimageloader;

import com.example.asyncimageloader.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SlothsImageLoader.setMemoryCacheSizeInPercentage(25);
		((GridView) findViewById(R.id.gridview)).setAdapter(new ImageAdapter(
				getApplicationContext()));
	}

}
