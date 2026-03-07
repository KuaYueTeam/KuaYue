package cn.kycraft.kuayue.content.scheduling.transportancy.dispatch.log;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

// Reference: https://blog.csdn.net/weixin_67378178/article/details/145840450
@Data()
public class DispatchDepartureLog {
    private String trainNumber; // 列车车次
    private String track; // 接车股道

    /* 接车部分 */
    private GameTime arrivalDepartureAccepted; // 同意邻站发车
    private GameTime arrivalDeparture; // 邻站出发
    private GameScheduleTime arrival; // 本站到达
    private HashMap<CarriageChangeDispatchInfo.CarriageType, CarriageChangeDispatchInfo> carriageDetaching; // 解挂
    private String arrivalInterlockingTicketing; // 联锁票(占用区间凭证号码)
    private DispatchContractRecordNumberRecord arrivalDispatchRecordNumber; // 电话记录号码

    private String arrivalNote; // 记事

    /* 发车部分 */
    private GameTime departureAccepted; // 发车同意
    private GameScheduleTime departure; // 本站发车
    private GameTime neighborArrival; // 邻站到达
    private List<CarriageChangeDispatchInfo.CarriageType> carriageAttaching; // 挂车
    private Double departureLong; // 发车换长
    private Double totalWeight; // 发车总重
    private DispatchContractRecordNumberRecord departureDispatchRecordNumber; // 电话记录号码

    private GameTime leadingShuntingTime;
    private String note;
}
