package cn.kycraft.kuayue.content.scheduling.transportancy.dispatch.log;

import lombok.Data;

@Data()
public class DispatchContractRecordNumberRecord {
    private Long interlockingAcknowledged;

    private Long bankerReturn;

    private Long interlockingBlockCancel;

    private Long departureShunting;

    private Long departureShuntingFinish;
}
