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

import android.util.Log;

import com.ericsson.research.owr.AudioRenderer;
import com.ericsson.research.owr.DataChannel;
import com.ericsson.research.owr.MediaSource;
import com.ericsson.research.owr.MediaType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SimpleStreamSet extends StreamSet {
    private static final String TAG = "SimpleStreamSet";

    private final boolean mWantVideo;
    private final boolean mWantAudio;
    private final boolean mWantData;

    private boolean linkFlag = false;

    private final AudioRenderer mAudioRenderer;

    private final SimpleMediaStream mAudioStream;
    private final SimpleMediaStream mVideoStream;

    private final DataStreamMock mDataStream;

    private VideoSourceProvider mRemoteVideoSourceProvider = new VideoSourceProvider();

    private SimpleStreamSet() {
        mWantVideo = false;
        mWantAudio = false;
        mAudioRenderer = null;
        mAudioStream = null;
        mVideoStream = null;

        mWantData = true;
        mDataStream = new DataStreamMock();
    }
    public DataStreamMock getmDataStream() {
        return mDataStream;
    }
    public DataChannel getDataStreamChannel() {
        return mDataStream.mReceivedDataChannels.get(0);
    }
    public StreamMode getmDataStreamMode() {
        return mDataStream.getStreamMode();
    }
    public void addDataChannel(DataChannel channel) {
        mDataStream.addDataChannel(channel);
    }


    public int getStreamDataLink() {
        if ( mAudioStream.getMediaSourceData() && mVideoStream.getMediaSourceData() ) {
            return 1;
        } else if ( !mAudioStream.getMediaSourceData() && !mVideoStream.getMediaSourceData() ) {
            return -1;
        } else {
            return 0;
        }
    }

    private SimpleStreamSet(MediaSourceProvider audioSourceProvider, MediaSourceProvider videoSourceProvider,
                            boolean sendAudio, boolean sendVideo) {
        mWantData = false;
        mDataStream = null;

        mWantAudio = sendAudio;
        mWantVideo = sendVideo;

        mAudioRenderer = new AudioRenderer();

        mAudioStream = new SimpleMediaStream(false);
        mVideoStream = new SimpleMediaStream(true);

        videoSourceProvider.addMediaSourceListener(new MediaSourceListener() {
            @Override
            public void setMediaSource(final MediaSource mediaSource) {
                mVideoStream.setMediaSource(mediaSource);
            }
        });

        audioSourceProvider.addMediaSourceListener(new MediaSourceListener() {
            @Override
            public void setMediaSource(final MediaSource mediaSource) {
                mAudioStream.setMediaSource(mediaSource);
            }
        });
    }

    private class VideoSourceProvider implements MediaSourceProvider {
        private MediaSourceListenerSet set = new MediaSourceListenerSet();

        public void notifyListeners(MediaSource mediaSource) {
            set.notifyListeners(mediaSource);
        }

        @Override
        public void addMediaSourceListener(final MediaSourceListener listener) {
            set.addListener(listener);
        }
    }

    public VideoView createRemoteView() {
        return new VideoViewImpl(mRemoteVideoSourceProvider, 0, 0, 0);
    }

    /**
     * Creates a configuration for setting up a basic audio/video call.
     *
     * @param sendAudio true if audio should be sent, audio may still be received
     * @param sendVideo true if video should be sent, video may still be received
     * @return a new RtcConfig with a simple audio/video call configuration
     */
    public static SimpleStreamSet defaultConfig(boolean sendAudio, boolean sendVideo) {
        return new SimpleStreamSet(MicrophoneSource.getInstance(), CameraSource.getInstance(), sendAudio, sendVideo);
    }
    public static SimpleStreamSet defaultConfig() {
        return new SimpleStreamSet();
    }

    @Override
    protected List<? extends Stream> getStreams() {
        if ( mDataStream == null ) {
            return Arrays.asList(mAudioStream, mVideoStream);
        } else {
            return Arrays.asList(mDataStream);
        }
    }

    /**
     * @return the current audio renderer pipeline graph in dot format.
     */
    public String dumpPipelineGraph() {
        return mAudioRenderer.getDotData();
    }

    private class SimpleMediaStream extends MediaStream {
        private final String mId;
        private final boolean mIsVideo;
        private MediaSource mMediaSource;
        private MediaSourceDelegate mMediaSourceDelegate;

        private SimpleMediaStream(boolean isVideo) {
            mIsVideo = isVideo;
            mId = Utils.randomString(27);
        }

        @Override
        protected String getId() {
            return mId;
        }

        @Override
        protected MediaType getMediaType() {
            return mIsVideo ? MediaType.VIDEO : MediaType.AUDIO;
        }

        @Override
        protected boolean wantSend() {
            return mIsVideo ? mWantVideo : mWantAudio;
        }

        @Override
        protected boolean wantReceive() {
            return true;
        }
        @Override
        protected boolean getMediaSourceData() {
            return linkFlag;
        }

        @Override
        protected void onRemoteMediaSource(final MediaSource mediaSource) {
            linkFlag = true;
            if (mIsVideo) {
                mRemoteVideoSourceProvider.notifyListeners(mediaSource);
            } else {
                mAudioRenderer.setSource(mediaSource);
            }
        }

        @Override
        protected synchronized void setMediaSourceDelegate(final MediaSourceDelegate mediaSourceDelegate) {
            mMediaSourceDelegate = mediaSourceDelegate;
            if (mMediaSource != null && mediaSourceDelegate != null) {
                mediaSourceDelegate.setMediaSource(mMediaSource);
            }
        }

        @Override
        public void setStreamMode(final StreamMode mode) {
            Log.i(TAG, (mIsVideo ? "video" : "audio") + " stream mode set: " + mode.name());
        }

        public synchronized void setMediaSource(final MediaSource mediaSource) {
            mMediaSource = mediaSource;
            if (mMediaSourceDelegate != null) {
                mMediaSourceDelegate.setMediaSource(mediaSource);
            }
        }
    }










    private class DataStreamMock extends DataStream {
        private StreamMode mStreamMode = null;
        private List<DataChannel> mReceivedDataChannels = new LinkedList<>();
        private CountDownLatch mDataChannelLatch = null;
        private CountDownLatch mModeSetLatch = null;
        private DataChannelDelegate mDataChannelDelegate;

        public synchronized void waitForDataChannels(CountDownLatch latch) {
            mDataChannelLatch = latch;
            for (DataChannel ignored : mReceivedDataChannels) {
                latch.countDown();
            }
        }

        public StreamMode getStreamMode() {
            return mStreamMode;
        }

        public List<DataChannel> getReceivedDataChannels() {
            return mReceivedDataChannels;
        }

        public void addDataChannel(DataChannel dataChannel) {
            mDataChannelDelegate.addDataChannel(dataChannel);
        }

        @Override
        protected synchronized boolean onDataChannelReceived(final DataChannel dataChannel) {
            Log.v("!!!", "[] data channel received: " + dataChannel);
            mReceivedDataChannels.add(dataChannel);
            if (mDataChannelLatch != null) {
                mDataChannelLatch.countDown();
            }
            return true;
        }

        @Override
        protected void setDataChannelDelegate(final DataChannelDelegate dataChannelDelegate) {
            mDataChannelDelegate = dataChannelDelegate;
            Log.v("!!!", "[] data channel delegate set: " + dataChannelDelegate);
        }

        @Override
        public void setStreamMode(final StreamMode mode) {
            mStreamMode = mode;
            if (mModeSetLatch != null) {
                mModeSetLatch.countDown();
                mModeSetLatch = null;
            }
        }

        public void waitUntilActive(final CountDownLatch latch) {
            mModeSetLatch = latch;
            if (mStreamMode != null) {
                mModeSetLatch.countDown();
                mModeSetLatch = null;
            }
        }
    }
}
