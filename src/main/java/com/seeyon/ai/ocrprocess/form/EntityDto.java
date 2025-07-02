package com.seeyon.ai.ocrprocess.form;

import com.seeyon.ai.ocrprocess.util.FilterUtil;
import lombok.Data;

import java.util.UUID;

@Data
public class EntityDto {
    // 实体名称
    private String caption;
    // 业务版型
    private String stereotype;
    // 实体编码
    private String name;
    // 父实体id
    private String parentEntityId;
    // 实体id
    private String id;

    private String ocrRelationId = UUID.randomUUID().toString();


    public static EntityDto convert(String caption, String stereotype, String parentEntityId, String id) {
        EntityDto entityDto = new EntityDto();
        entityDto.setCaption(FilterUtil.filter(caption));
        entityDto.setStereotype(stereotype);
        entityDto.setParentEntityId(parentEntityId);
        entityDto.setId(id);
        return entityDto;
    }

}