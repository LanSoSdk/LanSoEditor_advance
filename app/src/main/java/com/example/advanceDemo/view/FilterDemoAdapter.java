package com.example.advanceDemo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.advanceDemo.layerDemo.Demo3LayerFilterActivity.NameBitmap;
import com.lansoeditor.advanceDemo.R;
import com.lansosdk.videoeditor.FilterList;

import java.util.ArrayList;
import java.util.Map;

public class FilterDemoAdapter extends BaseAdapter {

    ArrayList<NameBitmap>  bmpList=new ArrayList<>();
    private LayoutInflater mInflater;

    public FilterDemoAdapter(Context con, ArrayList<NameBitmap> list) {
        this.bmpList = list;
        mInflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);// LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        if (bmpList != null) {
            return bmpList.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {

            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.filter_item, null);
            viewHolder.ivImage = (ImageView) convertView.findViewById(R.id.id_filter_item_iv);
            viewHolder.tvName = (TextView) convertView.findViewById(R.id.id_filter_item_tv);
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (position < bmpList.size()) {
            NameBitmap bmp = bmpList.get(position);
            if (bmp != null) {
                viewHolder.ivImage.setImageBitmap(bmp.bitmap);
                viewHolder.tvName.setText(bmp.name);
            }
        }
        return convertView;
    }

    private static class ViewHolder {
        private TextView tvName;
        private ImageView ivImage;
    }
}