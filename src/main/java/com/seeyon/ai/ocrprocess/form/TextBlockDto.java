package com.seeyon.ai.ocrprocess.form;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class TextBlockDto {
    private Map<String, Object> block;
    private String coordinates;
    private String text;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<TextBlockDto> init(List row) {
        List<TextBlockDto> list = new LinkedList<>();
        for (Object o : row) {
            LinkedHashMap map = new LinkedHashMap<>();

            if (o instanceof Map<?,?>) {
                map = (LinkedHashMap) o;
            } else {
                try {
                    map = objectMapper.readValue(String.valueOf(o),
                            new TypeReference<LinkedHashMap<String, Object>>() {});
                } catch (Exception e) {
                    // 处理解析异常
                    continue;
                }
            }
            TextBlockDto textBlockDto = new TextBlockDto();
            textBlockDto.setBlock(map);
            String key = String.valueOf(map.keySet().iterator().next());
            textBlockDto.setCoordinates(key);
            textBlockDto.setText(String.valueOf(map.get(key)));
            list.add(textBlockDto);
        }
        return list;
    }

    public static TextBlockDto initMap(Map<String, Object> map) {
        TextBlockDto textBlockDto = new TextBlockDto();
        textBlockDto.setBlock(map);
        String key = String.valueOf(map.keySet().iterator().next());
        textBlockDto.setCoordinates(key);
        textBlockDto.setText(String.valueOf(map.get(key)));
        return textBlockDto;
    }

    public static List<Integer> getParseCoordinatesList(TextBlockDto textBlock) {
        String[] coordsArray = textBlock.getCoordinates().substring(1,
                textBlock.getCoordinates().length() - 1).split(",");
        List<Integer> coords = new ArrayList<>();
        for (String coord : coordsArray) {
            coords.add(Integer.parseInt(coord.trim()));
        }
        return coords;
    }

    public Integer getparseCoordinateX1() {
        try {
            List<Integer> coordinateList = objectMapper.readValue(coordinates,
                    new TypeReference<List<Integer>>() {});
            return coordinateList.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getparseCoordinateY1() {
        try {
            List<Integer> coordinateList = objectMapper.readValue(coordinates,
                    new TypeReference<List<Integer>>() {});
            return coordinateList.get(1);
        } catch (Exception e) {
            return null;
        }
    }
}