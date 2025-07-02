package com.seeyon.ai.ocrprocess.form.response;


import com.seeyon.ai.ocrprocess.form.DataHistoryDto;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DataHistoryResponse {
    //    累计节省工时
    private Double totalSavedHours;
    //    任务总次数
    private Integer totalTasks;
    //    任务成功总次数
    private Integer completedTasks;
    //    任务失败总次数
    private Integer failedTasks;
    //    成功率
    private double successRate;
    //    生成元素总数
    private Long totalElements;
    //    平均每次耗时
    private double totalDurationSeconds;
    //    近一周任务量趋势
    private Map<String, Integer> weeklyTrend;
    //    助手使用分布
    private List<Map<String, Integer>> assistantDistribution;
    //    近期生成历史
    private List<DataHistoryDto> history;
}
