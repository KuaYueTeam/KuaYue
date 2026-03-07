package cn.kycraft.kuayue.content.scheduling.transportancy.dispatch.log;

import lombok.Data;

@Data()
public class GameScheduleTime {
    private GameTime scheduled;
    private GameTime actual;
}
