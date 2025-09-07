package dev.astatin3.favicon.mixin;

import dev.astatin3.favicon.MetadataAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(MinecraftServer.class)
public class ExampleMixin implements MetadataAccessor {
    @Shadow @Nullable private ServerMetadata metadata;
    @Shadow @Nullable private ServerMetadata.Favicon favicon;

    @Override
    public void setServerMetadata(ServerMetadata metadata) {
        this.metadata = metadata;
        this.favicon = metadata.favicon().orElse(null);
    }
}