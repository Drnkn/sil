package ru.vang.slothsimageloader.binder;

import ru.vang.slothsimageloader.SlothsImageLoader.ImageSourceType;
import android.graphics.Bitmap;
import android.view.View;

public interface ViewBinder {	
	
	public void displayImage(View view, Bitmap bitmap, ImageSourceType sourceType);
	
	public void displayStub(View view, int stubId);

}
