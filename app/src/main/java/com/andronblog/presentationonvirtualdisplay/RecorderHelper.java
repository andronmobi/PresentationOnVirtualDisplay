package com.andronblog.presentationonvirtualdisplay;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.util.Log;
import android.util.Size;

public class RecorderHelper {

    private final static String TAG = "MediaRecorderHelper";
    private final static boolean VERBOSE = true;

    private static final int BIT_RATE_1080P = 16000000;
    private static final int BIT_RATE_MIN = 64000;
    private static final int BIT_RATE_MAX = 40000000;

    /**
     * Calculate a video bit rate based on the size. The bit rate is scaled
     * based on ratio of video size to 1080p size.
     */
    public static int getVideoBitRate(Size sz) {
        int rate = BIT_RATE_1080P;
        float scaleFactor = sz.getHeight() * sz.getWidth() / (float)(1920 * 1080);
        rate = (int)(rate * scaleFactor);

        // Clamp to the MIN, MAX range.
        return Math.max(BIT_RATE_MIN, Math.min(BIT_RATE_MAX, rate));
    }

    /**
     * Check if encoder can support this size and frame rate combination by querying
     * MediaCodec capability. Check is based on size and frame rate. Ignore the bit rate
     * as the bit rates targeted in this test are well below the bit rate max value specified
     * by AVC specification for certain level.
     */
    public static boolean isSupportedByAVCEncoder(Size sz, int frameRate) {
        String mimeType = "video/avc";
        MediaCodecInfo codecInfo = getEncoderInfo(mimeType);
        if (codecInfo == null) {
            return false;
        }
        MediaCodecInfo.CodecCapabilities cap = codecInfo.getCapabilitiesForType(mimeType);
        if (cap == null) {
            return false;
        }

        int highestLevel = 0;
        for (MediaCodecInfo.CodecProfileLevel lvl : cap.profileLevels) {
            if (lvl.level > highestLevel) {
                highestLevel = lvl.level;
            }
        }

        if(VERBOSE) {
            Log.v(TAG, "The highest level supported by encoder is: " + highestLevel);
        }

        // Put bitRate here for future use.
        int maxW, maxH, bitRate;
        // Max encoding speed.
        int maxMacroblocksPerSecond = 0;
        switch(highestLevel) {
            case MediaCodecInfo.CodecProfileLevel.AVCLevel21:
                maxW = 352;
                maxH = 576;
                bitRate = 4000000;
                maxMacroblocksPerSecond = 19800;
                break;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel22:
                maxW = 720;
                maxH = 480;
                bitRate = 4000000;
                maxMacroblocksPerSecond = 20250;
                break;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel3:
                maxW = 720;
                maxH = 480;
                bitRate = 10000000;
                maxMacroblocksPerSecond = 40500;
                break;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel31:
                maxW = 1280;
                maxH = 720;
                bitRate = 14000000;
                maxMacroblocksPerSecond = 108000;
                break;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel32:
                maxW = 1280;
                maxH = 720;
                bitRate = 20000000;
                maxMacroblocksPerSecond = 216000;
                break;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel4:
                maxW = 1920;
                maxH = 1088; // It should be 1088 in terms of AVC capability.
                bitRate = 20000000;
                maxMacroblocksPerSecond = 245760;
                break;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel41:
                maxW = 1920;
                maxH = 1088; // It should be 1088 in terms of AVC capability.
                bitRate = 50000000;
                maxMacroblocksPerSecond = 245760;
                break;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel42:
                maxW = 2048;
                maxH = 1088; // It should be 1088 in terms of AVC capability.
                bitRate = 50000000;
                maxMacroblocksPerSecond = 522240;
                break;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel5:
                maxW = 3672;
                maxH = 1536;
                bitRate = 135000000;
                maxMacroblocksPerSecond = 589824;
                break;
            case MediaCodecInfo.CodecProfileLevel.AVCLevel51:
            default:
                maxW = 4096;
                maxH = 2304;
                bitRate = 240000000;
                maxMacroblocksPerSecond = 983040;
                break;
        }

        // Check size limit.
        if (sz.getWidth() > maxW || sz.getHeight() > maxH) {
            Log.i(TAG, "Requested resolution " + sz.toString() + " exceeds (" +
                    maxW + "," + maxH + ")");
            return false;
        }

        // Check frame rate limit.
        Size sizeInMb = new Size((sz.getWidth() + 15) / 16, (sz.getHeight() + 15) / 16);
        int maxFps = maxMacroblocksPerSecond / (sizeInMb.getWidth() * sizeInMb.getHeight());
        if (frameRate > maxFps) {
            Log.i(TAG, "Requested frame rate " + frameRate + " exceeds " + maxFps);
            return false;
        }

        return true;
    }


    private static MediaCodecInfo getEncoderInfo(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
}
