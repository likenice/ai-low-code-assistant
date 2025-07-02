//package com.seeyon.ai.schematransformer.dto;
////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by FernFlower decompiler)
////
//
//
//import com.seeyon.boot.annotation.AppService;
//import com.seeyon.boot.annotation.DtoAttribute;
//import com.seeyon.boot.annotation.DtoInfo;
//import com.seeyon.boot.starter.extmetadata.enums.OpenWayEnum;
//import com.seeyon.boot.starter.extmetadata.enums.PageSourceTypeEnum;
//import com.seeyon.udc.common.constant.UdcCommonConstant;
//import com.seeyon.udc.common.dto.PageReferDto;
//import com.seeyon.udc.common.dto.UdcBaseEntityDto;
//import com.seeyon.udc.common.enums.PageSchemaStateEnum;
//import com.seeyon.udc.common.enums.PageUrlTypeEnum;
//import java.util.Date;
//import java.util.List;
//import javax.validation.constraints.NotNull;
//
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.validator.constraints.Length;
//
//@Getter
//@Setter
//public class GwPageInfoItemDto extends UdcBaseEntityDto {
//    private static final long serialVersionUID = 107273960468350776L;
//    @DtoAttribute(
//            value = "应用ID",
//            example = "123"
//    )
//    private @NotNull(
//            groups = {AppService.ValidationGroup.Create.class}
//    ) Long appId;
//    @DtoAttribute(
//            value = "创建时间，时间戳",
//            example = "1607071824000"
//    )
//    private Date createTime;
//    @DtoAttribute(
//            value = "分组ID",
//            example = "分组1",
//            description = "页面分组ID"
//    )
//    private Long groupId;
//    @DtoAttribute(
//            value = "关联分组",
//            description = "关联分组（pc/移动/平板分为一组，便于在页面设计器统一设置）"
//    )
//    private Long relationGroupId;
//    @DtoAttribute(
//            value = "关联的外框id",
//            example = "1103926005900247141",
//            description = "新增、保存时生效，存草稿时不生效"
//    )
//    private Long layoutId;
//    @DtoAttribute(
//            value = "绑定的外框信息",
//            example = "{\"fullName\": 11111,\"appName\":\"app-approval\"}",
//            description = "绑定的外框信息,JSON数据"
//    )
//    private String layoutInfo;
//    @DtoAttribute(
//            value = "页面名称",
//            example = "bxdxq"
//    )
//    private String name;
//    @DtoAttribute(
//            value = "页面显示名称",
//            example = "报销单详情"
//    )
//    private @Length(
//            max = 50
//    ) @NotNull(
//            groups = {AppService.ValidationGroup.Create.class}
//    ) String caption;
//    @DtoAttribute(
//            value = "页面描述",
//            example = "日常费用报销单详情页。"
//    )
//    private @Length(
//            max = 255
//    ) String description;
//    @DtoAttribute(
//            value = "页面分类",
//            example = "DETAIL"
//    )
//    private @NotNull(
//            groups = {AppService.ValidationGroup.Create.class}
//    ) String pageType;
//    @DtoAttribute(
//            value = "手写页面（自定义页面）",
//            example = "0",
//            description = "手写页面（自定义页面）"
//    )
//    private Boolean handWritten;
//    @DtoAttribute(
//            value = "默认打开方式",
//            example = "WORKSPACE"
//    )
//    private String defaultOpenType;
//    @DtoAttribute(
//            value = "url应用类型",
//            example = "MOBILE"
//    )
//    private @NotNull(
//            groups = {AppService.ValidationGroup.Create.class}
//    ) PageUrlTypeEnum urlType;
//    @DtoAttribute(
//            value = "页面url,也用作唯一标识/同实体的Name",
//            example = "/sale/detail"
//    )
//    private @Length(
//            max = 100
//    ) @NotNull(
//            groups = {AppService.ValidationGroup.Create.class}
//    ) String pageUrl;
//    @DtoAttribute(
//            value = "排序",
//            example = "1"
//    )
//    private Integer sortNo;
//    @DtoAttribute(
//            value = "页面来源类型",
//            example = "DESIGNER"
//    )
//    private PageSourceTypeEnum sourceType;
//    @DtoAttribute(
//            value = "页面来源id",
//            description = "页面或模板id，用于通过模板或者页面传递，记录页面id或者是模板id",
//            example = "123"
//    )
//    private Long sourceId;
//    @DtoAttribute(
//            value = "页面对应的主实体Id",
//            example = "123"
//    )
//    private String masterEntityIds;
//    @DtoAttribute(
//            value = "页面包含的实体Id",
//            example = "123"
//    )
//    private String includeEntityIds;
//    @DtoAttribute("页面打开方式")
//    private OpenWayEnum openWay;
//    @DtoAttribute(
//            value = "页面使用的参照",
//            example = "json数组"
//    )
//    private List<PageReferDto> referConfigParameters;
//    @DtoAttribute(
//            value = "是否为公共页面",
//            example = "false"
//    )
//    private Boolean publicAccess;
//    @DtoAttribute(
//            value = "页面schema状态",
//            example = "DRAFT"
//    )
//    private PageSchemaStateEnum schemaState;
//
//}
//
