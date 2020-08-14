package com.lansosdk.videoeditor;

import android.util.Log;

import java.nio.IntBuffer;

/**
 * 内部使用
 */
@Deprecated
public class AVDecoder {
    // ------------------------------------------------------------------------

    public static native long decoderInit(String filepath);

    public static native long decoderFrame(long handle, long seekUs, int[] out);

    public static native int decoderRelease(long handle);

    public static native boolean decoderIsEnd(long handle);

}
