package com.example.advanceDemo.aeDemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;

import com.example.advanceDemo.utils.DemoLog;
import com.lansosdk.LanSongAe.LSOAeDrawable;
import com.lansosdk.LanSongAe.LSOAeImage;
import com.lansosdk.LanSongAe.LSOAeImageLayer;
import com.lansosdk.LanSongAe.OnLSOFontAssetListener;
import com.lansosdk.box.LSOAudioAsset;
import com.lansosdk.box.LSOLog;
import com.lansosdk.videoeditor.LanSongFileUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.advanceDemo.utils.CopyFileFromAssets.copyAeAssets;
import static com.example.advanceDemo.utils.CopyFileFromAssets.copyShanChu;

/**
 * 因为我们的SDK不联网, 各种资源都在演示工程的Asset文件夹里,
 * 我们这里先拷贝到手机存储卡里, 拿到路径. 开始演示.
 *
 * demo演示的流程是:
 * 素材在Asset文件夹里------>拷贝到手机里----->拿到路径---->演示.
 *
 *
 * 您的流程可能是:
 * 素材在服务器里---------->下载到手机里----->拿到路径--->演示.
 *
 * 说明:
 * 如果您要对json加密, 则在解密后,得到InputStream数据流--->输入到我们的LSOLoadAeJsons.loadAsync.其他步骤一致.
 */
public class AEDemoAsset {

    public LSOAeDrawable drawable1;
    public LSOAeDrawable drawable2;
    public String bgVideo;
    public String mvColorPath1;
    public String mvMaskPath1;
    public String json1Path;
    public String json2Path;


    public String mvColorPath2;
    public String mvMaskPath2;




    public String jsonUsedFontPath;

    public int startFrameIndex =0;
    public int endFrameIndex =Integer.MAX_VALUE;


    LSOAudioAsset audioAsset = null;
    String audioPath;

    int inputType = 0;
    HashMap<String, String> json1ReplaceBitmapPaths = new HashMap<>();
    HashMap<String, String> json1ReplaceVideos = new HashMap<>();
    HashMap<String, String> json1ReplaceTexts = new HashMap<>();


    HashMap<String, String> json2ReplaceBitmapPaths = new HashMap<>();
    HashMap<String, String> json2ReplaceVideos = new HashMap<>();
    HashMap<String, String> json2ReplaceTexts = new HashMap<>();
    Context context;


    public static final int AE_DEMO_AOBAMA = 101;
    public static final int AE_DEMO_XIAOHUANGYA = 102;
    public static final int AE_DEMO_HAOKAN = 103;
    public static final int AE_DEMO_XIANZI = 104;
    public static final int AE_DEMO_KUAISHAN = 106;
    public static final int AE_DEMO_VIDEOBITMAP = 107;
    public static final int AE_DEMO_MORE_PICTURE = 108;
    public static final int AE_DEMO_NONE = 109;


    //两个json叠加
    public static final int AE_DEMO_TWO_JSON_OVERLAY = 112;


    public static final int AE_DEMO_JSON_GAUSSIAN_BLUR1 = 113;


    public static final int AE_DEMO_JSON_GAUSSIAN_BLUR2 = 114;
    public static final int AE_DEMO_JSON_KA_DIAN = 115;
    public static final int AE_DEMO_JSON_SAMPLE_3D = 116;


