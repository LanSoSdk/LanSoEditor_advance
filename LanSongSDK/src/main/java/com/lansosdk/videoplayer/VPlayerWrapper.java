package com.lansosdk.videoplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;

import com.lansosdk.box.LSOLog;
import com.lansosdk.videoeditor.MediaInfo;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * 已经废弃,请不要使用
 */
@Deprecated
public class VPlayerWrapper {
    private Uri mUri;
    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;   //没有stop类型,因为stop就是release
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private int mCurrentState = STATE_IDLE;

    // All the stuff we need for playing and showing a video
    private VideoPlayer videoPlayer = null;
    private int mMainVideoWidth;
    private int mMainVideoHeight;

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoRotationDegree;

    private OnLSOPlayerVideoSizeChangedListener mOnSizeChangedListener;
    private OnLSOPlayerCompletionListener mOnCompletionListener;
    private OnLSOPlayeFrameUpdateListener mOnPlayerFrameUpdateListener;


    private OnLSOPlayerPreparedListener mOnPreparedListener;
    private OnLSOPlayerErrorListener mOnErrorListener;
    private OnLSOPlayerInfoListener mOnInfoListener;
    private OnLSOPlayerSeekCompleteListener mOnSeekCompleteListener;


    private int mCurrentBufferPercentage;


    private int mSeekWhenPrepared;  // recording the seek position while preparing
    private boolean mCanPause = true;
    private boolean mCanSeekBack = true;
    private boolean mCanSeekForward = true;

    private Context mAppContext;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private MediaInfo mediaInfo;

    public VPlayerWrapper(Context context) {
        mAppContext = context.getApplicationContext();
        mMainVideoWidth = 0;
        mMainVideoHeight = 0;
        mCurrentState = STATE_IDLE;
    }

    public void setVideoPath(String path) throws FileNotFoundException {
        if (mCurrentState == STATE_IDLE) {
            mediaInfo = new MediaInfo(path);
            if (mediaInfo.prepare()) {
                mUri = Uri.parse(path);
                mSeekWhenPrepared = 0;
            } else {
                throw new FileNotFoundException(" input videoPath is not found.mediaInfo is:" + mediaInfo.toString());
            }
        }
    }

    public void setVideoPath(MediaInfo info) {
        if (mCurrentState == STATE_IDLE) {
            mediaInfo=info;
            mUri = Uri.parse(mediaInfo.filePath);
            mSeekWhenPrepared = 0;
        }
    }


    private void setVideoURI(Uri path) {
        if (mCurrentState == STATE_IDLE) {
            mUri = path;
            mSeekWhenPrepared = 0;
        }
    }

    public void setSurface(Surface surface) {
        videoPlayer.setSurface(surface);
    }

    //播放速度
    private float playSpeed = 1.0f;
    private float playAudioPitch = 0.0f;
    private boolean exactlySeekEnable = false;


    @Deprecated
    public void setSpeedEnable() {  //废弃;

    }

    /**
     * 设置速度, 范围是 0.5---2.0;
     *
     * @param speed
     */
    public void setSpeed(float speed) {
        if (videoPlayer != null) {
            videoPlayer.setSpeed(speed);
        } else {
            playSpeed = speed;
        }
    }

    /**
     * 调节变声;
     * 最低:-1.0; (低沉的男声)
     * 最高: 1.0; (尖锐的女声);
     *
     * @param pitch 范围是-1.0 ---1.0;
     */
    public void setAudioPitch(float pitch) {
        if (videoPlayer != null) {
            videoPlayer.setAudioPitch(pitch * 12);
        } else {
            playAudioPitch = pitch * 12;
        }
    }

    /**
     * 当设置seek的时候, 是否要精确定位;
     *
     * @param is
     */
    public void setExactlySeekEnable(boolean is) {
        if (videoPlayer != null) {
            videoPlayer.setExactlySeekEnable(is);
        } else {
            exactlySeekEnable = is;
        }
    }

