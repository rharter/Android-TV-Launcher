/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ryanharter.atv.launcher.model;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Represents an item in the launcher.
 */
public class ItemInfo {

    static final int NO_ID = -1;

    /**
     * The id in the settings database for this item
     */
    long id = NO_ID;

    /**
     * Iindicates the screen in which the shortcut appears.
     */
    long screenId = -1;

    /**
     * Indicates the X position of the associated cell.
     */
    int cellX = -1;

    /**
     * Indicates the Y position of the associated cell.
     */
    int cellY = -1;

    /**
     * Indicates the X cell span.
     */
    int spanX = 1;

    /**
     * Indicates the Y cell span.
     */
    int spanY = 1;

    /**
     * Indicates the minimum X cell span.
     */
    int minSpanX = 1;

    /**
     * Indicates the minimum Y cell span.
     */
    int minSpanY = 1;

    /**
     * Indicates that this item needs to be updated in the db
     */
    boolean requiresDbUpdate = false;

    /**
     * Title of the item
     */
    public CharSequence title;

    /**
     * The position of the item in a drag-and-drop operation.
     */
    int[] dropPos = null;

    ItemInfo() {
    }

    ItemInfo(ItemInfo info) {
        id = info.id;
        cellX = info.cellX;
        cellY = info.cellY;
        spanX = info.spanX;
        spanY = info.spanY;
        screenId = info.screenId;
    }

    protected Intent getIntent() {
        throw new RuntimeException("Unexpected Intent");
    }

    static byte[] flattenBitmap(Bitmap bitmap) {
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write.
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w("Favorite", "Could not write icon");
            return null;
        }
    }

//    static void writeBitmap(ContentValues values, Bitmap bitmap) {
//        if (bitmap != null) {
//            byte[] data = flattenBitmap(bitmap);
//            values.put(LauncherSettings.Favorites.ICON, data);
//        }
//    }

    /**
     * It is very important that sub-classes implement this if they contain any references
     * to the activity (anything in the view hierarchy etc.). If not, leaks can result since
     * ItemInfo objects persist across rotation and can hence leak by holding stale references
     * to the old view hierarchy / activity.
     */
    void unbind() {
    }

    @Override
    public String toString() {
        return "Item(id=" + this.id + " screen=" + screenId + " cellX=" + cellX
                + " cellY=" + cellY + " spanX=" + spanX
                + " spanY=" + spanY + " dropPos=" + dropPos + ")";
    }
}