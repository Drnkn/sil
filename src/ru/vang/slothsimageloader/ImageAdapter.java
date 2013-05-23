package ru.vang.slothsimageloader;

import com.example.asyncimageloader.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageAdapter extends BaseAdapter {

	private static final String[] IMAGE_URLS = new String[] {
			"https://lh4.googleusercontent.com/-1uo14fqND14/UW2anlochnI/AAAAAAAAMK4/Q-ji9hBfOXM/s333-p-o/IMAG1562.jpg",
			"https://lh6.googleusercontent.com/-lVu5G3vU4AM/UW2a-hsuj-I/AAAAAAAAMLE/vIhyAkAXc6c/w143-h238/IMAG1563.jpg",
			"https://lh5.googleusercontent.com/-vMLoi2ZTbG0/UW2baRnapTI/AAAAAAAAMLU/NewjSFqyxoU/w143-h238/IMAG1564.jpg",
			"https://lh6.googleusercontent.com/-0pxmOfofMXc/UW2cHUrf7vI/AAAAAAAAMLg/IdKTKGazmGI/w143-h238/IMAG1565.jpg",
			"https://lh3.googleusercontent.com/-yJ8zfM4DfP0/UW2cieRYgvI/AAAAAAAAMLs/bWXqe3xBQX4/w143-h238/IMAG1566.jpg",
			"https://lh6.googleusercontent.com/-ArcocbYXMYY/UW2c-uxSkxI/AAAAAAAAML4/JYjhY6tD6VE/w143-h238/IMAG1567.jpg",
			"https://lh4.googleusercontent.com/-vggy1ebRVhs/UW2dinOwwoI/AAAAAAAAMME/yYmi8lDg_0g/s333-p-o/IMAG1568.jpg",
			"https://lh5.googleusercontent.com/-gm2QPbsCj8k/UW2wXloVbuI/AAAAAAAAMNk/f7BsI4klIm4/w238-h143/IMAG1569.jpg",
			"https://lh5.googleusercontent.com/-HDS2-Gjtm70/UW2eheHyWOI/AAAAAAAAMMU/1lJLU8Xu77E/w238-h143/IMAG1570.jpg",
			"https://lh3.googleusercontent.com/-r9enhMC6Zsg/UW2fEpeXVLI/AAAAAAAAMMg/lpokNpE0atY/w238-h143/IMAG1571.jpg",
			"https://lh3.googleusercontent.com/-5HMqox0Ylz8/UW7ZirUun9I/AAAAAAAAMRA/kvSU8eqlEik/w144-h241/IMAG1585.jpg",
			"https://lh6.googleusercontent.com/-fYJickqe87E/UW7Zvk45yMI/AAAAAAAAMRM/OTYWfHVLdI8/w144-h241/IMAG1586.jpg",
			"https://lh4.googleusercontent.com/-_ctoUH7GWF4/UW7aNUh4XYI/AAAAAAAAMRY/hW-UCAhE0Hw/w144-h241/IMAG1587.jpg",
			"https://lh3.googleusercontent.com/-r1XWlTpJYU8/UW7ai0nBXUI/AAAAAAAAMRk/9-1RL7Bay54/w144-h241/IMAG1588.jpg",
			"https://lh5.googleusercontent.com/--mUswaOOFbw/UW7X5chplqI/AAAAAAAAMQA/KUa0rGwblyc/w144-h241/IMAG1589.jpg",
			"https://lh3.googleusercontent.com/-Ye8ev7IgwDM/UW7ZQK0AKUI/AAAAAAAAMQ0/jsd_oheJS2I/w144-h241/IMAG1593.jpg" };

	private final SlothsImageLoader mImageLoader;
	private LayoutInflater mInflater;

	public ImageAdapter(final Context context) {
		mImageLoader = new SlothsImageLoader(context, new CacheParams.Builder(context).build());
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return IMAGE_URLS.length;
	}

	@Override
	public String getItem(int position) {
		// TODO Auto-generated method stub
		return IMAGE_URLS[position];
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (convertView == null) {
			row = mInflater.inflate(R.layout.list_item, parent, false);
		}

		((TextView) row.findViewById(R.id.count)).setText(Integer.toString(position));
		final String url = getItem(position);
		((TextView) row.findViewById(R.id.url)).setText(url);
		final ImageView imageView = (ImageView) row.findViewById(R.id.image);
		mImageLoader.loadImage(imageView, url);

		return row;
	}

}
