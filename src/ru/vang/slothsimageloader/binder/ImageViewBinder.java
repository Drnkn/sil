package ru.vang.slothsimageloader.binder;

import ru.vang.slothsimageloader.SlothsImageLoader.ImageSourceType;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

public class ImageViewBinder implements ViewBinder {

	@Override
	public void displayImage(final View view, final Bitmap bitmap, final ImageSourceType sourceType) {
		((ImageView) view).setImageBitmap(bitmap);
	}

	@Override
	public void displayStub(final View view, final int stubId) {
		((ImageView) view).setImageResource(stubId);
	}

}
