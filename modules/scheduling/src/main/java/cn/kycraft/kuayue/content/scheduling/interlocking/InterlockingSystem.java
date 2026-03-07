package cn.kycraft.kuayue.content.scheduling.interlocking;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;

@Context()
public class InterlockingSystem {
    @PostConstruct()
    public void onPostConstruction(){
        InterlockingBlocks.init();
    }
}
