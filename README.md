### This is the simplest demo of Lansong SDK.

#### The current version is 4.2.5
 [中文说明](https://github.com/LanSoSdk/LanSoEditor_advance/blob/master/README.cn.md)
- Including: Video editing SDK and AE template SDK;
- Our complete demo demo apk, you can download it from here:
- APK link ： https://www.pgyer.com/L20O
- If you want to test our SDK, you can ask us for all the demo source code.
 
## SDK function introduction.
#### Video editing SDK:
  - The name of the class is: LSOConcatComposition: meaning: splicing and synthesis, which can splice the video and the picture together, and can also be superimposed; corresponding to ConcatLayer and overlayLayer respectively;
  - **Front and back stitching**: Each stitching will return a layer object; use the layer object to adjust various attributes; insert, delete, sort, replace;
  - **Top and bottom overlay**: While stitching pictures and videos, you can overlay pictures or videos, text, animations and other effects on the stitching. These are called top and bottom overlays; for example, picture-in-picture, you can set the starting position of the overlay , Size, angle, start time point, end time point, looping, support all methods of the layer, can adjust the order of the layer;
  - **Thumbnail**:  After each video or picture is added, a corresponding thumbnail will be obtained, and the thumbnail API will be adjusted accordingly after the cut duration or reverse order or variable speed.
  - **Gesture operation**: after adding pictures and videos, return to a layer, which is a layer-by-layer design. All layers support gestures; can be selected and moved with one finger; zoom and rotate with two fingers ;
  - **Animation**: There are entrance animation, exit animation, and animation at any point in time; the animation is exported to the json format supported by the SDK after the Adobe After Effect software is completed, so that you can freely play different animations and SDKs Only one export file is needed at the end, and the animation effect can be presented after loading. Can be previewed, deleted, and applied to all;
  - **Transition**: The exported json format designed by AE can be used for mask transition or mobile rotation zoom transparent transition, which can be deleted, previewed, and applied to all;
  - **Special effects**: also designed with AE software, and then export the json or MP4 file we specified; the effect time can be adjusted, previewed, adjusted, deleted, and can be applied to all;
  - **Mask** : Produced in Photoshop on the PC, exported as a transparent image, and added to the SDK. With different effects, the SDK automatically adjusts the size of the picture, and adjusts the transparency of different areas of the video according to the transparency of the picture;
  - **Edit function** : duration cut, frame picture crop, rotation, mirror image, picture zoom, opacity;
  - **Filters** : Provide 18 common filters; and support customization;
  - **Adjustment** : Brightness, contrast, saturation, highlight, shadow; hue, white balance;
  - **Reverse Play** : Reverse playback; reverse play and variable speed can be set at the same time;
  - **Variable speed** : Support 0.1--10 times sound increase;
  - **Sound layer** :  add music, recording, mp3 sound, sound extracted from video;
  - **Volume**:  the volume can be adjusted from 0 to 8.0 times; 0.0 is silent; 8.0 is almost soundless, 2.0 or 3.0 is recommended;
  - **Picture layer**:  can be used as static stickers, dynamic stickers, Gif stickers;
  - **Picture in Picture**: Various other videos can be superimposed on the stitching layer, which is called a composite layer;
  - **Export**:  can export different resolutions
    
#### AE template SDK
- Designed the entire animation scene with Adobe After Effect on the PC, and allowed the user to replace the corresponding picture on the mobile phone. During the design, the picture can be rotated, zoomed, transparent, Gaussian blur, 3D effect;
- SDK, which supports replacing pictures with pictures or videos during playback
- If there is a deviation in the replaced position, you can use one finger to move, two fingers to zoom or rotate during playback.
- Support for replacing sounds, adding logos, filtering screens, adding other text, etc.
- Set different resolution when supporting export.

#### contact us
email: support@lansongtech.com
web: www.lansongtech.com

