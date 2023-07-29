package com.tugalsan.api.file.gif.server;

import com.tugalsan.api.file.gif.server.core.TS_FileGifWriterCoreUtils;
import java.awt.image.RenderedImage;
import java.nio.file.Path;

public class TS_FileGifWriter {

    private TS_FileGifWriter(Path file, long timeBetweenFramesMS, boolean loopContinuously) {
        this.file = file;
        this.timeBetweenFramesMS = timeBetweenFramesMS;
        this.loopContinuously = loopContinuously;
        this.writerBall = TS_FileGifWriterCoreUtils.openARGB(file, timeBetweenFramesMS, loopContinuously).orElse(null);
    }
    final public Path file;
    final public long timeBetweenFramesMS;
    final public boolean loopContinuously;
    final private TS_FileGifWriterBall writerBall;

    public static TS_FileGifWriter of(Path file, long timeBetweenFramesMS, boolean loopContinuously) {
        return new TS_FileGifWriter(file, timeBetweenFramesMS, loopContinuously);
    }

    private boolean closed = false;

    public boolean isReadyToAccept() {
        return closed || writerBall != null;
    }

    public boolean accept(RenderedImage img) {
        if (!isReadyToAccept()) {
            return false;
        }
        return TS_FileGifWriterCoreUtils.append(writerBall, img);
    }

    public void close() {
        closed = true;
        TS_FileGifWriterCoreUtils.close(writerBall);
    }
}
