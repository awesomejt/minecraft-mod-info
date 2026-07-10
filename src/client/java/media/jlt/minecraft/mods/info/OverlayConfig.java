package media.jlt.minecraft.mods.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OverlayConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModInfoClient.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mod-info.json");
	private static final int CURRENT_CONFIG_VERSION = 2;

	public int configVersion = CURRENT_CONFIG_VERSION;
	public boolean showCoordinates = true;
	public boolean showBiome = true;
	public boolean showDaysPlayed = true;
	public boolean showFieldLabels = true;
	public boolean showFacing = false;
	public boolean facingOnCoordinatesLine = true;
	public HeadingLabel headingLabel = HeadingLabel.FACING;
	public boolean showClock = false;
	public boolean clockShowDayNight = true;
	public boolean clockShowWeather = true;
	public boolean announceNewDay = true;
	public boolean announceNewBiome = false;
	public boolean biomeAnnouncementShowDimension = false;
	public boolean biomeAnnouncementShowEntering = true;
	public ManualAnnouncementContent manualAnnouncementContent = ManualAnnouncementContent.BOTH;
	public int manualAnnouncementKey = InputConstants.KEY_N;
	public int manualAnnouncementModifiers = InputConstants.MOD_CONTROL;
	public boolean technicalShowLight = true;
	public boolean technicalShowChunk = true;
	public boolean technicalShowSlimeChunk = false;
	public boolean technicalShowLabels = true;
	public int technicalToggleKey = InputConstants.KEY_I;
	public int technicalToggleModifiers = InputConstants.MOD_CONTROL;
	public int frameRateToggleKey = -1;
	public int frameRateToggleModifiers = 0;
	public Map<String, ServerSeedProfiles> serverSeedOverrides = new LinkedHashMap<>();
	public boolean showBackground = true;
	public int backgroundOpacity = 56;
	public int boxSize = 100;
	public int fontSize = 100;
	public TextAlignment textAlignment = TextAlignment.LEFT;
	public int toggleKey = -1;
	public int toggleModifiers = 0;
	public Position position = Position.TOP_LEFT;

	public static OverlayConfig load() {
		if (!Files.isRegularFile(CONFIG_PATH)) {
			return new OverlayConfig();
		}

		try {
			JsonObject json;
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				json = JsonParser.parseReader(reader).getAsJsonObject();
			}
			boolean legacyConfig = !json.has("configVersion");
			OverlayConfig loaded = GSON.fromJson(json, OverlayConfig.class);
			if (loaded == null) {
				return new OverlayConfig();
			}
			if (legacyConfig && loaded.toggleKey == InputConstants.KEY_O && loaded.toggleModifiers == 0) {
				loaded.toggleKey = -1;
			}
			loaded.configVersion = CURRENT_CONFIG_VERSION;
			loaded.normalized();
			if (legacyConfig) {
				loaded.save();
			}
			return loaded;
		} catch (Exception exception) {
			LOGGER.warn("Could not read {}; using defaults", CONFIG_PATH, exception);
			return new OverlayConfig();
		}
	}

	public void save() {
		normalized();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			LOGGER.error("Could not save {}", CONFIG_PATH, exception);
		}
	}

	public OverlayConfig copy() {
		OverlayConfig copy = new OverlayConfig();
		copy.configVersion = configVersion;
		copy.showCoordinates = showCoordinates;
		copy.showBiome = showBiome;
		copy.showDaysPlayed = showDaysPlayed;
		copy.showFieldLabels = showFieldLabels;
		copy.showFacing = showFacing;
		copy.facingOnCoordinatesLine = facingOnCoordinatesLine;
		copy.headingLabel = headingLabel;
		copy.showClock = showClock;
		copy.clockShowDayNight = clockShowDayNight;
		copy.clockShowWeather = clockShowWeather;
		copy.announceNewDay = announceNewDay;
		copy.announceNewBiome = announceNewBiome;
		copy.biomeAnnouncementShowDimension = biomeAnnouncementShowDimension;
		copy.biomeAnnouncementShowEntering = biomeAnnouncementShowEntering;
		copy.manualAnnouncementContent = manualAnnouncementContent;
		copy.manualAnnouncementKey = manualAnnouncementKey;
		copy.manualAnnouncementModifiers = manualAnnouncementModifiers;
		copy.technicalShowLight = technicalShowLight;
		copy.technicalShowChunk = technicalShowChunk;
		copy.technicalShowSlimeChunk = technicalShowSlimeChunk;
		copy.technicalShowLabels = technicalShowLabels;
		copy.technicalToggleKey = technicalToggleKey;
		copy.technicalToggleModifiers = technicalToggleModifiers;
		copy.frameRateToggleKey = frameRateToggleKey;
		copy.frameRateToggleModifiers = frameRateToggleModifiers;
		for (Map.Entry<String, ServerSeedProfiles> entry : serverSeedOverrides.entrySet()) {
			copy.serverSeedOverrides.put(entry.getKey(), entry.getValue().copy());
		}
		copy.showBackground = showBackground;
		copy.backgroundOpacity = backgroundOpacity;
		copy.boxSize = boxSize;
		copy.fontSize = fontSize;
		copy.textAlignment = textAlignment;
		copy.toggleKey = toggleKey;
		copy.toggleModifiers = toggleModifiers;
		copy.position = position;
		return copy;
	}

	private OverlayConfig normalized() {
		backgroundOpacity = clamp(backgroundOpacity, 0, 100);
		boxSize = clamp(boxSize, 50, 200);
		fontSize = clamp(fontSize, 50, 200);
		if (toggleKey < -1) {
			toggleKey = -1;
		}
		if (manualAnnouncementKey < -1) {
			manualAnnouncementKey = InputConstants.KEY_N;
		}
		if (technicalToggleKey < -1) {
			technicalToggleKey = InputConstants.KEY_I;
		}
		if (frameRateToggleKey < -1) {
			frameRateToggleKey = -1;
		}
		configVersion = CURRENT_CONFIG_VERSION;
		int allowedModifiers = InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL | InputConstants.MOD_ALT;
		toggleModifiers &= allowedModifiers;
		manualAnnouncementModifiers &= allowedModifiers;
		technicalToggleModifiers &= allowedModifiers;
		frameRateToggleModifiers &= allowedModifiers;
		if (serverSeedOverrides == null) {
			serverSeedOverrides = new LinkedHashMap<>();
		} else {
			serverSeedOverrides.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
			serverSeedOverrides.values().forEach(ServerSeedProfiles::normalize);
		}
		if (headingLabel == null) {
			headingLabel = HeadingLabel.FACING;
		}
		if (manualAnnouncementContent == null) {
			manualAnnouncementContent = ManualAnnouncementContent.BOTH;
		}
		if (position == null) {
			position = Position.TOP_LEFT;
		}
		if (textAlignment == null) {
			textAlignment = TextAlignment.LEFT;
		}
		return this;
	}

	private static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	Long activeServerSeed(String serverKey) {
		ServerSeedProfiles server = serverSeedOverrides.get(serverKey);
		return server == null ? null : server.activeSeed();
	}

	String activeServerSeedProfile(String serverKey) {
		ServerSeedProfiles server = serverSeedOverrides.get(serverKey);
		return server == null || server.activeProfile == null ? "None" : server.activeProfile;
	}

	void putServerSeed(String serverKey, String profileName, long seed) {
		ServerSeedProfiles server = serverSeedOverrides.computeIfAbsent(serverKey,
				ignored -> new ServerSeedProfiles());
		server.profiles.put(profileName, seed);
		server.activeProfile = profileName;
	}

	void clearActiveServerSeed(String serverKey) {
		ServerSeedProfiles server = serverSeedOverrides.get(serverKey);
		if (server == null || server.activeProfile == null) {
			return;
		}
		server.profiles.remove(server.activeProfile);
		server.activeProfile = server.profiles.keySet().stream().findFirst().orElse(null);
		if (server.profiles.isEmpty()) {
			serverSeedOverrides.remove(serverKey);
		}
	}

	void cycleServerSeedProfile(String serverKey) {
		ServerSeedProfiles server = serverSeedOverrides.get(serverKey);
		if (server == null || server.profiles.size() < 2) {
			return;
		}
		List<String> names = new ArrayList<>(server.profiles.keySet());
		int current = names.indexOf(server.activeProfile);
		server.activeProfile = names.get((current + 1) % names.size());
	}

	public static final class ServerSeedProfiles {
		public String activeProfile;
		public Map<String, Long> profiles = new LinkedHashMap<>();

		private Long activeSeed() {
			return activeProfile == null ? null : profiles.get(activeProfile);
		}

		private ServerSeedProfiles copy() {
			ServerSeedProfiles copy = new ServerSeedProfiles();
			copy.activeProfile = activeProfile;
			copy.profiles.putAll(profiles);
			return copy;
		}

		private void normalize() {
			if (profiles == null) {
				profiles = new LinkedHashMap<>();
			}
			profiles.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
			if (activeProfile == null || !profiles.containsKey(activeProfile)) {
				activeProfile = profiles.keySet().stream().findFirst().orElse(null);
			}
		}
	}

	public enum Position {
		TOP_LEFT("Top left"),
		TOP_CENTER("Top center"),
		TOP_RIGHT("Top right"),
		CENTER_LEFT("Center left"),
		CENTER("Center"),
		CENTER_RIGHT("Center right"),
		BOTTOM_LEFT("Bottom left"),
		BOTTOM_CENTER("Bottom center"),
		BOTTOM_RIGHT("Bottom right");

		private final String label;

		Position(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}

		public Position next() {
			Position[] positions = values();
			return positions[(ordinal() + 1) % positions.length];
		}
	}

	public enum HeadingLabel {
		FACING("Facing"),
		HEADING("Heading");

		private final String label;

		HeadingLabel(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}

		public HeadingLabel next() {
			return this == FACING ? HEADING : FACING;
		}
	}

	public enum TextAlignment {
		LEFT("Left"),
		CENTER("Center"),
		RIGHT("Right");

		private final String label;

		TextAlignment(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}

		public TextAlignment next() {
			TextAlignment[] alignments = values();
			return alignments[(ordinal() + 1) % alignments.length];
		}
	}

	public enum ManualAnnouncementContent {
		DAY("Day"),
		BIOME("Biome"),
		BOTH("Day + biome");

		private final String label;

		ManualAnnouncementContent(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}

		public ManualAnnouncementContent next() {
			ManualAnnouncementContent[] values = values();
			return values[(ordinal() + 1) % values.length];
		}
	}
}
