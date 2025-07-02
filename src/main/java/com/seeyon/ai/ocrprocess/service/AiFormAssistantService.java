package com.seeyon.ai.ocrprocess.service;

import cn.hutool.core.io.unit.DataSizeUtil;
import com.seeyon.ai.common.exception.PlatformException;
import com.seeyon.ai.ocrprocess.form.FileDto;
import com.seeyon.ai.ocrprocess.form.UploadRequestDto;
import com.seeyon.ai.ocrprocess.util.CommonProperties;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pb
 */
@Service
@Slf4j
public class AiFormAssistantService {

    @Autowired
    private CommonProperties commonProperties;
    @Autowired
    private FileService fileService;


    public List<String> upload(MultipartFile file, String apiKey) {
        if (file.getSize() > DataSizeUtil.parse(commonProperties.getMaxFileSize())) {
            double mb = (double) file.getSize() / (1024 * 1024);
            double size = Math.round(mb * 100.0) / 100.0;
            throw new PlatformException("图片过大，请上传小于 " + commonProperties.getMaxFileSize() + " 的截图（当前 " + size + " MB）。");
        }
        if (!(file.getContentType().equals("image/jpeg") || file.getContentType().equals("image/png")
                || file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || file.getContentType().equals("application/vnd.ms-excel"))) {
            throw new PlatformException("文件类型错误");
        }
        List<String> list = new ArrayList<>();
        if (file.isEmpty()) {
            log.error("文件为空");
            return null;
        }
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            boolean resize = false;
            if (file.getContentType().equals("image/jpeg") || file.getContentType().equals("image/png")) {
                BufferedImage image = ImageIO.read(file.getInputStream());
                int width = image.getWidth();
                int height = image.getHeight();
                if (width < commonProperties.getMinEdgePixels() && height < commonProperties.getMinEdgePixels()) {
                    throw new PlatformException("截图区域过小（长边需 大于等于" + commonProperties.getMinEdgePixels() + "px），请重新截取完整表单。");
                }
                double l = file.getSize() * 8 / (double) (width * height);
                log.info("图片width:,height:,bit:,fileSize:{},{},{},{}", width, height, l, file.getSize());
                if (width * height <= commonProperties.getMinAreaPixels() || l < 0.1) {
                    throw new PlatformException("截图压缩过度，文字可能无法识别，请使用高质量截屏后再上传。");
                }
                if (file.getSize() >= DataSizeUtil.parse(commonProperties.getAutoCompressThreshold())) {
                    BufferedImage resized = resizeWithLongEdge(image, 1024);
                    if (file.getContentType().equals("image/jpeg")) {
                        compressJpeg(resized, result, 0.8f);
                    } else {
                        compressPng(resized, result);
                    }
                    log.info("压缩后大小:{} KB", result.size() / 1024);
                    resize = true;
                }
            }
            UploadRequestDto uploadRequestDto = new UploadRequestDto();
            uploadRequestDto.setFileName(file.getOriginalFilename());
            uploadRequestDto.setAppName("ai-form");
            uploadRequestDto.setApiKey(apiKey);
            if (resize) {
                FileDto fileDto = fileService.upload(new ByteArrayInputStream(result.toByteArray()), uploadRequestDto);
                log.info("图片上传完成：{}", fileDto.getStorageKey());
                list.add(fileDto.getStorageKey());
            } else {
                FileDto fileDto = fileService.upload(file.getInputStream(), uploadRequestDto);
                log.info("图片上传完成：{}", fileDto.getStorageKey());
                list.add(fileDto.getStorageKey());
            }

            return list;
        } catch (IOException e) {
            log.error("文件上传失败:{}", e.getMessage());
            throw new PlatformException("upload file error");
        }
    }

    // 尺寸压缩（长边限制）
    private static BufferedImage resizeWithLongEdge(BufferedImage source, int maxEdge) {
        int w = source.getWidth();
        int h = source.getHeight();
        double ratio = (w < h) ? (double) maxEdge / w : (double) maxEdge / h;
        try {
            return ratio < 1 ? Thumbnails.of(source).scale(ratio).asBufferedImage() : source;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // JPEG压缩（固定质量80%）
    private static void compressJpeg(BufferedImage image, ByteArrayOutputStream out, float quality)
            throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("JPEG").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality); // 固定质量80%

        writer.setOutput(new MemoryCacheImageOutputStream(out));
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
    }

    // PNG压缩（最高压缩级别）
    private static void compressPng(BufferedImage image, ByteArrayOutputStream out)
            throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("PNG").next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType("Deflate");
            param.setCompressionQuality(0.8f); // 对应压缩级别
        }

        writer.setOutput(new MemoryCacheImageOutputStream(out));
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
    }


}
