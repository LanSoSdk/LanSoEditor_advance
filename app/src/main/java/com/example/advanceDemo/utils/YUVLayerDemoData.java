package com.example.advanceDemo.utils;

/**
 * 把yuv分开
 *
 * @author Administrator
 */
public class YUVLayerDemoData {
    int w;
    int h;

    public byte[] yuv;
    public byte[] y;
    public byte[] u;
    public byte[] v;
    public byte[] uv;

    public YUVLayerDemoData(int w, int h, byte[] yuv) {
        this.w = w;
        this.h = h;
        this.yuv = yuv;

        y = new byte[w * h];
        u = new byte[w * h / 4];
        v = new byte[w * h / 4];
        uv = new byte[w * h / 2];
        for (int i = 0; i < y.length; i++) {
            y[i] = yuv[i];
        }
        for (int i = 0; i < uv.length; i++) {
            uv[i] = yuv[w * h + i];
        }
        for (int i = 0, index = w * h; i < u.length; i++) {
            v[i] = yuv[index++];
            u[i] = yuv[index++];
        }
        int r, g, b;
    }

}
