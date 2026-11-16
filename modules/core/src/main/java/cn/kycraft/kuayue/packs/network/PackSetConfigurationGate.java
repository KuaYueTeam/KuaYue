package cn.kycraft.kuayue.packs.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/** Configuration task that rejects clients whose final pack set differs from the server. */
public final class PackSetConfigurationGate implements ICustomConfigurationTask {
    public static final ConfigurationTask.Type TYPE =
            new ConfigurationTask.Type("kuayue:pack_set");
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.configurationToClient(
                PackSetPayloads.QUERY_TYPE,
                PackSetPayloads.Query.STREAM_CODEC,
                PackSetConfigurationGate::onQuery
        );
        registrar.configurationToServer(
                PackSetPayloads.DECLARATION_TYPE,
                PackSetPayloads.Declaration.STREAM_CODEC,
                PackSetConfigurationGate::onDeclaration
        );
    }

    private static void onQuery(PackSetPayloads.Query payload, IPayloadContext context) {
        try {
            context.reply(new PackSetPayloads.Declaration(PackSetDeclaration.current()));
        } catch (RuntimeException failure) {
            context.disconnect(Component.translatable("kuayue.packs.network.not_ready"));
        }
    }

    private static void onDeclaration(
            PackSetPayloads.Declaration payload,
            IPayloadContext context
    ) {
        PackSetDeclaration server;
        try {
            server = PackSetDeclaration.current();
        } catch (RuntimeException failure) {
            context.disconnect(Component.translatable("kuayue.packs.network.not_ready"));
            return;
        }
        if (!server.compatibleWith(payload.declaration())) {
            context.disconnect(Component.translatable(
                    "kuayue.packs.network.mismatch",
                    server.entries().toString(),
                    payload.declaration().entries().toString()
            ));
            return;
        }
        context.finishCurrentTask(TYPE);
    }

    @Override
    public void run(Consumer<net.minecraft.network.protocol.common.custom.CustomPacketPayload> sender) {
        sender.accept(PackSetPayloads.Query.INSTANCE);
    }

    @Override
    public ConfigurationTask.Type type() {
        return TYPE;
    }
}
