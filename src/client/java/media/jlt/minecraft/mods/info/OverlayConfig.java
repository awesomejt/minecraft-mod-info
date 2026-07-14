package media.jlt.minecraft.mods.info;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OverlayConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModInfoClient.MOD_ID);
	private static final Gson LEGACY_JSON_GSON = new Gson();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mod-info.yaml");
	private static final Path LEGACY_JSON_CONFIG_PATH =
			FabricLoader.getInstance().getConfigDir().resolve("mod-info.json");
	private static final Yaml YAML = createYaml();
	private static final int CURRENT_CONFIG_VERSION = 3;

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
		if (Files.isRegularFile(CONFIG_PATH)) {
			try {
				PersistedConfig persisted;
				try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
					persisted = (PersistedConfig) YAML.load(reader);
				}
				if (persisted != null && persisted.configVersion >= CURRENT_CONFIG_VERSION) {
					OverlayConfig loaded = fromPersisted(persisted);
					loaded.configVersion = CURRENT_CONFIG_VERSION;
					loaded.normalized();
					return loaded;
				}
				// Unrecognized/older layout (e.g. a pre-grouping mod-info.yaml): keys won't line up
				// with PersistedConfig, so a lenient parse would silently come back mostly-default.
				// Keep the original bytes instead of discarding them without a trace.
				LOGGER.warn("{} uses an older, incompatible layout (configVersion {}); "
								+ "keeping it as mod-info.yaml.old.bak and starting fresh",
						CONFIG_PATH, persisted == null ? "unknown" : persisted.configVersion);
				Files.move(CONFIG_PATH, CONFIG_PATH.resolveSibling("mod-info.yaml.old.bak"),
						StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception exception) {
				LOGGER.warn("Could not read {}; using defaults", CONFIG_PATH, exception);
				return new OverlayConfig();
			}
		}

		if (Files.isRegularFile(LEGACY_JSON_CONFIG_PATH)) {
			return migrateLegacyJson();
		}

		return new OverlayConfig();
	}

	private static OverlayConfig migrateLegacyJson() {
		try {
			JsonObject json;
			try (Reader reader = Files.newBufferedReader(LEGACY_JSON_CONFIG_PATH)) {
				json = JsonParser.parseReader(reader).getAsJsonObject();
			}
			boolean legacyVersion = !json.has("configVersion");
			OverlayConfig loaded = LEGACY_JSON_GSON.fromJson(json, OverlayConfig.class);
			if (loaded == null) {
				return new OverlayConfig();
			}
			if (legacyVersion && loaded.toggleKey == InputConstants.KEY_O && loaded.toggleModifiers == 0) {
				loaded.toggleKey = -1;
			}
			loaded.configVersion = CURRENT_CONFIG_VERSION;
			loaded.normalized();
			loaded.save();
			if (Files.isRegularFile(CONFIG_PATH)) {
				Files.move(LEGACY_JSON_CONFIG_PATH,
						LEGACY_JSON_CONFIG_PATH.resolveSibling("mod-info.json.bak"),
						StandardCopyOption.REPLACE_EXISTING);
				LOGGER.info("Migrated {} to {}; old file kept as mod-info.json.bak",
						LEGACY_JSON_CONFIG_PATH, CONFIG_PATH);
			}
			return loaded;
		} catch (Exception exception) {
			LOGGER.warn("Could not read {}; using defaults", LEGACY_JSON_CONFIG_PATH, exception);
			return new OverlayConfig();
		}
	}

	public void save() {
		normalized();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				YAML.dump(toOrderedRepresentation(toPersisted()), writer);
			}
		} catch (IOException exception) {
			LOGGER.error("Could not save {}", CONFIG_PATH, exception);
		}
	}

	private PersistedConfig toPersisted() {
		PersistedConfig persisted = new PersistedConfig();
		persisted.configVersion = configVersion;

		persisted.info.navigation.coordinates = showCoordinates;
		persisted.info.navigation.facing.show = showFacing;
		persisted.info.navigation.facing.label = headingLabel;
		persisted.info.navigation.facing.onCoordinatesLine = facingOnCoordinatesLine;
		persisted.info.navigation.biome = showBiome;
		persisted.info.duration.days = showDaysPlayed;
		persisted.info.duration.clock.show = showClock;
		persisted.info.duration.clock.showDayNight = clockShowDayNight;
		persisted.info.duration.clock.showWeather = clockShowWeather;
		persisted.info.appearance.background.show = showBackground;
		persisted.info.appearance.background.opacity = backgroundOpacity;
		persisted.info.appearance.box.size = boxSize;
		persisted.info.appearance.box.fontSize = fontSize;
		persisted.info.appearance.box.textAlignment = textAlignment;
		persisted.info.appearance.box.position = position;
		persisted.info.appearance.box.fieldLabels = showFieldLabels;
		persisted.info.appearance.box.technicalLabels = technicalShowLabels;
		persisted.info.technical.light = technicalShowLight;
		persisted.info.technical.chunk = technicalShowChunk;
		persisted.info.technical.slimeChunk = technicalShowSlimeChunk;

		persisted.announcements.biome.announce.enabled = announceNewBiome;
		persisted.announcements.biome.announce.dimension = biomeAnnouncementShowDimension;
		persisted.announcements.biome.announce.entering = biomeAnnouncementShowEntering;
		persisted.announcements.day.announce.enabled = announceNewDay;
		persisted.announcements.manualContent = manualAnnouncementContent;

		persisted.bindings.info.key = toggleKey;
		persisted.bindings.info.modifiers = toggleModifiers;
		persisted.bindings.technical.key = technicalToggleKey;
		persisted.bindings.technical.modifiers = technicalToggleModifiers;
		persisted.bindings.frameRate.key = frameRateToggleKey;
		persisted.bindings.frameRate.modifiers = frameRateToggleModifiers;
		persisted.bindings.announcements.key = manualAnnouncementKey;
		persisted.bindings.announcements.modifiers = manualAnnouncementModifiers;

		persisted.serverSeeds = serverSeedOverrides;
		return persisted;
	}

	private static OverlayConfig fromPersisted(PersistedConfig persisted) {
		OverlayConfig config = new OverlayConfig();
		config.configVersion = persisted.configVersion;

		config.showCoordinates = persisted.info.navigation.coordinates;
		config.showFacing = persisted.info.navigation.facing.show;
		config.headingLabel = persisted.info.navigation.facing.label;
		config.facingOnCoordinatesLine = persisted.info.navigation.facing.onCoordinatesLine;
		config.showBiome = persisted.info.navigation.biome;
		config.showDaysPlayed = persisted.info.duration.days;
		config.showClock = persisted.info.duration.clock.show;
		config.clockShowDayNight = persisted.info.duration.clock.showDayNight;
		config.clockShowWeather = persisted.info.duration.clock.showWeather;
		config.showBackground = persisted.info.appearance.background.show;
		config.backgroundOpacity = persisted.info.appearance.background.opacity;
		config.boxSize = persisted.info.appearance.box.size;
		config.fontSize = persisted.info.appearance.box.fontSize;
		config.textAlignment = persisted.info.appearance.box.textAlignment;
		config.position = persisted.info.appearance.box.position;
		config.showFieldLabels = persisted.info.appearance.box.fieldLabels;
		config.technicalShowLabels = persisted.info.appearance.box.technicalLabels;
		config.technicalShowLight = persisted.info.technical.light;
		config.technicalShowChunk = persisted.info.technical.chunk;
		config.technicalShowSlimeChunk = persisted.info.technical.slimeChunk;

		config.announceNewBiome = persisted.announcements.biome.announce.enabled;
		config.biomeAnnouncementShowDimension = persisted.announcements.biome.announce.dimension;
		config.biomeAnnouncementShowEntering = persisted.announcements.biome.announce.entering;
		config.announceNewDay = persisted.announcements.day.announce.enabled;
		config.manualAnnouncementContent = persisted.announcements.manualContent;

		config.toggleKey = persisted.bindings.info.key;
		config.toggleModifiers = persisted.bindings.info.modifiers;
		config.technicalToggleKey = persisted.bindings.technical.key;
		config.technicalToggleModifiers = persisted.bindings.technical.modifiers;
		config.frameRateToggleKey = persisted.bindings.frameRate.key;
		config.frameRateToggleModifiers = persisted.bindings.frameRate.modifiers;
		config.manualAnnouncementKey = persisted.bindings.announcements.key;
		config.manualAnnouncementModifiers = persisted.bindings.announcements.modifiers;

		config.serverSeedOverrides = persisted.serverSeeds == null
				? new LinkedHashMap<>()
				: persisted.serverSeeds;
		return config;
	}

	private static Yaml createYaml() {
		LoaderOptions loaderOptions = new LoaderOptions();
		Constructor constructor = new Constructor(PersistedConfig.class, loaderOptions);
		PropertyUtils propertyUtils = new PropertyUtils();
		propertyUtils.setBeanAccess(BeanAccess.FIELD);
		propertyUtils.setSkipMissingProperties(true);
		constructor.setPropertyUtils(propertyUtils);

		TypeDescription configDescription = new TypeDescription(PersistedConfig.class);
		configDescription.addPropertyParameters("serverSeeds", String.class, ServerSeedProfiles.class);
		constructor.addTypeDescription(configDescription);
		TypeDescription profilesDescription = new TypeDescription(ServerSeedProfiles.class);
		profilesDescription.addPropertyParameters("profiles", String.class, Long.class);
		constructor.addTypeDescription(profilesDescription);

		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setIndent(2);
		Representer representer = new Representer(dumperOptions);
		representer.addClassTag(Position.class, Tag.STR);
		representer.addClassTag(HeadingLabel.class, Tag.STR);
		representer.addClassTag(TextAlignment.class, Tag.STR);
		representer.addClassTag(ManualAnnouncementContent.class, Tag.STR);

		return new Yaml(constructor, representer, dumperOptions);
	}

	/**
	 * Converts a bean graph into LinkedHashMaps ordered by field declaration, since SnakeYAML's
	 * bean representer does not preserve declaration order on its own.
	 */
	private static Object toOrderedRepresentation(Object value) {
		if (value == null || value instanceof Number || value instanceof String
				|| value instanceof Boolean || value instanceof Enum) {
			return value;
		}
		if (value instanceof Map<?, ?> map) {
			Map<Object, Object> ordered = new LinkedHashMap<>();
			map.forEach((key, entryValue) -> ordered.put(key, toOrderedRepresentation(entryValue)));
			return ordered;
		}
		Map<String, Object> ordered = new LinkedHashMap<>();
		for (Field field : value.getClass().getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			try {
				field.setAccessible(true);
				ordered.put(field.getName(), toOrderedRepresentation(field.get(value)));
			} catch (IllegalAccessException exception) {
				throw new IllegalStateException("Could not read field " + field.getName(), exception);
			}
		}
		return ordered;
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

	/**
	 * On-disk shape of {@code mod-info.yaml} (see {@code src/config/example.yaml}), grouped by
	 * theme rather than mirroring this class's flat field layout.
	 */
	private static final class PersistedConfig {
		int configVersion = CURRENT_CONFIG_VERSION;
		InfoSection info = new InfoSection();
		AnnouncementsSection announcements = new AnnouncementsSection();
		BindingsSection bindings = new BindingsSection();
		Map<String, ServerSeedProfiles> serverSeeds = new LinkedHashMap<>();
	}

	private static final class InfoSection {
		NavigationSection navigation = new NavigationSection();
		DurationSection duration = new DurationSection();
		AppearanceSection appearance = new AppearanceSection();
		TechnicalSection technical = new TechnicalSection();
	}

	private static final class NavigationSection {
		boolean coordinates = true;
		FacingSection facing = new FacingSection();
		boolean biome = true;
	}

	private static final class FacingSection {
		boolean show = false;
		HeadingLabel label = HeadingLabel.FACING;
		boolean onCoordinatesLine = true;
	}

	private static final class DurationSection {
		boolean days = true;
		ClockSection clock = new ClockSection();
	}

	private static final class ClockSection {
		boolean show = false;
		boolean showDayNight = true;
		boolean showWeather = true;
	}

	private static final class AppearanceSection {
		BackgroundSection background = new BackgroundSection();
		BoxSection box = new BoxSection();
	}

	private static final class BackgroundSection {
		boolean show = true;
		int opacity = 56;
	}

	private static final class BoxSection {
		int size = 100;
		int fontSize = 100;
		TextAlignment textAlignment = TextAlignment.LEFT;
		Position position = Position.TOP_LEFT;
		boolean fieldLabels = true;
		boolean technicalLabels = true;
	}

	private static final class TechnicalSection {
		boolean light = true;
		boolean chunk = true;
		boolean slimeChunk = false;
	}

	private static final class AnnouncementsSection {
		BiomeAnnouncementSection biome = new BiomeAnnouncementSection();
		DayAnnouncementSection day = new DayAnnouncementSection();
		ManualAnnouncementContent manualContent = ManualAnnouncementContent.BOTH;
	}

	private static final class BiomeAnnouncementSection {
		BiomeAnnounceDetails announce = new BiomeAnnounceDetails();
	}

	private static final class BiomeAnnounceDetails {
		boolean enabled = false;
		boolean dimension = false;
		boolean entering = true;
	}

	private static final class DayAnnouncementSection {
		DayAnnounceDetails announce = new DayAnnounceDetails();
	}

	private static final class DayAnnounceDetails {
		boolean enabled = true;
	}

	private static final class BindingsSection {
		KeyBinding info = new KeyBinding(-1, 0);
		KeyBinding technical = new KeyBinding(InputConstants.KEY_I, InputConstants.MOD_CONTROL);
		KeyBinding frameRate = new KeyBinding(-1, 0);
		KeyBinding announcements = new KeyBinding(InputConstants.KEY_N, InputConstants.MOD_CONTROL);
	}

	private static final class KeyBinding {
		int key;
		int modifiers;

		KeyBinding() {
			this(-1, 0);
		}

		KeyBinding(int key, int modifiers) {
			this.key = key;
			this.modifiers = modifiers;
		}
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
