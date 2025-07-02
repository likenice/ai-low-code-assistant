package com.seeyon.ai.ocrprocess.service;


import com.seeyon.ai.ocrprocess.form.CellDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CellsProcessorService {


    public void populateCellContents(List<CellDto> cells, List<Map<String, Object>> ocrResult) {
        List<CellDto> newCells = new ArrayList<>();
        int offset = 10;
        for (CellDto cell : cells) {
            List<Map<String,Object>> contents = cell.getContents();
            contents.clear();
            List<Double> location = cell.getLocation();
            double xMin = location.get(0);
            double yMin = location.get(1);
            double xMax = location.get(2);
            double yMax = location.get(3);
            for (Map<String, Object> ocr : ocrResult) {
                for (Map.Entry<String, Object> entry : ocr.entrySet()) {
                    String coordStr = entry.getKey();
                    String text = String.valueOf(entry.getValue());
                    String[] coordsArray = coordStr.substring(1, coordStr.length() - 1).split(",");
                    List<Integer> coords = new ArrayList<>();
                    for (String coord : coordsArray) {
                        coords.add(Integer.parseInt(coord.trim()));  // 转换为整数并添加到列表中
                    }
                    // 检查坐标是否在指定的范围内
                    if (xMin - offset <= coords.get(0) &&
                            yMin - offset <= coords.get(1) &&
                            coords.get(2) <= xMax + offset &&
                            coords.get(3) <= yMax + offset) {
                        contents.add(ocr);
                        break;
                    }
                }
            }
            newCells.add(cell);
        }
        cells.clear();
        cells.addAll(newCells);
    }


    public CellDto findCell(List<CellDto> cells,Map<String,Object> block){
        for (CellDto cell : cells) {
            List<Map<String, Object>> contents = cell.getContents();
            if(contents.contains(block)){
                return cell;
            }
        }
        return null;
    }


    public List<CellDto> getSingleContentCells(List<CellDto> cells){
        return cells.stream()  // 将 List 转换为 Stream
                .filter(cell -> cell.getContents().size() == 1)  // 过滤出 "contents" 长度为 1 的 cell
                .collect(Collectors.toList());
    }

}
