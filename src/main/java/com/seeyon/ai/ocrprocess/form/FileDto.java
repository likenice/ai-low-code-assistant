package com.seeyon.ai.ocrprocess.form;

import com.seeyon.boot.annotation.DtoAttribute;
import lombok.Data;

@Data
public class FileDto {
    @DtoAttribute("主键")
    private Long id;
    @DtoAttribute("存储中的key")
    private String storageKey;
    @DtoAttribute("文件名")
    private String fileName;
    @DtoAttribute("Mime Type")
    private String mimeType;
    @DtoAttribute("文件大小")
    private Long fileSize;
}
