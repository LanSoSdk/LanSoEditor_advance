/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, A0Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lansosdk.videoeditor.oldVersion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;

import com.lansosdk.LanSongFilter.LanSong3x3ConvolutionFilter;
import com.lansosdk.LanSongFilter.LanSong3x3TextureSamplingFilter;
import com.lansosdk.LanSongFilter.LanSongAddBlendFilter;
import com.lansosdk.LanSongFilter.LanSongAlphaBlendFilter;
import com.lansosdk.LanSongFilter.LanSongBeautyAdvanceFilter;
import com.lansosdk.LanSongFilter.LanSongBlurFilter;
import com.lansosdk.LanSongFilter.LanSongBrightnessFilter;
import com.lansosdk.LanSongFilter.LanSongBulgeDistortionFilter;
import com.lansosdk.LanSongFilter.LanSongCGAColorspaceFilter;
import com.lansosdk.LanSongFilter.LanSongChromaKeyBlendFilter;
import com.lansosdk.LanSongFilter.LanSongColorBalanceFilter;
import com.lansosdk.LanSongFilter.LanSongColorBlendFilter;
import com.lansosdk.LanSongFilter.LanSongColorBurnBlendFilter;
import com.lansosdk.LanSongFilter.LanSongColorDodgeBlendFilter;
import com.lansosdk.LanSongFilter.LanSongColorInvertFilter;
import com.lansosdk.LanSongFilter.LanSongContrastFilter;
import com.lansosdk.LanSongFilter.LanSongCrosshatchFilter;
import com.lansosdk.LanSongFilter.LanSongDarkenBlendFilter;
import com.lansosdk.LanSongFilter.LanSongDifferenceBlendFilter;
import com.lansosdk.LanSongFilter.LanSongDissolveBlendFilter;
import com.lansosdk.LanSongFilter.LanSongDistortionPinchFilter;
import com.lansosdk.LanSongFilter.LanSongDistortionStretchFilter;
import com.lansosdk.LanSongFilter.LanSongDivideBlendFilter;
import com.lansosdk.LanSongFilter.LanSongEmbossFilter;
import com.lansosdk.LanSongFilter.LanSongExclusionBlendFilter;
import com.lansosdk.LanSongFilter.LanSongExposureFilter;
import com.lansosdk.LanSongFilter.LanSongFalseColorFilter;
import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.LanSongFilter.LanSongGammaFilter;
import com.lansosdk.LanSongFilter.LanSongGaussianBlurFilter;
import com.lansosdk.LanSongFilter.LanSongGlassSphereFilter;
import com.lansosdk.LanSongFilter.LanSongGrayscaleFilter;
import com.lansosdk.LanSongFilter.LanSongHalftoneFilter;
import com.lansosdk.LanSongFilter.LanSongHardLightBlendFilter;
import com.lansosdk.LanSongFilter.LanSongHazeFilter;
import com.lansosdk.LanSongFilter.LanSongHighlightShadowFilter;
import com.lansosdk.LanSongFilter.LanSongHueBlendFilter;
import com.lansosdk.LanSongFilter.LanSongHueFilter;
import com.lansosdk.LanSongFilter.LanSongIF1977Filter;
import com.lansosdk.LanSongFilter.LanSongIFAmaroFilter;
import com.lansosdk.LanSongFilter.LanSongIFBrannanFilter;
import com.lansosdk.LanSongFilter.LanSongIFEarlybirdFilter;
import com.lansosdk.LanSongFilter.LanSongIFHefeFilter;
import com.lansosdk.LanSongFilter.LanSongIFHudsonFilter;
import com.lansosdk.LanSongFilter.LanSongIFInkwellFilter;
import com.lansosdk.LanSongFilter.LanSongIFLomofiFilter;
import com.lansosdk.LanSongFilter.LanSongIFLordKelvinFilter;
import com.lansosdk.LanSongFilter.LanSongIFNashvilleFilter;
import com.lansosdk.LanSongFilter.LanSongIFRiseFilter;
import com.lansosdk.LanSongFilter.LanSongIFSierraFilter;
import com.lansosdk.LanSongFilter.LanSongIFSutroFilter;
import com.lansosdk.LanSongFilter.LanSongIFToasterFilter;
import com.lansosdk.LanSongFilter.LanSongIFValenciaFilter;
import com.lansosdk.LanSongFilter.LanSongIFWaldenFilter;
import com.lansosdk.LanSongFilter.LanSongIFXproIIFilter;
import com.lansosdk.LanSongFilter.LanSongKuwaharaFilter;
import com.lansosdk.LanSongFilter.LanSongLaplacianFilter;
import com.lansosdk.LanSongFilter.LanSongLevelsFilter;
import com.lansosdk.LanSongFilter.LanSongLightenBlendFilter;
import com.lansosdk.LanSongFilter.LanSongLinearBurnBlendFilter;
import com.lansosdk.LanSongFilter.LanSongLookupFilter;
import com.lansosdk.LanSongFilter.LanSongLuminosityBlendFilter;
import com.lansosdk.LanSongFilter.LanSongMaskBlendFilter;
import com.lansosdk.LanSongFilter.LanSongMonochromeFilter;
import com.lansosdk.LanSongFilter.LanSongMultiplyBlendFilter;
import com.lansosdk.LanSongFilter.LanSongNormalBlendFilter;
import com.lansosdk.LanSongFilter.LanSongOpacityFilter;
import com.lansosdk.LanSongFilter.LanSongOverlayBlendFilter;
import com.lansosdk.LanSongFilter.LanSongPixelationFilter;
import com.lansosdk.LanSongFilter.LanSongPosterizeFilter;
import com.lansosdk.LanSongFilter.LanSongRGBFilter;
import com.lansosdk.LanSongFilter.LanSongSaturationBlendFilter;
import com.lansosdk.LanSongFilter.LanSongSaturationFilter;
import com.lansosdk.LanSongFilter.LanSongScreenBlendFilter;
import com.lansosdk.LanSongFilter.LanSongSepiaFilter;
import com.lansosdk.LanSongFilter.LanSongSoftLightBlendFilter;
import com.lansosdk.LanSongFilter.LanSongSourceOverBlendFilter;
import com.lansosdk.LanSongFilter.LanSongSphereRefractionFilter;
import com.lansosdk.LanSongFilter.LanSongSubtractBlendFilter;
import com.lansosdk.LanSongFilter.LanSongSwirlFilter;
import com.lansosdk.LanSongFilter.LanSongToonFilter;
import com.lansosdk.LanSongFilter.LanSongTwoInputFilter;
import com.lansosdk.LanSongFilter.LanSongVignetteFilter;
import com.lansosdk.LanSongFilter.LanSongWhiteBalanceFilter;
import com.lansosdk.box.BitmapLoader;
import com.lansosdk.box.LSOLog;


