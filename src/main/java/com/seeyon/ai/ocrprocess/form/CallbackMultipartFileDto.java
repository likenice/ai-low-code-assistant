package com.seeyon.ai.ocrprocess.form;

import com.seeyon.boot.annotation.DtoAttribute;
import lombok.Data;
import org.springframework.core.io.Resource;

import java.io.InputStream;

@Data
public class CallbackMultipartFileDto {
    @DtoAttribute("name")
    private String name;
    @DtoAttribute("originalFilename")
    private String originalFilename;
    @DtoAttribute("contentType")
    private String contentType;
    @DtoAttribute("size")
    private Long size;
    @DtoAttribute("文件流")
    private InputStream inputStream;
    @DtoAttribute("resource")
    private Resource resource;

    private byte[] bytes;

}
