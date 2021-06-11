package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.ILayerInterface;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOCamLayer;
import com.lansosdk.box.LSOCameraLiveRunnable;
import com.lansosdk.box.LSOCameraRunnable;
import com.lansosdk.box.LSOCameraSizeType;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.OnAddPathListener;
import com.lansosdk.box.OnCameraResumeErrorListener;
import com.lansosdk.box.OnCameraSizeChangedListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnLayerTextureOutListener;
import com.lansosdk.box.OnRemoveCompletedListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.box.OnSetCompletedListener;
import com.lansosdk.box.OnTakePictureListener;
import com.lansosdk.box.OnTextureAvailableListener;

import java.io.File;


public class LSOCameraLive extends LSOFrameLayout implements ILSOTouchInterface {

    private int compWidth = 1080;
    private int compHeight = 1920;

    private LSOCameraLiveRunnable render;

    public LSOCameraLive(Context context) {
        super(context);
    }

    public LSOCameraLive(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOCameraLive(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LSOCameraLive(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //---------------------copy code start---------------------
    protected void sendOnCreateListener() {
        super.sendOnCreateListener();
        if (render != null) {

            if (fullscreen) {
                DisplayMetrics dm = new DisplayMetrics();
                dm = getResources().getDisplayMetrics();
                compWidth = dm.widthPixels;
                compHeight = dm.heightPixels;
            }
            render.setSurface(compWidth, compHeight, getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    public void sendOnResumeListener() {

        super.sendOnResumeListener();
        if (render != null) {
            render.setSurface(compWidth, compHeight, getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    //旋转移动缩放
    public boolean onTextureViewTouchEvent(MotionEvent event) {
        if (isEnableTouch) {
            super.onTextureViewTouchEvent(event);
            return onTouchEvent(event);
        } else {
            return false;
        }
    }



    private OnCreateListener onCreateListener;
    private boolean fullscreen = false;

    public void onCreateFullScreen(OnCreateListener listener) {

        fullscreen = true;
        if (isTextureAvailable() && listener != null) {
            if (render == null) {
                render = new LSOCameraLiveRunnable(getContext(), getWidth(), getHeight());
            }
            listener.onCreate();
        } else {
            onCreateListener = listener;
            setOnTextureAvailableListener(new OnTextureAvailableListener() {
                @Override
                public void onTextureUpdate(int width, int height) {
                    if (render == null) {
                        render = new LSOCameraLiveRunnable(getContext(), getWidth(), getHeight());
                    }
                    onCreateListener.onCreate();
                }
            });
        }
    }


    public void onResumeAsync(OnResumeListener listener) {
        super.onResumeAsync(listener);
        if (render != null) {
            render.onActivityPaused(false);
        }
    }

    public void onPause() {
        super.onPause();
        setOnTextureAvailableListener(new OnTextureAvailableListener() {
            @Override
            public void onTextureUpdate(int width, int height) {
                if (render != null) {
                    render.setSurface(compWidth, compHeight, getSurfaceTexture(), getViewWidth(), getViewHeight());
                }
            }
        });


        if (render != null) {
            render.onActivityPaused(true);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        cancel();
    }

    //---------------render start-----------------------------------------
    private static boolean isCameraOpened = false;

    private boolean frontCamera = false;

    private OnFocusEventListener onFocusListener;

    /**
     * 设置前置摄像头,在开始前设置
     * 默认是后置摄像头;
     * @param is
     */
    public void setFrontCamera(boolean is) {
        if (!isRunning()) {
            frontCamera = is;
        } else {
            LSOLog.e("setFrontCamera error render have been setup .");
        }
    }

    /**
     * 设置预览尺寸, 不建议设置.
     * @param type
     */
    public void setPreviewSize(LSOCameraSizeType type) {
        if (render != null && !render.isRunning()) {
            render.setPreviewSize(type);
        }
    }

    /**
     * 在相机预览过程中, 设置分辨率
     * @param type 分辨率类型;
     */
    public void setCameraSize(LSOCameraSizeType type) {
        if (render != null && render.isRunning()) {
            render.setCameraSize(type);
        }
    }

    /**
     * 获取分辨率;
     * @return
     */
    public LSOCameraSizeType getCameraSize(){
        if (render != null ) {
            return render.getCameraSize();
        }else{
            return LSOCameraSizeType.TYPE_1080P;
        }
    }



    public boolean isRunning() {
        return render != null && render.isRunning();
    }


    /**
     * 当camera从后台回来时,
     * 如果相机被占用则会触发此错误回调;
     *
     * @param listener
     */
    public void setOnCameraResumeErrorListener(OnCameraResumeErrorListener listener) {
        if (render != null) {
            render.setOnCameraResumeErrorListener(listener);
        }
    }

    /**
     * 错误监听
     * @param listener
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        if (render != null) {
            render.setOnLanSongSDKErrorListener(listener);
        }
    }

    public boolean start() {
        super.start();
        if (isCameraOpened) {
            LSOLog.d("LSOCamera  start error. is opened...");
            return true;
        }

        if (getSurfaceTexture() != null) {
            render.setFrontCamera(frontCamera);

            if (render != null) {
                render.setDisplaySurface(getSurfaceTexture(), getViewWidth(), getViewHeight());
                isCameraOpened = render.start();
                if (!isCameraOpened) {
                    LSOLog.e("open LSOCamera error.\n");
                } else {
                    LSOLog.d("LSOCameraLive start preview...");
                }
            }
        } else {
            LSOLog.w("mSurfaceTexture error.");
        }
        return isCameraOpened;
    }

    public void setFilter(LanSongFilter filter) {
        if (render != null) {
            render.setFilter(filter);
        }
    }


    /**
     * 美颜, 范围是0.0---1.0;
     * 0.0 不做磨皮, 1.0:完全磨皮;
     * @param level
     */
    public void setBeautyLevel(float level) {
        if (render != null) {
            render.setBeautyLevel(level);
        }
    }


    /**
     * 禁止美颜;
     */
    public void setDisableBeauty() {
        if (render != null) {
            render.setBeautyLevel(0.0f);
        }
    }


    /**
     * 禁止绿幕抠图
     *
     * @return
     */
    public boolean isGreenMatting() {
        return render != null && render.isGreenMatting();
    }

    /**
     * 设置绿幕抠图
     */
    public void setGreenMatting() {
        if (render != null) {
            render.setGreenMatting();
        } else {
            LSOLog.e("setGreenMatting error. render is null");
        }
    }

    /**
     * 取消绿幕抠图
     * cancel  green matting;
     */
    public void cancelGreenMatting() {
        if (render != null) {
            render.cancelGreenMatting();
        }
    }


    private String bgPath = null;

    public String getBackGroundPath() {
        return bgPath;
    }

    /**
     * 设置背景图片;
     *
     * @param path
     */
    public void setBackGroundBitmapPath(String path) {
        if (render != null && isRunning() && path != null) {
            try {
                bgPath = path;
                render.setBackGroundBitmapPath(path);
            } catch (Exception e) {
                e.printStackTrace();
                LSOLog.e("setBackGroundPath error, input is:" + path);
                bgPath = null;
            }
        }
    }

    /**
     * 替换背景, 背景可以是图片或视频, 内部通过后缀名区分
     * @param path
     * @param audioVolume 音量, 视频时有效, 图片无效
     * @param listener 设置 好后的监听;
     */
    public void setBackGroundPath(String path, float audioVolume, OnSetCompletedListener listener) {

        if(!fileExist(path)){
            listener.onSuccess(false);
            return;
        }

        if (render != null && isRunning()) {
            try {

                String suffix=getFileSuffix(path);
                if(isBitmapSuffix(suffix)){
                    bgPath = path;
                    setBackGroundBitmapPath(path);
                    listener.onSuccess(true);
                }else if(isVideoSuffix(suffix)){
                    bgPath = path;
                    render.setBackGroundVideoPath(path, audioVolume,listener);
                }
            } catch (Exception e) {
                e.printStackTrace();
                LSOLog.e("setBackGroundPath error, input is:" + path);
                listener.onSuccess(false);
            }
        }else{
            listener.onSuccess(false);
        }

    }

    /**
     * 增加一个纹理图层;
     * @param width  纹理的宽度
     * @param height 纹理的高度
     * @return 返回一个图层对象;
     */
    public LSOCamLayer addSurfaceLayer(int width, int height) {
        if (render != null && render.isRunning()) {
            return render.addSurfaceLayer(width, height);
        } else {
            return null;
        }
    }


    /**
     *
     *
     *
     * @param yuvWidth
     * @param yuvHeight
     * @param yuvAngle
     * @return
     */
    /**
     *  增加一个yuv数据的图层;
     *  特定客户使用;
     * @param yuvWidth yuv图像的宽度;
     * @param yuvHeight yuv图像的高度
     * @param yuvAngle yuv图像的角度;
     * @param isNv12 图像数据是否是nv12; 如果是nv21则输入false;
     * @return
     */
    public LSOCamLayer addNv21Layer(int yuvWidth, int yuvHeight, int yuvAngle, boolean isNv12) {
        if (render != null && render.isRunning()) {
            return render.addNv21Layer(yuvWidth,yuvHeight,yuvAngle, isNv12);
        } else {
            return null;
        }
    }

    /**
     * 在摄像机上层增加
     * @return
     */
    public LSOCamLayer addBitmapLayer(String path) {
        if (render != null && render.isRunning()) {
            return render.addBitmapLayer(path);
        } else {
            return null;
        }
    }



    public LSOCamLayer addBitmapLayer(Bitmap bmp) {
        if (render != null && render.isRunning()) {
            return render.addBitmapLayer(bmp);
        } else {
            return null;
        }
    }

    /**
     * 在背景层上增加一层画面
     * @return 返回的是图层对象;
     */
    public LSOCamLayer addBitmapLayerAboveBackGround(Bitmap bmp) {
        if (render != null && render.isRunning()) {
            return render.addBitmapLayerAboveBackGround(bmp);
        } else {
            return null;
        }
    }


    /**
     * 特定客户使用.
     */
    public LSOCamLayer addSurfaceLayerAboveBackGround(int width, int height) {

        if (render != null && render.isRunning()) {
            return render.addSurfaceLayerAboveBackGround(width, height);
        } else {
            return null;
        }
    }



    /**
     *  增加绿色背景的视频或图片;
     *  限制: 绿背景视频的分辨率最大1080P, 时长2分钟, 同时可支持最大5个;
     * @param path 绿色背景的图片或视频,
     * @param cache 是否需要缓冲, 如果分辨率大于1080P,则默认增加缓冲;其他一般不建议缓冲, 填入false;
     * @param listener 完成的监听;
     */
    public void addGreenFileAsync(String path,boolean cache, OnAddPathListener listener){
        if (render != null && render.isRunning()) {
            render.addGreenFileAsync(path,cache, listener);
        }
    }

    /**
     * @param path gif路径;
     * @return
     */
    public LSOCamLayer addGifPath(String path){
        if (render != null && render.isRunning()) {
            return render.addGifPath(path);
        }else{
            LSOLog.e("addGifPath error. ");
            return null;
        }
    }

    /**
     * 删除一个图层
     * @param layer
     * @param listener
     */
    public void removeLayer(LSOCamLayer layer, OnRemoveCompletedListener listener) {
        if (render != null && render.isRunning()) {
            render.removeLayer(layer, listener);
        }
    }


    /**
     * 获取背景图层.
     * 在每次setBackGroundPath会更新背景图层对象, 需要重新获取;
     * 设置后需等待30毫秒后获取
     * 不建议使用;
     *
     * @return
     */
    public LSOCamLayer getBackGroundLayer() {
        if (render != null) {
            return render.getBackGroundLayer();
        }
        return null;
    }

    /**
     * 删除背景层;
     */
    public void removeBackGroundLayer() {
        if (render != null) {
            bgPath = null;
            render.removeBackGroundLayer();
        }
    }

    /**
     * 相机图层的纹理设置监听, 比如在图像流中插入美颜等;
     * @param listener
     */
    public void setOnCameraLayerTextureOutListener(OnLayerTextureOutListener listener){
        if(render!=null){
            render.setOnCameraLayerTextureOutListener(listener);
        }
    }

    /**
     * camera的分辨率改变监听; listener返回的是宽度和高度;
     * @param listener
     */
    public void setOnCameraSizeChangedListener(OnCameraSizeChangedListener listener){
        if(render!=null){
            render.setOnCameraSizeChangedListener(listener);
        }
    }

    /**
     * 获取相机图层; 如果分辨率改变后, 则应该重新获取;
     * @return
     */
    public LSOCamLayer getCameraLayer() {
        if (render != null) {
            return render.getCameraLayer();
        } else {
            return null;
        }
    }

    /**
     * 获取背景视频的播放器;
     * @return
     */
    public MediaPlayer getMediaPlayer() {
        if (render != null) {
            return render.getMediaPlayer();
        } else {
            return null;
        }
    }


    private String fgBitmapPath = null;
    private String fgColorPath = null;

    /**
     * 设置前景图片;
     *
     * @param path 图片路径
     */
    public void setForeGroundBitmap(String path) {

        if (fgBitmapPath != null && fgBitmapPath.equals(path)) {
            return;
        }

        if (render != null && isRunning()) {
            try {
                fgBitmapPath = path;
                fgColorPath = null;
                LSOLog.d("Camera setForeGroundBitmap...");
                render.setForeGroundBitmap(new LSOAsset(path));
            } catch (Exception e) {
                e.printStackTrace();
                fgBitmapPath = null;
            }
        }
    }

    /**
     * 设置前景透明动画,
     *
     * @param colorPath mv color path
     * @param maskPath  mv mask path
     */
    public void setForeGroundVideoPath(String colorPath, String maskPath) {

        if (fgColorPath != null && fgColorPath.equals(colorPath)) {
            return;
        }

        if (render != null && isRunning()) {
            fgBitmapPath = null;
            fgColorPath = colorPath;
            render.setForeGroundVideoPath(colorPath, maskPath);
        } else {
            LSOLog.e("add MVLayer error!");
        }
    }

    /**
     * 删除前景视频
     */
    public void removeForeGroundLayer() {
        fgBitmapPath = null;
        fgColorPath = null;

        if (render != null) {
            render.removeForeGroundLayer();
        }
    }

    /**
     * 拍照
     *
     * @param listener
     */
    public void takePictureAsync(OnTakePictureListener listener) {
        if (render != null && render.isRunning()) {
            render.takePictureAsync(listener);
        } else if (listener != null) {
            listener.onTakePicture(null);
        }
    }

    /**
     * 切换摄像头.
     * change front or back camera;
     */
    public void changeCamera() {
        if (render != null && LSOCameraRunnable.isSupportFrontCamera()) {
            frontCamera = !frontCamera;
            render.changeCamera();
        }
    }


    /**
     * 是否是前置摄像头
     *
     * @return
     */
    public boolean isFrontCamera() {
        return frontCamera;
    }

    /**
     * 开启或关闭闪光灯; 默认是不开启;
     * Turn on or off the flash; the default is not to turn on;
     */
    public void changeFlash() {
        if (render != null) {
            render.changeFlash();
        }
    }

    /**
     * 是否所有的图层都可以触摸事件;
     *
     * @param is
     */
    public void setAllLayerTouchEnable(boolean is) {
        if (render != null) {
            render.setAllLayerTouchEnable(is);
        }
    }


    private static String getFileSuffix(String path) {
        if (path == null)
            return "";
        int index = path.lastIndexOf('.');
        if (index > -1)
            return path.substring(index + 1);
        else
            return "";
    }


    private boolean isBitmapSuffix(String suffix) {

        return "jpg".equalsIgnoreCase(suffix)
                || "JPEG".equalsIgnoreCase(suffix)
                || "png".equalsIgnoreCase(suffix)
                || "heic".equalsIgnoreCase(suffix);
    }

    private boolean isVideoSuffix(String suffix) {
        return "mp4".equalsIgnoreCase(suffix)
                || "mov".equalsIgnoreCase(suffix);
    }

    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    private boolean isEnableTouch = true;

    public void setTouchEnable(boolean enable) {
        isEnableTouch = enable;
    }


    public void setCameraFocusListener(OnFocusEventListener listener) {
        this.onFocusListener = listener;
    }

    @Override
    public ILayerInterface getTouchPointLayer(float x, float y) {
        if (render != null) {
            return render.getTouchPointLayer(x, y);
        } else {
            return null;
        }
    }


    public interface OnFocusEventListener {
        void onFocus(int x, int y);
    }

    float x1 = 0;
    float x2 = 0;
    float y1 = 0;
    float y2 = 0;
    private long downTimeMs;
    private boolean isClickEvent = false;
    private boolean isSlideEvent = false;
    private boolean isZoomEvent = false;
    private float touching;
    private boolean disableZoom = false;

    public void setDisableZoom(boolean is) {
        disableZoom = is;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        if (render == null || !isEnableTouch) { // 如果禁止了touch事件,则直接返回false;
            return false;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // 手指压下屏幕
            case MotionEvent.ACTION_DOWN:
                isZoomEvent = false;
                isClickEvent = true;
                isSlideEvent = true;
                x1 = event.getX();
                y1 = event.getY();
                downTimeMs = System.currentTimeMillis();

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 计算两个手指间的距离
                if (isRunning()) {
                    touching = spacing(event);
                    isZoomEvent = true;
                    isClickEvent = false;
                    isSlideEvent = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isRunning()) {
                    if (isZoomEvent) {
//                        if (event.getPointerCount() >= 2 && !disableZoom) {// 触屏两个点时才执行
//                            float endDis = spacing(event);// 结束距离
//                            int scale = (int) ((endDis - touching) / 10f); // 每变化10f
//                            // zoom变1, 拉近拉远;
//                            if (scale != 0) {
//                                int zoom = render.getZoom() + scale;
//                                render.setZoom(zoom);
//                                touching = endDis;
//                            }
//                        }
                    }
                    if (isClickEvent && (Math.abs(x1 - event.getX()) > touchSlop ||
                            Math.abs(y1 - event.getY()) > touchSlop)) {
                        isClickEvent = false;
                        isSlideEvent = true;
                    }
                }
                break;
            // 手指离开屏幕
            case MotionEvent.ACTION_UP:
                if (isRunning()) {
                    if (isClickEvent && System.currentTimeMillis() - downTimeMs < 200) {
                        float x = event.getX();
                        float y = event.getY();
                        render.doFocus((int) x, (int) y);

                        if (onFocusListener != null) {
                            onFocusListener.onFocus((int) x, (int) y);
                        }

                        isClickEvent = false;
                    }

                    if (!isZoomEvent && !isClickEvent && isSlideEvent) {
                        float offsetX = x1 - event.getX();
                        float offsetY = y1 - event.getY();
                        if (Math.abs(offsetX) < touchSlop && Math.abs(offsetY) < touchSlop) {
                            break;
                        }

                        if (Math.abs(Math.abs(offsetX) - Math.abs(offsetY)) < touchSlop) {
                            break;
                        }

                        if (Math.abs(offsetX) > Math.abs(offsetY)) {
                            if (offsetX > 0) {
                                if (onSlideListener != null) {
                                    onSlideListener.onHorizontalSlide(true);
                                }
                            } else {
                                if (onSlideListener != null) {
                                    onSlideListener.onHorizontalSlide(false);
                                }
                            }

                        } else {
                            if (offsetY > 0) {
                                if (onSlideListener != null) {
                                    onSlideListener.onVerticalSlide(true);
                                }
                            } else {
                                if (onSlideListener != null) {
                                    onSlideListener.onVerticalSlide(false);
                                }
                            }
                        }
                    }

                }
                isZoomEvent = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                isZoomEvent = false;
                isClickEvent = false;
                break;
            default:
                break;
        }
        return true;
    }

    private void setup() {
        if (render == null) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            if (dm.widthPixels * dm.heightPixels < 1080 * 1920) {
                compWidth = 720;
                compHeight = 1280;
            }
            render = new LSOCameraLiveRunnable(getContext(), compWidth, compHeight);
        }
    }


    public void cancel() {
        isCameraOpened = false;
        bgPath = null;
        fgBitmapPath = null;
        fgColorPath = null;
        fullscreen = false;
        if (render != null) {
            render.cancel();
            render = null;
        }
    }

    OnSlideListener onSlideListener;

    public void setOnSlideListener(OnSlideListener onSlideListener) {
        this.onSlideListener = onSlideListener;
    }

    public static boolean fileExist(String absolutePath) {
        if (absolutePath == null)
            return false;
        else {
            File file = new File(absolutePath);
            if (file.exists()){
                return true;
            }
        }
        return false;
    }

    public interface OnSlideListener {

        void onHorizontalSlide(boolean slideLeft);

        void onVerticalSlide(boolean slideUp);
    }
    /**
     * yuv的演示
     *  private void testYuv(){
     *         if(lsoCamera.getCameraLayer()!=null){
     *             lsoCamera.getCameraLayer().setVisibility(false);
     *
     *             String nv12Copy= CopyFileFromAssets.copyAssets(getApplicationContext(),"gzj_nv12_1088x1920_oneFrame.yuv");
     *             byte[] bytes=readFile(nv12Copy);
     *
     *             LSOCamLayer layer=lsoCamera.addNv21Layer(1088,1920,0);
     *             layer.pushNV21Data(bytes,0,false,true);
     *             layer.setGreenMatting(true);
     *         }
     *     }


     public static byte[] readFile(String path) {
     int len= (int) new File(path).length();
     byte b[] = new byte[len];


     StringBuffer result=new StringBuffer();
     BufferedInputStream bis = null;
     FileInputStream fi = null;
     try {
     fi = new FileInputStream(new File(path));
     bis = new BufferedInputStream(fi);
     bis.read(b,0,len);

     } catch (FileNotFoundException e) {
     e.printStackTrace();
     } catch (IOException e) {
     e.printStackTrace();
     }finally {
     if (bis != null) {
     try {
     bis.close();
     } catch (IOException e) {
     e.printStackTrace();
     }
     }
     if (fi != null) {
     try {
     fi.close();
     } catch (IOException e) {
     e.printStackTrace();
     }
     }
     }
     return b;
     }

     */

}
