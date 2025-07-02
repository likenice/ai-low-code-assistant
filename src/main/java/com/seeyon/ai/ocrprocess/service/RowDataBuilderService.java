package com.seeyon.ai.ocrprocess.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyon.ai.ocrprocess.form.CellDto;
import com.seeyon.ai.ocrprocess.form.TextBlockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RowDataBuilderService {

    @Autowired
    private CellsProcessorService cellsProcessorService;

    public List process(List<Map<String, Object>> blocks, List<CellDto> cellsProcessor) {
        int absTolerance = 25;
        double relTolerance = 0.1;
        List rows = new ArrayList();
        List<Map<String, Object>> currentRow = new ArrayList();
        List<Map<String, Object>> sameCellBlocks = new ArrayList();
        List<Integer> prevCoords = new ArrayList<>();
        List<Integer> prevCellCoords = new ArrayList<>();
        Map<String, Object> prevBlock = new LinkedHashMap<>();
        List<List<String>> singleContentCoords = new ArrayList<>();
        if (cellsProcessor != null && cellsProcessor.size() > 0) {
            cellsProcessorService.populateCellContents(cellsProcessor, blocks);
            List<CellDto> singleContentCells = cellsProcessorService.getSingleContentCells(cellsProcessor);
            for (CellDto singleContentCell : singleContentCells) {
                List<String> list = new ArrayList<>();
                List<Double> location = singleContentCell.getLocation();
                String newLocation = "[" + Math.round(location.get(0)) + "," + Math.round(location.get(1)) + "," + Math.round(location.get(2)) + "," + Math.round(location.get(3)) + "]";
                list.add(newLocation);
                list.add(singleContentCell.getContents().get(0).keySet().stream().findFirst().orElse(null));
                singleContentCoords.add(list);
            }

        }
//        for (Map<String, Object> block : blocks) {
        for (int i = 0; i < blocks.size(); i++) {
            Map<String, Object> block = blocks.get(i);
            TextBlockDto textBlockDto = TextBlockDto.initMap(block);
            String coordsStr = textBlockDto.getCoordinates();
            String value = textBlockDto.getText();
            ObjectMapper objectMapper = new ObjectMapper();
            List<Integer> coords = null;
            try {
                coords = objectMapper.readValue(coordsStr, new TypeReference<List<Integer>>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            if (prevCoords.size() == 0) {
                currentRow.add(block);
                prevCoords.addAll(coords);
                prevBlock.putAll(block);
            } else {
                if (prevCoords.size() < 4 || coords.size() < 4) {
                    continue;
                }
                Integer x1 = prevCoords.get(0);
                Integer y1 = prevCoords.get(1);
                Integer x2 = prevCoords.get(2);
                Integer y2 = prevCoords.get(3);
                Integer x3 = coords.get(0);
                Integer y3 = coords.get(1);
                Integer x4 = coords.get(2);
                Integer y4 = coords.get(3);
                int absDiff = Math.max(Math.abs(y3 - y1), Math.abs(y4 - y2));
                int minHeight = Math.max(y2 - y1, y4 - y3);
                double relDiff = (minHeight > 0) ? (absDiff / minHeight) : 1;
                // 判定策略：如果两个 block 的 y 坐标差值大于 abs_tolerance 或者相对差值大于 rel_tolerance，则认为不在同一行
                boolean yAdjacent = absDiff < absTolerance || relDiff < relTolerance;
                // 判定策略：如果 text_block 的 y 坐标跨度能够覆盖 prev_block，或反之
                int delta = 3;
                boolean yContain = (y1 <= y3 && y2 > y4 + delta) || (y3 <= y1 && y4 > y2 + delta);
                // y_contain = (y1 <= y3 and y2 > y4 + delta)
                // 判定策略：通过 cells_processor.find_cell 查询对应的 cell，如果 cell['id'] 相等，则认为同一行
//                if (cellsProcessor != null && cellsProcessor.size() > 0) {
//                    boolean sameCell = false;
//                    CellDto prevCell = cellsProcessorService.findCell(cellsProcessor, prevBlock);
//                    CellDto currentCell = cellsProcessorService.findCell(cellsProcessor, block);
//                    sameCell = prevCell != null && currentCell != null && (prevCell.getId() == currentCell.getId());
//                    if (!yAdjacent && !yContain && !sameCell) {
//                        if (sameCellBlocks.size() > 0) {
//                            for (Map<String, Object> cellBlock : sameCellBlocks) {
//                                currentRow.add(cellBlock);
//                            }
//                        }
//                        List<Map<String, Object>> list = new ArrayList<>();
//                        for (Map<String, Object> map : currentRow) {
//                            list.add(map);
//                        }
//                        rows.add(list);
//                        currentRow.clear();
//                        currentRow.add(block);
//                    }
//                    else {
//                        if (sameCell) {
//                            if (!sameCellBlocks.contains(prevBlock)) {
//                                sameCellBlocks.add(prevBlock);
//                                if(currentRow.size()>=1){
//                                    currentRow.remove(currentRow.size() - 1);
//                                }
//                            }
//                            sameCellBlocks.add(block);
//                        } else {
//                            if (sameCellBlocks.size() > 0) {
//                                for (Map<String, Object> cellBlock : sameCellBlocks) {
//                                    currentRow.add(cellBlock);
//                                }
//                                sameCellBlocks.clear();
//                            }
//                            currentRow.add(block);
//                        }
//
//                    }
//                    prevCoords.clear();
//                    prevCoords.addAll(coords);
//                    prevBlock.clear();
//                    prevBlock.putAll(block);
//                } else {
                if ( !yAdjacent && !yContain) {
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
//                        currentRow.clear();
                    currentRow.add(block);
                } else {
                    currentRow.add(block);
                }
                prevCoords.clear();
                prevCoords.addAll(coords);
                prevBlock.clear();
                prevBlock.putAll(block);
//                }

            }
        }
        if (currentRow.size() > 0) {
            rows.add(currentRow);
        }
        orderRows(rows);

        return rows;

    }

    private void orderRows(List rows) {
        for (Object o : rows) {
            if (o == null) {
                continue;
            }
            List row = (List) o;
            List<TextBlockDto> textBlocks = TextBlockDto.init(row);
            textBlocks.sort(Comparator.comparingInt(blcok -> blcok.getparseCoordinateX1()));
            if (textBlocks.size() > 1 && Math.abs(textBlocks.get(0).getparseCoordinateY1() - textBlocks.get(1).getparseCoordinateY1()) > 10) {
                Collections.sort(textBlocks, new Comparator<TextBlockDto>() {
                    @Override
                    public int compare(TextBlockDto tb1, TextBlockDto tb2) {
                        if (Math.abs(tb1.getparseCoordinateY1() - tb2.getparseCoordinateY1()) <= 15) {
                            return Integer.compare(tb1.getparseCoordinateX1(), tb2.getparseCoordinateX1());
                        } else {
                            return Integer.compare(tb1.getparseCoordinateY1() , tb2.getparseCoordinateY1());
                        }
//                        // 先比较 y1，如果 y1 相同，则比较 x1
//                        int y1Compare = Integer.compare(tb1.getparseCoordinateY1(), tb2.getparseCoordinateY1());
//                        if (y1Compare != 0) {
//                            return y1Compare;
//                        }
//                        return Integer.compare(tb1.getparseCoordinateX1(), tb2.getparseCoordinateX1());
                    }
                });
            }
            int index = rows.indexOf(row);
            List list = new ArrayList();
            for (TextBlockDto textBlock : textBlocks) {
                list.add(textBlock.getBlock());
            }
            rows.set(index, list);

        }

    }


}
