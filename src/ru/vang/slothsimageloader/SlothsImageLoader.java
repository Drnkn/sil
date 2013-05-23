package ru.vang.slothsimageloader;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.vang.slowthsimageloader.cache.DiskCache;
import ru.vang.slowthsimageloader.cache.MemoryCache;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.widget.ImageView;

public class SlothsImageLoader {
	private final ExecutorService mExecutorService;
	private static final MemoryCache sMemoryCache = new MemoryCache();
	private final DiskCache mDiskCache;
	private CacheParams mCacheParams;
	private final Handler mHandler = new Handler();
	private Resources mResources;

	private Map<ImageView, String> mImageViews = Collections
			.synchronizedMap(new WeakHashMap<ImageView, String>());

	public SlothsImageLoader(final Context context, final CacheParams cacheParams) {
		HttpURLConnection.setFollowRedirects(true);
		mCacheParams = cacheParams;
		mExecutorService = Executors.newFixedThreadPool(cacheParams.threadNumber);
		mDiskCache = DiskCache.open(cacheParams.cacheDirPath, cacheParams.diskCacheSize);
		mResources = context.getResources();
	}

	public void loadImage(final ImageView imageView, final String imageSource) {
		if (imageSource == null || isSameTaskExecuted(imageView, imageSource)) {
			return;
		}

		mImageViews.put(imageView, imageSource);
		final Bitmap bitmap = sMemoryCache.getBitmapFromCache(imageSource);
		if (bitmap == null) {
			imageView.setImageResource(mCacheParams.stubResId);
			mExecutorService.submit(new LoadImageTask(imageView, imageSource));
		} else {
			imageView.setImageBitmap(bitmap);
		}
	}

	public void clearCache() {
		sMemoryCache.evictAll();
		mDiskCache.clearCacheAsync();
	}

	public void flush() {
		mDiskCache.flushAsync();
	}

	public void close() {
		mDiskCache.closeAsync();
	}

	public static void setMemoryCacheSize(final int cacheSize) {
		MemoryCache.setCacheSize(cacheSize);
	}

	public static void setMemoryCacheSizeInPercentage(final int percentage) {
		if (percentage < 0 || percentage >= 100) {
			throw new IllegalArgumentException(
					"Percentages can not be less than 0 or bigger than 100");
		}
		final int cacheSize = (int) Runtime.getRuntime().maxMemory() / (100 / percentage);
		MemoryCache.setCacheSize(cacheSize);
	}

	private boolean isImageViewBusy(final ImageView imageView, final String imageSource) {
		final String value = mImageViews.get(imageView);

		return value != null && !value.equals(imageSource) ? true : false;
	}

	private boolean isSameTaskExecuted(final ImageView imageView, final String imageSource) {
		final String value = mImageViews.get(imageView);
		if (value == null) {
			return false;
		}

		return value != null && value.equals(imageSource) ? true : false;
	}

	private static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					final int readByte = read();
					if (readByte < 0) {
						break; // we reached EOF
					} else {
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}

	private class LoadImageTask implements Runnable {
		private static final int BUFFER_SIZE = 8192;
		private WeakReference<ImageView> mImageViewReference;
		public String mUrl;

		public LoadImageTask(final ImageView imageView, final String url) {
			mImageViewReference = new WeakReference<ImageView>(imageView);
			mUrl = url;
		}

		@Override
		public void run() {
			if (isImageViewBusy(mImageViewReference.get(), mUrl)) {
				return;
			}

			Bitmap bitmap = mDiskCache.getBitmapFromDisk(mUrl);
			if (bitmap == null) {
				bitmap = loadBitmap(mUrl);
			}
			final ImageView imageView = mImageViewReference.get();
			if (imageView != null && bitmap != null) {
				saveBitmapToCache(mUrl, bitmap);
				mHandler.post(new DisplayImageTask(imageView, bitmap, mUrl));
			}
		}

		private void saveBitmapToCache(final String url, final Bitmap bitmap) {
			if (sMemoryCache.getBitmapFromCache(url) == null) {
				sMemoryCache.putBitmapInCache(url, bitmap);
			}

			mDiskCache.putBitmapInCache(url, bitmap);
		}

		private Bitmap loadBitmap(final String imageSource) {
			disableConnectionReuseIfNecessary();
			Bitmap bitmap = null;
			HttpURLConnection connection = null;
			try {
				final URL url = new URL(imageSource);
				connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(mCacheParams.connectionTimeout);
				connection.setReadTimeout(mCacheParams.readTimeout);
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					final BufferedInputStream bis = new BufferedInputStream(
							new FlushedInputStream(connection.getInputStream()),
							BUFFER_SIZE);
					bitmap = BitmapFactory.decodeStream(bis);
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}

			return bitmap;
		}
	}

	private class DisplayImageTask implements Runnable {
		private static final int FADING_DURATION = 200;

		private ImageView mImageView;
		private Bitmap mBitmap;
		private String mUrl;

		public DisplayImageTask(final ImageView imageView, final Bitmap bitmap,
				final String url) {
			mImageView = imageView;
			mBitmap = bitmap;
			mUrl = url;
		}

		@Override
		public void run() {
			if (isImageViewBusy(mImageView, mUrl)) {
				return;
			}
			if (mCacheParams.useFading) {
				// We use transparent ColorDrawable instead of current image
				// drawable because bitmaps in TransitionDrawable should be the
				// same size
				final TransitionDrawable transitionDrawable = new TransitionDrawable(
						new Drawable[] { new ColorDrawable(android.R.color.transparent),
								new BitmapDrawable(mResources, mBitmap) });
				transitionDrawable.setCrossFadeEnabled(true);
				mImageView.setImageDrawable(transitionDrawable);
				transitionDrawable.startTransition(FADING_DURATION);
			} else {
				mImageView.setImageBitmap(mBitmap);
			}
		}
	}

	/**
	 * Workaround for bug pre-Froyo, see here for more info:
	 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 */
	public static void disableConnectionReuseIfNecessary() {
		// HTTP connection reuse which was buggy pre-froyo
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}
}
