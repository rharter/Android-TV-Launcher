package com.ryanharter.android.tv;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;

class ApplicationInfo {
	/**
	 * The application name.
	 */
	CharSequence title;
	
	/**
	 * The intent used to start the application.
	 */
	Intent intent;
	
	/**
	 * The application icon
	 */
	Drawable icon;
	
	/**
	 * When set to true, indicates that the icon has been resized.
	 */
	boolean filtered;
	
	/**
	 * Creates the application intent based on a component name and various launch flags.
	 *
	 * @param className the class name of the component representing the intent
	 * @param launchFlags the launch flags
	 */
	final void setActivity(ComponentName className, int launchFlags) {
		intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(className);
		intent.setFlags(launchFlags);
	}
	
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationInfo)) {
            return false;
        }

        ApplicationInfo that = (ApplicationInfo) o;
        return title.equals(that.title) &&
                intent.getComponent().getClassName().equals(
                        that.intent.getComponent().getClassName());
    }

    @Override
    public int hashCode() {
        int result;
        result = (title != null ? title.hashCode() : 0);
        final String name = intent.getComponent().getClassName();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
