package ru.vang.slowthsimageloader.utils;

import java.io.File;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

public class StorageUtils {

	@TargetApi(9)
	public static long getUsableSpace(File path) {
		if (VersionUtils.hasGingerbread()) {
			return path.getUsableSpace();
		}
		final StatFs stats = new StatFs(path.getPath());
		return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
	}

	public static String getDiskCacheDirPath(final Context context,
			final String uniqueName) {
		final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState()) || !isExternalStorageRemovable() ? getExternalCacheDir(
				context).getPath()
				: context.getCacheDir().getPath();

		return new File(cachePath, uniqueName).getAbsolutePath();
	}

	@TargetApi(8)
	public static File getExternalCacheDir(final Context context) {
		if (VersionUtils.hasFroyo()) {
			return context.getExternalCacheDir();
		}

		final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
		return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
	}

	@TargetApi(9)
	public static boolean isExternalStorageRemovable() {
		if (VersionUtils.hasGingerbread()) {
			return Environment.isExternalStorageRemovable();
		}
		return true;
	}

}
