package ru.vang.slothsimageloader.binder;

import ru.vang.slothsimageloader.SlothsImageLoader.ImageSourceType;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.widget.ImageView;

public class FadeImageViewBinder implements ViewBinder {
	private static final int FADE_DURRATION = 200;

	private final Resources mResources;
	private final int mFadeDuration;

	public FadeImageViewBinder(final Context context) {
		this(context, FADE_DURRATION);
	}

	public FadeImageViewBinder(final Context context, final int duration) {
		mResources = context.getResources();
		mFadeDuration = duration;
	}

	// TODO check why getDrawable returns null
	// TODO get context from view?
	@Override
	public void displayImage(final View view, final Bitmap bitmap, final ImageSourceType sourceType) {
		final ImageView imageView = (ImageView) view;
		if (sourceType == ImageSourceType.MEMORY) {
			imageView.setImageBitmap(bitmap);
		} else {			
			Drawable currentDrawable = imageView.getDrawable();
			if (currentDrawable == null) {
				currentDrawable = new ColorDrawable(android.R.color.transparent);
			}
			final TransitionDrawable transition = new TransitionDrawable(new Drawable[] {
					currentDrawable, new BitmapDrawable(mResources, bitmap) });
			transition.setCrossFadeEnabled(true);
			imageView.setImageDrawable(transition);
			transition.startTransition(mFadeDuration);
		}		
	}

	@Override
	public void displayStub(final View view, final int stubId) {
		((ImageView) view).setImageResource(stubId);
	}

}
