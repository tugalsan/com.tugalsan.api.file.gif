package com.tugalsan.api.file.gif.server.core;

import com.tugalsan.api.file.gif.server.TS_FileGifWriterBall;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.FileImageOutputStream;

public class TS_FileGifWriterCoreUtils {

    public static Optional<TS_FileGifWriterBall> openARGB(Path file, long timeBetweenFramesMS, boolean loopContinuously) {
        return open(file, BufferedImage.TYPE_INT_ARGB, timeBetweenFramesMS, loopContinuously);
    }

    private static Optional<TS_FileGifWriterBall> open(Path file, int imageType, long timeBetweenFramesMS, boolean loopContinuously) {
        var gifWriter = createWriter().orElse(null);
        if (gifWriter == null) {
            return Optional.empty();
        }
        var meta = openWriter(file, gifWriter, imageType, timeBetweenFramesMS, loopContinuously).orElse(null);
        return meta == null ? Optional.empty() : Optional.of(new TS_FileGifWriterBall(gifWriter, meta, timeBetweenFramesMS));
    }

    private static Optional<ImageWriter> createWriter() {
        return TGS_UnSafe.call(() -> {
            var iter = ImageIO.getImageWritersBySuffix("gif");
            if (!iter.hasNext()) {
                throw new IIOException("No GIF Image Writers Exist");
            } else {
                var iw = iter.next();

                return Optional.of(iw);
            }
        }, e -> {
            e.printStackTrace();
            return Optional.empty();
        });
    }

    private static Optional<IIOMetadata> openWriter(Path file, ImageWriter gifWriter, int imageType, long timeBetweenFramesMS, boolean loopContinuously) {
        return TGS_UnSafe.call(() -> {
            var imageWriteParam = gifWriter.getDefaultWriteParam();
            var imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
            var imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);
            var metaFormatName = imageMetaData.getNativeMetadataFormatName();
            var root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);
            var graphicsControlExtensionNode = TS_FileGifWriterCoreUtils.find(root, "GraphicControlExtension");
            graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
            graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
            graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
            graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString((int) timeBetweenFramesMS / 10));
            graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");
            var commentsNode = TS_FileGifWriterCoreUtils.find(root, "CommentExtensions");
            commentsNode.setAttribute("CommentExtension", "Created by " + TS_FileGifWriterCoreUtils.class.getName());
            var appEntensionsNode = TS_FileGifWriterCoreUtils.find(root, "ApplicationExtensions");
            var child = new IIOMetadataNode("ApplicationExtension");
            child.setAttribute("applicationID", "NETSCAPE");
            child.setAttribute("authenticationCode", "2.0");
            var loop = loopContinuously ? 0 : 1;
            child.setUserObject(new byte[]{0x1, (byte) (loop & 0xFF), (byte) ((loop >> 8) & 0xFF)});
            appEntensionsNode.appendChild(child);
            imageMetaData.setFromTree(metaFormatName, root);
            gifWriter.setOutput(new FileImageOutputStream(file.toFile()));
            gifWriter.prepareWriteSequence(null);
            imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            imageWriteParam.setCompressionType("LZW");
            return Optional.of(imageMetaData);
        }, e -> {
            e.printStackTrace();
            return Optional.empty();
        });
    }

    public static boolean append(TS_FileGifWriterBall writerBall, RenderedImage img) {
        return TGS_UnSafe.call(() -> {
            writerBall.gifWriter().writeToSequence(new IIOImage(img, null, writerBall.meta()), writerBall.gifWriter().getDefaultWriteParam());
            return true;
        }, e -> {
            e.printStackTrace();
            return false;
        });
    }

    public static boolean close(TS_FileGifWriterBall writerBall) {
        return TGS_UnSafe.call(() -> {
            writerBall.gifWriter().endWriteSequence();
            return true;
        }, e -> {
            e.printStackTrace();
            return false;
        });
    }

    private static IIOMetadataNode find(IIOMetadataNode rootNode, String nodeName) {
        var nodeSelected = IntStream.range(0, rootNode.getLength())
                .mapToObj(i -> rootNode.item(i))
                .filter(node -> node.getNodeName().compareToIgnoreCase(nodeName) == 0)
                .map(node -> (IIOMetadataNode) node)
                .findAny().orElse(null);
        if (nodeSelected == null) {
            nodeSelected = new IIOMetadataNode(nodeName);
            rootNode.appendChild(nodeSelected);
        }
        return nodeSelected;
    }
}
