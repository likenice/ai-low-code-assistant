package com.seeyon.ai.ocrprocess.service;

import com.seeyon.ai.ocrprocess.form.FileDto;
import com.seeyon.ai.ocrprocess.form.UploadRequestDto;
import com.seeyon.ai.ocrprocess.util.CommonProperties;
import com.seeyon.boot.util.id.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    CommonProperties commonProperties;

    public FileDto upload(InputStream inputStream, UploadRequestDto uploadRequestDto){
        String dirPath = commonProperties.getFilePath()+"/"+ uploadRequestDto.getApiKey();
        FileDto fileDto = new FileDto();
        fileDto.setId(Ids.gidLong());
        fileDto.setStorageKey(dirPath+"/"+uploadRequestDto.getFileName());
        fileDto.setFileName(uploadRequestDto.getFileName());
        try {
            saveInputStreamToFile(inputStream, dirPath, uploadRequestDto.getFileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileDto;

    }

    public InputStream download(String path){
        try {
            // 读取文件内容到字节数组
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            // 将字节数组转换为InputStream
            return new ByteArrayInputStream(bytes);
        } catch (IOException e) {
            // 更详细的错误处理（实际项目应记录日志）
            throw new RuntimeException("文件下载失败: " + path, e);
        }

    }

    public FileDto selectOrigFileByStorageKey(String path){
        int lastSlashIndex = path.lastIndexOf('/');
        String fileName = path.substring(lastSlashIndex + 1);
        FileDto fileDto = new FileDto();
        fileDto.setStorageKey(path);
        fileDto.setFileName(fileName);
        return fileDto;
    }

    public  void saveInputStreamToFile(InputStream inputStream, String absoluteDir, String fileName)
            throws IOException {

        // 1. 创建目录（如果不存在）
        Path dirPath = Paths.get(absoluteDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath); // 创建所有不存在的父目录
        }
        // 2. 构建完整文件路径
        Path filePath = dirPath.resolve(fileName);
        // 3. 使用缓冲流写入文件（自动关闭资源）
        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(filePath))) {
            byte[] buffer = new byte[8192]; // 8KB缓冲区
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } // 自动关闭流
    }
}
