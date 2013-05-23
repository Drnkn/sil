package ru.vang.slowthsimageloader.cache;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class MemoryCache {
	public static final int DEFAULT_MEMORY_CACHE_SIZE = 1024 * 1024 * 5;
	private static int cacheSize = DEFAULT_MEMORY_CACHE_SIZE;
	private LruCache<String, Bitmap> mCachedBitmaps;

	public MemoryCache() {
		initCache();
	}

	public void putBitmapInCache(final String key, final Bitmap value) {
		if (mCachedBitmaps.get(key) == null) {
			mCachedBitmaps.put(key, value);
		}
	}

	public Bitmap getBitmapFromCache(final String key) {
		return mCachedBitmaps.get(key);
	}

	public void evictAll() {
		mCachedBitmaps.evictAll();
	}

	public static void setCacheSize(final int cacheSize) {
		MemoryCache.cacheSize = cacheSize;		
	}

	private void initCache() {
		mCachedBitmaps = new LruCache<String, Bitmap>(cacheSize) {

			@Override
			public int sizeOf(final String key, final Bitmap value) {
				return getBitmapByteCount(value);
			}

		};
	}

	private int getBitmapByteCount(final Bitmap bitmap) {
		return bitmap.getRowBytes() * bitmap.getHeight();
	}
}