@Deprecated
public class FilterLibrary {

    private static FilterList filterList = null;

    public static FilterList getFilterList() {
        if (filterList == null) {
            showAllFilter();
        }
        return filterList;
    }

    public static void showAllFilter() {
        filterList = new FilterList();
        // 2017年11月10日09:38:40 83
        filterList.addFilter("无", FilterType.NONE);
        filterList.addFilter("美颜", FilterType.BEAUTIFUL);

        filterList.addFilter("苦味", FilterType.AMARO);
        filterList.addFilter("玫瑰", FilterType.RISE);
        filterList.addFilter("天蓝", FilterType.HUDSON);  //
        filterList.addFilter("甘菊", FilterType.XPROII);
        filterList.addFilter("常青树", FilterType.SIERRA);  //
        filterList.addFilter("湛蓝", FilterType.LOMOFI);
        filterList.addFilter("早起", FilterType.EARLYBIRD);
        filterList.addFilter("枫树", FilterType.SUTRO);
        filterList.addFilter("收获", FilterType.TOASTER);
        filterList.addFilter("布兰南", FilterType.BRANNAN);
        filterList.addFilter("黑白", FilterType.INKWELL);
        filterList.addFilter("华尔兹", FilterType.WALDEN);
        filterList.addFilter("黄昏", FilterType.HEFE);
        filterList.addFilter("零点", FilterType.VALENCIA);
        filterList.addFilter("乳酪", FilterType.NASHVILLE);
        filterList.addFilter("粉红", FilterType.IF1977);
        filterList.addFilter("金黄", FilterType.LORDKELVIN);

        filterList.addFilter("区域透明", FilterType.LanSongMASK);

        filterList.addFilter("负片", FilterType.INVERT);
        filterList.addFilter("马赛克", FilterType.PIXELATION);

        filterList.addFilter("黑色轮廓", FilterType.VIGNETTE);
        filterList.addFilter("加减雾", FilterType.HAZE);
        filterList.addFilter("玻璃球效果", FilterType.GLASS_SPHERE);
        filterList.addFilter("球面折射",
                FilterType.SPHERE_REFRACTION);

        // 新增
        filterList.addFilter("扩散扭曲", FilterType.PINCH_DISTORTION);
        filterList.addFilter("中心扭曲",FilterType.STRETCH_DISTORTION);
        filterList.addFilter("失真",FilterType.BULGE_DISTORTION);

        filterList.addFilter("亮度", FilterType.BRIGHTNESS);

        filterList.addFilter("高斯模糊", FilterType.LanSongBLUR);

        filterList.addFilter("旋涡", FilterType.SWIRL);
        filterList.addFilter("色调分离", FilterType.POSTERIZE);
        filterList.addFilter("复古", FilterType.SEPIA);

        filterList.addFilter("阴影高亮", FilterType.HIGHLIGHT_SHADOW);
        filterList.addFilter("单色", FilterType.MONOCHROME);
        filterList.addFilter("白平衡", FilterType.WHITE_BALANCE);
        filterList.addFilter("曝光度", FilterType.EXPOSURE);
        filterList.addFilter("色调", FilterType.HUE);
        filterList.addFilter("伽玛", FilterType.GAMMA);

        filterList.addFilter("假彩色", FilterType.FALSE_COLOR);
        filterList.addFilter("颜色平衡", FilterType.COLOR_BALANCE);
        filterList.addFilter("暗色调节",FilterType.LEVELS_FILTER_MIN);
        filterList
                .addFilter("图片查找表", FilterType.LOOKUP_AMATORKA);
        filterList.addFilter("阴影线", FilterType.CROSSHATCH);

        filterList.addFilter("色空间", FilterType.CGA_COLORSPACE);
        filterList.addFilter("Kuwahara", FilterType.KUWAHARA);
        filterList.addFilter("棉麻", FilterType.HALFTONE);

        filterList.addFilter("透明度", FilterType.OPACITY);
        filterList.addFilter("颜色调整", FilterType.RGB);

        filterList.addFilter("灰度", FilterType.GRAYSCALE);
        filterList.addFilter("对比度", FilterType.CONTRAST);
        filterList.addFilter("饱和度", FilterType.SATURATION);

        filterList.addFilter("Blend (Difference)", FilterType.BLEND_DIFFERENCE);
        filterList.addFilter("Blend (Source Over)", FilterType.BLEND_SOURCE_OVER);
        filterList.addFilter("Blend (Color Burn)", FilterType.BLEND_COLOR_BURN);
        filterList.addFilter("Blend (Color Dodge)",FilterType.BLEND_COLOR_DODGE);
        filterList.addFilter("Blend (Darken)", FilterType.BLEND_DARKEN);
        filterList.addFilter("Blend (Dissolve)", FilterType.BLEND_DISSOLVE);
        filterList.addFilter("Blend (Exclusion)", FilterType.BLEND_EXCLUSION);
        filterList.addFilter("Blend (Hard Light)", FilterType.BLEND_HARD_LIGHT);
        filterList.addFilter("Blend (Lighten)", FilterType.BLEND_LIGHTEN);
        filterList.addFilter("Blend (Add)", FilterType.BLEND_ADD);
        filterList.addFilter("Blend (Divide)", FilterType.BLEND_DIVIDE);
        filterList.addFilter("Blend (Multiply)", FilterType.BLEND_MULTIPLY);
        filterList.addFilter("Blend (Overlay)", FilterType.BLEND_OVERLAY);
        filterList.addFilter("Blend (Screen)", FilterType.BLEND_SCREEN);
        filterList.addFilter("Blend (Alpha)", FilterType.BLEND_ALPHA);
        filterList.addFilter("Blend (Color)", FilterType.BLEND_COLOR);
        filterList.addFilter("Blend (Hue)", FilterType.BLEND_HUE);
        filterList.addFilter("Blend (Saturation)", FilterType.BLEND_SATURATION);
        filterList.addFilter("Blend (Luminosity)", FilterType.BLEND_LUMINOSITY);
        filterList.addFilter("Blend (Linear Burn)",FilterType.BLEND_LINEAR_BURN);
        filterList.addFilter("Blend (Soft Light)", FilterType.BLEND_SOFT_LIGHT);
        filterList.addFilter("Blend (Subtract)", FilterType.BLEND_SUBTRACT);
        filterList.addFilter("Blend (Chroma Key)", FilterType.BLEND_CHROMA_KEY);
        filterList.addFilter("Blend (Normal)", FilterType.BLEND_NORMAL);

        filterList.addFilter("浮雕", FilterType.EMBOSS);
        filterList.addFilter("3x3转换", FilterType.THREE_X_THREE_CONVOLUTION);
        filterList.addFilter("复杂锐化", FilterType.LAPLACIAN);
        filterList.addFilter("卡通", FilterType.TOON);
    }

