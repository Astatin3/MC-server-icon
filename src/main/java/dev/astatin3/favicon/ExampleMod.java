package dev.astatin3.favicon;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.literal;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "favicon";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

//		LOGGER.info("Hello Fabric world!");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("favicon")
            .executes(context -> {
                // For versions below 1.19, replace "Text.literal" with "new LiteralText".
                // For versions below 1.20, remode "() ->" directly.

                ServerPlayerEntity player = context.getSource().getPlayer();
                ItemStack mainHandStack = player.getMainHandStack();
                if(mainHandStack == null) {
                    context.getSource().sendFeedback(() -> Text.literal("Error getting held item"), false);
                    return 1;
                }
                Item item = mainHandStack.getItem();
                if(!Objects.equals(item.toString(), "minecraft:filled_map")) {
                    context.getSource().sendFeedback(() -> Text.literal("Invalid held item. You must be holding a Filled Map"), false);
                    return 1;
                }


                MapIdComponent mapIdComponent = mainHandStack.get(DataComponentTypes.MAP_ID);
                MapState mapState =  player.getWorld().getMapState(mapIdComponent);

                if(mapState == null) {
                    context.getSource().sendFeedback(() -> Text.literal("Error getting map data"), false);
                    return 1;
                }

                MinecraftServer server = player.getServer();

                ServerMetadata metadata = server.getServerMetadata();

                if(metadata == null) {
                    context.getSource().sendFeedback(() -> Text.literal("Error getting server metadata"), false);
                    return 1;
                }

                context.getSource().sendFeedback(() -> Text.literal("Encoding icon data..."), false);


//                for (int y = 0; y < 64; y++) {
//                    String text = "";
//                    for (int x = 0; x < 64; x++) {
//                        text += mapState.colors[y * 128 + x] + ", ";
//                    }
//                    String finalText = text;
//                    context.getSource().sendFeedback(() -> Text.literal(finalText), false);
//                }

                BufferedImage bufImg = convertImage(mapState.colors);
                byte[] favicon = toBytes(bufImg);

                if(favicon == null) {
                    context.getSource().sendFeedback(() -> Text.literal("Error encoding icon"), false);
                    return 1;
                }

                context.getSource().sendFeedback(() -> Text.literal("Setting current server icon..."), false);

                ServerMetadata newMetadata = new ServerMetadata(
                        metadata.description(),
                        metadata.players(),
                        metadata.version(),
                        Optional.of(new ServerMetadata.Favicon(favicon)),
                        metadata.secureChatEnforced()
                );

                ((MetadataAccessor) server).setServerMetadata(newMetadata);

                context.getSource().sendFeedback(() -> Text.literal("Saving new icon file..."), false);

                Path iconpath = server.getPath("server-icon.png");
                try {
                    FileUtils.writeByteArrayToFile(iconpath.toFile(), favicon);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                context.getSource().sendFeedback(() -> Text.literal("Done!"), false);

                context.getSource().sendFeedback(() -> Text.literal("Current icon: " + server.getServerMetadata().favicon().get().iconBytes().length + " bytes"), false);


                return 1;
            })));



