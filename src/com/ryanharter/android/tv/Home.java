package com.ryanharter.android.tv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.TextView;

public class Home extends Activity {
	/**
	 * Tag used for logging
	 */
	private static final String TAG = "Home";
	
	private static boolean mWallpaperChecked;
	private static ArrayList<ApplicationInfo> mApplications;
	private static LinkedList<ApplicationInfo> mFavorites;
	
	private final BroadcastReceiver mWallpaperReceiver = new WallpaperIntentReceiver();
	private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();
	
	private Gallery mAppsList;
	
	private LayoutAnimationController mShowLayoutAnimation;
	private LayoutAnimationController mHideLayoutAnimation;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.home);
		
		setDefaultWallpaper();
		
		registerIntentReceivers();
		
		loadApplications(true);
		
		bindApplications();
		bindButtons();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		// Close the menu
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(mWallpaperReceiver);
		unregisterReceiver(mApplicationsReceiver);
	}
	
	/**
     * Registers various intent receivers. The current implementation registers
     * only a wallpaper intent receiver to let other applications change the
     * wallpaper and an application intent receiver to let us know when packages 
	 * change.
     */
	private void registerIntentReceivers() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
		registerReceiver(mWallpaperReceiver, filter);
		
		filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addDataScheme("package");
		registerReceiver(mApplicationsReceiver, filter);
	}
	
	/**
	 * Creates a new applications adapter for the gallery view and registers it.
	 */
	private void bindApplications() {
		if (mAppsList == null) {
			mAppsList = (Gallery) findViewById(R.id.all_apps);
		}
		mAppsList.setAdapter(new ApplicationsAdapter(this, mApplications));
		mAppsList.setSelection(0);
	}
	
	/**
	 * Binds actions to the various buttons.
	 */
	private void bindButtons() {
		mAppsList.setOnItemClickListener(new ApplicationLauncher());
	}
	
	/**
	 * When no wallpaper was manually set, a default wallpaper is used instead.
	 */
	private void setDefaultWallpaper() {
		Log.d(TAG, "In setDefaultWallpaper()");
		if (!mWallpaperChecked) {
			WallpaperManager manager = WallpaperManager.getInstance(this);
			Drawable wallpaper = manager.peekDrawable();
			if (wallpaper == null) {
				try {
					manager.clear();
				} catch (IOException e) {
					Log.e(TAG, "Failed to clear wallpaper " + e);
				}
			} else {
				Log.d(TAG, "Setting wallpaper");
				getWindow().setBackgroundDrawable(wallpaper);
			}
			mWallpaperChecked = true;
		}
	}
	
	private static ApplicationInfo getApplicationInfo(PackageManager manager, Intent intent) {
		final ResolveInfo resolveInfo = manager.resolveActivity(intent, 0);
		
		if (resolveInfo == null) {
			return null;
		}
		
		final ApplicationInfo info = new ApplicationInfo();
		final ActivityInfo activityInfo = resolveInfo.activityInfo;
		info.icon = activityInfo.loadIcon(manager);
		if (info.title == null || info.title.length() == 0) {
			info.title = activityInfo.loadLabel(manager);
		}
		if (info.title == null) {
			info.title = "";
		}
		return info;
	}
	
	/**
	 * Loads the list of installed applications in mApplications.
	 */
	private void loadApplications(boolean isLaunching) {
		if (isLaunching && mApplications != null) {
			return;
		}
		
		PackageManager manager = getPackageManager();
		
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		
		final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
		Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));
		
		if (apps != null) {
			final int count = apps.size();
			
			if (mApplications == null) {
				mApplications = new ArrayList<ApplicationInfo>(count);
			}
			mApplications.clear();
			
			for (int i = 0; i < count; i++) {
				ApplicationInfo application = new ApplicationInfo();
				ResolveInfo info = apps.get(i);
				
				application.title = info.loadLabel(manager);
				application.setActivity(new ComponentName(
						info.activityInfo.applicationInfo.packageName,
						info.activityInfo.name),
						Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				application.icon = info.activityInfo.loadIcon(manager);
				
				mApplications.add(application);
			}
		}
	}
	
	/**
     * GridView adapter to show the list of all installed applications.
     */
    private class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo> {
        private Rect mOldBounds = new Rect();
		private Context mContext;

        public ApplicationsAdapter(Context context, ArrayList<ApplicationInfo> apps) {
            super(context, 0, apps);
			mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
			final ApplicationInfo info = mApplications.get(position);

            if (convertView == null) {
                final LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.application, parent, false);
            }

            Drawable icon = info.icon;

            if (!info.filtered) {
                //final Resources resources = getContext().getResources();
                int width = 64;//(int) resources.getDimension(android.R.dimen.app_icon_size);
                int height = 64;//(int) resources.getDimension(android.R.dimen.app_icon_size);

                final int iconWidth = icon.getIntrinsicWidth();
                final int iconHeight = icon.getIntrinsicHeight();

                if (icon instanceof PaintDrawable) {
                    PaintDrawable painter = (PaintDrawable) icon;
                    painter.setIntrinsicWidth(width);
                    painter.setIntrinsicHeight(height);
                }


//                if (width > 0 && height > 0 && (width < iconWidth || height < iconHeight)) {
                    final float ratio = (float) iconWidth / iconHeight;

                    if (iconWidth > iconHeight) {
                        height = (int) (width / ratio);
                    } else if (iconHeight > iconWidth) {
                        width = (int) (height * ratio);
                    }

                    final Bitmap.Config c =
                            icon.getOpacity() != PixelFormat.OPAQUE ?
                                Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                    final Bitmap thumb = Bitmap.createBitmap(width, height, c);
                    final Canvas canvas = new Canvas(thumb);
                    canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG, 0));
                    // Copy the old bounds to restore them later
                    // If we were to do oldBounds = icon.getBounds(),
                    // the call to setBounds() that follows would
                    // change the same instance and we would lose the
                    // old bounds
                    mOldBounds.set(icon.getBounds());
                    icon.setBounds(0, 0, width, height);
                    icon.draw(canvas);
                    icon.setBounds(mOldBounds);
                    icon = info.icon = new BitmapDrawable(thumb);
                    info.filtered = true;
//                }

            }

            final TextView textView = (TextView) convertView.findViewById(R.id.label);
            textView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
            textView.setText(info.title);

            return convertView;
        }
    }

	/**
	 * Starts the selected activity/application in the gallery view.
	 */
	private class ApplicationLauncher implements AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView parent, View v, int position, long id) {
			ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
			startActivity(app.intent);
		}
	}
	
	/**
	 * Receives intents from other applications to change the wallpaper
	 */
	private class WallpaperIntentReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			getWindow().setBackgroundDrawable(getWallpaper());
		}
	}
	
	/**
	 * Receives notifications when applications are added/removed
	 */
	private class ApplicationsIntentReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			loadApplications(false);
			bindApplications();
		}
	}
}
