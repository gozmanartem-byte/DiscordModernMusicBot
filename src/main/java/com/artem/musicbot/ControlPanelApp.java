package com.artem.musicbot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

public class ControlPanelApp {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Path CONFIG_PATH = Path.of("ModernMusicBot.properties");
    private static final Path APP_HOME_PATH = Path.of(System.getProperty("user.home"), ".modernmusicbot");
    private static final Path DESKTOP_CONFIG_PATH = APP_HOME_PATH.resolve("ModernMusicBot.properties");
    private static final Path FIRST_LAUNCH_MARKER = APP_HOME_PATH.resolve("first-launch-banner-seen.flag");
    private static final LanguageOption[] LANGUAGE_OPTIONS = {
        new LanguageOption("English", "en"),
        new LanguageOption("Russian", "ru"),
        new LanguageOption("Armenian", "hy"),
        new LanguageOption("Georgian", "ka"),
        new LanguageOption("Azerbaijani", "az"),
        new LanguageOption("Kazakh", "kk"),
        new LanguageOption("Uzbek", "uz"),
        new LanguageOption("Ukrainian", "uk"),
        new LanguageOption("German", "de"),
        new LanguageOption("Spanish", "es"),
        new LanguageOption("Italian", "it"),
        new LanguageOption("Portuguese", "pt"),
        new LanguageOption("Chinese", "zh"),
        new LanguageOption("Japanese", "ja")
    };

