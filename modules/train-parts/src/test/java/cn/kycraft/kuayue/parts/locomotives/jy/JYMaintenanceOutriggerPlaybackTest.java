package cn.kycraft.kuayue.parts.locomotives.jy;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JYMaintenanceOutriggerPlaybackTest {
    @Test
    void extendedDrivesTheOuttriggerClipToBothEndpointsWithoutOpen() {
        try (var fixture = fixture(false)) {
            assertFalse(fixture.state.get().hasProperty(BlockStateProperties.OPEN));
            assertNotNull(fixture.preparedModel.getModel().getAnimations().get("outtrigger_on"));
            assertNull(fixture.preparedModel.getModel().getAnimations().get("door_open"));
            fixture.tick();
            assertEquals(0, fixture.sample.get().time(), 1e-6);
            fixture.request(true);
            fixture.tick();
            fixture.ticks(5);
            assertTrue(fixture.state.get().getValue(JYMaintenanceOutriggerBlock.EXTENDED));
            assertEquals(0.25, fixture.sample.get().time(), 1e-6);
            fixture.request(false);
            fixture.tick();
            fixture.ticks(5);
            assertFalse(fixture.state.get().getValue(JYMaintenanceOutriggerBlock.EXTENDED));
            assertEquals(0, fixture.sample.get().time(), 1e-6);
        }
    }

    @Test
    void reversingAnOuTriggerReusesItsSessionAndCurrentSourceTime() {
        try (var fixture = fixture(false)) {
            var session = fixture.session;
            fixture.tick();
            fixture.request(true);
            fixture.tick();
            fixture.ticks(2);
            double before = fixture.sample.get().time();
            fixture.request(false);
            fixture.tick();
            double reversed = fixture.sample.get().time();
            assertTrue(Math.abs(reversed - before) <= 0.050001);
            assertTrue(reversed > 0 && reversed < 0.25);
            fixture.tick();
            assertTrue(fixture.sample.get().time() < reversed);
            assertSame(session, fixture.session);
        }
    }

    @Test
    void renderFramesDoNotAdvanceOrResampleTheOuttriggerClip() {
        try (var fixture = fixture(false)) {
            fixture.tick();
            fixture.request(true);
            fixture.tick();
            fixture.ticks(2);
            long revision = fixture.session.revision();
            int samples = fixture.poseSamples.get();
            for (int frame = 0; frame < 20; frame++) assertTrue(fixture.session.publishFrame(null));
            assertEquals(revision, fixture.session.revision());
            assertEquals(samples, fixture.poseSamples.get());
            assertEquals(20, fixture.renderSamples.get());
        }
    }

    private static JYMaintenanceDoorPlaybackTest.PlaybackFixture fixture(boolean extended) {
        return new JYMaintenanceDoorPlaybackTest.PlaybackFixture(
                JYContraptionOutriggerFixtures.outrigger.defaultBlockState()
                        .setValue(JYMaintenanceOutriggerBlock.EXTENDED, extended),
                JYMaintenanceOutriggerBlock.EXTENDED, "outtrigger_on", "outrigger", false);
    }
}
