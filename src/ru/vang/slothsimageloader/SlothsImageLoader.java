package ru.vang.slothsimageloader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.vang.slothsimageloader.cache.DiskCache;
import ru.vang.slothsimageloader.cache.MemoryCache;
import ru.vang.slothsimageloader.utils.FlushedInputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.view.View;

public class SlothsImageLoader {
	private final ExecutorService mExecutorService;
	private static final MemoryCache sMemoryCache = new MemoryCache();
	private final DiskCache mDiskCache;
	private CacheParams mCacheParams;
	private final Handler mHandler = new Handler();

	private Map<View, String> mViews = Collections
			.synchronizedMap(new WeakHashMap<View, String>());

	public enum ImageSourceType {
		MEMORY, DISK, NETWORK
	}

	public interface OnPostProcessListener {

		public Bitmap onPostProcess(final Bitmap bitmap);

	}

	public SlothsImageLoader(final CacheParams cacheParams) {
		HttpURLConnection.setFollowRedirects(true);
		mCacheParams = cacheParams;
		mExecutorService = Executors.newFixedThreadPool(cacheParams.threadNumber);
		mDiskCache = DiskCache.open(cacheParams.cacheDirPath, cacheParams.diskCacheSize);
	}

	public void loadImage(final String source, final View view) {
		if (isSameTaskExecuted(view, source)) {
			return;
		}

		mViews.put(view, source);
		final Bitmap bitmap = sMemoryCache.getBitmapFromCache(source);
		if (bitmap == null) {
			mCacheParams.binder.displayStub(view, mCacheParams.stubResId);
			mExecutorService.submit(new LoadImageTask(source, view));
		} else {
			mCacheParams.binder.displayImage(view, bitmap, ImageSourceType.MEMORY);
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

	private boolean isViewBusy(final View view, final String source) {
		final String value = mViews.get(view);

		return value != null && !value.equals(source);
	}

	private boolean isSameTaskExecuted(final View view, final String imageSource) {
		final String value = mViews.get(view);

		return value != null && value.equals(imageSource);
	}

	private class LoadImageTask implements Runnable {
		private static final int BUFFER_SIZE = 8192;
		private View mView;
		public String mSource;

		public LoadImageTask(final String url, final View view) {
			mSource = url;
			mView = view;
			;
		}

		@Override
		public void run() {
			if (isViewBusy(mView, mSource))
				return;

			ImageSourceType sourceType = ImageSourceType.DISK;
			Bitmap bitmap = mDiskCache.getBitmapFromDisk(mSource);
			if (bitmap == null) {
				bitmap = loadBitmap(mSource);
				sourceType = ImageSourceType.NETWORK;
			}

			if (bitmap != null) {
				if (mCacheParams.postProcess != null) {
					bitmap = mCacheParams.postProcess.onPostProcess(bitmap);
				}
				saveBitmapToCache(mSource, bitmap);

				if (isViewBusy(mView, mSource))
					return;

				mHandler.post(new DisplayImageTask(bitmap, mView, mSource, sourceType));
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

		private View mView;
		private Bitmap mBitmap;
		private String mSource;
		private ImageSourceType mSourceType;

		public DisplayImageTask(final Bitmap bitmap, final View view,
				final String source, final ImageSourceType sourceType) {
			mBitmap = bitmap;
			mView = view;
			mSource = source;
		}

		@Override
		public void run() {
			if (isViewBusy(mView, mSource))
				return;

			mCacheParams.binder.displayImage(mView, mBitmap, mSourceType);
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
