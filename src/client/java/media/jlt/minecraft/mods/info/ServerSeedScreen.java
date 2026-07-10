package media.jlt.minecraft.mods.info;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ServerSeedScreen extends Screen {
	private final Screen parent;
	private final OverlayConfig config;
	private final String serverKey;
	private EditBox profileInput;
	private EditBox seedInput;
	private String error = "";

	ServerSeedScreen(Screen parent, OverlayConfig config, String serverKey) {
		super(Component.literal("Server Seed Override"));
		this.parent = parent;
		this.config = config;
		this.serverKey = serverKey;
	}

	@Override
	protected void init() {
		int fieldWidth = Math.min(300, width - 40);
		int fieldX = (width - fieldWidth) / 2;

		profileInput = new EditBox(font, fieldX, 76, fieldWidth, 20, Component.literal("Seed profile name"));
		profileInput.setMaxLength(48);
		profileInput.setHint(Component.literal("Example: season-4"));
		String activeProfile = config.activeServerSeedProfile(serverKey);
		if (!"None".equals(activeProfile)) {
			profileInput.setValue(activeProfile);
		}
		addRenderableWidget(profileInput);

		seedInput = new EditBox(font, fieldX, 122, fieldWidth, 20, Component.literal("Signed numeric seed"));
		seedInput.setMaxLength(20);
		seedInput.setHint(Component.literal("Example: -1234567890123456789"));
		Long activeSeed = config.activeServerSeed(serverKey);
		if (activeSeed != null) {
			seedInput.setValue(Long.toString(activeSeed));
		}
		addRenderableWidget(seedInput);

		int buttonY = height - 28;
		addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
				.bounds(width / 2 - 154, buttonY, 100, 20).build());
		Button clear = Button.builder(Component.literal("Clear Active"), button -> clearActive())
				.bounds(width / 2 - 50, buttonY, 100, 20).build();
		clear.active = activeSeed != null;
		addRenderableWidget(clear);
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
				.bounds(width / 2 + 54, buttonY, 100, 20).build());

		setInitialFocus(profileInput);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
		graphics.centeredText(font, "Server: " + serverKey, width / 2, 36, 0xFFAAAAAA);
		graphics.text(font, "Profile name", profileInput.getX(), 64, 0xFFFFFFFF);
		graphics.text(font, "World seed (signed 64-bit number)", seedInput.getX(), 110, 0xFFFFFFFF);
		graphics.centeredText(font, "Wrong seeds produce plausible but incorrect slime-chunk results.",
				width / 2, 150, 0xFFFFCC55);
		if (!error.isEmpty()) {
			graphics.centeredText(font, error, width / 2, 168, 0xFFFF5555);
		}
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	private void save() {
		String profile = profileInput.getValue().trim();
		if (profile.isEmpty()) {
			error = "Enter a profile name.";
			return;
		}
		String seedText = seedInput.getValue().trim();
		try {
			long seed = Long.parseLong(seedText);
			config.putServerSeed(serverKey, profile, seed);
			onClose();
		} catch (NumberFormatException exception) {
			error = "Seed must be a signed number from -9223372036854775808 to 9223372036854775807.";
		}
	}

	private void clearActive() {
		config.clearActiveServerSeed(serverKey);
		onClose();
	}
}
