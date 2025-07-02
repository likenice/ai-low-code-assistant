package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 坐标信息
@Data
@Slf4j
public class CoordinatesDto {
    private String coordinate;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Integer getparseCoordinateX1(String coordinate) {
        try {
            List<Integer> coordinateList = objectMapper.readValue(coordinate, new TypeReference<List<Integer>>() {});
            return coordinateList.get(0);
        } catch (Exception e) {
            log.info("x1坐标提取失败：{}",e);
            return null;
        }
    }

    public static Integer getparseCoordinateY1(String coordinate) {
        try {
            List<Integer> coordinateList = objectMapper.readValue(coordinate, new TypeReference<List<Integer>>() {});
            return coordinateList.get(1);
        } catch (Exception e) {
            log.info("y1坐标提取失败：{}",e);
            return null;
        }
    }
    public static Map<String,Integer> getparseCoordinates(String coordinate) {
        Map<String,Integer> map = new HashMap<>();
        try {
            List<Integer> coordinateList = objectMapper.readValue(coordinate, new TypeReference<List<Integer>>() {});
            map.put("x1",coordinateList.get(0));
            map.put("y1",coordinateList.get(1));
            map.put("x2",coordinateList.get(2));
            map.put("y2",coordinateList.get(3));
            return map;
        } catch (Exception e) {
            log.info("y1坐标提取失败：{}",e);
            return null;
        }
    }
    public static List<Integer> getparseCoordinatesToList(String coordinate) {
        try {
            List<Integer> coordinateList = objectMapper.readValue(coordinate, new TypeReference<List<Integer>>() {});
            return coordinateList;
        } catch (Exception e) {
            log.info("y1坐标提取失败：{}",e);
            return null;
        }
    }
}
