package ru.vang.slothsimageloader;

import ru.vang.slothsimageloader.SlothsImageLoader.OnPostProcessListener;
import ru.vang.slothsimageloader.binder.FadeImageViewBinder;
import ru.vang.slothsimageloader.binder.ViewBinder;
import ru.vang.slothsimageloader.utils.StorageUtils;
import android.content.Context;

public class CacheParams {
	public static final int DEFAULT_THREAD_NUMBER = 5;
	public static final String DEFAULT_CACHE_DIR = "images";
	public static final int DEFAULT_STUB_RES_ID = android.R.color.transparent;
	public static final int DEFAULT_CONNECTION_TIMEOUT = 15000;
	public static final int DEFAULT_READ_TIMEOUT = 15000;
	public static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 20;

	public final int threadNumber;
	public final String cacheDirPath;
	public final int stubResId;
	public final int connectionTimeout;
	public final int readTimeout;
	public final int diskCacheSize;
	public final ViewBinder binder;
	public final OnPostProcessListener postProcess;

	private CacheParams(final Builder builder) {
		threadNumber = builder.threadNumber;
		cacheDirPath = builder.cacheDirPath;
		stubResId = builder.stubResId;
		connectionTimeout = builder.connectionTimeout;
		readTimeout = builder.readTimeout;
		diskCacheSize = builder.diskCacheSize;
		binder = builder.binder;
		postProcess = builder.postProcess;
	}

	public static CacheParams getDefaultParams(final Context context) {
		return new Builder(context).build();
	}

	public static class Builder {
		private int threadNumber = DEFAULT_THREAD_NUMBER;
		private String cacheDirPath;
		private int stubResId = DEFAULT_STUB_RES_ID;
		private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
		private int readTimeout = DEFAULT_READ_TIMEOUT;
		private int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
		private ViewBinder binder;
		private OnPostProcessListener postProcess = null;

		public Builder(final Context context) {
			cacheDirPath = StorageUtils.getDiskCacheDirPath(context, DEFAULT_CACHE_DIR);
			binder = new FadeImageViewBinder(context);
		}

		public Builder setThreadNumber(final int threadNumber) {
			this.threadNumber = threadNumber;
			return this;
		}

		public Builder setCacheDir(final Context context, final String cacheDir) {
			cacheDirPath = StorageUtils.getDiskCacheDirPath(context, cacheDir);
			return this;
		}

		public Builder setCacheDirPath(final String cacheDirPath) {
			this.cacheDirPath = cacheDirPath;
			return this;
		}

		public Builder setStubResId(final int stubResId) {
			this.stubResId = stubResId;
			return this;
		}

		public Builder setConnectionTimeout(final int connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
			return this;
		}

		public Builder setReadTimeout(final int readTimeout) {
			this.readTimeout = readTimeout;
			return this;
		}

		public Builder setDiskCacheSize(final int diskCacheSize) {
			this.diskCacheSize = diskCacheSize;
			return this;
		}

		public Builder setViewBinder(final ViewBinder binder) {
			this.binder = binder;
			return this;
		}

		public Builder setPostProcessListener(final OnPostProcessListener postProcess) {
			this.postProcess = postProcess;
			return this;
		}

		public CacheParams build() {
			final CacheParams params = new CacheParams(this);
			return params;
		}
	}

}
