package com.tugalsan.api.file.gif.server;

import javax.imageio.*;
import javax.imageio.metadata.*;

public record TS_FileGifWriterBall(ImageWriter gifWriter, IIOMetadata meta, long timeBetweenFramesMS) {

}
