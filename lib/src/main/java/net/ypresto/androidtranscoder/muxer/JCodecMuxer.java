package net.ypresto.androidtranscoder.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;

import net.ypresto.androidtranscoder.utils.MediaFormatUtils;

import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JCodecMuxer implements Muxer {
    private final FileChannelWrapper mFileChannelWrapper;
    private final MP4Muxer mMP4Muxer;
    private final List<TrackContainer> mTrackContainers = new ArrayList<TrackContainer>();

    public JCodecMuxer(String path, int bufferSize) throws IOException {
        File out = new File(path);
        mFileChannelWrapper = NIOUtils.writableFileChannel(out);
        mMP4Muxer = new MP4Muxer(mFileChannelWrapper);
    }

    @Override
    public void setOrientationHint(int degrees) {
    }

    @Override
    public void setLocation(float latitude, float longitude) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public int addTrack(MediaFormat format) {
        int index = mTrackContainers.size();
        String mime = MediaFormatUtils.getMime(format);
        if (mime.startsWith("video/")) {
            mTrackContainers.add(addVideoTrack(format));
        } else if (mime.startsWith("audio/")) {
            mTrackContainers.add(addAudioTrack(format));
        } else {
            throw new UnsupportedOperationException("Unsupported track mime type: " + mime);
        }
        return index;
    }

    @Override
    public void writeSampleData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        byteBuf.position(bufferInfo.offset);
        byteBuf.limit(bufferInfo.offset + bufferInfo.size);
        TrackContainer trackContainer = mTrackContainers.get(trackIndex);
        trackContainer.mTrack.addFrame(new MP4Packet(byteBuf, bufferInfo.presentationTimeUs, ));
    }

    @Override
    public void release() {

    }

    private TrackContainer addVideoTrack(MediaFormat format) {
        // Refer: AVCMP4Mux.java in jcodec

        SeqParameterSet sps = SeqParameterSet.read(MediaFormatUtils.getSpsBuffer(format));
        int timeScale = sps.vuiParams.time_scale;

        FramesMP4MuxerTrack track = mMP4Muxer.addTrack(TrackType.VIDEO, timeScale);
        Size size = new Size(MediaFormatUtils.getWidth(format), MediaFormatUtils.getHeight(format));
        SampleEntry se = MP4Muxer.videoSampleEntry("avc1", size, "AndroidTranscoder");
        AvcCBox avcC = new AvcCBox(sps.profile_idc, 0, sps.level_idc, 4,
                Arrays.asList(MediaFormatUtils.getSpsBuffer(format)),
                Arrays.asList(MediaFormatUtils.getPpsBuffer(format)));
        se.add(avcC);
        track.addSampleEntry(se);

        return new TrackContainer(track, timeScale);
    }

    private FramesMP4MuxerTrack addAudioTrack(MediaFormat format) {
        // TODO
        return null;
    }

    private static class TrackContainer {
        public final FramesMP4MuxerTrack mTrack;
        public final int mTimeScale;

        public TrackContainer(FramesMP4MuxerTrack track, int timeScale) {
            mTrack = track;
            mTimeScale = timeScale;
        }
    }
}