    public AEDemoAsset(Context context, int inputType) {
        this.context = context;
        this.inputType = inputType;

        json1ReplaceBitmapPaths.clear();
        json1ReplaceVideos.clear();
        json1ReplaceTexts.clear();


        json2ReplaceBitmapPaths.clear();
        json2ReplaceVideos.clear();
        json2ReplaceTexts.clear();

        mvColorPath1 = null;
        mvMaskPath1 = null;
        bgVideo = null;
        json1Path = null;
        json2Path = null;

        if (inputType == AE_DEMO_AOBAMA) {
            json1Path = copyAeAssets(context, "aobama.json");
        } else if (inputType == AE_DEMO_HAOKAN) {
            json1Path = copyAeAssets(context, "haokan.json");
        } else if (inputType == AE_DEMO_XIAOHUANGYA) {
            json1Path = copyAeAssets(context, "xiaoYa.json");
        } else if (inputType == AE_DEMO_XIANZI) {
            json1Path = copyAeAssets(context, "zixiaxianzi.json");
        } else if (inputType == AE_DEMO_KUAISHAN) {
            //文字快闪;
            json1Path = copyAeAssets(context, "kuaishan1.json");
        } else if (inputType == AE_DEMO_VIDEOBITMAP) {
            //早安替换视频;
            json1Path = copyAeAssets(context, "zaoan.json");
        } else if (inputType == AE_DEMO_MORE_PICTURE) {
            json1Path = copyAeAssets(context, "morePicture.json");
        }else if(inputType==AE_DEMO_TWO_JSON_OVERLAY) {
            json1Path = copyAeAssets(context, "tianQi_c2.json");
        }else if(inputType==AE_DEMO_JSON_GAUSSIAN_BLUR1) {  //高斯模糊1
            json1Path = copyAeAssets(context, "gaussianBlur.json");
        }else if(inputType== AE_DEMO_JSON_GAUSSIAN_BLUR2){  //高斯模糊2
            json1Path = copyAeAssets(context, "gaussianBlur2.json");
        }else if(inputType== AE_DEMO_JSON_KA_DIAN) {
            json1Path = copyAeAssets(context, "kadian.json");
        }else if(inputType== AE_DEMO_JSON_SAMPLE_3D) {
            json1Path = copyAeAssets(context, "sample3D.json");
        }else{
            LSOLog.e("AEDemoAsset   input type  unknown(类型未知).");
            testJson();
        }
        CopyAEDemoAssetAsyncTask task=new CopyAEDemoAssetAsyncTask();
        task.execute();
    }

    /**
     * 开始拷贝Ae的各种资源.
     * 因为我们的资源在demo工程里,要拷贝到手机内存中.
     * 您可能来自服务器.则不需要拷贝,只需要下载下来即可;
     * @param listener
     */
    public void waitForCopyCompleted(OnAeAssetCopyedListener listener){

        onAeAssetCopyedListener=listener;
        if(copyCompleted.get()){
            onAeAssetCopyedListener.onCopyed();
        }
    }






    private AtomicBoolean copyCompleted=new AtomicBoolean(false);
    private OnAeAssetCopyedListener onAeAssetCopyedListener=null;

