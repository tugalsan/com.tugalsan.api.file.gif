package com.tugalsan.api.file.gif.server.core;

import com.tugalsan.api.file.gif.server.TS_FileGifWriterBall;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.union.client.TGS_UnionExcuse;
import com.tugalsan.api.union.client.TGS_UnionExcuseVoid;
import com.tugalsan.api.function.client.maythrow.checkedexceptions.TGS_FuncMTCEUtils;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.nio.file.Path;
import java.util.stream.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.FileImageOutputStream;

public class TS_FileGifWriterCoreUtils {

    final private static TS_Log d = TS_Log.of(TS_FileGifWriterCoreUtils.class);

    public static TGS_UnionExcuse<TS_FileGifWriterBall> openARGB(Path file, long timeBetweenFramesMS, boolean loopContinuously) {
        return open(file, BufferedImage.TYPE_INT_ARGB, timeBetweenFramesMS, loopContinuously);
    }

    private static TGS_UnionExcuse<TS_FileGifWriterBall> open(Path file, int imageType, long timeBetweenFramesMS, boolean loopContinuously) {
        var gifWriter = createWriter().orElse(null);
        if (gifWriter == null) {
            return TGS_UnionExcuse.ofExcuse(d.className, "open", "gifWriter == null");
        }
        var meta = openWriter(file, gifWriter, imageType, timeBetweenFramesMS, loopContinuously).orElse(null);
        if (meta == null) {
            return TGS_UnionExcuse.ofExcuse(d.className, "open", "meta == null");
        }
        return TGS_UnionExcuse.of(new TS_FileGifWriterBall(gifWriter, meta, timeBetweenFramesMS));
    }

    private static TGS_UnionExcuse<ImageWriter> createWriter() {
        return TGS_FuncMTCEUtils.call(() -> {
            var iter = ImageIO.getImageWritersBySuffix("gif");
            if (!iter.hasNext()) {
                return TGS_UnionExcuse.ofExcuse(d.className, "createWriter", "No GIF Image Writers Exist");
            } else {
                var iw = iter.next();

                return TGS_UnionExcuse.of(iw);
            }
        }, e -> {
            return TGS_UnionExcuse.ofExcuse(e);
        });
    }

    private static TGS_UnionExcuse<IIOMetadata> openWriter(Path file, ImageWriter gifWriter, int imageType, long timeBetweenFramesMS, boolean loopContinuously) {
        return TGS_FuncMTCEUtils.call(() -> {
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
            return TGS_UnionExcuse.of(imageMetaData);
        }, e -> {
            return TGS_UnionExcuse.ofExcuse(e);
        });
    }

    public static TGS_UnionExcuseVoid append(TS_FileGifWriterBall writerBall, RenderedImage img) {
        return TGS_FuncMTCEUtils.call(() -> {
            writerBall.gifWriter().writeToSequence(new IIOImage(img, null, writerBall.meta()), writerBall.gifWriter().getDefaultWriteParam());
            return TGS_UnionExcuseVoid.ofVoid();
        }, e -> {
            return TGS_UnionExcuseVoid.ofExcuse(e);
        });
    }

    public static TGS_UnionExcuseVoid close(TS_FileGifWriterBall writerBall) {
        return TGS_FuncMTCEUtils.call(() -> {
            writerBall.gifWriter().endWriteSequence();
            return TGS_UnionExcuseVoid.ofVoid();
        }, e -> {
            return TGS_UnionExcuseVoid.ofExcuse(e);
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
