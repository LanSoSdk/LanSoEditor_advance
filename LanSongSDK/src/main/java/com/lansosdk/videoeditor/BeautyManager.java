package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.lansosdk.box.CameraLayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.LanSongFilter.LanSongLookupFilter;
import com.lansosdk.LanSongFilter.LanSongBeautyTuneFilter;
import com.lansosdk.LanSongFilter.LanSongBeautyWhiteFilter;
import com.lansosdk.box.LSOLog;

public class BeautyManager {
    private boolean isTuneBeauting;
    private LanSongLookupFilter mlookupFilter;
    private LanSongBeautyTuneFilter beautyTuneFilter;
    private LanSongBeautyWhiteFilter beautyWhiteFilter;

    private Context mContext;

    public BeautyManager(Context ctx) {
        mContext = ctx;
    }

    /**
     * 增加美颜
     *
     * @param camlayer
     */
    public void addBeauty(CameraLayer camlayer) {
        if (camlayer != null) {
            isTuneBeauting = true;


            ArrayList<LanSongFilter> filters = new ArrayList<LanSongFilter>();

//
            if (LanSoEditor.getCPULevel() >= 0) {
                beautyTuneFilter = new LanSongBeautyTuneFilter();
                beautyWhiteFilter = null;
                filters.add(beautyTuneFilter);
            } else {
                beautyTuneFilter = null;
                beautyWhiteFilter = new LanSongBeautyWhiteFilter();
                filters.add(beautyWhiteFilter);
            }


            camlayer.setBeautyBrightness(1); // 设置亮度;

            String bmpStr = copyAssets(mContext, "lansongbeauty.png");
            if (bmpStr != null) {
                mlookupFilter = new LanSongLookupFilter(0.22f);
                Bitmap bmp = BitmapFactory.decodeFile(bmpStr);
                mlookupFilter.setBitmap(bmp);
                filters.add(mlookupFilter);
            } else {
                LSOLog.e("无法获取lansongbeauty图片文件");
            }

            camlayer.switchFilterList(filters);
        } else {
            LSOLog.e( "add beauty error. camlayer is null");
        }
    }
    /**
     * 删除美颜
     *
     * @param camlayer
     */
    public void deleteBeauty(CameraLayer camlayer) {
        if (camlayer != null) {
            camlayer.switchFilterList(null);
            mlookupFilter = null;
            beautyTuneFilter = null;
            beautyWhiteFilter = null;
            isTuneBeauting = false;
        } else {
            LSOLog.e("delete beauty error. camlayer is null");
        }
    }

    /**
     * 是否正在美颜;
     *
     * @return
     */
    public boolean isBeauting() {
        return this.isTuneBeauting;
    }

    /**
     * 提高亮度
     *
     * @param camlayer
     */
    public void increaseBrightness(CameraLayer camlayer) {
        if (camlayer != null) {
            camlayer.adjustBeautyHigh();
        }
    }

    /**
     * 降低亮度
     */
    public void discreaseBrightness(CameraLayer camlayer) {
        if (camlayer != null) {
            camlayer.adjustBeautyLow();
        }
    }

    /**
     * 微调当前点缀的红色成分; 为0.0,则不调整; 为1.0则呈现淡淡的白色;.默认是0.22;
     *
     * @param level
     */
    public void setPinkColor(float level) {
        if (mlookupFilter != null) {
            mlookupFilter.setIntensity(level);
        }
    }

    /**
     * 调整美颜中的色温, 可以调整红润或白皙; 为0,则红润; 为1则是白冷色; 默认是0.22; 如果您需要调整别的参数,
     * 也可以直接调用LanSongBeautyTuneFilter对象,来进行调整;
     *
     * @param level
     */
    public void setWarmCool(float level) {
        if (beautyTuneFilter != null) {
            beautyTuneFilter.setWarmCoolLevel(level);
        }
    }

    public LanSongBeautyTuneFilter getTuneFilter() {
        return beautyTuneFilter;
    }

    public LanSongLookupFilter getLookupFilter() {
        return mlookupFilter;
    }


    /**
     * 获取美颜滤镜;
     * @param ctx 语境
     * @return
     */
    public static List<LanSongFilter> getBeaufulFilters(Context ctx) {
        List<LanSongFilter> filters = new ArrayList<LanSongFilter>();

        if (LanSoEditor.getCPULevel() >= 0) {
            LanSongBeautyTuneFilter beautyTuneFilter = new LanSongBeautyTuneFilter();
            filters.add(beautyTuneFilter);
        } else {
            LanSongBeautyWhiteFilter beautyWhiteFilter = new LanSongBeautyWhiteFilter();
            filters.add(beautyWhiteFilter);
        }

        String bmpStr = copyAssets(ctx, "lansongbeauty.png");
        if (bmpStr != null) {
            LanSongLookupFilter  lookupFilter = new LanSongLookupFilter(0.22f);
            Bitmap bmp = BitmapFactory.decodeFile(bmpStr);
            lookupFilter.setBitmap(bmp);
            filters.add(lookupFilter);
        } else {
            LSOLog.e("无法获取lansongbeauty图片文件");
        }
        return filters;
    }

    public static String copyAssets(Context context, String assetsName) {

        String filePath;
        if(LanSongFileUtil.FileCacheDir !=null && !LanSongFileUtil.FileCacheDir.endsWith("/")){
            filePath = LanSongFileUtil.FileCacheDir + "/" + assetsName;
        }else{
            filePath = LanSongFileUtil.FileCacheDir +  assetsName;
        }

        File dir = new File(LanSongFileUtil.FileCacheDir);
        // 如果目录不中存在，创建这个目录
        if (!dir.exists())
            dir.mkdirs();
        try {
            if (!(new File(filePath)).exists()) { // 如果不存在.
//                InputStream is = mContext.getResources().getAssets().open(assetsName);  //原来的
                InputStream is = context.getAssets().open(assetsName);
                FileOutputStream fos = new FileOutputStream(filePath);
                byte[] buffer = new byte[7168];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            } else {
                Log.i("copyFile","copyAssets() is not work. file existe:"+filePath);
            }
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
