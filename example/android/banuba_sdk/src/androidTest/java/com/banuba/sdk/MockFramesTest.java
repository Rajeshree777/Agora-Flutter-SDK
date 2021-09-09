package com.banuba.sdk;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.banuba.sdk.effect_player.EffectPlayer;
import com.banuba.sdk.effect_player.EffectStatus;
import com.banuba.sdk.effect_player.PushFrameMocker;
import com.banuba.sdk.internal.gl.EglCore;
import com.banuba.sdk.internal.gl.OffscreenSurface;
import com.banuba.sdk.manager.BanubaSdkManager;
import com.banuba.sdk.spal.FramesProvider;
import com.banuba.sdk.types.FrameData;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device mocked frames.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MockFramesTest {
    @Test
    public void mockFrames() {
        Bundle args = InstrumentationRegistry.getArguments();

        String effectName = args.getString("bnbEffectName", "BulldogHarlamov");
        String inputFile = args.getString("bnbInputFile", "/sdcard/bnb_sdk_capture.bin");
        String outputFile = args.getString("bnbOutputFile", "/sdcard/bnb_sdk_processed.bin");
        int drawWidth = Integer.parseInt(args.getString("bnbDrawWidth", "480"));
        int drawHeight = Integer.parseInt(args.getString("bnbDrawHeight", "640"));

        File f = new File(inputFile);
        assertTrue("Input file isn't exists", f.exists());
        assertTrue("No permissions to read file", f.canRead());

        // Context of the app under test.
        Context context = InstrumentationRegistry.getTargetContext();
        BanubaSdkManager.createInstance(context);
        EffectPlayer ep = requireNonNull(BanubaSdkManager.getInstance().getEffectPlayer());
        EglCore egl = new EglCore(null, EglCore.FLAG_TRY_GLES3);
        OffscreenSurface sf = new OffscreenSurface(egl, drawWidth, drawHeight);

        try {
            sf.makeCurrent();
            ep.setEffectSize(drawWidth, drawHeight);
            ep.surfaceCreated(drawWidth, drawHeight);
            ep.playbackPlay();
            ep.loadEffect("effects/" + effectName);

            // draw with empty FrameData to load effect
            FrameData fd = FrameData.makeEmpty();
            while (ep.getEffectStatus() == EffectStatus.LOADING) {
                ep.drawWithExternalFrameData(fd);
            }

            PushFrameMocker mocker = PushFrameMocker.create(
                BanubaSdkManager.getInstance().getEffectPlayer(), FramesProvider.create(inputFile));

            mocker.process(outputFile);

        } finally {
            ep.playbackStop();
            ep.surfaceDestroyed();
            sf.release();
            egl.release();
            BanubaSdkManager.destroyInstance();
        }
    }
}
