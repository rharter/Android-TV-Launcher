package com.ryanharter.android.tv;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllAppsFragment extends Fragment {

	private static ArrayList<ApplicationInfo> mApplications;

	private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();

	private GridView mAppsList;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_all_apps, container, false);

		mAppsList = (GridView) v.findViewById(R.id.grid);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerIntentReceivers();
		loadApplications(true);
		bindApplications();
		bindButtons();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		getActivity().unregisterReceiver(mApplicationsReceiver);
	}
	
	/**
     * Registers various intent receivers. The current implementation registers
     * only a wallpaper intent receiver to let other applications change the
     * wallpaper and an application intent receiver to let us know when packages 
	 * change.
     */
	private void registerIntentReceivers() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addDataScheme("package");
		getActivity().registerReceiver(mApplicationsReceiver, filter);
	}
	
	/**
	 * Creates a new applications adapter for the gallery view and registers it.
	 */
	private void bindApplications() {
		if (mAppsList == null) {
			mAppsList = (GridView) getView().findViewById(R.id.grid);
		}
		mAppsList.setAdapter(new ApplicationsAdapter(getActivity(), mApplications));
		mAppsList.setSelection(0);
	}
	
	/**
	 * Binds actions to the various buttons.
	 */
	private void bindButtons() {
		mAppsList.setOnItemClickListener(new ApplicationLauncher());
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
		
		PackageManager manager = getActivity().getPackageManager();
		
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
                final LayoutInflater inflater = getActivity().getLayoutInflater();
                convertView = inflater.inflate(R.layout.application, parent, false);
            }

            Drawable icon = info.icon;

            if (!info.filtered) {
                final Resources resources = getActivity().getResources();
                int width = (int) resources.getDimension(android.R.dimen.app_icon_size);
                int height = (int) resources.getDimension(android.R.dimen.app_icon_size);

                final int iconWidth = icon.getIntrinsicWidth();
                final int iconHeight = icon.getIntrinsicHeight();

                if (icon instanceof PaintDrawable) {
                    PaintDrawable painter = (PaintDrawable) icon;
                    painter.setIntrinsicWidth(width);
                    painter.setIntrinsicHeight(height);
                }


                if (width > 0 && height > 0 && (width < iconWidth || height < iconHeight)) {
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
                }

            }

            final TextView textView = (TextView) convertView.findViewById(R.id.label);
            final ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);
            textView.setText(info.title);
            iconView.setImageDrawable(icon);

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
	 * Receives notifications when applications are added/removed
	 */
	private class ApplicationsIntentReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			loadApplications(false);
			bindApplications();
		}
	}
}