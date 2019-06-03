package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.lansosdk.box.CameraLayer;

import java.util.ArrayList;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.LanSongFilter.LanSongLookupFilter;
import com.lansosdk.LanSongFilter.LanSongBeautyTuneFilter;
import com.lansosdk.LanSongFilter.LanSongBeautyWhiteFilter;

public class BeautyManager {
    private static final String TAG ="LanSongSDK";
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

//			boolean isR9s=false;
//			if(Build.MODEL!=null){
//				isR9s=Build.MODEL.contains("OPPO R9s");
//			}
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

            String bmpStr = CopyFileFromAssets.copyAssets(mContext, "lansongbeauty.png");
            if (bmpStr != null) {
                mlookupFilter = new LanSongLookupFilter(0.22f);
                Bitmap bmp = BitmapFactory.decodeFile(bmpStr);
                mlookupFilter.setBitmap(bmp);
                filters.add(mlookupFilter);
            } else {
                Log.e(TAG, "无法获取lansongbeauty图片文件");
            }

            camlayer.switchFilterList(filters);
        } else {
            Log.e(TAG, "add beauty error. camlayer is null");
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
            Log.e(TAG, "delete beauty error. camlayer is null");
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
}
