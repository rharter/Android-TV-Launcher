package com.ryanharter.atv.launcher.ui;

import com.ryanharter.atv.launcher.LauncherAppState;
import com.ryanharter.atv.launcher.R;
import com.ryanharter.atv.launcher.loader.AppLoader;
import com.ryanharter.atv.launcher.model.AppInfo;
import com.ryanharter.atv.launcher.widget.TextClock;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by rharter on 1/11/14.
 */
public class AppListFragment extends Fragment implements LoaderCallbacks<List<AppInfo>> {

    private TextClock mClock;
    private ImageButton mAllAppsButton;
    private GridView mAppsGrid;

    private AppAdapter mAdapter;

    interface Callbacks {
        void onExpandButtonClick();
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override public void onExpandButtonClick() {}
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_app_list, container, false);

        mClock = (TextClock) v.findViewById(R.id.clock);

        mAllAppsButton = (ImageButton) v.findViewById(R.id.expand_button);
        mAllAppsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbacks.onExpandButtonClick();
            }
        });

        mAppsGrid = (GridView) v.findViewById(R.id.app_grid);
        mAppsGrid.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final AppInfo info = mAdapter.getItem(i);
                startActivity(info.intent);
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalArgumentException("Parent activity must implement Callbacks.");
        }
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        mCallbacks = sDummyCallbacks;
        super.onDetach();
    }

    @Override
    public Loader<List<AppInfo>> onCreateLoader(int i, Bundle bundle) {
        return new AppLoader(getActivity(), LauncherAppState.getInstance().getIconCache());
    }

    @Override
    public void onLoadFinished(Loader<List<AppInfo>> listLoader, List<AppInfo> appInfos) {
        mAdapter = new AppAdapter(getActivity(), appInfos);
        mAppsGrid.setAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<List<AppInfo>> listLoader) {
        mAppsGrid.setAdapter(null);
    }

    public class AppAdapter extends ArrayAdapter<AppInfo> {

        private LayoutInflater mLayoutInflater;
        private Resources mResources;

        public AppAdapter(Context context, List<AppInfo> objects) {
            super(context, 0, objects);
            mLayoutInflater = LayoutInflater.from(context);
            mResources = context.getResources();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            ViewHolder holder;
            if (v == null) {
                v = mLayoutInflater.inflate(R.layout.row_app, parent, false);
                holder = new ViewHolder();

                holder.image = (ImageView) v.findViewById(R.id.image);
                holder.title = (TextView) v.findViewById(R.id.title);

                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }

            final AppInfo info = getItem(position);

            holder.image.setImageDrawable(new BitmapDrawable(mResources, info.iconBitmap));
            holder.title.setText(info.title);

            return v;
        }

        class ViewHolder {
            ImageView image;
            TextView title;
        }
    }
}
