package com.lansosdk.videoeditor.oldVersion;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.lansosdk.videoeditor.oldVersion.FilterLibrary.FilterType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.lansosdk.LanSongFilter.LanSongFilter;


@Deprecated
public class FilterList {
    public List<String> names = new LinkedList<String>();
    public List<FilterType> filters = new LinkedList<FilterType>();

    public List<Bitmap> filterBitmaps = new LinkedList<Bitmap>();

    public void addFilter(final String name, final FilterType filter) {
        names.add(name);
        filters.add(filter);
    }

    public synchronized void addBitmap(Bitmap bmp) {
        filterBitmaps.add(bmp);
    }

    public synchronized int getBitmapSize() {
        return filterBitmaps.size();
    }

    public synchronized Bitmap getBitmap(int index) {
        if (index < filterBitmaps.size()) {
            return filterBitmaps.get(index);
        } else {
            return null;
        }
    }

    public synchronized String getName(int index) {
        if (index < names.size()) {
            return names.get(index);
        } else {
            return null;
        }
    }

    /**
     * 所有的滤镜都有图片了
     *
     * @return
     */
    public synchronized boolean isAllFilterBitmap() {
        return filters.size() == filterBitmaps.size();
    }

    /**
     * 获取滤镜个数
     *
     * @return
     */
    public synchronized int getSize() {
        return names.size();
    }

    /**
     * 根据 您起的名字, 获取滤镜对象.
     *
     * @param ctx
     * @param name
     * @return
     */
    public synchronized LanSongFilter getFilter(Context ctx, String name) {
        if (name == null) {
            return null;
        }

        if (names.contains(name)) {
            int index = names.indexOf(name);
            FilterType type = filters.get(index);
            return getFilter(ctx, type);
        } else {
            Log.e("filter", "getFilter is error, name error!");
            return null;
        }
    }

    /**
     * 根据 枚举类型获取滤镜对象
     * @param ctx
     * @param filter
     * @return
     */
    public synchronized LanSongFilter getFilter(Context ctx, FilterType filter) {
        return FilterLibrary.getFilterObject(ctx, filter);
    }

    /**
     * 根据索引获取滤镜对象
     *
     * @param ctx
     * @param index
     * @return
     */
    public LanSongFilter getFilter(Context ctx, int index) {
        FilterType type = filters.get(index);
        return getFilter(ctx, type);
    }

    /**
     * 获取当前所有的滤镜列表
     *
     * @param ctx
     * @return
     */
    public ArrayList<LanSongFilter> getFilters(Context ctx) {
        ArrayList<LanSongFilter> retFilters = new ArrayList<LanSongFilter>();

        for (FilterType item : filters) {
            retFilters.add(getFilter(ctx, item));
        }
        return retFilters;
    }
}