    public void prepareAsync() {
        if (mUri == null) {
            LSOLog.e("mUri==mull, open video error.");
            return;
        }
        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        try {
            videoPlayer = createPlayer();
            videoPlayer.setOnPreparedListener(mPreparedListener);
            videoPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            videoPlayer.setOnCompletionListener(mCompletionListener);
            videoPlayer.setOnErrorListener(mErrorListener);
            videoPlayer.setOnInfoListener(mInfoListener);
            videoPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            videoPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            videoPlayer.setOnPlayerFrameUpdateListener(mOnPlayerFrameUpdateListener);

            mCurrentBufferPercentage = 0;
            videoPlayer.setDataSource(mAppContext, mUri);

            if (playSpeed != 1.0f) {
                videoPlayer.setSpeed(playSpeed);
            }
            if (playAudioPitch != 0.0) {
                videoPlayer.setAudioPitch(playAudioPitch);
            }
            if (exactlySeekEnable) {
                videoPlayer.setExactlySeekEnable(true);
            }
            videoPlayer.setLooping(loopEnable);

            videoPlayer.setScreenOnWhilePlaying(true);
            videoPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            LSOLog.e("Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mErrorListener.onError(videoPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            LSOLog.e("Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mErrorListener.onError(videoPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }
    }

    OnLSOPlayerVideoSizeChangedListener mSizeChangedListener = new OnLSOPlayerVideoSizeChangedListener() {
        public void onVideoSizeChanged(VideoPlayer mp, int width, int height, int sarNum, int sarDen) {
            mMainVideoWidth = mp.getVideoWidth();
            mMainVideoHeight = mp.getVideoHeight();
            mVideoSarNum = mp.getVideoSarNum();
            mVideoSarDen = mp.getVideoSarDen();
            if (mMainVideoWidth != 0 && mMainVideoHeight != 0) {
                if (mOnSizeChangedListener != null)
                    mOnSizeChangedListener.onVideoSizeChanged(mp, width, height, sarNum, sarDen);
            }
        }
    };

    OnLSOPlayerPreparedListener mPreparedListener = new OnLSOPlayerPreparedListener() {
        public void onPrepared(VideoPlayer mp) {
            mCurrentState = STATE_PREPARED;

            mMainVideoWidth = mp.getVideoWidth();
            mMainVideoHeight = mp.getVideoHeight();

            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(videoPlayer);
            }

        }
    };

    private OnLSOPlayerCompletionListener mCompletionListener =new OnLSOPlayerCompletionListener() {
                public void onCompletion(VideoPlayer mp) {
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(videoPlayer);
                    }
                }
            };

    private OnLSOPlayerInfoListener mInfoListener = new OnLSOPlayerInfoListener() {
        public boolean onInfo(VideoPlayer mp, int arg1, int arg2) {
            if (mOnInfoListener != null) {
                return mOnInfoListener.onInfo(mp, arg1, arg2);
            }
            return true;
        }
    };

