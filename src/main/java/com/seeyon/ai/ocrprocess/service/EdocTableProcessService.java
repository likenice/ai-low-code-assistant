package com.seeyon.ai.ocrprocess.service;

import cn.hutool.json.JSONUtil;
import com.seeyon.ai.ocrprocess.form.CellDto;
import com.seeyon.ai.ocrprocess.form.EdocGroupDto;
import com.seeyon.ai.ocrprocess.form.EdocTableDto;
import com.seeyon.ai.ocrprocess.form.EdocTableStructureDto;
import com.seeyon.boot.util.id.Ids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EdocTableProcessService {
    private static final int lineDiff = 14;

    /**
     * group识别 大group 与小group之分
     *
     * @param tableStructure 表结构
     * @param cellDtos       tsr数据
     */
    public Map<String, Object> groupIdentify(List<EdocTableStructureDto> tableStructure, List<CellDto> cellDtos, Map<String, String> layoutMapping, int maxX) {
        List<Integer> allFieldStartXList = layoutProcess(tableStructure);
        Map<String, Object> map = new HashMap<>();
        List<EdocTableDto> list = new ArrayList<>();
        List<List<Integer>> tsrList = new ArrayList<>();
        int a = 0;
        while (true) {
            EdocTableDto edocTableDto = new EdocTableDto();
            List<EdocGroupDto> edocGroupDtos = new ArrayList<>();
            Boolean prevTsr = null;
            boolean prevSelfTsr = false;
            boolean prevBigTsr = false;
            String fieldType = "";
            boolean b = false;
            List<String> layoutList = new ArrayList<>();
            for (int i = a; i < tableStructure.size(); i++) {
                EdocGroupDto edocGroupDto = new EdocGroupDto();
                EdocTableStructureDto edocTableStructureDto = tableStructure.get(i);
                String fn = edocTableStructureDto.getFn();
                String nLayout = edocTableStructureDto.getNLayout();
                List<Integer> nLayoutList = layoutStrToList(nLayout);
                String type = edocTableStructureDto.getType();
                String fv = "";
                String vLayout = "";
                if (type.equals("f")) {
                    fv = edocTableStructureDto.getFv();
                    vLayout = edocTableStructureDto.getVLayout();
                }
                Map<String, Object> keyMap = tsrJudge(nLayout, cellDtos, fv, fn);
                boolean selfTsr = (boolean) keyMap.get("selfTsr");
                boolean bigTsr = (boolean) keyMap.get("bigTsr");
                List<Integer> tsrLayout = (List<Integer>) keyMap.get("layout");

                Long relationId = Ids.gidLong();
//                Random rand = new Random();
//                Long relationId =rand.nextLong();
                if (prevTsr != null) {
                    if (!selfTsr && !bigTsr) {
                        // 当前key没有tsr线 并且上一个字段有tsr的线
                        if (prevTsr) {
                            EdocGroupDto prevEdocGroupDto = edocGroupDtos.get(edocGroupDtos.size() - 1);
                            String dataType = prevEdocGroupDto.getDataType();
                            String prevTsrlayout = "";
                            if (dataType.equals("field")) {
                                prevTsrlayout = prevEdocGroupDto.getValueLayout().toString();
                            } else {
                                prevTsrlayout = prevEdocGroupDto.getLayout().toString();

                            }
                            EdocTableStructureDto prevEdocTableStructureDto = tableStructure.get(i - 1);
                            String prevNlayout = prevEdocTableStructureDto.getNLayout();
                            boolean tsrLine = isLine(nLayout, prevNlayout);
                            boolean line = false;
                            if (!tsrLine) {
                                String ocrLayout = layoutMapping.get(prevNlayout);
                                if (ocrLayout != null && !ocrLayout.equals("")) {
                                    boolean ocrLine = isLine(nLayout, ocrLayout);
                                    if (ocrLine) {
                                        line = true;
                                    }
                                }
                            } else {
                                line = true;
                            }
                            // 上一个字段和当前字段在同一行
                            if (line) {
                                selfTsr = prevSelfTsr;
                                bigTsr = prevBigTsr;
                                tsrLayout.set(0, layoutStrToList(prevTsrlayout).get(2));
                                tsrLayout.set(1, layoutStrToList(prevTsrlayout).get(1));
                                tsrLayout.set(3, layoutStrToList(prevTsrlayout).get(3));
                                edocGroupDto.setTsr(false);
                            } else {
                                boolean valueTsr = false;
                                // 如果有value 判断value是否有线
                                if (!vLayout.equals("")) {
                                    Map<String, Object> valueMap = tsrJudge(vLayout, cellDtos, fn, fv);
                                    boolean valueSelfTsr = (boolean) valueMap.get("selfTsr");
                                    boolean valueBigTsr = (boolean) valueMap.get("bigTsr");
                                    // value有线 key无线
                                    if (valueSelfTsr) {
                                        edocGroupDto.setTsr(false);
                                        selfTsr = true;
                                        valueTsr = true;
                                        tsrLayout.set(1, layoutStrToList(vLayout).get(1));
                                        tsrLayout.set(2, layoutStrToList(vLayout).get(2));
                                        tsrLayout.set(3, layoutStrToList(vLayout).get(3));
                                    } else if (valueBigTsr) {
                                        edocGroupDto.setTsr(false);
                                        bigTsr = true;
                                        valueTsr = true;
                                        tsrLayout.set(1, layoutStrToList(vLayout).get(1));
                                        tsrLayout.set(2, layoutStrToList(vLayout).get(2));
                                        tsrLayout.set(3, layoutStrToList(vLayout).get(3));
                                    }
                                }
                                // 没有value 或value也没线
                                if (vLayout.equals("") || !valueTsr) {
                                    // 不是最后一个字段 那么遍历同行的所有字段 看有没有有线的
                                    if (i + 1 < tableStructure.size()) {
                                        for (int m = i + 1; m < tableStructure.size(); m++) {
                                            EdocTableStructureDto nextEdocTableStructureDto = tableStructure.get(m);
                                            String nextFn = nextEdocTableStructureDto.getFn();
                                            String nextNLayout = nextEdocTableStructureDto.getNLayout();
                                            String nextType = nextEdocTableStructureDto.getType();
                                            String nextFv = "";
                                            String nextVLayout = "";
                                            if (nextType.equals("f")) {
                                                nextFv = nextEdocTableStructureDto.getFv();
                                                nextVLayout = nextEdocTableStructureDto.getVLayout();
                                            }
                                            Map<String, Object> nextmap = tsrJudge(nextNLayout, cellDtos, nextFv, nextFn);
                                            boolean nextSelfTsr = (boolean) nextmap.get("selfTsr");
                                            boolean nextBigTsr = (boolean) nextmap.get("bigTsr");
                                            boolean nextLine = false;
                                            boolean nextTsrLine = isLine(nLayout, nextNLayout);
                                            if (!nextTsrLine) {
                                                String ocrLayout = layoutMapping.get(nextNLayout);
                                                if (ocrLayout != null && !ocrLayout.equals("")) {
                                                    boolean ocrLine = isLine(nLayout, ocrLayout);
                                                    if (ocrLine) {
                                                        nextLine = true;
                                                    }
                                                }
                                            } else {
                                                nextLine = true;
                                            }
                                            if (nextLine) {
                                                // 下一个key有线
                                                if (nextSelfTsr) {
                                                    edocGroupDto.setTsr(false);
                                                    selfTsr = true;
                                                    tsrLayout.set(1, layoutStrToList(nextNLayout).get(1));
                                                    tsrLayout.set(3, layoutStrToList(nextNLayout).get(3));
                                                    break;
                                                } else if (nextBigTsr) {
                                                    edocGroupDto.setTsr(false);
                                                    bigTsr = true;
                                                    tsrLayout.set(1, layoutStrToList(nextNLayout).get(1));
                                                    tsrLayout.set(3, layoutStrToList(nextNLayout).get(3));
                                                    break;
                                                }
                                                if (!nextSelfTsr && !nextBigTsr) {
                                                    // 下一个value有线
                                                    if (!nextVLayout.equals("")) {
                                                        Map<String, Object> nextValueMap = tsrJudge(nextVLayout, cellDtos, nextFn, nextFv);
                                                        boolean nextVSelfTsr = (boolean) nextValueMap.get("selfTsr");
                                                        boolean nextVBigTsr = (boolean) nextValueMap.get("bigTsr");
                                                        if (nextVSelfTsr) {
                                                            edocGroupDto.setTsr(false);
                                                            tsrLayout.set(1, layoutStrToList(nextVLayout).get(1));
                                                            tsrLayout.set(3, layoutStrToList(nextVLayout).get(3));
                                                            selfTsr = true;
                                                            break;
                                                        } else if (nextVBigTsr) {
                                                            edocGroupDto.setTsr(false);
                                                            tsrLayout.set(1, layoutStrToList(nextVLayout).get(1));
                                                            tsrLayout.set(3, layoutStrToList(nextVLayout).get(3));
                                                            bigTsr = true;
                                                            break;
                                                        }
                                                    } else {
                                                        continue;
                                                    }
                                                }

                                            } else {
                                                break;
                                            }


                                        }
                                    }
                                }

                            }

                        }
                    }
                    if (prevTsr) {
                        if (!selfTsr && !bigTsr) {
                            b = true;
                            break;
                        }
                    } else {
                        if (selfTsr || bigTsr) {
                            b = true;
                            break;
                        }
                    }
                }
                prevSelfTsr = selfTsr;
                prevBigTsr = bigTsr;
                maxX = Math.max(maxX, tsrLayout.get(2));
                String bigTsrtoNLayout = "";
                if (bigTsr) {
                    if (!vLayout.equals("")) {
                        bigTsrtoNLayout = "[" + layoutStrToList(nLayout).get(0) + "," + layoutStrToList(nLayout).get(1) + "," + layoutStrToList(vLayout).get(2) + "," + layoutStrToList(nLayout).get(3);
                    } else {
                        bigTsrtoNLayout = nLayout;
                    }
                }
                prevTsr = insertEdocGroup(selfTsr, bigTsr, fn, relationId, tsrLayout, edocGroupDto, tsrList, edocGroupDtos, "label", fn, nLayout);
                if (fieldType.equals("")) {
                    if (prevTsr) {
                        fieldType = "grid";
                    } else {
                        fieldType = "container";
                    }
                }
                if (edocGroupDto != null && edocGroupDto.getLayout() != null) {
                    edocGroupDtos.add(edocGroupDto);
                }
                if (bigTsr) {
                    tsrList.add(tsrLayout);
                    a++;
                    continue;
                }
                if (!vLayout.equals("")) {
                    boolean isField = false;
                    if (i + 1 < tableStructure.size()) {
                        EdocTableStructureDto next = tableStructure.get(i + 1);
                        String nLayout1 = next.getNLayout();
                        if(!matchLayout(vLayout,layoutList,nLayout).equals("")){
                            nLayout1 = matchLayout(vLayout,layoutList,nLayout);
                        }
                        List<Integer> nextLayout = layoutStrToList(nLayout1);
                        List<Integer> vLayoutList = layoutStrToList(vLayout);
                        int nX1 = nextLayout.get(0);
                        int x2 = vLayoutList.get(2);
                        // 不合并判断是同一行|| 当前单元格合并，判断y2 是否能包含下一个的合并 || 下一个单元格合并 判断 y1>下一个y1 y2《= 下一个y2
                        if (x2 > nX1&&(isLine(nLayout1,vLayout)||vLayoutList.get(3)-nextLayout.get(3)>50 ||vLayoutList.get(1)-nextLayout.get(1)>50&&vLayoutList.get(3)-nextLayout.get(3)<=lineDiff)) {
                            isField = true;
                        }

                    }
                    // value的起始坐标等于下一个fileLd的起始坐标
                    if (isField) {
                        vLayout = JSONUtil.toJsonStr(tsrLayout);
                    } else {
                        List<Integer> vLayoutList = layoutStrToList(vLayout);
                        int vX1 = vLayoutList.get(0);
                        int vX2 = vLayoutList.get(2);
                        int nX1 = nLayoutList.get(0);
                        int nX2 = nLayoutList.get(2);
                        if (vX1 == vX2) {
                            vLayoutList.set(2, vLayoutList.get(0) + 50);
                            vLayout = JSONUtil.toJsonStr(vLayoutList);
                        } else if ((vX1 == nX1 && vX2 == nX2) || (vX1 == nX2 && vX2 == nX2)) {
                            vLayoutList.set(2, vLayoutList.get(0) + 50);
                            vLayout = JSONUtil.toJsonStr(vLayoutList);
                        }
                    }
                    EdocGroupDto valueEdocGroupDto = new EdocGroupDto();
                    Map<String, Object> valueMap = tsrJudge(vLayout, cellDtos, fn, fv);
                    boolean valueSelfTsr = (boolean) valueMap.get("selfTsr");
                    boolean valueBigTsr = (boolean) valueMap.get("bigTsr");
                    List<Integer> valueTsrLayout = (List<Integer>) valueMap.get("layout");
                    if (valueBigTsr || valueSelfTsr) {
                        if (valueTsrLayout.equals(tsrLayout)) {
                            edocGroupDto.setDataType("field");
                            edocGroupDto.setValue(fv);
                            edocGroupDto.setValueLayout(valueTsrLayout);
                        } else {
                            prevSelfTsr = insertEdocGroup(valueSelfTsr, valueBigTsr, fv, relationId, valueTsrLayout, valueEdocGroupDto, tsrList, edocGroupDtos, "component", fn, vLayout);
                            if (valueBigTsr) {
                                tsrList.add(tsrLayout);
                            }
                            if (valueEdocGroupDto != null && valueEdocGroupDto.getLayout() != null) {
                                edocGroupDtos.add(valueEdocGroupDto);
                            }
                        }
                        maxX = Math.max(maxX, valueTsrLayout.get(2));
                    } else {
                        // value无线 key有线
                        if (!valueBigTsr && bigTsr) {
                            valueBigTsr = true;
                            edocGroupDto.setTsr(false);
                            valueTsrLayout.set(1, tsrLayout.get(1));
                            valueTsrLayout.set(3, tsrLayout.get(3));
                        }
                        // value无线 key有线
                        else if (!valueSelfTsr && selfTsr) {
                            valueEdocGroupDto.setTsr(false);
                            valueSelfTsr = true;
                            valueTsrLayout.set(1, tsrLayout.get(1));
                            valueTsrLayout.set(3, tsrLayout.get(3));
                        }
                        if (i + 1 < tableStructure.size()) {
                            EdocTableStructureDto nextEdocTableStructureDto = tableStructure.get(i + 1);
                            String nextNLayout = nextEdocTableStructureDto.getNLayout();
                            boolean nextLine = false;
                            boolean nextTsrLine = isLine(nLayout, nextNLayout);
                            if (!nextTsrLine) {
                                String ocrLayout = layoutMapping.get(nextNLayout);
                                if (ocrLayout != null && !ocrLayout.equals("")) {
                                    boolean ocrLine = isLine(nLayout, ocrLayout);
                                    if (ocrLine) {
                                        nextLine = true;
                                    }
                                }
                            } else {
                                nextLine = true;
                            }
                            if (nextLine) {
                                valueTsrLayout.set(2, layoutStrToList(nextNLayout).get(0));
                            } else {
                                valueTsrLayout.set(2, maxX);
                            }
                        } else {
                            valueTsrLayout.set(2, maxX);
                        }
//                        || (fv.equals("") && Math.abs(valueTsrLayout.get(1) - tsrLayout.get(1)) < 15) && Math.abs(valueTsrLayout.get(3) - tsrLayout.get(3)) < 15
                        maxX = Math.max(maxX, valueTsrLayout.get(2));
                        if (fieldType.equals("container")) {
                            edocGroupDto.setDataType("field");
                            edocGroupDto.setValue(fv);
                            edocGroupDto.setValueLayout(valueTsrLayout);
                        } else if (vLayout.equals(nLayout)) {
                            edocGroupDto.setDataType("field");
                            edocGroupDto.setValue(fv);
                            edocGroupDto.setValueLayout(tsrLayout);
                        } else if ((fv.equals("") && valueTsrLayout.get(2) - tsrLayout.get(2) < 60)) {
                            edocGroupDto.setDataType("field");
                            edocGroupDto.setValue(fv);
                            edocGroupDto.setValueLayout(valueTsrLayout);
                        } else if (!fv.equals("")) {
                            if (valueTsrLayout.equals(tsrLayout) || valueTsrLayout.get(2) - tsrLayout.get(2) < 60) {
                                edocGroupDto.setDataType("field");
                                edocGroupDto.setValue(fv);
                                edocGroupDto.setValueLayout(valueTsrLayout);
                            } else {
                              /*  if (i + 1 < tableStructure.size()) {
                                    EdocTableStructureDto nextEdocTableStructureDto = tableStructure.get(i + 1);
                                    String nextNLayout = nextEdocTableStructureDto.getNLayout();
                                    boolean nextLine = false;
                                    boolean nextTsrLine = isLine(nLayout, nextNLayout);
                                    if (!nextTsrLine) {
                                        String ocrLayout = layoutMapping.get(nextNLayout);
                                        if (ocrLayout != null && !ocrLayout.equals("")) {
                                            boolean ocrLine = isLine(nLayout, ocrLayout);
                                            if (ocrLine) {
                                                nextLine = true;
                                            }
                                        }
                                    } else {
                                        nextLine = true;
                                    }
                                    if (nextLine) {
                                        valueTsrLayout.set(2, layoutStrToList(nextNLayout).get(0));
                                    } else {
                                        valueTsrLayout.set(2, 999999);
                                    }
                                } else {
                                    valueTsrLayout.set(2, 999999);
                                }*/

                                prevSelfTsr = insertEdocGroup(valueSelfTsr, valueBigTsr, fv, relationId, valueTsrLayout, valueEdocGroupDto, tsrList, edocGroupDtos, "component", fn, vLayout);
                                if (valueBigTsr) {
                                    tsrList.add(tsrLayout);
                                }
                                if (valueEdocGroupDto != null && valueEdocGroupDto.getLayout() != null) {
                                    edocGroupDtos.add(valueEdocGroupDto);
                                }
                            }
                        } else {
                          /*  if (i + 1 < tableStructure.size()) {
                                EdocTableStructureDto nextEdocTableStructureDto = tableStructure.get(i + 1);
                                String nextNLayout = nextEdocTableStructureDto.getNLayout();
                                boolean nextLine = false;
                                boolean nextTsrLine = isLine(nLayout, nextNLayout);
                                if (!nextTsrLine) {
                                    String ocrLayout = layoutMapping.get(nextNLayout);
                                    if (ocrLayout != null && !ocrLayout.equals("")) {
                                        boolean ocrLine = isLine(nLayout, ocrLayout);
                                        if (ocrLine) {
                                            nextLine = true;
                                        }
                                    }
                                } else {
                                    nextLine = true;
                                }
                                if (nextLine) {
                                    valueTsrLayout.set(2, layoutStrToList(nextNLayout).get(0));
                                } else {
                                    valueTsrLayout.set(2, 999999);
                                }
                            } else {
                                valueTsrLayout.set(2, 999999);
                            }*/
                            prevSelfTsr = insertEdocGroup(valueSelfTsr, valueBigTsr, fv, relationId, valueTsrLayout, valueEdocGroupDto, tsrList, edocGroupDtos, "component", fn, vLayout);
                            if (valueBigTsr) {
                                tsrList.add(tsrLayout);
                            }
                            if (valueEdocGroupDto != null && valueEdocGroupDto.getLayout() != null) {
                                edocGroupDtos.add(valueEdocGroupDto);
                            }
                        }
                    }
                }
                layoutList.add(nLayout);
                a++;
            }

            edocTableDto.setEdocGroupDtos(edocGroupDtos);
            edocTableDto.setType(fieldType);
            list.add(edocTableDto);
            if (a == tableStructure.size()) {
                break;
            }
            if (b) {
                continue;
            }
        }
        map.put("groups", list);
        map.put("maxX", maxX);
        return map;
    }
    
    private String matchLayout (String layout,List<String> layoutList,String nLayout){
        List<Integer> layouts = layoutStrToList(layout);
        List<Integer> nLayouts = layoutStrToList(nLayout);
        for (String s : layoutList) {
            List<Integer> list = layoutStrToList(s);
            if(layouts.get(1)>= list.get(1)&&layouts.get(3)<=list.get(3)&&list.get(0)>nLayouts.get(0)){
                return s;
            }
        }
        return "";
    }

    private boolean isLine(String layout1, String layout2) {
        List<Integer> list1 = layoutStrToList(layout1);
        List<Integer> list2 = layoutStrToList(layout2);
        int y1 = list1.get(1);
        int y2 = list2.get(1);
        return Math.abs(y1 - y2) < lineDiff;
    }

    private boolean insertEdocGroup(boolean selfTsr, boolean bigTsr, String fn, Long relationId,
                                    List<Integer> tsrLayout, EdocGroupDto edocGroupDto,
                                    List<List<Integer>> tsrList, List<EdocGroupDto> edocGroupDtos, String dataType, String relationName, String selfLayout) {
        boolean prevSelfTsr = false;
        if (selfTsr) {
            prevSelfTsr = true;
            edocGroupDto.setName(fn);
            edocGroupDto.setType("cell");
            edocGroupDto.setRelationId(relationId);
            edocGroupDto.setRelationName(relationName);
            edocGroupDto.setLayout(tsrLayout);
            edocGroupDto.setDataType(dataType);
        } else if (bigTsr) {
            prevSelfTsr = true;
            // 判断之前的数据是否存入了此组
            if (tsrList.contains(tsrLayout)) {
                for (EdocGroupDto groupDto : edocGroupDtos) {
                    List<Integer> layout = groupDto.getLayout();
                    // 查询到当前组
                    if (layout.equals(tsrLayout)) {
                        List<EdocGroupDto> subGroups = groupDto.getEdocGroupDtos();
                        EdocGroupDto subGroup = new EdocGroupDto();
                        subGroup.setName(fn);
                        subGroup.setType("cell");
                        subGroup.setDataType("field");
                        subGroup.setRelationId(relationId);
                        subGroup.setRelationName(fn);
                        subGroup.setLayout(layoutStrToList(selfLayout));
                        subGroups.add(subGroup);
                    }
                }
            } else {
                edocGroupDto.setType("subGroup");
                edocGroupDto.setDataType("");
                edocGroupDto.setLayout(tsrLayout);
                List<EdocGroupDto> subGroups = edocGroupDto.getEdocGroupDtos();
                EdocGroupDto subGroup = new EdocGroupDto();
                subGroup.setName(fn);
                subGroup.setDataType("field");
                subGroup.setType("cell");
                subGroup.setRelationId(relationId);
                subGroup.setRelationName(fn);
                subGroup.setLayout(layoutStrToList(selfLayout));
                subGroups.add(subGroup);
            }

        } else {
            prevSelfTsr = false;
            edocGroupDto.setName(fn);
            edocGroupDto.setType("cell");
            edocGroupDto.setRelationId(relationId);
            edocGroupDto.setRelationName(relationName);
            edocGroupDto.setDataType(dataType);
            edocGroupDto.setLayout(tsrLayout);
        }
        return prevSelfTsr;
    }

    private Map<String, Object> tsrJudge(String layout, List<CellDto> cellDtos, String fv, String selefName) {
        Map<String, Object> map = new HashMap<>();
        // 是否有自己的表格线
        boolean selfTsr = false;
        // 是否有大表格线包含
        boolean bigTsr = false;
        List<Integer> ocrLyouts = layoutStrToList(layout);
        int x1 = ocrLyouts.get(0);
        int y1 = ocrLyouts.get(1);
        int x2 = ocrLyouts.get(2);
        int y2 = ocrLyouts.get(3);
        for (CellDto cellDto : cellDtos) {
            List<Double> location = cellDto.getLocation();
            int tsrX1 = (int) Math.round(location.get(0));
            int tsrY1 = (int) Math.round(location.get(1));
            int tsrX2 = (int) Math.round(location.get(2));
            int tsrY2 = (int) Math.round(location.get(3));
            List<Integer> li = new ArrayList<>();
            li.add(tsrX1);
            li.add(tsrY1);
            li.add(tsrX2);
            li.add(tsrY2);
            if (x1 == tsrX1 && y1 == tsrY1 && x2 == tsrX2 && y2 == tsrY2) {
                selfTsr = true;
                map.put("layout", li);
                break;
            } else {
                List<Map<String, Object>> contents = cellDto.getContents();
                boolean b = false;
                for (Map<String, Object> content : contents) {
                    Iterator<String> iterator = content.keySet().iterator();
                    while (iterator.hasNext()) {
                        String next = iterator.next();
                        List<Integer> list = layoutStrToList(next);
                        list.set(1, tsrY1);
                        list.set(3, tsrY2);
                        if (next.equals(layout) || layoutStrToList(layout).equals(list)) {
                            b = true;
                            break;
                        }
                    }
                }
                if (b || areBlocksOverlapped(ocrLyouts, li.toString())) {
                    if (contents.size() > 1) {
                        int size = contents.size();
                        for (Map<String, Object> stringObjectMap : contents) {
                            String valueKey = stringObjectMap.keySet().iterator().next();
                            String value = String.valueOf(stringObjectMap.get(valueKey));
                            if (value.equals("白") || value.equals("与") || value.equals("⊕")) {
                                size = size - 1;
                                if (size == 1) {
                                    map.put("layout", li);
                                    selfTsr = true;
                                    break;
                                }
                            }
                        }
                        if (size > 1) {
                            for (Map<String, Object> stringObjectMap : contents) {
                                String valueKey = stringObjectMap.keySet().iterator().next();
                                String value = String.valueOf(stringObjectMap.get(valueKey));
                                if (value.equals(selefName) || value.replaceFirst(":$", "").equals(selefName)) {
                                    continue;
                                }
                                if (!value.equals("白") && !value.equals("与") && !value.equals("⊕")) {
                                    if (!value.equals(fv) && !value.equals(selefName) && !selefName.equals("")) {
                                        bigTsr = true;
                                        selfTsr = false;
                                        map.put("layout", li);
                                        break;
                                    } else {
                                        if (selefName.equals("")) {
                                            map.put("layout", layoutStrToList(layout));
                                        } else {
                                            map.put("layout", li);
                                        }
                                        selfTsr = true;
//                                    break;
                                    }
                                }

                            }
                        }
                        map.put("layout", li);
                        break;
                    } else {
                        if (contents.size() == 1) {
                            Map<String, Object> stringObjectMap = contents.get(0);
                            map.put("layout", li);
                            String valueKey = stringObjectMap.keySet().iterator().next();
                            String value = String.valueOf(stringObjectMap.get(valueKey));
                            if (selefName.equals("") && value.equals(fv)) {
                                map.put("layout", li);
                                selfTsr = true;
                                break;
                            }
                        } else {
                            map.put("layout", li);
                            selfTsr = true;
                            break;
                        }

                    }


                }
                if (bigTsr || selfTsr) {
                    break;
                }
            }
        }
        if (!bigTsr && !selfTsr) {
            map.put("layout", ocrLyouts);
        }
        map.put("selfTsr", selfTsr);
        map.put("bigTsr", bigTsr);
        return map;
    }

    private boolean areBlocksOverlapped(List<Integer> ocrLyouts, String contentLayout) {
        List<Integer> list = layoutStrToList(contentLayout);
        Integer x1 = ocrLyouts.get(0);
        Integer y1 = ocrLyouts.get(1);
        Integer x2 = ocrLyouts.get(2);
        Integer y2 = ocrLyouts.get(3);
        Integer x3 = list.get(0);
        Integer y3 = list.get(1);
        Integer x4 = list.get(2);
        Integer y4 = list.get(3);
//        if (((x1 <= x3 && x3 <= x2) || (x1 <= x4 && x4 <= x2) || (x3 <= x1 && x1 <= x4) || (x3 <= x2 && x2 <= x4)) && isApproxEqual(y4, y2, y3, 5, 0.0)) {
        if (((x1 <= x3 && x4 <= x2) || (x1 >= x3 && x4 >= x2)) && isApproxEqual(y4, y1, y3, 5, 0.0)) {
            return true;
        }
//        if (((y1 <= y3 && y3 <= y2) || (y1 <= y4 && y4 <= y2) || (y3 <= y1 && y1 <= y4) || (y3 <= y2 && y2 <= y4)) && isApproxEqual(x4, x2, x3, 5, 0.0)) {
//            return true;
//        }
        return false;
    }

    private boolean isApproxEqual(int val1, int val2, int val3, int absTolerance, double relTolerance) {
        int absDiff = Math.abs(val3 - val2);
        double refDiff = (double) absDiff / Math.abs(val1 - val3);
        return absDiff < absTolerance || Double.compare(refDiff, relTolerance) == -1;

    }

    private List<Integer> layoutStrToList(String layout) {
        layout = layout.replaceAll(" ", "");
        return Arrays.stream(layout.substring(1, layout.length() - 1).split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private List<Integer> layoutProcess(List<EdocTableStructureDto> tableStructure) {
        List<Integer> list = new ArrayList<>();
        for (EdocTableStructureDto edocTableStructureDto : tableStructure) {
            String nLayout = edocTableStructureDto.getNLayout();
            List<Integer> nLayoutList = layoutStrToList(nLayout);
            list.add(nLayoutList.get(0));
        }
        return list;
    }


}
