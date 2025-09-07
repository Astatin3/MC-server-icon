package dev.astatin3.favicon;

import net.minecraft.server.ServerMetadata;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface MetadataAccessor {
    default void setServerMetadata(ServerMetadata metadata) {}
}