    private final BotRuntime runtime = new BotRuntime();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private JFrame frame;
    private JPasswordField tokenField;
    private JTextField prefixField;
    private JComboBox<LanguageOption> languageCombo;
    private JTextArea console;
    private JButton startButton;
    private JButton stopButton;
    private JButton saveButton;
    private JComboBox<GuildOption> guildCombo;
    private JComboBox<ChannelOption> channelCombo;
    private JTextField addSongField;
    private JButton addSongButton;
    private JButton pauseButton;
    private JButton resumeButton;
    private JButton skipButton;
    private JButton stopPlaybackButton;
    private JButton refreshDesktopButton;
    private JTextArea playerSummaryArea;
    private Timer desktopRefreshTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ControlPanelApp().show());
    }

    private void show() {
        frame = new JFrame("ModernMusicBot Control Panel");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        if (isDesktopOnboardingEnabled()) {
            frame.setJMenuBar(buildMenuBar());
        }

        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createTitledBorder("Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        top.add(new JLabel("Bot Token:"), c);

        tokenField = new JPasswordField();
        tokenField.setPreferredSize(new Dimension(420, 26));
        c.gridx = 1;
        c.weightx = 1.0;
        top.add(tokenField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        top.add(new JLabel("Prefix:"), c);

        prefixField = new JTextField("!");
        c.gridx = 1;
        c.weightx = 1.0;
        top.add(prefixField, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        top.add(new JLabel("Language:"), c);

        languageCombo = new JComboBox<>(LANGUAGE_OPTIONS);
        languageCombo.setSelectedIndex(0);
        c.gridx = 1;
        c.weightx = 1.0;
        top.add(languageCombo, c);

        JPanel buttons = new JPanel();
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        saveButton = new JButton("Save Settings");
        stopButton.setEnabled(false);
        buttons.add(startButton);
        buttons.add(stopButton);
        buttons.add(saveButton);

        console = new JTextArea();
        console.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(console);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Console"));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(buttons, BorderLayout.NORTH);
        if (isDesktopOnboardingEnabled()) {
            centerPanel.add(buildDesktopPlayerPanel(), BorderLayout.CENTER);
        }

        frame.add(top, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);

        scrollPane.setPreferredSize(new Dimension(760, 320));
        frame.setMinimumSize(new Dimension(900, isDesktopOnboardingEnabled() ? 760 : 560));

        loadConfigIntoFields();
        wireActions();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        log("Control panel ready.");
        showFirstLaunchBannerIfNeeded();

        if (isDesktopOnboardingEnabled()) {
            desktopRefreshTimer = new Timer(3000, ignored -> refreshPlayerSummaryAsync());
            desktopRefreshTimer.start();
            refreshDesktopSelectorsAsync();
        }

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (desktopRefreshTimer != null) {
                    desktopRefreshTimer.stop();
                }
                worker.shutdownNow();
                runtime.stop(ControlPanelApp.this::log);
            }
        });
    }

    private void wireActions() {
        saveButton.addActionListener(e -> {
            try {
                saveConfigFromFields();
                log("Settings saved.");
            } catch (Exception ex) {
                showError("Failed to save settings: " + ex.getMessage());
            }
        });

        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            worker.submit(() -> {
                try {
                    saveConfigFromFields();
                    log("Starting bot...");
                    runtime.start(resolveConfigPath(), true, this::log);
                    SwingUtilities.invokeLater(() -> {
                        stopButton.setEnabled(true);
                        startButton.setEnabled(false);
                        setDesktopControlsEnabled(true);
                    });
                    refreshDesktopSelectorsAsync();
                    refreshPlayerSummaryAsync();
                } catch (Exception ex) {
                    log("Start failed: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
                }
            });
        });

        stopButton.addActionListener(e -> worker.submit(() -> {
            runtime.stop(this::log);
            SwingUtilities.invokeLater(() -> {
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                setDesktopControlsEnabled(false);
                if (playerSummaryArea != null) {
                    playerSummaryArea.setText("Bot is not running.");
                }
            });
        }));

        if (isDesktopOnboardingEnabled()) {
            guildCombo.addActionListener(ignored -> refreshChannelsAsync());
            guildCombo.addActionListener(ignored -> refreshPlayerSummaryAsync());

            addSongButton.addActionListener(ignored -> worker.submit(() -> {
                Long guildId = selectedGuildId();
                Long channelId = selectedChannelId();
                String query = addSongField.getText().trim();
                if (guildId == null || channelId == null || query.isBlank()) {
                    SwingUtilities.invokeLater(() -> showError("Select guild/channel and enter a song or URL."));
                    return;
                }

                try {
                    runtime.addSongFromDesktop(guildId, channelId, query);
                    log("Desktop queued: " + query);
                    SwingUtilities.invokeLater(() -> addSongField.setText(""));
                    refreshPlayerSummaryAsync();
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
                }
            }));

            pauseButton.addActionListener(ignored -> desktopControlAsync("pause", BotRuntime::pauseFromDesktop));
            resumeButton.addActionListener(ignored -> desktopControlAsync("resume", BotRuntime::resumeFromDesktop));
            skipButton.addActionListener(ignored -> desktopControlAsync("skip", BotRuntime::skipFromDesktop));
            stopPlaybackButton.addActionListener(ignored -> desktopControlAsync("stop", BotRuntime::stopFromDesktop));
            refreshDesktopButton.addActionListener(ignored -> {
                refreshDesktopSelectorsAsync();
                refreshPlayerSummaryAsync();
            });
        }
    }

    private JPanel buildDesktopPlayerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Host Player (Desktop)"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        guildCombo = new JComboBox<>();
        channelCombo = new JComboBox<>();
        addSongField = new JTextField();
        addSongButton = new JButton("Add Song");
        pauseButton = new JButton("Pause");
        resumeButton = new JButton("Resume");
        skipButton = new JButton("Skip");
        stopPlaybackButton = new JButton("Stop");
        refreshDesktopButton = new JButton("Refresh Lists");
        playerSummaryArea = new JTextArea(5, 40);
        playerSummaryArea.setEditable(false);
        playerSummaryArea.setText("Bot is not running.");

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        panel.add(new JLabel("Guild:"), c);

        c.gridx = 1;
        c.weightx = 1;
        panel.add(guildCombo, c);

        c.gridx = 2;
        c.weightx = 0;
        panel.add(refreshDesktopButton, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        panel.add(new JLabel("Text channel:"), c);

        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        panel.add(channelCombo, c);
        c.gridwidth = 1;

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        panel.add(new JLabel("Song / URL:"), c);

        c.gridx = 1;
        c.weightx = 1;
        panel.add(addSongField, c);

        c.gridx = 2;
        c.weightx = 0;
        panel.add(addSongButton, c);

        JPanel controls = new JPanel();
        controls.add(pauseButton);
        controls.add(resumeButton);
        controls.add(skipButton);
        controls.add(stopPlaybackButton);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        panel.add(controls, c);

        c.gridy = 4;
        panel.add(new JScrollPane(playerSummaryArea), c);

        setDesktopControlsEnabled(false);
        return panel;
    }

    private void setDesktopControlsEnabled(boolean enabled) {
        if (!isDesktopOnboardingEnabled() || guildCombo == null) {
            return;
        }
        guildCombo.setEnabled(enabled);
        channelCombo.setEnabled(enabled);
        addSongField.setEnabled(enabled);
        addSongButton.setEnabled(enabled);
        pauseButton.setEnabled(enabled);
        resumeButton.setEnabled(enabled);
        skipButton.setEnabled(enabled);
        stopPlaybackButton.setEnabled(enabled);
        refreshDesktopButton.setEnabled(enabled);
    }

    private void refreshDesktopSelectorsAsync() {
        if (!isDesktopOnboardingEnabled()) {
            return;
        }
        worker.submit(() -> {
            List<BotRuntime.GuildRef> guilds = runtime.guildRefs();
            SwingUtilities.invokeLater(() -> {
                GuildOption selected = (GuildOption) guildCombo.getSelectedItem();
                guildCombo.removeAllItems();
                for (BotRuntime.GuildRef guild : guilds) {
                    guildCombo.addItem(new GuildOption(guild.id(), guild.name()));
                }
                if (selected != null) {
                    selectGuildById(selected.id());
                }
                refreshChannelsAsync();
            });
        });
    }

    private void refreshChannelsAsync() {
        if (!isDesktopOnboardingEnabled()) {
            return;
        }
        Long guildId = selectedGuildId();
        if (guildId == null) {
            SwingUtilities.invokeLater(() -> channelCombo.removeAllItems());
            return;
        }

        worker.submit(() -> {
            List<BotRuntime.ChannelRef> channels = runtime.textChannelRefs(guildId);
            long preferredChannelId = runtime.preferredTextChannelId(guildId);
            SwingUtilities.invokeLater(() -> {
                ChannelOption selected = (ChannelOption) channelCombo.getSelectedItem();
                channelCombo.removeAllItems();
                for (BotRuntime.ChannelRef channel : channels) {
                    channelCombo.addItem(new ChannelOption(channel.id(), channel.name()));
                }
                if (selected != null) {
                    selectChannelById(selected.id());
                } else if (preferredChannelId != 0L) {
                    selectChannelById(preferredChannelId);
                } else if (channelCombo.getItemCount() > 0) {
                    channelCombo.setSelectedIndex(0);
                }
            });
        });
    }

    private void refreshPlayerSummaryAsync() {
        if (!isDesktopOnboardingEnabled() || playerSummaryArea == null) {
            return;
        }
        worker.submit(() -> {
            Long guildId = selectedGuildId();
            String summary = guildId == null ? runtime.playerSummary() : runtime.playerSummaryForGuild(guildId);
            SwingUtilities.invokeLater(() -> playerSummaryArea.setText(summary));
        });
    }

    private void desktopControlAsync(String actionName, DesktopAction action) {
        worker.submit(() -> {
            Long guildId = selectedGuildId();
            Long channelId = selectedChannelId();
            if (guildId == null || channelId == null) {
                SwingUtilities.invokeLater(() -> showError("Select guild and text channel first."));
                return;
            }

            try {
                action.apply(runtime, guildId, channelId);
                log("Desktop action: " + actionName);
                refreshPlayerSummaryAsync();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
            }
        });
    }

    private Long selectedGuildId() {
        GuildOption option = (GuildOption) guildCombo.getSelectedItem();
        return option == null ? null : option.id();
    }

    private Long selectedChannelId() {
        ChannelOption option = (ChannelOption) channelCombo.getSelectedItem();
        return option == null ? null : option.id();
    }

    private void selectGuildById(long guildId) {
        for (int i = 0; i < guildCombo.getItemCount(); i++) {
            GuildOption option = guildCombo.getItemAt(i);
            if (option.id() == guildId) {
                guildCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectChannelById(long channelId) {
        for (int i = 0; i < channelCombo.getItemCount(); i++) {
            ChannelOption option = channelCombo.getItemAt(i);
            if (option.id() == channelId) {
                channelCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void loadConfigIntoFields() {
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            log("No config found yet. Enter token and prefix, then press Start.");
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
        } catch (IOException e) {
            log("Could not read config: " + e.getMessage());
            return;
        }

        tokenField.setText(props.getProperty("bot.token", ""));
        prefixField.setText(props.getProperty("bot.prefix", "!"));
        String languageCode = props.getProperty("bot.language", "en").trim();
        selectLanguage(languageCode);
    }

    private void saveConfigFromFields() throws IOException {
        Path configPath = resolveConfigPath();
        String token = new String(tokenField.getPassword()).trim();
        String prefix = prefixField.getText().trim();
        LanguageOption selected = (LanguageOption) languageCombo.getSelectedItem();
        String language = selected == null ? "en" : selected.code();

        if (token.isEmpty()) {
            throw new IllegalStateException("Token cannot be empty.");
        }

        if (prefix.isEmpty()) {
            prefix = "!";
            prefixField.setText(prefix);
        }

        Properties props = new Properties();
        if (configPath.getParent() != null) {
            Files.createDirectories(configPath.getParent());
        }

        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            }
        }

        props.setProperty("bot.token", token);
        props.setProperty("bot.prefix", prefix);
        props.setProperty("bot.language", language);
        props.putIfAbsent("youtube.poToken", "");
        props.putIfAbsent("youtube.visitorData", "");

        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "ModernMusicBot settings");
        }

        log("Config saved to: " + configPath.toAbsolutePath());
    }

    private Path resolveConfigPath() {
        return isDesktopOnboardingEnabled() ? DESKTOP_CONFIG_PATH : CONFIG_PATH;
    }

    private void showFirstLaunchBannerIfNeeded() {
        if (!isDesktopOnboardingEnabled()) {
            return;
        }

        try {
            Files.createDirectories(APP_HOME_PATH);
            if (Files.exists(FIRST_LAUNCH_MARKER)) {
                return;
            }

            showChecklistDialog();
            Files.writeString(FIRST_LAUNCH_MARKER, "shown\n");
        } catch (Exception ex) {
            log("Could not show first-launch banner: " + ex.getMessage());
        }
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem checklistItem = new JMenuItem("First Launch Checklist");
        checklistItem.addActionListener(e -> showChecklistDialog());
        helpMenu.add(checklistItem);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void showChecklistDialog() {
        String message = String.join("\n",
                "Welcome to ModernMusicBot.",
                "",
                "Before first run, make sure:",
                "- You have a Discord bot token (Discord Developer Portal).",
                "- The bot is invited with required permissions (voice + messages).",
                "- Internet access is available.",
                "- This app can write files in its folder (config + database).",
                "- Optional for better YouTube reliability: set youtube.poToken and youtube.visitorData.",
                "",
                "Desktop installers already include Java runtime."
        );

        JOptionPane.showMessageDialog(frame, message, "First Launch Checklist", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean isDesktopOnboardingEnabled() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("mac") || osName.contains("win");
    }

    private void log(String message) {
        String line = "[" + LocalTime.now().format(TIME_FORMAT) + "] " + message;
        SwingUtilities.invokeLater(() -> {
            console.append(line + System.lineSeparator());
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void selectLanguage(String languageCode) {
        LanguageOption option = Arrays.stream(LANGUAGE_OPTIONS)
                .filter(item -> item.code().equalsIgnoreCase(languageCode))
                .findFirst()
                .orElse(LANGUAGE_OPTIONS[0]);
        languageCombo.setSelectedItem(option);
    }

    private record LanguageOption(String label, String code) {
        @Override
        public String toString() {
            return label + " (" + code + ")";
        }
    }

    @FunctionalInterface
    private interface DesktopAction {
        void apply(BotRuntime runtime, long guildId, long channelId);
    }

    private record GuildOption(long id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record ChannelOption(long id, String name) {
        @Override
        public String toString() {
            return "#" + name;
        }
    }
}
