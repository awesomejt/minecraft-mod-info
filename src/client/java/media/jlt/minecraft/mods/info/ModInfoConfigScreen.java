package media.jlt.minecraft.mods.info;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

public final class ModInfoConfigScreen extends Screen {
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW_HEIGHT = 24;
	private static final int FIRST_ROW = 64;

	private final Screen parent;
	private OverlayConfig editing;
	private Page page = Page.INFO;
	private CaptureTarget captureTarget;
	private Button captureButton;

	public ModInfoConfigScreen(Screen parent) {
		super(Component.literal("Mod Info Overlay Settings"));
		this.parent = parent;
		this.editing = ModInfoClient.config().copy();
	}

	@Override
	protected void init() {
		int left = width / 2 - 156;
		int right = width / 2 + 6;
		addPageTabs(left);

		switch (page) {
			case INFO -> buildInfoPage(left, right);
			case APPEARANCE -> buildAppearancePage(left, right);
			case ANNOUNCEMENTS -> buildAnnouncementsPage(left, right);
			case TECHNICAL -> buildTechnicalPage(left, right);
		}

		int bottom = height - 28;
		addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
			editing = new OverlayConfig();
			rebuildWidgets();
		}).bounds(width / 2 - 156, bottom, 100, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> closeWithoutSaving())
				.bounds(width / 2 - 50, bottom, 100, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> saveAndClose())
				.bounds(width / 2 + 56, bottom, 100, BUTTON_HEIGHT).build());
	}

	private void addPageTabs(int left) {
		Page[] pages = Page.values();
		for (int i = 0; i < pages.length; i++) {
			Page target = pages[i];
			String label = target == page ? "[" + target.label + "]" : target.label;
			addRenderableWidget(Button.builder(Component.literal(label), button -> {
				page = target;
				captureTarget = null;
				captureButton = null;
				rebuildWidgets();
			}).bounds(left + i * 79, 29, 76, BUTTON_HEIGHT).build());
		}
	}

	private void buildInfoPage(int left, int right) {
		addBooleanButton(left, row(0), "Coordinates", () -> editing.showCoordinates,
				value -> editing.showCoordinates = value);
		addBooleanButton(left, row(1), "Biome", () -> editing.showBiome,
				value -> editing.showBiome = value);
		addBooleanButton(left, row(2), "Days played", () -> editing.showDaysPlayed,
				value -> editing.showDaysPlayed = value);
		addBooleanButton(left, row(3), "Field labels", () -> editing.showFieldLabels,
				value -> editing.showFieldLabels = value);
		addBooleanButton(left, row(4), "Facing direction", () -> editing.showFacing,
				value -> editing.showFacing = value);
		addBooleanButton(left, row(5), "Facing with coordinates", () -> editing.facingOnCoordinatesLine,
				value -> editing.facingOnCoordinatesLine = value);

		addCycleButton(right, row(0), () -> "Direction label: " + editing.headingLabel.label(), () -> {
			editing.headingLabel = editing.headingLabel.next();
		});
		addBooleanButton(right, row(1), "Clock", () -> editing.showClock,
				value -> editing.showClock = value);
		addBooleanButton(right, row(2), "Clock day/night", () -> editing.clockShowDayNight,
				value -> editing.clockShowDayNight = value);
		addBooleanButton(right, row(3), "Clock weather", () -> editing.clockShowWeather,
				value -> editing.clockShowWeather = value);
	}

	private void buildAppearancePage(int left, int right) {
		addBooleanButton(left, row(0), "Background", () -> editing.showBackground,
				value -> editing.showBackground = value);
		addCycleButton(left, row(1), () -> "Position: " + editing.position.label(), () -> {
			editing.position = editing.position.next();
		});
		addShortcutButton(left, row(2), "Overlay toggle", CaptureTarget.OVERLAY,
				() -> shortcutMessage("Overlay toggle", editing.toggleKey, editing.toggleModifiers));
		addCycleButton(left, row(3), () -> "Text alignment: " + editing.textAlignment.label(), () -> {
			editing.textAlignment = editing.textAlignment.next();
		});

		addRenderableWidget(new PercentageSlider(right, row(0), "Opacity", 0, 100,
				editing.backgroundOpacity, value -> editing.backgroundOpacity = value));
		addRenderableWidget(new PercentageSlider(right, row(1), "Box size", 50, 200,
				editing.boxSize, value -> editing.boxSize = value));
		addRenderableWidget(new PercentageSlider(right, row(2), "Font size", 50, 200,
				editing.fontSize, value -> editing.fontSize = value));
	}

	private void buildAnnouncementsPage(int left, int right) {
		addBooleanButton(left, row(0), "Announce new day", () -> editing.announceNewDay,
				value -> editing.announceNewDay = value);
		addBooleanButton(left, row(1), "Announce biome", () -> editing.announceNewBiome,
				value -> editing.announceNewBiome = value);
		addBooleanButton(left, row(2), "Include dimension", () -> editing.biomeAnnouncementShowDimension,
				value -> editing.biomeAnnouncementShowDimension = value);
		addBooleanButton(left, row(3), "Include “Entering”", () -> editing.biomeAnnouncementShowEntering,
				value -> editing.biomeAnnouncementShowEntering = value);

		addCycleButton(right, row(0), () -> "Manual content: " + editing.manualAnnouncementContent.label(), () -> {
			editing.manualAnnouncementContent = editing.manualAnnouncementContent.next();
		});
		addShortcutButton(right, row(1), "Manual announce", CaptureTarget.MANUAL_ANNOUNCEMENT,
				() -> shortcutMessage("Manual announce", editing.manualAnnouncementKey,
						editing.manualAnnouncementModifiers));
	}

	private void buildTechnicalPage(int left, int right) {
		addBooleanButton(left, row(0), "Block + sky light", () -> editing.technicalShowLight,
				value -> editing.technicalShowLight = value);
		addBooleanButton(left, row(1), "Chunk position", () -> editing.technicalShowChunk,
				value -> editing.technicalShowChunk = value);
		addBooleanButton(left, row(2), "Slime chunk (single-player)", () -> editing.technicalShowSlimeChunk,
				value -> editing.technicalShowSlimeChunk = value);
		addBooleanButton(left, row(3), "Technical labels", () -> editing.technicalShowLabels,
				value -> editing.technicalShowLabels = value);

		addShortcutButton(right, row(0), "Technical toggle", CaptureTarget.TECHNICAL,
				() -> shortcutMessage("Technical toggle", editing.technicalToggleKey,
						editing.technicalToggleModifiers));
		addShortcutButton(right, row(1), "Frame-rate toggle", CaptureTarget.FRAME_RATE,
				() -> shortcutMessage("Frame-rate toggle", editing.frameRateToggleKey,
						editing.frameRateToggleModifiers));
	}

	private void addBooleanButton(int x, int y, String label, BooleanGetter getter, BooleanSetter setter) {
		Button button = Button.builder(booleanMessage(label, getter.get()), pressed -> {
			boolean value = !getter.get();
			setter.set(value);
			pressed.setMessage(booleanMessage(label, value));
		}).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
		addRenderableWidget(button);
	}

	private void addCycleButton(int x, int y, TextGetter getter, Runnable cycle) {
		Button button = Button.builder(Component.literal(getter.get()), pressed -> {
			cycle.run();
			pressed.setMessage(Component.literal(getter.get()));
		}).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
		addRenderableWidget(button);
	}

	private void addShortcutButton(int x, int y, String label, CaptureTarget target, ComponentGetter getter) {
		Button button = Button.builder(getter.get(), pressed -> {
			captureTarget = target;
			captureButton = pressed;
			pressed.setMessage(Component.literal(label + ": press keys…"));
		}).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
		addRenderableWidget(button);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
		if (page == Page.TECHNICAL) {
			graphics.centeredText(font, "Shortcut toggles the selected technical rows together",
					width / 2, row(4), 0xFFAAAAAA);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (captureTarget != null) {
			if (event.key() == InputConstants.KEY_ESCAPE) {
				finishCapture();
				return true;
			}
			if (event.key() == InputConstants.KEY_BACKSPACE || event.key() == InputConstants.KEY_DELETE) {
				setCapturedShortcut(-1, 0);
				finishCapture();
				return true;
			}
			if (isModifierKey(event.key())) {
				return true;
			}

			int modifiers = event.modifiers()
					& (InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL | InputConstants.MOD_ALT);
			setCapturedShortcut(event.key(), modifiers);
			finishCapture();
			return true;
		}
		return super.keyPressed(event);
	}

	private void setCapturedShortcut(int key, int modifiers) {
		switch (captureTarget) {
				case OVERLAY -> {
					editing.toggleKey = key;
					editing.toggleModifiers = modifiers;
				}
				case MANUAL_ANNOUNCEMENT -> {
					editing.manualAnnouncementKey = key;
					editing.manualAnnouncementModifiers = modifiers;
				}
				case TECHNICAL -> {
					editing.technicalToggleKey = key;
					editing.technicalToggleModifiers = modifiers;
				}
				case FRAME_RATE -> {
					editing.frameRateToggleKey = key;
					editing.frameRateToggleModifiers = modifiers;
				}
			}
	}

	@Override
	public void onClose() {
		closeWithoutSaving();
	}

	private void finishCapture() {
		captureTarget = null;
		captureButton = null;
		rebuildWidgets();
	}

	private void saveAndClose() {
		ModInfoClient.applyConfig(editing);
		minecraft.gui.setScreen(parent);
	}

	private void closeWithoutSaving() {
		minecraft.gui.setScreen(parent);
	}

	private static int row(int index) {
		return FIRST_ROW + index * ROW_HEIGHT;
	}

	private static Component booleanMessage(String label, boolean enabled) {
		return Component.literal(label + ": " + (enabled ? "On" : "Off"));
	}

	private static Component shortcutMessage(String label, int key, int modifiers) {
		StringBuilder text = new StringBuilder(label).append(": ");
		if (key < 0) {
			return Component.literal(text.append("Unbound").toString());
		}
		if ((modifiers & InputConstants.MOD_CONTROL) != 0) {
			text.append("Ctrl+");
		}
		if ((modifiers & InputConstants.MOD_SHIFT) != 0) {
			text.append("Shift+");
		}
		if ((modifiers & InputConstants.MOD_ALT) != 0) {
			text.append("Alt+");
		}
		text.append(InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString());
		return Component.literal(text.toString());
	}

	private static boolean isModifierKey(int key) {
		return key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT
				|| key == InputConstants.KEY_LCONTROL || key == InputConstants.KEY_RCONTROL
				|| key == InputConstants.KEY_LALT || key == InputConstants.KEY_RALT;
	}

	private enum Page {
		INFO("Info"),
		APPEARANCE("Appearance"),
		ANNOUNCEMENTS("Announce"),
		TECHNICAL("Technical");

		private final String label;

		Page(String label) {
			this.label = label;
		}
	}

	private enum CaptureTarget {
		OVERLAY,
		MANUAL_ANNOUNCEMENT,
		TECHNICAL,
		FRAME_RATE
	}

	@FunctionalInterface
	private interface BooleanGetter {
		boolean get();
	}

	@FunctionalInterface
	private interface BooleanSetter {
		void set(boolean value);
	}

	@FunctionalInterface
	private interface TextGetter {
		String get();
	}

	@FunctionalInterface
	private interface ComponentGetter {
		Component get();
	}

	private static final class PercentageSlider extends AbstractSliderButton {
		private final String label;
		private final int minimum;
		private final int maximum;
		private final IntConsumer consumer;

		private PercentageSlider(int x, int y, String label, int minimum, int maximum, int current,
				IntConsumer consumer) {
			super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.empty(),
					(current - minimum) / (double) (maximum - minimum));
			this.label = label;
			this.minimum = minimum;
			this.maximum = maximum;
			this.consumer = consumer;
			updateMessage();
		}

		private int currentValue() {
			return minimum + (int) Math.round(value * (maximum - minimum));
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(label + ": " + currentValue() + "%"));
		}

		@Override
		protected void applyValue() {
			consumer.accept(currentValue());
		}
	}
}
