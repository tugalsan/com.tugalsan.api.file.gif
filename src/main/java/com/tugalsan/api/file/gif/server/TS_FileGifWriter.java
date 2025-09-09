package com.tugalsan.api.file.gif.server;

import com.tugalsan.api.file.gif.server.core.TS_FileGifWriterCoreUtils;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.union.client.TGS_UnionExcuseVoid;
import java.awt.image.RenderedImage;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Supplier;

public class TS_FileGifWriter implements AutoCloseable {

    final private static Supplier<TS_Log> d = StableValue.supplier(() -> TS_Log.of(TS_FileGifWriter.class));

    private TS_FileGifWriter(Path file, long timeBetweenFramesMS, boolean loopContinuously) {
        this.file = file;
        this.timeBetweenFramesMS = timeBetweenFramesMS;
        this.dur_timeBetweenFramesMS = Duration.ofMillis(timeBetweenFramesMS);
        this.loopContinuously = loopContinuously;
        this.writerBall = TS_FileGifWriterCoreUtils.openARGB(file, timeBetweenFramesMS, loopContinuously).orElse(null);
    }
    final public Path file;
    final public long timeBetweenFramesMS;
    final public boolean loopContinuously;
    final private TS_FileGifWriterBall writerBall;

    public Duration timeBetweenFramesMS() {
        return dur_timeBetweenFramesMS;
    }
    final private Duration dur_timeBetweenFramesMS;

    public static TS_FileGifWriter open(Path file, long timeBetweenFramesMS, boolean loopContinuously) {
        return new TS_FileGifWriter(file, timeBetweenFramesMS, loopContinuously);
    }

    private boolean closed = false;

    public boolean isReadyToAccept() {
        return closed || writerBall != null;
    }

    public TGS_UnionExcuseVoid write(RenderedImage img) {
        if (img == null) {
            return TGS_UnionExcuseVoid.ofExcuse(d.get().className, "write", "img == null");
        }
        if (!isReadyToAccept()) {
            return TGS_UnionExcuseVoid.ofExcuse(d.get().className, "write", "!isReadyToAccept()");
        }
        return TS_FileGifWriterCoreUtils.append(writerBall, img);
    }

    @Override
    public void close() {
        close_withExcuse();
    }

    public TGS_UnionExcuseVoid close_withExcuse() {
        closed = true;
        return TS_FileGifWriterCoreUtils.close(writerBall);
    }
}
