package com.example.advanceDemo.view;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.lansoeditor.advanceDemo.R;

import java.util.List;

/*
 * Created by abc on 2019-11-28.
 */public class RecyclerViewAdapter extends BaseQuickAdapter<Bitmap, BaseViewHolder> {
    public RecyclerViewAdapter(int layoutResId, @Nullable List<Bitmap> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Bitmap item) {
        helper.setImageBitmap(R.id.start_recycler_img, item);
    }
}