    private OnLSOPlayerErrorListener mErrorListener = new OnLSOPlayerErrorListener() {
        public boolean onError(VideoPlayer mp, int framework_err, int impl_err) {
            mCurrentState = STATE_ERROR;
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(videoPlayer, framework_err, impl_err)) {
                    return true;
                }
            }
            return true;
        }
    };

    private OnLSOPlayerBufferingUpdateListener mBufferingUpdateListener = new OnLSOPlayerBufferingUpdateListener() {
                public void onBufferingUpdate(VideoPlayer mp, int percent) {
                    mCurrentBufferPercentage = percent;
                }
            };


    public void setOnPreparedListener(OnLSOPlayerPreparedListener l) {
        mOnPreparedListener = l;
    }

    public void setOnCompletionListener(OnLSOPlayerCompletionListener l) {
        mOnCompletionListener = l;
    }

    public void setOnFrameUpateListener(OnLSOPlayeFrameUpdateListener listener) {
        mOnPlayerFrameUpdateListener = listener;
    }

    public void setOnErrorListener(OnLSOPlayerErrorListener l) {
        mOnErrorListener = l;
    }


    public void setOnSeekCompleteListener(OnLSOPlayerSeekCompleteListener l) {
        mOnSeekCompleteListener = l;
        if (videoPlayer != null) {
            videoPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
        }
    }

    public void setOnInfoListener(OnLSOPlayerInfoListener l) {
        mOnInfoListener = l;
    }

    public void release() {
        if (videoPlayer != null) {
            videoPlayer.reset();
            videoPlayer.release();
            videoPlayer = null;
            mCurrentState = STATE_IDLE;
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    public void start() {
        if (isInPlaybackState()) {
            videoPlayer.start();
            mCurrentState = STATE_PLAYING;
        } else if (mUri != null && mCurrentState == STATE_IDLE) {
            setVideoURI(mUri);
        }
    }

    public void pause() {
        if (isInPlaybackState()) {
            if (videoPlayer.isPlaying()) {
                videoPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
    }

    public void stop() {
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.release();
            videoPlayer = null;
            mCurrentState = STATE_IDLE;
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    public boolean isPlaying() {
        return isInPlaybackState() && videoPlayer.isPlaying();
    }

    private boolean loopEnable = false;

    public void setLooping(boolean looping) {
        if (videoPlayer != null) {
            videoPlayer.setLooping(looping);
        } else {
            this.loopEnable = looping;
        }
    }

    public boolean isLooping() {
        return (videoPlayer != null) && videoPlayer.isLooping();
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if (videoPlayer != null)
            videoPlayer.setVolume(leftVolume, rightVolume);
    }

    /**
     * 获取时间, 单位是MS, 毫秒;
     * @return
     */
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) videoPlayer.getDuration();
        }

        return -1;
    }

    /**
     * 获取当前播放位置,
     * 单位ms;
     * @return
     */
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) videoPlayer.getCurrentPosition();
        }
        return 0;
    }
    /**
     * 定位,  单位毫秒;
     * 属于不精确定位, 定位到当前指定的时间的前一个关键帧;
     * @param msec
     */
    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            videoPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    /**
     * 获取视频宽度
     */
    public int getVideoWidth() {
        if (mediaInfo != null) {
            return mediaInfo.getWidth();
        } else {
            return videoPlayer != null ? videoPlayer.getVideoWidth() : 0;
        }
    }

    /**
     * 获取视频高度
     */
    public int getVideoHeight() {
        if (mediaInfo != null) {
            return mediaInfo.getHeight();
        } else {
            return videoPlayer != null ? videoPlayer.getVideoHeight() : 0;
        }
    }

    public int getBufferPercentage() {
        if (videoPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (videoPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    public boolean canPause() {
        return mCanPause;
    }

    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    public int getAudioSessionId() {
        return 0;
    }

    static final int AR_ASPECT_FIT_PARENT = 0; // without clip
    static final int AR_ASPECT_FILL_PARENT = 1; // may clip
    static final int AR_ASPECT_WRAP_CONTENT = 2;
    static final int AR_MATCH_PARENT = 3;
    static final int AR_16_9_FIT_PARENT = 4;
    static final int AR_4_3_FIT_PARENT = 5;

    private static final int[] s_allAspectRatio = {
            AR_ASPECT_FIT_PARENT,
            AR_ASPECT_FILL_PARENT,
            AR_ASPECT_WRAP_CONTENT,
            AR_16_9_FIT_PARENT,
            AR_4_3_FIT_PARENT};
    private int mCurrentAspectRatioIndex = 0;
    private int mCurrentAspectRatio = s_allAspectRatio[0];

    public int toggleAspectRatio() {
        mCurrentAspectRatioIndex++;
        mCurrentAspectRatioIndex %= s_allAspectRatio.length;
        mCurrentAspectRatio = s_allAspectRatio[mCurrentAspectRatioIndex];
        return mCurrentAspectRatio;
    }

    private VideoPlayer createPlayer() {
        VideoPlayer mediaPlayer = null;

        VideoPlayer player = null;
        if (mUri != null) {
            player = new VideoPlayer();
            player.setOption(VideoPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
            player.setOption(VideoPlayer.OPT_CATEGORY_PLAYER, "overlay-format", VideoPlayer.SDL_FCC_RV32);
            player.setOption(VideoPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
            player.setOption(VideoPlayer.OPT_CATEGORY_PLAYER, "startPreview-on-prepared", 0);
            player.setOption(VideoPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
            player.setOption(VideoPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);

        }
        mediaPlayer = player;
        return mediaPlayer;
    }

    /**
     * 可以获取mediaPlayer,然后如果后台操作,则可以把MediaPlayer放到service中进行.
     *
     * @return
     */
    public VideoPlayer getVideoPlayer() {
        return videoPlayer;
    }
}

