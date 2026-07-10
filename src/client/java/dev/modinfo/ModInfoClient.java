package dev.modinfo;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class ModInfoClient implements ClientModInitializer {
	public static final String MOD_ID = "mod-info";

	private static final int MARGIN = 6;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final long ANNOUNCEMENT_DURATION_MS = 3_000L;
	private static final long ANNOUNCEMENT_FADE_IN_MS = 350L;
	private static final long ANNOUNCEMENT_FADE_OUT_MS = 650L;
	private static final float ANNOUNCEMENT_SCALE = 2.0F;
	private static final int DAY_ANNOUNCEMENT_COLOR = 0xFFD84A;
	private static final int BIOME_ANNOUNCEMENT_COLOR = 0x55FFFF;

	private static OverlayConfig config;
	private static boolean visible = true;
	private static boolean toggleKeyWasDown;
	private static boolean technicalVisible;
	private static boolean technicalKeyWasDown;
	private static boolean manualAnnouncementKeyWasDown;
	private static ClientLevel trackedLevel;
	private static Identifier lastBiome;
	private static long lastDay = Long.MIN_VALUE;
	private static final Deque<Announcement> announcementQueue = new ArrayDeque<>();
	private static Announcement activeAnnouncement;
	private static long announcementStartedAt;

	@Override
	public void onInitializeClient() {
		config = OverlayConfig.load();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			updateAnnouncements(client);

			boolean toggleKeyDown = shortcutDown(client, config.toggleKey, config.toggleModifiers);
			if (client.gui.screen() == null && toggleKeyDown && !toggleKeyWasDown) {
				visible = !visible;
			}
			toggleKeyWasDown = toggleKeyDown;

			boolean technicalKeyDown = shortcutDown(client, config.technicalToggleKey,
					config.technicalToggleModifiers);
			if (client.gui.screen() == null && technicalKeyDown && !technicalKeyWasDown) {
				technicalVisible = !technicalVisible;
			}
			technicalKeyWasDown = technicalKeyDown;

			boolean manualKeyDown = shortcutDown(client, config.manualAnnouncementKey,
					config.manualAnnouncementModifiers);
			if (client.gui.screen() == null && manualKeyDown && !manualAnnouncementKeyWasDown) {
				queueManualAnnouncements(client);
			}
			manualAnnouncementKeyWasDown = manualKeyDown;
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof OptionsScreen) {
				int buttonY = findFreeSettingsButtonY(Screens.getWidgets(screen), scaledHeight);
				Screens.getWidgets(screen).add(Button.builder(
						Component.literal("Mod Info Settings…"),
						button -> client.gui.setScreen(new ModInfoConfigScreen(screen))
				).bounds(6, buttonY, 120, 20).build());
			}
		});

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "information_overlay"),
				ModInfoClient::renderOverlay
		);
	}

	private static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null || minecraft.gui.hud.isHidden()) {
			return;
		}

		renderAnnouncement(graphics, minecraft);

		if (!visible) {
			return;
		}
		BlockPos position = minecraft.player.blockPosition();
		List<String> lines = new ArrayList<>(10);
		String facing = facingDirection(minecraft.player.getYRot());
		if (config.showCoordinates) {
			String values = "%d / %d / %d".formatted(position.getX(), position.getY(), position.getZ());
			String coordinates = config.showFieldLabels ? "XYZ: " + values : values;
			if (config.showFacing && config.facingOnCoordinatesLine) {
				coordinates += " • " + formatFacing(facing);
			}
			lines.add(coordinates);
		}
		if (config.showFacing && (!config.facingOnCoordinatesLine || !config.showCoordinates)) {
			lines.add(formatFacing(facing));
		}
		if (config.showBiome) {
			String biomeName = minecraft.level.getBiome(position)
					.unwrapKey()
					.map(key -> readableName(key.identifier().getPath()))
					.orElse("Unknown");
			lines.add(config.showFieldLabels ? "Biome: " + biomeName : biomeName);
		}
		if (config.showDaysPlayed) {
			long day = Math.floorDiv(minecraft.level.getOverworldClockTime(), 24_000L) + 1L;
			lines.add(config.showFieldLabels ? "Days played: " + day : Long.toString(day));
		}
		if (config.showClock) {
			lines.add(formatClock(minecraft));
		}
		if (technicalVisible) {
			addTechnicalLines(lines, minecraft, position);
		}
		if (lines.isEmpty()) {
			return;
		}

		double fontScale = config.fontSize / 100.0;
		double boxScale = config.boxSize / 100.0;
		int padding = Math.max(1, (int) Math.round(5 * boxScale));
		int lineGap = Math.max(0, (int) Math.round(2 * boxScale));
		int scaledFontHeight = Math.max(1, (int) Math.ceil(9 * fontScale));
		int contentWidth = 0;
		for (String line : lines) {
			contentWidth = Math.max(contentWidth, (int) Math.ceil(minecraft.font.width(line) * fontScale));
		}

		int boxWidth = contentWidth + padding * 2;
		int boxHeight = scaledFontHeight * lines.size() + lineGap * (lines.size() - 1) + padding * 2;
		int x = horizontalPosition(config.position, graphics.guiWidth(), boxWidth);
		int y = verticalPosition(config.position, graphics.guiHeight(), boxHeight);

		if (config.showBackground && config.backgroundOpacity > 0) {
			int alpha = (int) Math.round(config.backgroundOpacity * 255.0 / 100.0);
			graphics.fill(x, y, x + boxWidth, y + boxHeight, alpha << 24);
		}

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + padding, y + padding);
		graphics.pose().scale((float) fontScale, (float) fontScale);
		for (int i = 0; i < lines.size(); i++) {
			int textY = (int) Math.round(i * (scaledFontHeight + lineGap) / fontScale);
			int scaledLineWidth = (int) Math.ceil(minecraft.font.width(lines.get(i)) * fontScale);
			int actualOffset = switch (config.textAlignment) {
				case LEFT -> 0;
				case CENTER -> (contentWidth - scaledLineWidth) / 2;
				case RIGHT -> contentWidth - scaledLineWidth;
			};
			int textX = (int) Math.round(actualOffset / fontScale);
			graphics.text(
					minecraft.font,
					lines.get(i),
					textX,
					textY,
					TEXT_COLOR,
					true
			);
		}
		graphics.pose().popMatrix();
	}

	private static void updateAnnouncements(Minecraft minecraft) {
		if (minecraft.level == null || minecraft.player == null) {
			resetAnnouncementTracking();
			return;
		}

		BlockPos position = minecraft.player.blockPosition();
		Identifier currentBiome = minecraft.level.getBiome(position)
				.unwrapKey()
				.map(key -> key.identifier())
				.orElse(null);
		long currentDay = Math.floorDiv(minecraft.level.getOverworldClockTime(), 24_000L) + 1L;

		if (minecraft.level != trackedLevel) {
			boolean firstWorldLoad = trackedLevel == null;
			if (!firstWorldLoad && currentBiome != null && !currentBiome.equals(lastBiome)
					&& config.announceNewBiome) {
				queueAnnouncement(new Announcement(
						formatBiomeAnnouncement(minecraft, currentBiome),
						BIOME_ANNOUNCEMENT_COLOR
				));
			}
			trackedLevel = minecraft.level;
			lastBiome = currentBiome;
			lastDay = currentDay;
			if (firstWorldLoad) {
				announcementQueue.clear();
				activeAnnouncement = null;
			}
			return;
		}

		if (currentDay != lastDay) {
			if (config.announceNewDay) {
				queueAnnouncement(new Announcement("Day " + currentDay, DAY_ANNOUNCEMENT_COLOR));
			}
			lastDay = currentDay;
		}

		if (currentBiome != null && !currentBiome.equals(lastBiome)) {
			if (config.announceNewBiome) {
				queueAnnouncement(new Announcement(
						formatBiomeAnnouncement(minecraft, currentBiome),
						BIOME_ANNOUNCEMENT_COLOR
				));
			}
			lastBiome = currentBiome;
		}
	}

	private static void queueAnnouncement(Announcement announcement) {
		if (announcementQueue.size() < 4) {
			announcementQueue.addLast(announcement);
		}
	}

	private static void queueManualAnnouncements(Minecraft minecraft) {
		if (minecraft.level == null || minecraft.player == null) {
			return;
		}
		if (config.manualAnnouncementContent == OverlayConfig.ManualAnnouncementContent.DAY
				|| config.manualAnnouncementContent == OverlayConfig.ManualAnnouncementContent.BOTH) {
			long day = Math.floorDiv(minecraft.level.getOverworldClockTime(), 24_000L) + 1L;
			queueAnnouncement(new Announcement("Day " + day, DAY_ANNOUNCEMENT_COLOR));
		}
		if (config.manualAnnouncementContent == OverlayConfig.ManualAnnouncementContent.BIOME
				|| config.manualAnnouncementContent == OverlayConfig.ManualAnnouncementContent.BOTH) {
			minecraft.level.getBiome(minecraft.player.blockPosition()).unwrapKey().ifPresent(key ->
					queueAnnouncement(new Announcement(
							formatBiomeAnnouncement(minecraft, key.identifier()),
							BIOME_ANNOUNCEMENT_COLOR
					)));
		}
	}

	private static String formatBiomeAnnouncement(Minecraft minecraft, Identifier biome) {
		StringBuilder text = new StringBuilder();
		if (config.biomeAnnouncementShowEntering) {
			text.append("Entering ");
		}
		if (config.biomeAnnouncementShowDimension && minecraft.level != null) {
			text.append(readableName(minecraft.level.dimension().identifier().getPath())).append(": ");
		}
		return text.append(readableName(biome.getPath())).toString();
	}

	private static void resetAnnouncementTracking() {
		trackedLevel = null;
		lastBiome = null;
		lastDay = Long.MIN_VALUE;
		announcementQueue.clear();
		activeAnnouncement = null;
	}

	private static void renderAnnouncement(GuiGraphicsExtractor graphics, Minecraft minecraft) {
		long now = Util.getMillis();
		if (activeAnnouncement == null) {
			activeAnnouncement = announcementQueue.pollFirst();
			announcementStartedAt = now;
		}
		if (activeAnnouncement == null) {
			return;
		}

		long elapsed = now - announcementStartedAt;
		if (elapsed >= ANNOUNCEMENT_DURATION_MS) {
			activeAnnouncement = announcementQueue.pollFirst();
			announcementStartedAt = now;
			if (activeAnnouncement == null) {
				return;
			}
			elapsed = 0L;
		}

		float opacity = announcementOpacity(elapsed);
		int textAlpha = Math.max(0, Math.min(255, Math.round(opacity * 255.0F)));
		int backgroundAlpha = Math.max(0, Math.min(160, Math.round(opacity * 160.0F)));
		int scaledWidth = (int) Math.ceil(minecraft.font.width(activeAnnouncement.text()) * ANNOUNCEMENT_SCALE);
		int scaledHeight = (int) Math.ceil(9 * ANNOUNCEMENT_SCALE);
		int x = (graphics.guiWidth() - scaledWidth) / 2;
		int y = (graphics.guiHeight() - scaledHeight) / 2;
		int paddingX = 8;
		int paddingY = 5;

		graphics.fill(
				x - paddingX,
				y - paddingY,
				x + scaledWidth + paddingX,
				y + scaledHeight + paddingY,
				backgroundAlpha << 24
		);

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(ANNOUNCEMENT_SCALE, ANNOUNCEMENT_SCALE);
		graphics.text(
				minecraft.font,
				activeAnnouncement.text(),
				0,
				0,
				(textAlpha << 24) | activeAnnouncement.rgbColor(),
				true
		);
		graphics.pose().popMatrix();
	}

	private static float announcementOpacity(long elapsed) {
		if (elapsed < ANNOUNCEMENT_FADE_IN_MS) {
			return smoothStep(elapsed / (float) ANNOUNCEMENT_FADE_IN_MS);
		}
		long fadeOutStart = ANNOUNCEMENT_DURATION_MS - ANNOUNCEMENT_FADE_OUT_MS;
		if (elapsed > fadeOutStart) {
			return smoothStep((ANNOUNCEMENT_DURATION_MS - elapsed) / (float) ANNOUNCEMENT_FADE_OUT_MS);
		}
		return 1.0F;
	}

	private static float smoothStep(float value) {
		float clamped = Math.max(0.0F, Math.min(1.0F, value));
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	public static OverlayConfig config() {
		return config;
	}

	public static void applyConfig(OverlayConfig updated) {
		config = updated.copy();
		config.save();
		toggleKeyWasDown = false;
		technicalKeyWasDown = false;
		manualAnnouncementKeyWasDown = false;
	}

	private static boolean shortcutDown(Minecraft minecraft, int key, int modifiers) {
		if (!InputConstants.isKeyDown(minecraft.getWindow(), key)) {
			return false;
		}
		if ((modifiers & InputConstants.MOD_CONTROL) != 0
				&& !eitherKeyDown(minecraft, InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL)) {
			return false;
		}
		if ((modifiers & InputConstants.MOD_SHIFT) != 0
				&& !eitherKeyDown(minecraft, InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT)) {
			return false;
		}
		return (modifiers & InputConstants.MOD_ALT) == 0
				|| eitherKeyDown(minecraft, InputConstants.KEY_LALT, InputConstants.KEY_RALT);
	}

	private static boolean eitherKeyDown(Minecraft minecraft, int left, int right) {
		return InputConstants.isKeyDown(minecraft.getWindow(), left)
				|| InputConstants.isKeyDown(minecraft.getWindow(), right);
	}

	private static String formatFacing(String facing) {
		if (!config.showFieldLabels) {
			return facing;
		}
		return config.headingLabel.label() + ": " + facing;
	}

	private static String facingDirection(float yaw) {
		String[] directions = {
				"South", "Southwest", "West", "Northwest",
				"North", "Northeast", "East", "Southeast"
		};
		int index = Math.floorMod((int) Math.floor(yaw / 45.0F + 0.5F), 8);
		return directions[index];
	}

	private static String formatClock(Minecraft minecraft) {
		long time = Math.floorMod(minecraft.level.getOverworldClockTime(), 24_000L);
		long hour = (time / 1_000L + 6L) % 24L;
		long minute = (time % 1_000L) * 60L / 1_000L;
		StringBuilder value = new StringBuilder("%02d:%02d".formatted(hour, minute));
		if (config.clockShowDayNight) {
			value.append(" • ").append(time < 13_000L ? "Day" : "Night");
		}
		if (config.clockShowWeather) {
			String weather = minecraft.level.isThundering() ? "Thunder"
					: minecraft.level.isRaining() ? "Rain" : "Clear";
			value.append(" • ").append(weather);
		}
		return config.showFieldLabels ? "Time: " + value : value.toString();
	}

	private static void addTechnicalLines(List<String> lines, Minecraft minecraft, BlockPos position) {
		if (config.technicalShowLight) {
			int block = minecraft.level.getBrightness(LightLayer.BLOCK, position);
			int sky = minecraft.level.getBrightness(LightLayer.SKY, position);
			String value = block + " block / " + sky + " sky";
			lines.add(config.technicalShowLabels ? "Light: " + value : value);
		}
		int chunkX = Math.floorDiv(position.getX(), 16);
		int chunkZ = Math.floorDiv(position.getZ(), 16);
		if (config.technicalShowChunk) {
			String value = "%d, %d • local %d, %d".formatted(
					chunkX, chunkZ, Math.floorMod(position.getX(), 16), Math.floorMod(position.getZ(), 16));
			lines.add(config.technicalShowLabels ? "Chunk: " + value : value);
		}
		if (config.technicalShowSlimeChunk) {
			String slimeStatus = slimeChunkStatus(minecraft, chunkX, chunkZ);
			if (config.technicalShowLabels) {
				lines.add("Slime chunk: " + slimeStatus);
			} else if ("Yes".equals(slimeStatus)) {
				lines.add("Slimes");
			} else if ("No".equals(slimeStatus)) {
				lines.add("No Slimes");
			} else {
				lines.add(slimeStatus);
			}
		}
	}

	private static String slimeChunkStatus(Minecraft minecraft, int chunkX, int chunkZ) {
		if (minecraft.getSingleplayerServer() == null) {
			return "Unavailable (server seed unknown)";
		}
		long seed = minecraft.getSingleplayerServer().getWorldGenSettings().options().seed();
		boolean slimeChunk = WorldgenRandom.seedSlimeChunk(chunkX, chunkZ, seed, 987234911L).nextInt(10) == 0;
		return slimeChunk ? "Yes" : "No";
	}

	private static int findFreeSettingsButtonY(List<AbstractWidget> widgets, int screenHeight) {
		for (int candidateY = 6; candidateY <= screenHeight - 26; candidateY += 24) {
			boolean overlaps = false;
			for (AbstractWidget widget : widgets) {
				if (widget.visible && rectanglesOverlap(6, candidateY, 120, 20,
						widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight())) {
					overlaps = true;
					break;
				}
			}
			if (!overlaps) {
				return candidateY;
			}
		}
		return 6;
	}

	private static boolean rectanglesOverlap(int x1, int y1, int width1, int height1,
			int x2, int y2, int width2, int height2) {
		return x1 < x2 + width2 && x1 + width1 > x2 && y1 < y2 + height2 && y1 + height1 > y2;
	}

	private static int horizontalPosition(OverlayConfig.Position position, int screenWidth, int boxWidth) {
		return switch (position) {
			case TOP_CENTER, CENTER, BOTTOM_CENTER -> (screenWidth - boxWidth) / 2;
			case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> screenWidth - boxWidth - MARGIN;
			default -> MARGIN;
		};
	}

	private static int verticalPosition(OverlayConfig.Position position, int screenHeight, int boxHeight) {
		return switch (position) {
			case CENTER_LEFT, CENTER, CENTER_RIGHT -> (screenHeight - boxHeight) / 2;
			case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - boxHeight - MARGIN;
			default -> MARGIN;
		};
	}

	static String readableName(String path) {
		String[] words = path.split("_");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (!result.isEmpty()) {
				result.append(' ');
			}
			result.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
			result.append(word.substring(1));
		}
		return result.toString();
	}

	private record Announcement(String text, int rgbColor) {
	}
}
