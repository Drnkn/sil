package ru.vang.slowthsimageloader.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ru.vang.slowthsimageloader.utils.StorageUtils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.jakewharton.DiskLruCache;

public class DiskCache {
	private static final int APP_VERSION = 1;
	private static final int DISK_CACHE_INDEX = 0;

	private static final int MESSAGE_INDEX = 0;
	private static final int MESSAGE_CLEAR = 0;
	private static final int MESSAGE_INIT = 1;
	private static final int MESSAGE_FLUSH = 2;
	private static final int MESSAGE_CLOSE = 3;

	private DiskLruCache mDiskCache;
	private final Object mDiskCacheLock = new Object();
	private boolean mDiskCacheStarting = true;
	private final String mCacheDirPath;
	private final int mDiskCacheSize;

	private DiskCache(final String cacheDirPath, final int diskCacheSize) {
		mCacheDirPath = cacheDirPath;
		mDiskCacheSize = diskCacheSize;
		initDiskCacheAsync();
	}

	public static DiskCache open(final String cacheDirPath, final int diskCacheSize) {
		return new DiskCache(cacheDirPath, diskCacheSize);
	}

	private void initDiskCacheAsync() {
		new DiskAsyncTask().execute(MESSAGE_INIT);
	}

	/**
	 * Initializes the disk cache. Note that this includes disk access so this
	 * should not be executed on the main/UI thread. By default an ImageCache
	 * does not initialize the disk cache when it is created, instead you should
	 * call initDiskCache() to initialize it on a background thread.
	 */
	private void initDiskCache() {
		// Set up disk cache
		synchronized (mDiskCacheLock) {
			if (mDiskCache == null || mDiskCache.isClosed()) {
				final File diskCacheDir = new File(mCacheDirPath);
				if (!diskCacheDir.exists()) {
					diskCacheDir.mkdirs();
				}
				if (StorageUtils.getUsableSpace(diskCacheDir) > mDiskCacheSize) {
					try {
						mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, 1,
								mDiskCacheSize);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}

			}
			mDiskCacheStarting = false;
			mDiskCacheLock.notifyAll();
		}
	}

	public void putBitmapInCache(final String url, final Bitmap bitmap) {
		synchronized (mDiskCacheLock) {
			if (mDiskCache != null) {
				OutputStream os = null;
				final String key = hashKeyForDisk(url);
				try {
					final DiskLruCache.Snapshot shapshot = mDiskCache.get(key);
					if (shapshot == null) {
						final DiskLruCache.Editor editor = mDiskCache.edit(key);
						if (editor != null) {
							os = editor.newOutputStream(DISK_CACHE_INDEX);
							bitmap.compress(CompressFormat.PNG, 100, os);
							editor.commit();
						}
					} else {
						shapshot.getInputStream(DISK_CACHE_INDEX).close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						if (os != null) {
							os.close();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	public Bitmap getBitmapFromDisk(final String url) {		
		Bitmap bitmap = null;
		synchronized (mDiskCacheLock) {
			while (mDiskCacheStarting) {
				try {
					mDiskCacheLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (mDiskCache != null) {
				BufferedInputStream bis = null;
				try {
					final String key = hashKeyForDisk(url);
					final DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
					if (snapshot != null) {
						final InputStream in = snapshot.getInputStream(DISK_CACHE_INDEX);
						bis = new BufferedInputStream(in, 8192);
						if (in != null) {
							bitmap = BitmapFactory.decodeStream(bis);
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						if (bis != null) {
							bis.close();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		return bitmap;
	}

	public void clearCacheAsync() {
		new DiskAsyncTask().execute(MESSAGE_CLEAR);		
	}

	public void clearCache() {
		synchronized (mDiskCacheLock) {
			mDiskCacheStarting = true;
			if (mDiskCache != null && !mDiskCache.isClosed()) {
				try {
					mDiskCache.delete();
				} catch (IOException e) {
				}
				mDiskCache = null;
				initDiskCache();
			}
		}
	}
	
	public void flushAsync() {
		new DiskAsyncTask().execute(MESSAGE_FLUSH);
	}

	/**
	 * Flushes the disk cache associated with this ImageCache object. Note that
	 * this includes disk access so this should not be executed on the main/UI
	 * thread.
	 */
	public void flush() {
		synchronized (mDiskCacheLock) {
			if (mDiskCache != null) {
				try {
					mDiskCache.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void closeAsync() {
		new DiskAsyncTask().execute(MESSAGE_CLOSE);
	}

	/**
	 * Closes the disk cache associated with this ImageCache object. Note that
	 * this includes disk access so this should not be executed on the main/UI
	 * thread.
	 */
	public void close() {
		synchronized (mDiskCacheLock) {
			if (mDiskCache != null) {
				try {
					if (!mDiskCache.isClosed()) {
						mDiskCache.close();
						mDiskCache = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static String hashKeyForDisk(final String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	private static String bytesToHexString(byte[] bytes) {
		// http://stackoverflow.com/questions/332079
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}
	
	private class DiskAsyncTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            switch (params[MESSAGE_INDEX]) {
                case MESSAGE_CLEAR:
                	clearCache();
                    break;
                case MESSAGE_INIT:
                	initDiskCache();
                    break;
                case MESSAGE_FLUSH:
                	flush();
                    break;
                case MESSAGE_CLOSE:
                    close();
                    break;
            }
            return null;
        }
    }

}