    public static void showDialog(final Context context, final OnLanSongFilterChosenListener listener) {
        if (filterList == null) {
            showAllFilter();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose a filter(total:" + filterList.names.size()
                + " )");
        builder.setItems(filterList.names.toArray(new String[filterList.names.size()]),new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,final int item) {
                        listener.onLanSongFilterChosenListener( getFilterObject(context,filterList.filters.get(item)),
                                filterList.names.get(item));
                    }
                });
        builder.create().show();
    }

    /**
     * 获取滤镜对象;
     * @param context
     * @param type
     * @return
     */
    public static LanSongFilter getFilterObject(final Context context,
                                                final FilterType type) {
        switch (type) {
            case NONE:
                return null;
            /**
             * 注意: 以下五种美颜级别,仅仅是列举,详情可看@BeautylLevel ; 实际您可以任意组合.
             * LanSongBeautyLevel1--5是不存在的滤镜, 仅仅是为了兼容其他滤镜而做的参考.
             */
            case BEAUTIFUL:
                return new LanSongBeautyAdvanceFilter(); // 美颜默认全开.
            // case BEAUTIFUL2: //白皙美颜默认不再使用.
            // return new LanSongBeautyWhiteFilter();
            case CONTRAST:
                return new LanSongContrastFilter(2.0f);
            case GAMMA:
                return new LanSongGammaFilter(2.0f);
            case INVERT:
                return new LanSongColorInvertFilter();
            case PIXELATION:
                return new LanSongPixelationFilter();
            case HUE:
                return new LanSongHueFilter(90.0f);
            case BRIGHTNESS:
                return new LanSongBrightnessFilter(0.5f);
            case GRAYSCALE:
                return new LanSongGrayscaleFilter();
            case SEPIA:
                return new LanSongSepiaFilter();
            case POSTERIZE:
                return new LanSongPosterizeFilter();
            case SATURATION:
                return new LanSongSaturationFilter(1.0f);
            case EXPOSURE:
                return new LanSongExposureFilter(0.0f);
            case HIGHLIGHT_SHADOW:
                return new LanSongHighlightShadowFilter(0.0f, 1.0f);
            case MONOCHROME:
                return new LanSongMonochromeFilter(1.0f, new float[]{0.6f,0.45f, 0.3f, 1.0f});
            case OPACITY:
                return new LanSongOpacityFilter(1.0f);
            case RGB:
                return new LanSongRGBFilter(1.0f, 1.0f, 1.0f);
            case WHITE_BALANCE:
                return new LanSongWhiteBalanceFilter(5000.0f, 0.0f);
            case GaussionBLUR:
                return new LanSongGaussianBlurFilter();
            case LanSongBLUR:
                return new LanSongBlurFilter();
            case VIGNETTE:
                PointF centerPoint = new PointF();
                centerPoint.x = 0.5f;
                centerPoint.y = 0.5f;
                return new LanSongVignetteFilter(centerPoint, new float[]{0.0f,
                        0.0f, 0.0f}, 0.3f, 0.75f);
            case LanSongMASK:
                /**
                 * 这个滤镜的效果是: 把输入源的某区域 处理成透明.
                 *
                 * 详情是: 把一张有透明区域的图片, 叠加到 输入源的中心位置上, 图片中有透明的地方,则把输入源的对应的地方,透明处理.
                 * 等于是把输入源中的一部分抠去.
                 */
                return createBlendFilter(context, LanSongMaskBlendFilter.class);

            case BLEND_DIFFERENCE:
                return createBlendFilter(context,LanSongDifferenceBlendFilter.class);
            case BLEND_SOURCE_OVER:
                return createBlendFilter(context,LanSongSourceOverBlendFilter.class);
            case BLEND_COLOR_BURN:
                return createBlendFilter(context,LanSongColorBurnBlendFilter.class);
            case BLEND_COLOR_DODGE:
                return createBlendFilter(context, LanSongColorDodgeBlendFilter.class);
            case BLEND_DARKEN:
                return createBlendFilter(context, LanSongDarkenBlendFilter.class);
            case BLEND_DISSOLVE:
                return createBlendFilter(context, LanSongDissolveBlendFilter.class);
            case BLEND_EXCLUSION:
                return createBlendFilter(context,LanSongExclusionBlendFilter.class);
            case BLEND_HARD_LIGHT:
                return createBlendFilter(context,LanSongHardLightBlendFilter.class);
            case BLEND_LIGHTEN:
                return createBlendFilter(context, LanSongLightenBlendFilter.class);
            case BLEND_ADD:
                return createBlendFilter(context, LanSongAddBlendFilter.class);
            case BLEND_DIVIDE:
                return createBlendFilter(context, LanSongDivideBlendFilter.class);
            case BLEND_MULTIPLY:
                return createBlendFilter(context, LanSongMultiplyBlendFilter.class);
            case BLEND_OVERLAY:
                return createBlendFilter(context, LanSongOverlayBlendFilter.class);
            case BLEND_SCREEN:
                return createBlendFilter(context, LanSongScreenBlendFilter.class);
            case BLEND_ALPHA:
                return createBlendFilter(context, LanSongAlphaBlendFilter.class);
            case BLEND_COLOR:
                return createBlendFilter(context, LanSongColorBlendFilter.class);
            case BLEND_HUE:
                return createBlendFilter(context, LanSongHueBlendFilter.class);
            case BLEND_SATURATION:
                return createBlendFilter(context,LanSongSaturationBlendFilter.class);
            case BLEND_LUMINOSITY:
                return createBlendFilter(context,LanSongLuminosityBlendFilter.class);
            case BLEND_LINEAR_BURN:
                return createBlendFilter(context,LanSongLinearBurnBlendFilter.class);
            case BLEND_SOFT_LIGHT:
                return createBlendFilter(context,LanSongSoftLightBlendFilter.class);
            case BLEND_SUBTRACT:
                return createBlendFilter(context, LanSongSubtractBlendFilter.class);
            case BLEND_CHROMA_KEY:
                return createBlendFilter(context, LanSongChromaKeyBlendFilter.class);
            case BLEND_NORMAL:
                return createBlendFilter(context, LanSongNormalBlendFilter.class);

            case LOOKUP_AMATORKA:
                LanSongLookupFilter amatorka = new LanSongLookupFilter();
                String var3 = "assets://LSResource/lookup_amatorka.png";
                amatorka.setBitmap(BitmapLoader.load(context, var3, 0, 0));
                return amatorka;
            case CROSSHATCH:
                return new LanSongCrosshatchFilter();
            case CGA_COLORSPACE:
                return new LanSongCGAColorspaceFilter();
            case KUWAHARA:
                return new LanSongKuwaharaFilter();

            case BULGE_DISTORTION:
                /**
                 * 凸凹 可以设置凸凹的地方, 凸凹的范围, 凸起还是凹下.
                 */
                return new LanSongBulgeDistortionFilter();

            // 新增
            case PINCH_DISTORTION:
                return new LanSongDistortionPinchFilter();
            case STRETCH_DISTORTION:
                return new LanSongDistortionStretchFilter();

            case GLASS_SPHERE:
                return new LanSongGlassSphereFilter();
            case HAZE:
                return new LanSongHazeFilter();
            case SPHERE_REFRACTION:
                return new LanSongSphereRefractionFilter();
            case SWIRL:
                return new LanSongSwirlFilter();
            case FALSE_COLOR:
                return new LanSongFalseColorFilter();
            case COLOR_BALANCE:
                return new LanSongColorBalanceFilter();
            case LEVELS_FILTER_MIN:
                LanSongLevelsFilter levelsFilter = new LanSongLevelsFilter();
                levelsFilter.setMin(0.0f, 3.0f, 1.0f);
                return levelsFilter;
            case HALFTONE:
                return new LanSongHalftoneFilter();
            case AMARO:
                return new LanSongIFAmaroFilter(context);
            case RISE:
                return new LanSongIFRiseFilter(context);
            case HUDSON:
                return new LanSongIFHudsonFilter(context);
            case XPROII:
                return new LanSongIFXproIIFilter(context);
            case SIERRA:
                return new LanSongIFSierraFilter(context);
            case LOMOFI:
                return new LanSongIFLomofiFilter(context);
            case EARLYBIRD:
                return new LanSongIFEarlybirdFilter(context);
            case SUTRO:
                return new LanSongIFSutroFilter(context);
            case TOASTER:
                return new LanSongIFToasterFilter(context);
            case BRANNAN:
                return new LanSongIFBrannanFilter(context);
            case INKWELL:
                return new LanSongIFInkwellFilter(context);
            case WALDEN:
                return new LanSongIFWaldenFilter(context);
            case HEFE:
                return new LanSongIFHefeFilter(context);
            case VALENCIA:
                return new LanSongIFValenciaFilter(context);
            case NASHVILLE:
                return new LanSongIFNashvilleFilter(context);
            case LORDKELVIN:
                return new LanSongIFLordKelvinFilter(context);
            case IF1977:
                return new LanSongIF1977Filter(context);

            case EMBOSS:
                return new LanSongEmbossFilter();
            case THREE_X_THREE_CONVOLUTION:
                LanSong3x3ConvolutionFilter convolution = new LanSong3x3ConvolutionFilter();
                convolution.setConvolutionKernel(new float[]{-1.0f, 0.0f, 1.0f,
                        -2.0f, 0.0f, 2.0f, -1.0f, 0.0f, 1.0f});
                return convolution;
            case LAPLACIAN:
                return new LanSongLaplacianFilter();
            case TOON:
                return new LanSongToonFilter();
            default:
                LSOLog.w("No filter of that type!, return null");
                return null;
//                throw new IllegalStateException("No filter of that type!");
        }

    }

    private static LanSongFilter createBlendFilter(Context context,
                                                   Class<? extends LanSongTwoInputFilter> filterClass) {
        try {
            LanSongTwoInputFilter filter = filterClass.newInstance();
            String var3 = "assets://LSResource/blend_demo.png"; //这里只是为了方便,用默认图片;
            filter.setBitmap(BitmapLoader.load(context, var3, 0, 0));
            return filter;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public enum FilterType {
        NONE, BEAUTIFUL, BEAUTIFUL2, LanSongMASK, LanSongBLUR,GaussionBLUR, CONTRAST, GRAYSCALE, SEPIA,
        POSTERIZE, GAMMA, BRIGHTNESS, INVERT, HUE, PIXELATION, SATURATION, EXPOSURE, HIGHLIGHT_SHADOW, MONOCHROME,
        OPACITY, RGB, WHITE_BALANCE, VIGNETTE,
        BLEND_COLOR_BURN, BLEND_COLOR_DODGE, BLEND_DARKEN, BLEND_DIFFERENCE,
        BLEND_DISSOLVE, BLEND_EXCLUSION, BLEND_SOURCE_OVER, BLEND_HARD_LIGHT, BLEND_LIGHTEN, BLEND_ADD, BLEND_DIVIDE,
        BLEND_MULTIPLY, BLEND_OVERLAY, BLEND_SCREEN, BLEND_ALPHA, BLEND_COLOR, BLEND_HUE, BLEND_SATURATION,
        BLEND_LUMINOSITY, BLEND_LINEAR_BURN, BLEND_SOFT_LIGHT, BLEND_SUBTRACT, BLEND_CHROMA_KEY, BLEND_NORMAL,
        LOOKUP_AMATORKA, CROSSHATCH, CGA_COLORSPACE, KUWAHARA, BULGE_DISTORTION, PINCH_DISTORTION,
        STRETCH_DISTORTION, GLASS_SPHERE, HAZE, SPHERE_REFRACTION, SWIRL, FALSE_COLOR, COLOR_BALANCE,
        LEVELS_FILTER_MIN, HALFTONE,

        /* 新增 */
        EMBOSS, THREE_X_THREE_CONVOLUTION, LAPLACIAN, TOON,

        AMARO, RISE, HUDSON, XPROII, SIERRA, LOMOFI, EARLYBIRD, SUTRO, TOASTER, BRANNAN, INKWELL, WALDEN, HEFE,
        VALENCIA, NASHVILLE, IF1977, LORDKELVIN
    }

    public interface OnLanSongFilterChosenListener {
        void onLanSongFilterChosenListener(LanSongFilter filter, String name);
    }

    public static class FilterAdjuster {
        private final Adjuster<? extends LanSongFilter> adjuster;

        public FilterAdjuster(final LanSongFilter filter) {
            if (filter instanceof LanSongBeautyAdvanceFilter) {
                adjuster = new BeautyAdvanceAdjuster().filter(filter);
            } else if (filter instanceof LanSongSepiaFilter) {
                adjuster = new SepiaAdjuster().filter(filter);
            } else if (filter instanceof LanSongContrastFilter) {
                adjuster = new ContrastAdjuster().filter(filter);
            } else if (filter instanceof LanSongGammaFilter) {
                adjuster = new GammaAdjuster().filter(filter);
            } else if (filter instanceof LanSongBrightnessFilter) {
                adjuster = new BrightnessAdjuster().filter(filter);
            } else if (filter instanceof LanSongHueFilter) {
                adjuster = new HueAdjuster().filter(filter);
            } else if (filter instanceof LanSongPosterizeFilter) {
                adjuster = new PosterizeAdjuster().filter(filter);
            } else if (filter instanceof LanSongPixelationFilter) {
                adjuster = new PixelationAdjuster().filter(filter);
            } else if (filter instanceof LanSongSaturationFilter) {
                adjuster = new SaturationAdjuster().filter(filter);
            } else if (filter instanceof LanSongExposureFilter) {
                adjuster = new ExposureAdjuster().filter(filter);
            } else if (filter instanceof LanSongHighlightShadowFilter) {
                adjuster = new HighlightShadowAdjuster().filter(filter);
            } else if (filter instanceof LanSongMonochromeFilter) {
                adjuster = new MonochromeAdjuster().filter(filter);
            } else if (filter instanceof LanSongOpacityFilter) {
                adjuster = new OpacityAdjuster().filter(filter);
            } else if (filter instanceof LanSongRGBFilter) {
                adjuster = new RGBAdjuster().filter(filter);

            } else if (filter instanceof LanSongWhiteBalanceFilter) {
                adjuster = new WhiteBalanceAdjuster().filter(filter);

            } else if (filter instanceof LanSongGaussianBlurFilter) {
                adjuster = new LanSongBlurFilterAdjuster().filter(filter);

            } else if (filter instanceof LanSongVignetteFilter) {
                adjuster = new VignetteAdjuster().filter(filter);
            } else if (filter instanceof LanSongDissolveBlendFilter) {
                adjuster = new DissolveBlendAdjuster().filter(filter);
            } else if (filter instanceof LanSongCrosshatchFilter) {
                adjuster = new CrosshatchBlurAdjuster().filter(filter);
            } else if (filter instanceof LanSongBulgeDistortionFilter) {
                adjuster = new BulgeDistortionAdjuster().filter(filter);
            } else if (filter instanceof LanSongGlassSphereFilter) {
                adjuster = new GlassSphereAdjuster().filter(filter);
            } else if (filter instanceof LanSongHazeFilter) {
                adjuster = new HazeAdjuster().filter(filter);
            } else if (filter instanceof LanSongSphereRefractionFilter) {
                adjuster = new SphereRefractionAdjuster().filter(filter);
            } else if (filter instanceof LanSongSwirlFilter) {
                adjuster = new SwirlAdjuster().filter(filter);
            } else if (filter instanceof LanSongColorBalanceFilter) {
                adjuster = new ColorBalanceAdjuster().filter(filter);
            } else if (filter instanceof LanSongLevelsFilter) {
                adjuster = new LevelsMinMidAdjuster().filter(filter);
            }
            // 2017年8月5日17:56:16 新增.
            else if (filter instanceof LanSongEmbossFilter) {
                adjuster = new EmbossAdjuster().filter(filter);
            } else if (filter instanceof LanSong3x3TextureSamplingFilter) {
                adjuster = new GPU3x3TextureAdjuster().filter(filter);
            } else {

                adjuster = null;
            }
        }

        public boolean canAdjust() {
            return adjuster != null;
        }

        public void adjust(final int percentage) {
            if (adjuster != null) {
                adjuster.adjust(percentage);
            }
        }

        private abstract class Adjuster<T extends LanSongFilter> {
            private T filter;

            @SuppressWarnings("unchecked")
            public Adjuster<T> filter(final LanSongFilter filter) {
                this.filter = (T) filter;
                return this;
            }

            public T getFilter() {
                return filter;
            }

            public abstract void adjust(int percentage);

            protected float range(final int percentage, final float start,
                                  final float end) {
                return (end - start) * percentage / 100.0f + start;
            }

            protected int range(final int percentage, final int start,
                                final int end) {
                return (end - start) * percentage / 100 + start;
            }
        }

        private class BeautyAdvanceAdjuster extends  Adjuster<LanSongBeautyAdvanceFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setBeautyLevel(range(percentage, 0.0f, 1.0f));
            }
        }

        private class PixelationAdjuster extends
                Adjuster<LanSongPixelationFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setPixel(range(percentage, 1.0f, 100.0f));
            }
        }

        private class HueAdjuster extends Adjuster<LanSongHueFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setHue(range(percentage, 0.0f, 360.0f));
            }
        }

        private class ContrastAdjuster extends Adjuster<LanSongContrastFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setContrast(range(percentage, 0.0f, 2.0f));
            }
        }

        private class GammaAdjuster extends Adjuster<LanSongGammaFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setGamma(range(percentage, 0.0f, 3.0f));
            }
        }

        private class BrightnessAdjuster extends
                Adjuster<LanSongBrightnessFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setBrightness(range(percentage, -1.0f, 1.0f));
            }
        }

        private class SepiaAdjuster extends Adjuster<LanSongSepiaFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setIntensity(range(percentage, 0.0f, 2.0f));
            }
        }

        private class PosterizeAdjuster extends
                Adjuster<LanSongPosterizeFilter> {
            @Override
            public void adjust(final int percentage) {
                // In theorie to 256, but only first 50 are interesting
                getFilter().setColorLevels(range(percentage, 1, 50));
            }
        }

        private class SaturationAdjuster extends
                Adjuster<LanSongSaturationFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setSaturation(range(percentage, 0.0f, 2.0f));
            }
        }

        private class ExposureAdjuster extends Adjuster<LanSongExposureFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setExposure(range(percentage, -10.0f, 10.0f));
            }
        }

        private class HighlightShadowAdjuster extends
                Adjuster<LanSongHighlightShadowFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setShadows(range(percentage, 0.0f, 1.0f));
                getFilter().setHighlights(range(percentage, 0.0f, 1.0f));
            }
        }

        private class MonochromeAdjuster extends Adjuster<LanSongMonochromeFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setIntensity(range(percentage, 0.0f, 1.0f));
                // getFilter().setColor(new float[]{0.6f, 0.45f, 0.3f, 1.0f});
            }
        }

        private class OpacityAdjuster extends Adjuster<LanSongOpacityFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setOpacity(range(percentage, 0.0f, 1.0f));
            }
        }

        private class RGBAdjuster extends Adjuster<LanSongRGBFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setRed(range(percentage, 0.0f, 1.0f));
                // getFilter().setGreen(range(percentage, 0.0f, 1.0f));
                // getFilter().setBlue(range(percentage, 0.0f, 1.0f));
            }
        }

        private class WhiteBalanceAdjuster extends  Adjuster<LanSongWhiteBalanceFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setTemperature(range(percentage, 2000.0f, 8000.0f));
                // getFilter().setTint(range(percentage, -100.0f, 100.0f));
            }
        }

        private class LanSongBlurFilterAdjuster extends  Adjuster<LanSongGaussianBlurFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setBlurFactor(range(percentage, 0.0f, 8.0f));
            }
        }

        private class VignetteAdjuster extends Adjuster<LanSongVignetteFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setVignetteStart(range(percentage, 0.0f, 1.0f));
            }
        }

        private class DissolveBlendAdjuster extends
                Adjuster<LanSongDissolveBlendFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setMix(range(percentage, 0.0f, 1.0f));
            }
        }

        private class CrosshatchBlurAdjuster extends
                Adjuster<LanSongCrosshatchFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter()
                        .setCrossHatchSpacing(range(percentage, 0.0f, 0.06f));
                getFilter().setLineWidth(range(percentage, 0.0f, 0.006f));
            }
        }

        private class BulgeDistortionAdjuster extends
                Adjuster<LanSongBulgeDistortionFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setRadius(range(percentage, 0.0f, 1.0f));
                getFilter().setDistortionFactor(range(percentage, -1.0f, 1.0f));
            }
        }

        private class GlassSphereAdjuster extends  Adjuster<LanSongGlassSphereFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setRadius(range(percentage, 0.0f, 1.0f));
            }
        }

        private class HazeAdjuster extends Adjuster<LanSongHazeFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setDistance(range(percentage, -0.3f, 0.3f));
                getFilter().setSlope(range(percentage, -0.3f, 0.3f));
            }
        }

        private class SphereRefractionAdjuster extends
                Adjuster<LanSongSphereRefractionFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setRadius(range(percentage, 0.0f, 1.0f));
            }
        }

        private class SwirlAdjuster extends Adjuster<LanSongSwirlFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setAngle(range(percentage, 0.0f, 2.0f));
            }
        }

        private class ColorBalanceAdjuster extends
                Adjuster<LanSongColorBalanceFilter> {

            @Override
            public void adjust(int percentage) {
                getFilter().setMidtones(
                        new float[]{range(percentage, 0.0f, 1.0f),
                                range(percentage / 2, 0.0f, 1.0f),
                                range(percentage / 3, 0.0f, 1.0f)});
            }
        }

        private class LevelsMinMidAdjuster extends
                Adjuster<LanSongLevelsFilter> {
            @Override
            public void adjust(int percentage) {
                getFilter().setMin(0.0f, range(percentage, 0.0f, 1.0f), 1.0f);
            }
        }

        // ---------------2017年8月5日18:06:01新增的滤镜
        private class EmbossAdjuster extends Adjuster<LanSongEmbossFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setIntensity(range(percentage, 0.0f, 4.0f));
            }
        }

        private class GPU3x3TextureAdjuster extends
                Adjuster<LanSong3x3TextureSamplingFilter> {
            @Override
            public void adjust(final int percentage) {
                getFilter().setLineSize(range(percentage, 0.0f, 5.0f));
            }
        }
    }
}