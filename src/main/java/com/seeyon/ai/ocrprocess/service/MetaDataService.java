package com.seeyon.ai.ocrprocess.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.seeyon.ai.ocrprocess.enums.UdcDataTypeEnum;
import com.seeyon.ai.ocrprocess.form.ApplicationDto;
import com.seeyon.ai.ocrprocess.form.AssociationEntityDto;
import com.seeyon.ai.ocrprocess.form.AttributeDto;
import com.seeyon.ai.ocrprocess.form.AttributeGroupDto;
import com.seeyon.ai.ocrprocess.form.EntityDto;
import com.seeyon.ai.ocrprocess.form.RelationDto;
import com.seeyon.ai.ocrprocess.form.RootEnumDtos;
import com.seeyon.ai.ocrprocess.form.SelectCtpEnumDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MetaDataService {

    public List<Map<String, String>> getEntityInfo(List<EntityDto> entityInfo) {
        List<Map<String, String>> list = new ArrayList<>();
        if (entityInfo != null && !entityInfo.isEmpty()) {
            for (EntityDto entityDto : entityInfo) {
                Map<String, String> map = new HashMap<>();
                map.put("name", entityDto.getCaption());
                map.put("id", entityDto.getId());
                list.add(map);
            }
        }
        return list;
    }

    public Map<String, AttributeDto> getEntityField(List<AttributeGroupDto> attributeGroupInfo) {
        Map<String, AttributeDto> map = new HashMap<>();
        if (attributeGroupInfo != null && !attributeGroupInfo.isEmpty()) {
            for (AttributeGroupDto attributeGroupDto : attributeGroupInfo) {
                List<AttributeDto> attributeDtoList = attributeGroupDto.getAttributeDtoList();
                if (attributeDtoList != null && !attributeDtoList.isEmpty()) {
                    for (AttributeDto attributeDto : attributeDtoList) {
                        map.put(attributeDto.getCaption(), attributeDto);
                    }
                }
            }
        }
        return map;

    }


    public Map<String, RelationDto> getRelationInfo(String appName, String type, List<AssociationEntityDto> associationEntityInfo, List<SelectCtpEnumDto> selectCtpEnumInfo) {
        Map<String, RelationDto> map = new HashMap<>();
        if (type.equals(UdcDataTypeEnum.ENTITY.code())) {
            if (associationEntityInfo != null && !associationEntityInfo.isEmpty()) {
                for (AssociationEntityDto associationEntityDto : associationEntityInfo) {
                    String caption = associationEntityDto.getCaption();

                    List<RelationDto> entityDtos = associationEntityDto.getEntityDtos();
                    if (entityDtos != null && !entityDtos.isEmpty()) {
                        for (RelationDto entityDto : entityDtos) {
                            map.put(entityDto.getRelationEntityName(), entityDto);
                            if (!caption.contains("[当前应用]")) {
                                entityDto.setRelationEntityName(caption + "/" + entityDto.getRelationEntityName());
                            }
                        }
                    }
                }
            }
        } else if (type.equals(UdcDataTypeEnum.CTPENUM.code())) {
            if (selectCtpEnumInfo != null && !selectCtpEnumInfo.isEmpty()) {
                for (SelectCtpEnumDto selectCtpEnumDto : selectCtpEnumInfo) {
                    ApplicationDto applicationInfo = selectCtpEnumDto.getApplicationInfo();
                    if (applicationInfo == null) {
                        continue;
                    }
                    String appId = applicationInfo.getId();
                    String caption = applicationInfo.getCaption();
                    String relationApp = "";
                    if (appId != null) {
                        relationApp = appId;
                    } else {
                        relationApp = applicationInfo.getName();
                    }
                    List<RootEnumDtos> rootEnumDtos = selectCtpEnumDto.getRootEnumDtos();
                    if (!rootEnumDtos.isEmpty()) {
                        for (RootEnumDtos rootEnumDto : rootEnumDtos) {
                            String name = rootEnumDto.getName();
                            if (name.contains("zh")) {
                                ObjectMapper objectMapper = new ObjectMapper(); // 创建 ObjectMapper 实例
                                try {
                                    JsonNode jsonObject = objectMapper.readTree(name);
                                    name = jsonObject.get("zh_CN").asText();
                                } catch (IOException e) {
                                    log.error("解析 JSON 字符串失败", e);
                                }
                            }
                            RelationDto relationDto = new RelationDto();
                            if (appId != null) {
                                relationDto.setRelationEntityName(name);
                            } else {
                                relationDto.setRelationEntityName(caption + "/" + name);
                            }
                            relationDto.setRelationAppName(applicationInfo.getName());
                            relationDto.setRelationCode(rootEnumDto.getCode());
                            String id = rootEnumDto.getId();
                            relationDto.setRelationApp(relationApp);
                            if (id == null) {
                                relationDto.setRelationEntity(rootEnumDto.getFullName());
                            } else {
                                relationDto.setRelationEntity(rootEnumDto.getId());
                            }
                            RelationDto relationDto1 = map.get(name);
                            if (relationDto1 != null) {
                                if(relationDto.getRelationAppName().equals(appName)){
                                    continue;
                                }
                            }
                            map.put(name, relationDto);
                            List<Map<String, Object>> children = rootEnumDto.getChildren();
                            if (children != null && children.size() > 0) {
                                for (Map<String, Object> child : children) {
                                    RelationDto relationDto2 = new RelationDto();
                                    relationDto2.setRelationEntityName(String.valueOf(child.get("name")));
                                    relationDto2.setRelationCode(String.valueOf(child.get("code")));
                                    String childrenId = String.valueOf(child.get("id"));
                                    relationDto2.setRelationApp(relationApp);
                                    if (childrenId == null||childrenId.equals("null")) {
                                        relationDto2.setRelationEntity(String.valueOf(child.get("fullName")));
                                    } else {
                                        relationDto2.setRelationEntity(childrenId);
                                    }
                                    map.put(String.valueOf(child.get("name")), relationDto2);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            log.error("获取关联信息类型错误:{}", type);
        }
        return map;
    }


}
