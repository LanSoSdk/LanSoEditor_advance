package com.example.advanceDemo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Enumeration;
import java.util.Hashtable;

public class BitmapCache {
    static private BitmapCache cache;
    private Hashtable<String, MySoftRef> hashRefs;
    private ReferenceQueue<Bitmap> q;

    private BitmapCache() {
        hashRefs = new Hashtable<String, MySoftRef>();
        q = new ReferenceQueue<Bitmap>();
    }

    public static BitmapCache getInstance() {
        if (cache == null) {
            cache = new BitmapCache();
        }
        return cache;
    }

    public static Bitmap getBitmapByLM(int resid, Context context, int w, int h) {
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inJustDecodeBounds = false;
        Bitmap bit = BitmapFactory
                .decodeResource(context.getResources(), resid);
        // Bitmap bit = new BitmapFactory().decodeFile(path, op);
        if (bit == null) {

            return null;
        }
        float realw = bit.getWidth();
        float realh = bit.getHeight();
        int WIDTH_NEED, HEIGHT_NEED;

        WIDTH_NEED = w;
        HEIGHT_NEED = h;

        int scalew = (int) (realw / WIDTH_NEED);
        int scaleh = (int) (realh / HEIGHT_NEED);
        int scale = (scalew > scaleh ? scalew : scaleh);
        if (scale < 1)
            scale = 1;

        System.out.println("ͼƬ��" + realw + " ����" + realh + "  scale:"
                + scale);

        op.inPreferredConfig = Bitmap.Config.RGB_565;
        op.inPurgeable = true;
        op.inInputShareable = true;
        op.inSampleSize = scale;

        // bit = new BitmapFactory().decodeFile(path,op);
        bit = new BitmapFactory().decodeResource(context.getResources(), resid,
                op);
        return bit;
    }

    public static Bitmap getBitmapByLM(int resid, Context context, int n) {
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inJustDecodeBounds = false;
        Bitmap bit = BitmapFactory
                .decodeResource(context.getResources(), resid);
        // Bitmap bit = new BitmapFactory().decodeFile(path, op);
        if (bit == null) {

            return null;
        }
        int scale = n;
        if (scale < 1)
            scale = 1;

        op.inPreferredConfig = Bitmap.Config.RGB_565;
        op.inPurgeable = true;
        op.inInputShareable = true;
        op.inSampleSize = scale;

        // bit = new BitmapFactory().decodeFile(path,op);
        bit = new BitmapFactory().decodeResource(context.getResources(), resid,
                op);
        return bit;
    }

    private void addCacheBitmap(Bitmap bmp, String key) {
        cleanCache();
        MySoftRef ref = new MySoftRef(bmp, q, key);
        hashRefs.put(key, ref);
    }

    public Bitmap getBitmap(int resId, Context context) {
        Bitmap bmp = null;
        if (hashRefs.containsKey(String.valueOf(resId))) {
            MySoftRef ref = (MySoftRef) hashRefs.get(String.valueOf(resId));
            bmp = (Bitmap) ref.get();
        }
        if (bmp == null) {
            bmp = BitmapFactory.decodeStream(context.getResources()
                    .openRawResource(resId));
            this.addCacheBitmap(bmp, String.valueOf(resId));
        }
        return bmp;
    }

    public Bitmap getBitmap(int resId, Context context, int w, int h) {
        Bitmap bmp = null;
        if (hashRefs.containsKey(String.valueOf(resId))) {
            MySoftRef ref = (MySoftRef) hashRefs.get(String.valueOf(resId));
            bmp = (Bitmap) ref.get();
        }
        if (bmp == null) {
            bmp = getBitmapByLM(resId, context, w, h);
            this.addCacheBitmap(bmp, String.valueOf(resId));
        }
        return bmp;
    }

    private void cleanCache() {
        MySoftRef ref = null;
        while ((ref = (MySoftRef) q.poll()) != null) {
            hashRefs.remove(ref._key);
        }
    }

    public void deleteByID(String key) {
        if (hashRefs.containsKey(key)) {
            hashRefs.remove(key);
            cleanCache();
            // q
        }
    }

    public void clearCache() {

        cleanCache();
        // int i=0;
        Enumeration<String> en = hashRefs.keys();
        while (en.hasMoreElements()) {
            // i++;
            // Log.v("CAMERA", "i:"+i);
            String s = en.nextElement();
            hashRefs.get(s).get().recycle();
        }

        hashRefs.clear();

        System.gc();
        System.runFinalization();
    }

    public int getCount() {
        return hashRefs.size();
    }

    /**
     */
    private class MySoftRef extends SoftReference<Bitmap> {
        private String _key = "0";

        public MySoftRef(Bitmap bmp, ReferenceQueue<Bitmap> q, String key) {
            super(bmp, q);
            _key = key;
        }
    }
}