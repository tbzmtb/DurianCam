/*
 * Copyright (c) 2015, Ericsson AB.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package com.ericsson.research.owr.sdk;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.ericsson.research.owr.MediaSource;
import com.ericsson.research.owr.VideoRenderer;
import com.ericsson.research.owr.WindowRegistry;

public class VideoViewImpl implements VideoView, MediaSourceListener {
    private static final String TAG = "VideoViewImpl";

    private TextureViewTagger mTextureViewTagger;
    private VideoRenderer mVideoRenderer;
    private MediaSource mVideoSource;
    private final String mTag;

    private static Context context;

    VideoViewImpl(MediaSourceProvider mediaSourceProvider, int width, int height, double framerate) {
        mVideoSource = null;

        mTag = Utils.randomString(32);

        mVideoRenderer = new VideoRenderer(mTag);
        mVideoRenderer.setRotation(0);
        if (width > 0) {
            mVideoRenderer.setWidth(width);
        }
        if (height > 0) {
            mVideoRenderer.setHeight(height);
        }
        if (framerate > 0) {
            mVideoRenderer.setMaxFramerate(framerate);
        }

        mVideoRenderer.setMaxFramerate(8.0);
        mVideoRenderer.setWidth(640);
        mVideoRenderer.setHeight(480);


        mediaSourceProvider.addMediaSourceListener(this);
    }

    @Override
    public void setRotation(final int rotation) {
        if (rotation < 0 || rotation > 3) {
            throw new IllegalArgumentException(rotation + " is an invalid rotation, must be between 0 and 3");
        }
        mVideoRenderer.setRotation(rotation);
    }

    @Override
    public int getRotation() {
        return mVideoRenderer.getRotation();
    }

    @Override
    public void setMirrored(final boolean mirrored) {
        mVideoRenderer.setMirror(mirrored);
    }

    @Override
    public boolean isMirrored() {
        return mVideoRenderer.getMirror();
    }

    @Override
    public synchronized void setMediaSource(final MediaSource mediaSource) {
        Log.e("!!!", "mTag = [" + mTag + "], setMediaSource = " + mediaSource);
        if( mediaSource != null ) {
            Log.e("!!!", "mTag = ["+mTag+"], setMediaSource = "+mediaSource.getType().name());
        } else {
            Log.e("!!!", "mTag = ["+mTag+"], setMediaSource is null = " + mediaSource);
        }
        mVideoSource = mediaSource;
        mVideoRenderer.setSource(mediaSource);

        if ( context != null ) {
            AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMicrophoneMute(false);
            audioManager.setMicrophoneMute(true);
        }
    }

    private boolean viewIsActive() {
        return mTextureViewTagger != null;
    }

    public synchronized void setView(TextureView textureView) {
        if (textureView == null) {
            throw new NullPointerException("texture view may not be null");
        }
        if (!viewIsActive() && mVideoSource != null) {
            mVideoRenderer.setSource(mVideoSource);
        }
        stopViewTagger();
        mTextureViewTagger = new TextureViewTagger(mTag, textureView);
    }
    public synchronized void setView(TextureView textureView, Context con) {
        context = con;
        if (textureView == null) {
            throw new NullPointerException("texture view may not be null");
        }
        if (!viewIsActive() && mVideoSource != null) {
            mVideoRenderer.setSource(mVideoSource);
        }
        stopViewTagger();
        mTextureViewTagger = new TextureViewTagger(mTag, textureView);
    }
    public TextureView getView() {
        return mTextureViewTagger.getTextureView();
    }

    public synchronized void stop() {
        stopViewTagger();
        mVideoRenderer.setSource(null);
    }

    private void stopViewTagger() {
        if (mTextureViewTagger != null) {
            mTextureViewTagger.stop();
            mTextureViewTagger = null;
        }
    }

    @Override
    public long checkVideoView() {
        if ( mTextureViewTagger != null ) {
            return mTextureViewTagger.mTextureViewTimestemp;
        } else {
            return -1;
        }
    }

    private static class TextureViewTagger implements TextureView.SurfaceTextureListener {
        private final String mTag;
        private TextureView mTextureView;

        private long mTextureViewTimestemp = 0;

        private TextureViewTagger(String tag, TextureView textureView) {
            mTag = tag;
            mTextureView = textureView;
            if (textureView.isAvailable()) {
                Surface surface = new Surface(textureView.getSurfaceTexture());
                WindowRegistry.get().register(mTag, surface);
            }
            mTextureView.setSurfaceTextureListener(this);
        }

        private synchronized void stop() {
            mTextureView.setSurfaceTextureListener(null);
            WindowRegistry.get().unregister(mTag);
        }

        @Override
        public synchronized void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            Log.e("!!!", "mTag = ["+mTag+"], -----------------------------   onSurfaceTextureAvailable is start");
            Surface surface = new Surface(surfaceTexture);
            WindowRegistry.get().register(mTag, surface);
            Log.e("!!!", "mTag = [" + mTag + "], -----------------------------   onSurfaceTextureAvailable is end");

            if ( context != null ) {
                AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setMicrophoneMute(false);
                audioManager.setMicrophoneMute(true);
            }
        }

        @Override
        public synchronized boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            WindowRegistry.get().unregister(mTag);
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            mTextureViewTimestemp = surface.getTimestamp();
        }

        public TextureView getTextureView() {
            return mTextureView;
        }
    }
}