//        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("motd")
//            .then(CommandManager.argument("MOTD", StringArgumentType.greedyString())
//            .executes(context -> {
//
//                ServerPlayerEntity player = context.getSource().getPlayer();
//                MinecraftServer server = player.getServer();
//                ServerMetadata metadata = server.getServerMetadata();
//
//                if(metadata == null) {
//                    context.getSource().sendFeedback(() -> Text.literal("Error getting server metadata"), false);
//                    return 1;
//                }
//
//                context.getSource().sendFeedback(() -> Text.literal("Setting server metadata..."), false);
//                context.getSource().sendFeedback(() -> Text.literal("Preview:"), false);
//
//                String motdText = StringArgumentType.getString(context, "MOTD");
//                Text motd = Text.of(motdText);
//
//                context.getSource().sendFeedback(() -> motd, false);
//
//                ServerMetadata newMetadata = new ServerMetadata(
//                        motd,
//                        metadata.players(),
//                        metadata.version(),
//                        metadata.favicon(),
//                        metadata.secureChatEnforced()
//                );
//
//                ((MetadataAccessor) server).setServerMetadata(newMetadata);
//
//
//            return 1;
//        }))));
    }

    private BufferedImage convertImage(byte[] mapBytes) {

        if (mapBytes.length != 16384) {
            throw new IllegalArgumentException("Image data must be exactly 16384 bytes (128x128)");
        }

        // Create cropped image data (64x64 = 4096 bytes)
//        byte[] croppedData = new byte[64 * 64];

        BufferedImage bufferedImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);

        // Copy top-left 64x64 quadrant
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                bufferedImage.setRGB(x, y, mapByteToColor(mapBytes[y * 128 + x]));
            }
        }

        return bufferedImage;
    }

    private byte[] toBytes(BufferedImage bufferedImage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "PNG", baos);
        } catch (Exception e) {
            return null;
        }

        return baos.toByteArray();
    }

    // https://minecraft.wiki/w/Map_item_format
    private static final int[][] BASE_COLORS = {
            {0, 0, 0},       // 0: None/Transparent
            {127, 178, 56},  // 1: Grass
            {247, 233, 163}, // 2: Sand
            {199, 199, 199}, // 3: Wool
            {255, 0, 0},     // 4: Fire
            {160, 160, 255}, // 5: Ice
            {167, 167, 167}, // 6: Metal
            {0, 124, 0},     // 7: Plant
            {255, 255, 255}, // 8: Snow
            {164, 168, 184}, // 9: Clay
            {151, 109, 77},  // 10: Dirt
            {112, 112, 112}, // 11: Stone
            {64, 64, 255},   // 12: Water
            {143, 119, 72},  // 13: Wood
            {255, 252, 245}, // 14: Quartz
            {216, 127, 51},  // 15: Color Orange
            {178, 76, 216},  // 16: Color Magenta
            {102, 153, 216}, // 17: Color Light Blue
            {229, 229, 51},  // 18: Color Yellow
            {127, 204, 25},  // 19: Color Light Green
            {242, 127, 165}, // 20: Color Pink
            {76, 76, 76},    // 21: Color Gray
            {153, 153, 153}, // 22: Color Light Gray
            {76, 127, 153},  // 23: Color Cyan
            {127, 63, 178},  // 24: Color Purple
            {51, 76, 178},   // 25: Color Blue
            {102, 76, 51},   // 26: Color Brown
            {102, 127, 51},  // 27: Color Green
            {153, 51, 51},   // 28: Color Red
            {25, 25, 25},    // 29: Color Black
            {250, 238, 77},  // 30: Gold
            {92, 219, 213},  // 31: Diamond
            {74, 128, 255},  // 32: Lapis
            {0, 217, 58},    // 33: Emerald
            {129, 86, 49},   // 34: Podzol
            {112, 2, 0},     // 35: Nether
            {209, 177, 161}, // 36: Terracotta White
            {159, 82, 36},   // 37: Terracotta Orange
            {149, 87, 108},  // 38: Terracotta Magenta
            {112, 108, 138}, // 39: Terracotta Light Blue
            {186, 133, 36},  // 40: Terracotta Yellow
            {103, 117, 53},  // 41: Terracotta Light Green
            {160, 77, 78},   // 42: Terracotta Pink
            {57, 41, 35},    // 43: Terracotta Gray
            {135, 107, 98},  // 44: Terracotta Light Gray
            {87, 92, 92},    // 45: Terracotta Cyan
            {122, 73, 88},   // 46: Terracotta Purple
            {76, 62, 92},    // 47: Terracotta Blue
            {76, 50, 35},    // 48: Terracotta Brown
            {76, 82, 42},    // 49: Terracotta Green
            {142, 60, 46},   // 50: Terracotta Red
            {37, 22, 16},    // 51: Terracotta Black
            {189, 48, 49},   // 52: Crimson Nylium
            {148, 63, 97},   // 53: Crimson Stem
            {92, 25, 29},    // 54: Crimson Hyphae
            {22, 126, 134},  // 55: Warped Nylium
            {58, 142, 140},  // 56: Warped Stem
            {86, 44, 62},    // 57: Warped Hyphae
            {20, 180, 133},  // 58: Warped Wart Block
            {100, 100, 100}, // 59: Deepslate
            {216, 175, 147}, // 60: Raw Iron
            {127, 167, 150}  // 61: Glow Lichen
    };

    // Brightness multipliers for each shade level
    private static final double[] BRIGHTNESS_MULTIPLIERS = {
            180.0 / 255.0, // 0: Darkest
            220.0 / 255.0, // 1: Dark
            255.0 / 255.0, // 2: Normal (brightest)
            135.0 / 255.0  // 3: Very dark
    };

    public static int mapByteToColor(byte mapByte) {
        int unsignedByte = mapByte & 0xFF;

        // Color ID 0 is transparent
        if (unsignedByte == 0) {
            return 0;
        }

        // Decode base color and brightness level
        int baseColorIndex = (unsignedByte - 1) / 4;
        int brightnessLevel = (unsignedByte - 1) % 4;

        // Validate base color index
        if (baseColorIndex < 0 || baseColorIndex >= BASE_COLORS.length) {
            return 0; // Return transparent for invalid colors
        }
        // Get base color RGB values
        int[] baseRgb = BASE_COLORS[baseColorIndex];
        double multiplier = BRIGHTNESS_MULTIPLIERS[brightnessLevel];

        // Apply brightness multiplier
        int r = (int) Math.round(baseRgb[0] * multiplier);
        int g = (int) Math.round(baseRgb[1] * multiplier);
        int b = (int) Math.round(baseRgb[2] * multiplier);

        // Clamp values to 0-255 range
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        // Combine into single RGB integer
        return (r << 16) | (g << 8) | b;
    }

//    public static int convertRgbByteToInt(byte rgbByte) {
//        // Convert byte to unsigned int (0-255 range)
//        int unsignedByte = rgbByte & 0xFF;
//
//        // Extract the color components using bit masks and shifts
//        int r = (unsignedByte >> 5) & 0x07;  // Get top 3 bits (RRR)
//        int g = (unsignedByte >> 2) & 0x07;  // Get middle 3 bits (GGG)
//        int b = unsignedByte & 0x03;         // Get bottom 2 bits (BB)
//
//        // Scale the values to full 8-bit range (0-255)
//        // 3 bits (0-7) -> 8 bits (0-255): multiply by 255/7 ≈ 36.43
//        // 2 bits (0-3) -> 8 bits (0-255): multiply by 255/3 = 85
//        r = (r * 255) / 7;  // Scale 3-bit to 8-bit
//        g = (g * 255) / 7;  // Scale 3-bit to 8-bit
//        b = (b * 255) / 3;  // Scale 2-bit to 8-bit
//
//        // Combine into ARGB format: 0xAARRGGBB
//        // Alpha = 255 (0xFF), fully opaque
//        int argb = (255 << 24) | (r << 16) | (g << 8) | b;
//
//        return argb;
//    }
}