    private class CopyAEDemoAssetAsyncTask extends AsyncTask<Object, Object, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            copyCompleted.set(false);
        }

        @Override
        protected synchronized Boolean doInBackground(Object... params) {

            copyAssetBody();
            copyCompleted.set(true);
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(onAeAssetCopyedListener!=null){
                onAeAssetCopyedListener.onCopyed();
            }
        }
    }


    interface  OnAeAssetCopyedListener{
        void onCopyed();
    }
    /**
     * 拷贝各种资源;
     */
    private void copyAssetBody(){
        if (inputType == AE_DEMO_AOBAMA) {
            bgVideo = copyAeAssets(context, "aobamaEx.mp4");
            mvColorPath1 = copyAeAssets(context, "ao_color.mp4");
            mvMaskPath1 = copyAeAssets(context, "ao_mask.mp4");
        } else if (inputType == AE_DEMO_HAOKAN) {
            mvColorPath1 = copyAeAssets(context, "haokan_mvColor.mp4");
            mvMaskPath1 = copyAeAssets(context, "haokan_mvMask.mp4");
            putBitmapToMap("image_0", "haokan_img_0.jpeg");
            putBitmapToMap("image_1", "haokan_img_1.jpeg");
            putBitmapToMap("image_2", "haokan_img_2.jpeg");
            putBitmapToMap("image_3", "haokan_img_3.jpeg");
            putBitmapToMap("image_4", "haokan_img_4.jpeg");




        } else if (inputType == AE_DEMO_XIAOHUANGYA) {
            mvColorPath1 = copyAeAssets(context, "xiaoYa_mvColor.mp4");
            mvMaskPath1 = copyAeAssets(context, "xiaoYa_mvMask.mp4");
            putBitmapToMap("image_0", "xiaoYa_img_0.jpeg");
            putBitmapToMap("image_1", "xiaoYa_img_1.jpeg");
            putBitmapToMap("image_2", "xiaoYa_img_2.jpeg");
        } else if (inputType == AE_DEMO_XIANZI) {
            mvColorPath1 = copyAeAssets(context, "zixiaxianzi_mvColor.mp4");
            mvMaskPath1 = copyAeAssets(context, "zixiaxianzi_mvMask.mp4");
            putBitmapToMap("image_0", "zixiaxianzi_img0.jpeg");
            putBitmapToMap("image_1", "zixiaxianzi_img1.jpeg");
        } else if (inputType == AE_DEMO_KUAISHAN) {  //文字快闪;
            jsonUsedFontPath = copyAeAssets(context, "STHeiti.ttf");

            bgVideo = null;
            mvColorPath1 = copyAeAssets(context, "kuaishan_mvColor.mp4");
            mvMaskPath1 = copyAeAssets(context, "kuaishan_mvMask.mp4");

            json1ReplaceTexts.put("蓝", "可");
            json1ReplaceTexts.put("松", "替");
            json1ReplaceTexts.put("短", "换");
            json1ReplaceTexts.put("视", "文");
            json1ReplaceTexts.put("频", "字");
        } else if (inputType == AE_DEMO_VIDEOBITMAP) {  //早安替换视频;
            mvColorPath1 = copyAeAssets(context, "zaoan_mvColor.mp4");
            mvMaskPath1 = copyAeAssets(context, "zaoan_mvMask.mp4");
            json1ReplaceVideos.put("image_0", copyAeAssets(context, "zaoan_replace.mp4"));
        } else if (inputType == AE_DEMO_MORE_PICTURE) {

            for (int i = 0; i < 10; i++) {
                String key = "image_" + i;
                String name = "morePicture_img_" + i + ".jpeg";
                putBitmapToMap(key, name);
            }

            String audioPath = copyAeAssets(context, "morePicture.mp3");

            try {
                audioAsset = new LSOAudioAsset(audioPath);//多个图片没有视频,只有mp3,则增加mp3;
                this.audioPath=audioPath;
            } catch (Exception e) {
                e.printStackTrace();
                audioAsset = null;
            }
        }else if(inputType==AE_DEMO_TWO_JSON_OVERLAY) {
            json1ReplaceBitmapPaths.put("image_0", copyAeAssets(context, "tianQi_c2_img_0.jpeg"));

            mvColorPath1 = copyAeAssets(context, "tianQi_c3_mvColor.mp4");
            mvMaskPath1 = copyAeAssets(context, "tianQi_c3_mvMask.mp4");

            json2Path = copyAeAssets(context, "tianQi_c4.json");
            mvColorPath2 = copyAeAssets(context, "tianQi_c5_mvColor.mp4");
            mvMaskPath2 = copyAeAssets(context, "tianQi_c5_mvMask.mp4");

        }else if(inputType==AE_DEMO_JSON_GAUSSIAN_BLUR1) {  //高斯模糊1

            json1ReplaceBitmapPaths.put("image_0", copyAeAssets(context, "gaussianBlur_img_0.jpeg"));
            json1ReplaceBitmapPaths.put("image_1", copyAeAssets(context, "gaussianBlur_img_1.jpeg"));
            json1ReplaceBitmapPaths.put("image_2", copyAeAssets(context, "gaussianBlur_img_2.jpeg"));
            json1ReplaceBitmapPaths.put("image_3", copyAeAssets(context, "gaussianBlur_img_3.jpeg"));
            json1ReplaceBitmapPaths.put("image_4", copyAeAssets(context, "gaussianBlur_img_4.jpeg"));
            json1ReplaceBitmapPaths.put("image_5", copyAeAssets(context, "gaussianBlur_img_5.jpeg"));

            mvColorPath1=copyAeAssets(context,"gaussianBlur_mvColor.mp4");
            mvMaskPath1=copyAeAssets(context,"gaussianBlur_mvMask.mp4");

        }else if(inputType== AE_DEMO_JSON_GAUSSIAN_BLUR2){  //高斯模糊2

            mvColorPath1=copyAeAssets(context,"gaussianBlur2_mvColor.mp4");
            mvMaskPath1=copyAeAssets(context,"gaussianBlur2_mvMask.mp4");
            json1ReplaceBitmapPaths.put("image_0",copyAeAssets(context,"gaussianBlur2_img_0.jpeg"));
            json1ReplaceBitmapPaths.put("image_1",copyAeAssets(context,"gaussianBlur2_img_1.jpeg"));
            json1ReplaceBitmapPaths.put("image_2",copyAeAssets(context,"gaussianBlur2_img_2.jpeg"));
            json1ReplaceBitmapPaths.put("image_3",copyAeAssets(context,"gaussianBlur2_img_3.jpeg"));
            json1ReplaceBitmapPaths.put("image_4",copyAeAssets(context,"gaussianBlur2_img_4.jpeg"));
            json1ReplaceBitmapPaths.put("image_5",copyAeAssets(context,"gaussianBlur2_img_5.jpeg"));
            json1ReplaceBitmapPaths.put("image_6",copyAeAssets(context,"gaussianBlur2_img_6.jpeg"));
        }else if(inputType== AE_DEMO_JSON_KA_DIAN) {
            json1ReplaceBitmapPaths.put("image_0", copyAeAssets(context, "kadian_img_0.jpeg"));
            json1ReplaceBitmapPaths.put("image_1", copyAeAssets(context, "kadian_img_1.jpeg"));
            json1ReplaceBitmapPaths.put("image_2", copyAeAssets(context, "kadian_img_2.jpeg"));
            json1ReplaceBitmapPaths.put("image_3", copyAeAssets(context, "kadian_img_3.jpeg"));
            json1ReplaceBitmapPaths.put("image_4", copyAeAssets(context, "kadian_img_4.jpeg"));
            json1ReplaceBitmapPaths.put("image_5", copyAeAssets(context, "kadian_img_5.jpeg"));
            json1ReplaceBitmapPaths.put("image_6", copyAeAssets(context, "kadian_img_6.jpeg"));
            json1ReplaceBitmapPaths.put("image_7", copyAeAssets(context, "kadian_img_7.jpeg"));
            json1ReplaceBitmapPaths.put("image_8", copyAeAssets(context, "kadian_img_8.jpeg"));
            json1ReplaceBitmapPaths.put("image_9", copyAeAssets(context, "kadian_img_9.jpeg"));
            String audioPath = copyAeAssets(context, "kadian.mp3");
            try {
                audioAsset = new LSOAudioAsset(audioPath);//多个图片没有视频,只有mp3,则增加mp3;
                this.audioPath = audioPath;
            } catch (Exception e) {
                e.printStackTrace();
                audioAsset = null;
            }
        }else if(inputType==AE_DEMO_JSON_SAMPLE_3D){
            //LSNEW  增加3D模板演示; 只是简单的3D演示;
            bgVideo=copyAeAssets(context,"s3d_bgVideo.mp4");
            mvColorPath1=copyAeAssets(context,"s3d_mvColor.mp4");
            mvMaskPath1=copyAeAssets(context,"s3d_mvMask.mp4");

            for(int i=0;i<4;i++){
                String key="image_"+i;
                String name="kadian_img_"+(i+4)+".jpeg";
                putBitmapToMap(key, name);
            }
        }else{
            LSOLog.e("AEDemoAsset   input type  unknown(类型未知).");
        }
    }
    /**
     * 替换各种资源.
     */
    public void replaceJsonAsset() {

        if (drawable1 != null) {
            replaceJsonAsset(drawable1, json1ReplaceBitmapPaths, json1ReplaceVideos, json1ReplaceTexts);
        }

        if (drawable2 != null) {
            replaceJsonAsset(drawable2, json2ReplaceBitmapPaths, json2ReplaceVideos, json2ReplaceTexts);
        }
    }

    /**
     * 替换json文件里的图片/视频/文字
     * @param drawable
     * @param bitmapMaps
     * @param videoMaps
     * @param textpMaps
     */
    private void replaceJsonAsset(LSOAeDrawable drawable, HashMap<String, String> bitmapMaps,
                                  HashMap<String, String> videoMaps,
                                  HashMap<String, String> textpMaps) {

        if (inputType == AEDemoAsset.AE_DEMO_AOBAMA) {

            //奥巴马模板要拿到图片宽高,然后把文字绘制成图片.
            Map<String, LSOAeImage> maps = drawable.getJsonImages();
            for (String key : maps.keySet()) {
                LSOAeImage asset = maps.get(key);

                //因为整个图片是临时生成的, 故直接以bitmap对象的方式替换;
                drawable.updateBitmap("image_0",textToBitmap3Line(asset.getWidth(), asset.getHeight()));
                json1ReplaceBitmapPaths.clear();
            }
        }
        //替换图片
        if (bitmapMaps != null) {
            for (String key : bitmapMaps.keySet()) {
                drawable.updateBitmap(key, bitmapMaps.get(key));
            }
        }

        //替换视频
        if (videoMaps != null) {
            for (String key : videoMaps.keySet()) {
                drawable.updateVideoBitmap(key, videoMaps.get(key));
            }
        }

        //替换文字;
        if (textpMaps != null) {
            for (String key : textpMaps.keySet()) {
                drawable.updateTextWithJsonText(key, textpMaps.get(key));
            }
        }



        //替换字体;
        setJsonFontPath(drawable);

        //在处理过程中, 对图片或视频做调整;
//        drawable.setOnLSOAeImageLayerListenerById("image_0", new OnLSOAeImageLayerListener() {
//            @Override
//            public Bitmap onLSOAeImageLayerProcess(String img_id, String layerName, Bitmap bmp, long ptsUs) {
//                return bmp;
//            }
//        });
    }

    //设置字体;
    private void setJsonFontPath(LSOAeDrawable drawable) {

        /**
         * 设置字体监听, json在处理过程中, 会在第一次找字体的时候, 调用这个监听, 从而您设置json中字体文件的绝对路径
         * 如果您没有设置字体,则默认用系统字体, (系统字体可能不是您要的效果)
         */
        drawable.setFontAssetListener(new OnLSOFontAssetListener() {
            public String getFontPath(String fontFamily) {
                if (fontFamily.equals("STHeiti.ttf")) {
                    Log.e("TAG", "------设置文字字体....: ");
                    return "/sdcard/lansongBox/STHeiti.ttf";  //返回这个字体名字对应的字体文件绝对路径
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * 把图片放到 map中;
     * @param key
     * @param name
     */
    private void putBitmapToMap(String key, String name) {
        String path = copyAeAssets(context, name);
        if (LanSongFileUtil.fileExist(path)) {
            json1ReplaceBitmapPaths.put(key,path);
        } else {
            DemoLog.e("AEPreviewActivity.java json1ReplaceBitmapPaths Demo put byteBuffer  error. key:" + key + " name:" + name);
        }
    }

    private Bitmap textToBitmap3Line(int width, int height) {

        String str1 = "第一行文字";
        String str2 = "第二行文字";
        String str3 = "第三行文字";
        int fontSize = 30;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setTextSize(fontSize);
        if (inputType == AEDemoAsset.AE_DEMO_AOBAMA) {
            canvas.drawColor(Color.RED);
        }
        paint.setColor(Color.BLUE);

        int y = 40;
        int interval = 40;
        canvas.drawText(str1, 0, y, paint);
        y = y + interval;
        canvas.drawText(str2, 0, y, paint);
        y = y + interval;
        canvas.drawText(str3, 0, y, paint);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        return bitmap;
    }

    private void testJson(){
        bgVideo=copyShanChu(context,"bgVideo.mp4");
        json1Path = copyShanChu(context, "data2.json");
//        for(int i=0;i<4;i++){
//            String key="image_"+i;
//            String name="kadian_img_"+(i+4)+".jpeg";
//            putBitmapToMap(key, name);
//        }
    }
}
