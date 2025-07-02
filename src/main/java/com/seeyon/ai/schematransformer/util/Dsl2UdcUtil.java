package com.seeyon.ai.schematransformer.util;//package com.seeyon.ai.schematransformer.util;
//
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.seeyon.app.common.common.DslTransformUtil;
//
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//
//public class Dsl2UdcUtil {
//    private static final Map<String, String> referceFullNameMap = new ConcurrentHashMap<>();
//    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
//
//    public static class ReferenceRequest {
//        private String entityFullName;
//        private String appName;
//        private String entityId;
//        private String starterName;
//
//        public ReferenceRequest(String entityFullName, String appName, String entityId, String starterName) {
//            this.entityFullName = entityFullName;
//            this.appName = appName;
//            this.entityId = entityId;
//            this.starterName = starterName;
//        }
//
//        public String getEntityFullName() { return entityFullName; }
//        public String getAppName() { return appName; }
//        public String getEntityId() { return entityId; }
//        public String getStarterName() { return starterName; }
//    }
//
//    public static class ReferenceResponse {
//        private List<ReferMetaDataDto> referMetaDataDtoList;
//        private boolean processSign;
//
//        public ReferenceResponse(List<ReferMetaDataDto> referMetaDataDtoList, boolean processSign) {
//            this.referMetaDataDtoList = referMetaDataDtoList;
//            this.processSign = processSign;
//        }
//
//        public List<ReferMetaDataDto> getReferMetaDataDtoList() { return referMetaDataDtoList; }
//        public boolean isProcessSign() { return processSign; }
//    }
//
//    public static class ReferMetaDataDto {
//        private String fullName;
//
//        public ReferMetaDataDto(String fullName) {
//            this.fullName = fullName;
//        }
//
//        public String getFullName() { return fullName; }
//    }
////
////    @SuppressWarnings("unchecked")
////    public CompletableFuture<ReferenceResponse> processReferenceData(List<Map<String, Object>> data, boolean isDoc) {
////        CompletableFuture<ReferenceResponse> future = new CompletableFuture<>();
////
////        executorService.execute(() -> {
////            try {
////                List<String> fieldLists = new ArrayList<>();
////                List<CompletableFuture<List<ReferMetaDataDto>>> referRequests = new ArrayList<>();
////
////                if (isDoc) {
////                    traverseDocData(data, fieldLists, referRequests);
////                } else {
////                    processNonDocData(data, fieldLists, referRequests);
////                }
////
////                if (!referRequests.isEmpty()) {
////                    CompletableFuture.allOf(referRequests.toArray(new CompletableFuture[0]))
////                        .thenAccept(v -> {
////                            List<List<ReferMetaDataDto>> referListsRes = new ArrayList<>();
////                            for (CompletableFuture<List<ReferMetaDataDto>> request : referRequests) {
////                                try {
////                                    referListsRes.add(request.get());
////                                } catch (Exception e) {
////                                    e.printStackTrace();
////                                }
////                            }
////
////                            for (int i = 0; i < fieldLists.size(); i++) {
////                                String entityFullName = fieldLists.get(i);
////                                List<ReferMetaDataDto> referLists = referListsRes.get(i);
////                                if (referLists != null && !referLists.isEmpty()) {
////                                    ReferMetaDataDto defaultRefer = referLists.get(0);
////                                    referceFullNameMap.put(entityFullName, defaultRefer.getFullName());
////                                }
////                            }
////
////                            future.complete(new ReferenceResponse(
////                                referListsRes.stream()
////                                    .flatMap(List::stream)
////                                    .collect(Collectors.toList()),
////                                true
////                            ));
////                        });
////                } else {
////                    future.complete(new ReferenceResponse(Collections.emptyList(), true));
////                }
////            } catch (Exception e) {
////                future.completeExceptionally(e);
////            }
////        });
////
////        return future;
////    }
////
////    @SuppressWarnings("unchecked")
////    private void traverseDocData(List<Map<String, Object>> data, List<String> fieldLists,
////            List<CompletableFuture<List<ReferMetaDataDto>>> referRequests) {
////        for (Map<String, Object> item : data) {
////            Map<String, Object> dataSource = (Map<String, Object>) item.get("dataSource");
////
////            if (dataSource != null && dataSource.get("relationApp") != null && dataSource.get("relationEntity") != null) {
////                String entityFullName = (String) dataSource.get("relationEntity");
////                String appName = (String) dataSource.get("relationApp");
////
////                if (!fieldLists.contains(entityFullName)) {
////                    fieldLists.add(entityFullName);
////                    referRequests.add(fetchReferListCompatibility(
////                        new ReferenceRequest(entityFullName, appName, null, null)));
////                }
////            }
////
////            List<Map<String, Object>> children = (List<Map<String, Object>>) item.get("children");
////            if (children != null) {
////                traverseDocData(children, fieldLists, referRequests);
////            }
////        }
////    }
////
////    @SuppressWarnings("unchecked")
////    private void processNonDocData(List<Map<String, Object>> data, List<String> fieldLists,
////            List<CompletableFuture<List<ReferMetaDataDto>>> referRequests) {
////        for (Map<String, Object> item : data) {
////            List<Map<String, Object>> entityData = (List<Map<String, Object>>) item.get("entityData");
////
////            if (entityData != null) {
////                for (Map<String, Object> entity : entityData) {
////                    if ("ENTITY".equals(entity.get("fieldType"))) {
////                        Map<String, Object> relation = (Map<String, Object>) entity.get("relation");
////                        if (relation != null) {
////                            String entityFullName = (String) relation.get("id");
////                            String appName = (String) relation.get("appId");
////
////                            if (!fieldLists.contains(entityFullName)) {
////                                fieldLists.add(entityFullName);
////                                referRequests.add(fetchReferListCompatibility(
////                                    new ReferenceRequest(entityFullName, appName, null, null)));
////                            }
////                        }
////                    }
////                }
////            }
////        }
////    }
////
////    private CompletableFuture<List<ReferMetaDataDto>> fetchReferListCompatibility(ReferenceRequest request) {
////        // TODO: Implement actual API call to fetch reference list
////        // This is a mock implementation that should be replaced with actual API call
////        return CompletableFuture.completedFuture(Collections.singletonList(
////            new ReferMetaDataDto(request.getEntityFullName())
////        ));
////    }
//}
