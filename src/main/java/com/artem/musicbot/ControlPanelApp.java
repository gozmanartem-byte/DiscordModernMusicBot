package com.artem.musicbot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
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
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
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
    private final ExecutorService realtimeWorker = Executors.newSingleThreadExecutor();

    private JFrame frame;
    private JPasswordField tokenField;
    private JTextField prefixField;
    private JComboBox<LanguageOption> languageCombo;
    private JComboBox<LanguageOption> controlPanelLanguageCombo;
    private JTextArea console;
    private JButton startButton;
    private JButton stopButton;
    private JButton saveButton;
    private JButton clearConsoleButton;
    private JButton copySummaryButton;
    private JComboBox<GuildOption> guildCombo;
    private JComboBox<ChannelOption> channelCombo;
    private JTextField addSongField;
    private JButton addSongButton;
    private JButton playNextButton;
    private JButton pauseButton;
    private JButton resumeButton;
    private JButton skipButton;
    private JButton stopPlaybackButton;
    private JTextField removeIndexField;
    private JButton removeQueueButton;
    private JButton shuffleQueueButton;
    private JButton clearQueueButton;
    private JButton cleanupScopeButton;
    private JButton earRapeToggleButton;
    private boolean earRapeEnabled;
    private JButton refreshDesktopButton;
    private JButton launchPlayerButton;
    private JTextArea playerSummaryArea;
    private JList<String> queueList;
    private DefaultListModel<String> queueListModel;
    private Timer desktopRefreshTimer;
    private String controlPanelLanguageCode = "en";
    private Color brandAccent = new Color(52, 120, 214);
    private Color brandText = new Color(22, 29, 36);
    private Color brandBackground = new Color(236, 242, 248);
    private Color brandPanel = new Color(248, 251, 255);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ControlPanelApp().show());
    }

    private void show() {
        loadControlPanelLanguagePreference();
        applyBrandTheme();
        frame = new JFrame(ui("appTitle"));
        applyAppIcon();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(brandBackground);
        frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        if (isDesktopOnboardingEnabled()) {
            frame.setJMenuBar(buildMenuBar());
        }

        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createTitledBorder(ui("settings")));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        top.add(new JLabel(ui("botToken") + ":"), c);

        tokenField = new JPasswordField();
        tokenField.setPreferredSize(new Dimension(420, 26));
        c.gridx = 1;
        c.weightx = 1.0;
        top.add(tokenField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        top.add(new JLabel(ui("prefix") + ":"), c);

        prefixField = new JTextField("!");
        c.gridx = 1;
        c.weightx = 1.0;
        top.add(prefixField, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        top.add(new JLabel(ui("botLanguage") + ":"), c);

        languageCombo = new JComboBox<>(LANGUAGE_OPTIONS);
        languageCombo.setSelectedIndex(0);
        c.gridx = 1;
        c.weightx = 1.0;
        top.add(languageCombo, c);

        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0;
        top.add(new JLabel(ui("controlPanelLanguage") + ":"), c);

        controlPanelLanguageCombo = new JComboBox<>(LANGUAGE_OPTIONS);
        controlPanelLanguageCombo.setSelectedIndex(0);
        c.gridx = 1;
        c.weightx = 1.0;
        top.add(controlPanelLanguageCombo, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        startButton = new JButton(ui("start"));
        stopButton = new JButton(ui("stop"));
        saveButton = new JButton(ui("saveSettings"));
        clearConsoleButton = new JButton(ui("clearConsole"));
        copySummaryButton = new JButton(ui("copySummary"));
        stylePrimaryButton(startButton);
        styleSecondaryButton(stopButton);
        styleSecondaryButton(saveButton);
        styleSecondaryButton(clearConsoleButton);
        styleSecondaryButton(copySummaryButton);
        stopButton.setEnabled(false);
        buttons.add(startButton);
        buttons.add(stopButton);
        buttons.add(saveButton);
        buttons.add(clearConsoleButton);
        buttons.add(copySummaryButton);

        console = new JTextArea();
        console.setEditable(false);
        console.setBackground(new Color(18, 24, 32));
        console.setForeground(new Color(220, 233, 246));
        JScrollPane scrollPane = new JScrollPane(console);
        scrollPane.setBorder(BorderFactory.createTitledBorder(ui("console")));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        centerPanel.add(buttons, BorderLayout.NORTH);
        if (isDesktopOnboardingEnabled()) {
            centerPanel.add(buildDesktopPlayerPanel(), BorderLayout.CENTER);
        }

        frame.add(top, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);

        scrollPane.setPreferredSize(new Dimension(900, 180));
        if (isDesktopOnboardingEnabled()) {
            frame.setSize(1024, 860);
            frame.setMinimumSize(new Dimension(980, 800));
        } else {
            frame.setSize(900, 620);
            frame.setMinimumSize(new Dimension(860, 560));
        }

        loadConfigIntoFields();
        wireActions();
        wireKeyboardShortcuts();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        log(ui("controlPanelReady"));
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
                realtimeWorker.shutdownNow();
                worker.shutdownNow();
                runtime.stop(ControlPanelApp.this::log);
            }
        });
    }

    private void applyAppIcon() {
        try (InputStream iconStream = ControlPanelApp.class.getResourceAsStream("/app-icon.png")) {
            if (iconStream == null) {
                return;
            }
            Image appIcon = ImageIO.read(iconStream);
            if (appIcon == null) {
                return;
            }
            frame.setIconImage(appIcon);
            if (Taskbar.isTaskbarSupported()) {
                Taskbar.getTaskbar().setIconImage(appIcon);
            }
        } catch (Exception ignored) {
            // Keep startup resilient if icon loading is unsupported on a platform.
        }
    }

    private void wireActions() {
        saveButton.addActionListener(e -> {
            try {
                saveConfigFromFields();
                log(ui("settingsSaved"));
            } catch (Exception ex) {
                showError(ui("saveFailed") + ": " + ex.getMessage());
            }
        });

        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            worker.submit(() -> {
                try {
                    saveConfigFromFields();
                    log(ui("startingBot"));
                    runtime.start(resolveConfigPath(), true, this::log);
                    SwingUtilities.invokeLater(() -> {
                        stopButton.setEnabled(true);
                        startButton.setEnabled(false);
                        setDesktopControlsEnabled(true);
                        refreshCleanupScopeButtonAsync();
                    });
                    refreshDesktopSelectorsAsync();
                    refreshPlayerSummaryAsync();
                } catch (Exception ex) {
                    log(ui("startFailed") + ": " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
                }
            });
        });

        stopButton.addActionListener(e -> worker.submit(() -> {
            runtime.stop(this::log);
            SwingUtilities.invokeLater(() -> {
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                setEarRapeEnabled(false);
                setDesktopControlsEnabled(false);
                setCleanupScopeButtonState(false, false);
                if (playerSummaryArea != null) {
                    playerSummaryArea.setText(ui("botNotRunning"));
                }
            });
        }));

        clearConsoleButton.addActionListener(e -> console.setText(""));

        copySummaryButton.addActionListener(e -> {
            if (playerSummaryArea == null) {
                showError("Player summary is not available on this platform mode.");
                return;
            }
            String summary = playerSummaryArea.getText();
            if (summary == null || summary.isBlank()) {
                showError("Nothing to copy yet.");
                return;
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(summary), null);
            log("Player summary copied to clipboard.");
        });

        if (isDesktopOnboardingEnabled()) {
            guildCombo.addActionListener(ignored -> refreshChannelsAsync());
            guildCombo.addActionListener(ignored -> refreshPlayerSummaryAsync());
            guildCombo.addActionListener(ignored -> setEarRapeEnabled(false));

            addSongButton.addActionListener(ignored -> submitDesktopEnqueue(false));
            playNextButton.addActionListener(ignored -> submitDesktopEnqueue(true));

            addSongField.addActionListener(ignored -> addSongButton.doClick());

            pauseButton.addActionListener(ignored -> desktopControlAsync("pause", BotRuntime::pauseFromDesktop));
            resumeButton.addActionListener(ignored -> desktopControlAsync("resume", BotRuntime::resumeFromDesktop));
            skipButton.addActionListener(ignored -> desktopControlAsync("skip", BotRuntime::skipFromDesktop));
            stopPlaybackButton.addActionListener(ignored -> desktopControlAsync("stop", BotRuntime::stopFromDesktop));
            removeQueueButton.addActionListener(ignored -> worker.submit(() -> {
                Long guildId = selectedGuildId();
                Long channelId = selectedChannelId();
                if (guildId == null || channelId == null) {
                    SwingUtilities.invokeLater(() -> showError(ui("selectGuildTextChannelFirst")));
                    return;
                }

                int index = queueList == null ? -1 : queueList.getSelectedIndex() + 1;
                if (index <= 0) {
                    String raw = removeIndexField.getText().trim();
                    if (raw.isBlank()) {
                        SwingUtilities.invokeLater(() -> showError(ui("queueIndexRequired")));
                        return;
                    }

                    try {
                        index = Integer.parseInt(raw);
                    } catch (NumberFormatException ex) {
                        SwingUtilities.invokeLater(() -> showError(ui("queueIndexRequired")));
                        return;
                    }
                }

                if (index < 1) {
                    SwingUtilities.invokeLater(() -> showError(ui("queueIndexRequired")));
                    return;
                }

                try {
                    runtime.removeQueueFromDesktop(guildId, channelId, index);
                    log(ui("desktopAction") + ": remove #" + index);
                    refreshPlayerSummaryAsync();
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
                }
            }));
            shuffleQueueButton.addActionListener(ignored -> desktopControlAsync("shuffle", BotRuntime::shuffleQueueFromDesktop));
            clearQueueButton.addActionListener(ignored -> desktopControlAsync("clear", BotRuntime::clearQueueFromDesktop));
            cleanupScopeButton.addActionListener(ignored -> worker.submit(() -> {
                if (!runtime.isRunning()) {
                    SwingUtilities.invokeLater(() -> showError(ui("botNotRunning")));
                    return;
                }

                try {
                    boolean nextBotOnly = !runtime.isStopCleanupBotOnly();
                    runtime.setStopCleanupBotOnly(nextBotOnly);
                    log(ui("desktopAction") + ": " + (nextBotOnly ? ui("cleanupScopeBotOnly") : ui("cleanupScopeAll")));
                    setCleanupScopeButtonState(true, nextBotOnly);
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
                }
            }));
            earRapeToggleButton.addActionListener(ignored -> {
                final boolean targetState = !earRapeEnabled;
                Long guildId = selectedGuildId();
                Long channelId = selectedChannelId();
                if (guildId == null || channelId == null) {
                    showError(ui("selectGuildTextChannelFirst"));
                    return;
                }

                setEarRapeEnabled(targetState);
                earRapeToggleButton.setEnabled(false);
                realtimeWorker.submit(() -> {
                    try {
                        if (targetState) {
                            runtime.enableEarRapeFromDesktop(guildId, channelId);
                        } else {
                            runtime.disableEarRapeFromDesktop(guildId, channelId);
                        }
                        refreshPlayerSummaryAsync();
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            setEarRapeEnabled(!targetState);
                            showError(ex.getMessage());
                        });
                    } finally {
                        SwingUtilities.invokeLater(() -> earRapeToggleButton.setEnabled(runtime.isRunning()));
                    }
                });
            });
            refreshDesktopButton.addActionListener(ignored -> {
                refreshDesktopSelectorsAsync();
                refreshPlayerSummaryAsync();
            });
            launchPlayerButton.addActionListener(ignored -> worker.submit(() -> {
                Long guildId = selectedGuildId();
                Long channelId = selectedChannelId();
                if (guildId == null || channelId == null) {
                    SwingUtilities.invokeLater(() -> showError(ui("selectGuildTextChannelFirst")));
                    return;
                }

                try {
                    runtime.removePlayerPanelFromDesktop(guildId, channelId);
                    log(ui("playerPanelRemoved"));
                    refreshPlayerSummaryAsync();
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
                }
            }));
        }
    }

    private JPanel buildDesktopPlayerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(ui("desktopPlayer")));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        guildCombo = new JComboBox<>();
        channelCombo = new JComboBox<>();
        addSongField = new JTextField();
        addSongButton = new JButton(ui("addSong"));
        playNextButton = new JButton(ui("playNext"));
        stylePrimaryButton(addSongButton);
        styleSecondaryButton(playNextButton);
        pauseButton = new JButton(ui("pause"));
        resumeButton = new JButton(ui("resume"));
        skipButton = new JButton(ui("skip"));
        stopPlaybackButton = new JButton(ui("stop"));
        removeIndexField = new JTextField(4);
        removeIndexField.setToolTipText(ui("queueIndexHint"));
        removeQueueButton = new JButton(ui("queueRemove"));
        shuffleQueueButton = new JButton(ui("queueShuffle"));
        clearQueueButton = new JButton(ui("queueClear"));
        styleSecondaryButton(pauseButton);
        styleSecondaryButton(resumeButton);
        styleSecondaryButton(skipButton);
        styleSecondaryButton(stopPlaybackButton);
        styleSecondaryButton(removeQueueButton);
        styleSecondaryButton(shuffleQueueButton);
        styleSecondaryButton(clearQueueButton);
        cleanupScopeButton = new JButton();
        styleSecondaryButton(cleanupScopeButton);
        setCleanupScopeButtonState(false, false);
        earRapeToggleButton = new JButton();
        styleSecondaryButton(earRapeToggleButton);
        setEarRapeEnabled(false);
        refreshDesktopButton = new JButton(ui("refreshLists"));
        launchPlayerButton = new JButton(ui("removePlayer"));
        styleSecondaryButton(refreshDesktopButton);
        stylePrimaryButton(launchPlayerButton);
        playerSummaryArea = new JTextArea(16, 58);
        playerSummaryArea.setEditable(false);
        playerSummaryArea.setBackground(new Color(20, 27, 36));
        playerSummaryArea.setForeground(new Color(218, 232, 246));
        playerSummaryArea.setText(ui("botNotRunning"));
        queueListModel = new DefaultListModel<>();
        queueList = new JList<>(queueListModel);
        queueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queueList.setVisibleRowCount(10);
        queueList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selected = queueList.getSelectedIndex();
                if (selected >= 0) {
                    removeIndexField.setText(String.valueOf(selected + 1));
                }
            }
        });

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        panel.add(new JLabel(ui("guild") + ":"), c);

        c.gridx = 1;
        c.weightx = 1;
        panel.add(guildCombo, c);

        c.gridx = 2;
        c.weightx = 0;
        panel.add(refreshDesktopButton, c);

        c.gridx = 3;
        panel.add(launchPlayerButton, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        panel.add(new JLabel(ui("textChannel") + ":"), c);

        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        panel.add(channelCombo, c);
        c.gridwidth = 1;

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        panel.add(new JLabel(ui("songUrl") + ":"), c);

        c.gridx = 1;
        c.weightx = 1;
        panel.add(addSongField, c);

        c.gridx = 2;
        c.weightx = 0;
        panel.add(addSongButton, c);

        c.gridx = 3;
        panel.add(playNextButton, c);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.add(pauseButton);
        controls.add(resumeButton);
        controls.add(skipButton);
        controls.add(stopPlaybackButton);
        controls.add(cleanupScopeButton);
        controls.add(earRapeToggleButton);

        controls.add(new JLabel(ui("queueIndex")));
        controls.add(removeIndexField);
        controls.add(removeQueueButton);
        controls.add(shuffleQueueButton);
        controls.add(clearQueueButton);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 4;
        panel.add(controls, c);

        c.gridwidth = 3;
        c.gridy = 4;
        c.gridx = 0;
        c.weighty = 1.0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(playerSummaryArea), c);

        c.gridx = 3;
        c.gridwidth = 1;
        c.weightx = 0.55;
        JScrollPane queueScroll = new JScrollPane(queueList);
        queueScroll.setBorder(BorderFactory.createTitledBorder(ui("queueList")));
        panel.add(queueScroll, c);

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
        playNextButton.setEnabled(enabled);
        pauseButton.setEnabled(enabled);
        resumeButton.setEnabled(enabled);
        skipButton.setEnabled(enabled);
        stopPlaybackButton.setEnabled(enabled);
        removeIndexField.setEnabled(enabled);
        removeQueueButton.setEnabled(enabled);
        shuffleQueueButton.setEnabled(enabled);
        clearQueueButton.setEnabled(enabled);
        cleanupScopeButton.setEnabled(enabled);
        earRapeToggleButton.setEnabled(enabled);
        refreshDesktopButton.setEnabled(enabled);
        launchPlayerButton.setEnabled(enabled);
    }

    private void refreshCleanupScopeButtonAsync() {
        worker.submit(() -> {
            boolean botOnly = runtime.isStopCleanupBotOnly();
            setCleanupScopeButtonState(runtime.isRunning(), botOnly);
        });
    }

    private void setCleanupScopeButtonState(boolean enabled, boolean botOnly) {
        SwingUtilities.invokeLater(() -> {
            if (cleanupScopeButton == null) {
                return;
            }
            cleanupScopeButton.setEnabled(enabled);
            cleanupScopeButton.setText(ui("cleanupScope") + ": " + ui(botOnly ? "cleanupScopeBotOnly" : "cleanupScopeAll"));
        });
    }

    private void setEarRapeEnabled(boolean enabled) {
        earRapeEnabled = enabled;
        if (earRapeToggleButton != null) {
            earRapeToggleButton.setText("EarRape: " + ui(enabled ? "toggleOn" : "toggleOff"));
            earRapeToggleButton.setBackground(enabled ? new Color(192, 57, 43) : new Color(232, 237, 244));
            earRapeToggleButton.setForeground(enabled ? new Color(255, 247, 246) : brandText);
        }
    }

    private void wireKeyboardShortcuts() {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        frame.getRootPane().registerKeyboardAction(
            e -> {
                if (addSongField != null && addSongField.isEnabled()) {
                    addSongField.requestFocusInWindow();
                    addSongField.selectAll();
                }
            },
            javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_L, menuMask),
            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void applyBrandTheme() {
        brandAccent = resolveAccentFromIcon();
        brandText = new Color(22, 29, 36);
        brandBackground = tintColor(brandAccent, 0.90f);
        brandPanel = tintColor(brandAccent, 0.95f);

        UIManager.put("Panel.background", brandPanel);
        UIManager.put("OptionPane.background", brandPanel);
        UIManager.put("Label.foreground", brandText);
        UIManager.put("Button.background", new Color(232, 237, 244));
        UIManager.put("Button.foreground", brandText);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("PasswordField.background", Color.WHITE);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("TextArea.background", Color.WHITE);
        UIManager.put("TextArea.foreground", brandText);
        UIManager.put("TitledBorder.titleColor", brandText);
    }

    private Color resolveAccentFromIcon() {
        try (InputStream iconStream = ControlPanelApp.class.getResourceAsStream("/app-icon.png")) {
            if (iconStream == null) {
                return new Color(52, 120, 214);
            }
            var image = ImageIO.read(iconStream);
            if (image == null) {
                return new Color(52, 120, 214);
            }

            long sumR = 0;
            long sumG = 0;
            long sumB = 0;
            long count = 0;

            int step = Math.max(1, Math.min(image.getWidth(), image.getHeight()) / 64);
            for (int y = 0; y < image.getHeight(); y += step) {
                for (int x = 0; x < image.getWidth(); x += step) {
                    int argb = image.getRGB(x, y);
                    int a = (argb >>> 24) & 0xff;
                    if (a < 20) {
                        continue;
                    }
                    int r = (argb >>> 16) & 0xff;
                    int g = (argb >>> 8) & 0xff;
                    int b = argb & 0xff;
                    if (r + g + b < 45) {
                        continue;
                    }
                    sumR += r;
                    sumG += g;
                    sumB += b;
                    count++;
                }
            }

            if (count == 0) {
                return new Color(52, 120, 214);
            }

            int r = (int) (sumR / count);
            int g = (int) (sumG / count);
            int b = (int) (sumB / count);

            // Keep accent vivid enough so controls feel branded instead of gray.
            r = Math.max(40, Math.min(215, (int) (r * 1.10)));
            g = Math.max(60, Math.min(220, (int) (g * 1.05)));
            b = Math.max(90, Math.min(235, (int) (b * 1.08)));
            return new Color(r, g, b);
        } catch (Exception ignored) {
            return new Color(52, 120, 214);
        }
    }

    private Color tintColor(Color color, float amountToWhite) {
        float mix = Math.max(0.0f, Math.min(1.0f, amountToWhite));
        int r = (int) (color.getRed() * (1.0f - mix) + 255 * mix);
        int g = (int) (color.getGreen() * (1.0f - mix) + 255 * mix);
        int b = (int) (color.getBlue() * (1.0f - mix) + 255 * mix);
        return new Color(r, g, b);
    }

    private void stylePrimaryButton(JButton button) {
        if (isMacUi()) {
            // macOS Aqua can ignore custom backgrounds; use accent text + border for readability.
            button.setBackground(new Color(245, 249, 255));
            button.setForeground(brandAccent.darker());
            button.setBorder(BorderFactory.createLineBorder(brandAccent, 2, true));
            button.setOpaque(false);
            button.putClientProperty("JButton.buttonType", "roundRect");
        } else {
            button.setBackground(brandAccent);
            button.setForeground(new Color(247, 251, 255));
            button.setOpaque(true);
        }
        button.setFocusPainted(false);
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(new Color(232, 237, 244));
        button.setForeground(brandText);
        if (isMacUi()) {
            button.setOpaque(false);
            button.putClientProperty("JButton.buttonType", "roundRect");
        } else {
            button.setOpaque(true);
        }
        button.setFocusPainted(false);
    }

    private boolean isMacUi() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
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
            List<String> queue = guildId == null ? List.of() : runtime.playerQueueForGuild(guildId);
            SwingUtilities.invokeLater(() -> {
                playerSummaryArea.setText(summary);
                if (queueListModel != null) {
                    queueListModel.clear();
                    for (int i = 0; i < queue.size(); i++) {
                        queueListModel.addElement((i + 1) + ". " + queue.get(i));
                    }
                }
            });
        });
    }

    private void desktopControlAsync(String actionName, DesktopAction action) {
        worker.submit(() -> {
            Long guildId = selectedGuildId();
            Long channelId = selectedChannelId();
            if (guildId == null || channelId == null) {
                SwingUtilities.invokeLater(() -> showError(ui("selectGuildTextChannelFirst")));
                return;
            }

            try {
                action.apply(runtime, guildId, channelId);
                log(ui("desktopAction") + ": " + actionName);
                refreshPlayerSummaryAsync();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
            }
        });
    }

    private void submitDesktopEnqueue(boolean playNext) {
        worker.submit(() -> {
            Long guildId = selectedGuildId();
            Long channelId = selectedChannelId();
            String query = addSongField.getText().trim();
            if (guildId == null || channelId == null || query.isBlank()) {
                SwingUtilities.invokeLater(() -> showError(ui("selectGuildChannelSong")));
                return;
            }

            try {
                String enqueueQuery = query;
                if (isSearchQuery(query)) {
                    List<BotRuntime.SearchTrackOptionRef> options = runtime.searchTracksFromDesktop(query, 3);
                    if (options.isEmpty()) {
                        SwingUtilities.invokeLater(() -> showError(ui("nothingFound") + ": " + query));
                        return;
                    }

                    int selected = pickSearchResultIndex(query, options);
                    if (selected < 0) {
                        log(ui("desktopSearchCancelled") + ": " + query);
                        return;
                    }

                    enqueueQuery = options.get(selected).uri();
                }

                if (playNext) {
                    runtime.addSongNextFromDesktop(guildId, channelId, enqueueQuery);
                    log(ui("desktopQueuedNext") + ": " + query);
                } else {
                    runtime.addSongFromDesktop(guildId, channelId, enqueueQuery);
                    log(ui("desktopQueued") + ": " + query);
                }

                SwingUtilities.invokeLater(() -> addSongField.setText(""));
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
            log(ui("noConfigFound"));
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
        } catch (IOException e) {
            log(ui("couldNotReadConfig") + ": " + e.getMessage());
            return;
        }

        tokenField.setText(props.getProperty("bot.token", ""));
        prefixField.setText(props.getProperty("bot.prefix", "!"));
        String languageCode = props.getProperty("bot.language", "en").trim();
        String controlLanguageCode = props.getProperty("app.language", "en").trim();
        selectLanguage(languageCode);
        selectControlPanelLanguage(controlLanguageCode);
    }

    private void saveConfigFromFields() throws IOException {
        Path configPath = resolveConfigPath();
        String token = new String(tokenField.getPassword()).trim();
        String prefix = prefixField.getText().trim();
        LanguageOption selected = (LanguageOption) languageCombo.getSelectedItem();
        String language = selected == null ? "en" : selected.code();
        LanguageOption selectedControl = (LanguageOption) controlPanelLanguageCombo.getSelectedItem();
        String controlLanguage = selectedControl == null ? "en" : selectedControl.code();

        if (token.isEmpty()) {
            throw new IllegalStateException(ui("tokenCannotBeEmpty"));
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
        props.setProperty("app.language", controlLanguage);
        props.putIfAbsent("youtube.poToken", "");
        props.putIfAbsent("youtube.visitorData", "");

        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "ModernMusicBot settings");
        }

        log(ui("configSavedTo") + ": " + configPath.toAbsolutePath());
        if (!controlLanguage.equalsIgnoreCase(controlPanelLanguageCode)) {
            log(ui("restartRequired"));
        }
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
            log(ui("couldNotShowFirstLaunch") + ": " + ex.getMessage());
        }
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu(ui("help"));
        JMenuItem checklistItem = new JMenuItem(ui("firstLaunchChecklist"));
        checklistItem.addActionListener(e -> showChecklistDialog());
        helpMenu.add(checklistItem);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void showChecklistDialog() {
        String message = String.join("\n",
            ui("welcome"),
                "",
            ui("beforeFirstRun"),
            "- " + ui("checkToken"),
            "- " + ui("checkInvite"),
            "- " + ui("checkInternet"),
            "- " + ui("checkWrite"),
            "- " + ui("checkYoutube"),
                "",
            ui("javaIncluded")
        );

        JOptionPane.showMessageDialog(frame, message, ui("firstLaunchChecklist"), JOptionPane.INFORMATION_MESSAGE);
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
        JOptionPane.showMessageDialog(frame, message, ui("error"), JOptionPane.ERROR_MESSAGE);
    }

    private void loadControlPanelLanguagePreference() {
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            controlPanelLanguageCode = "en";
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
            controlPanelLanguageCode = props.getProperty("app.language", "en").trim().toLowerCase();
        } catch (Exception ignored) {
            controlPanelLanguageCode = "en";
        }
    }

    private String ui(String key) {
        String lang = controlPanelLanguageCode == null ? "en" : controlPanelLanguageCode.toLowerCase();
        return switch (lang) {
            case "ru" -> uiRu(key);
            case "hy" -> uiHy(key);
            case "ka" -> uiKa(key);
            case "az" -> uiAz(key);
            case "kk" -> uiKk(key);
            case "uz" -> uiUz(key);
            case "uk" -> uiUk(key);
            case "de" -> uiDe(key);
            case "es" -> uiEs(key);
            case "it" -> uiIt(key);
            case "pt" -> uiPt(key);
            case "zh" -> uiZh(key);
            case "ja" -> uiJa(key);
            default -> uiEn(key);
        };
    }

    private String uiEn(String key) {
        return switch (key) {
            case "appTitle" -> "ModernMusicBot Control Panel";
            case "settings" -> "Settings";
            case "botToken" -> "Bot Token";
            case "prefix" -> "Prefix";
            case "botLanguage" -> "Bot Language";
            case "controlPanelLanguage" -> "Control Panel Language";
            case "start" -> "Start";
            case "stop" -> "Stop";
            case "saveSettings" -> "Save Settings";
            case "console" -> "Console";
            case "desktopPlayer" -> "Host Player (Desktop)";
            case "addSong" -> "Add Song";
            case "playNext" -> "Play Next";
            case "pause" -> "Pause";
            case "resume" -> "Resume";
            case "skip" -> "Skip";
            case "refreshLists" -> "Refresh Lists";
            case "removePlayer" -> "Remove Player in Discord";
            case "botNotRunning" -> "Bot is not running.";
            case "guild" -> "Guild";
            case "textChannel" -> "Text channel";
            case "songUrl" -> "Song / URL";
            case "restartRequired" -> "Restart app to apply control panel language change.";
            case "error" -> "Error";
            case "controlPanelReady" -> "Control panel ready.";
            case "settingsSaved" -> "Settings saved.";
            case "saveFailed" -> "Failed to save settings";
            case "startingBot" -> "Starting bot...";
            case "startFailed" -> "Start failed";
            case "selectGuildChannelSong" -> "Select guild/channel and enter a song or URL.";
            case "nothingFound" -> "Nothing found";
            case "selectGuildTextChannelFirst" -> "Select guild and text channel first.";
            case "noConfigFound" -> "No config found yet. Enter token and prefix, then press Start.";
            case "couldNotReadConfig" -> "Could not read config";
            case "tokenCannotBeEmpty" -> "Token cannot be empty.";
            case "configSavedTo" -> "Config saved to";
            case "couldNotShowFirstLaunch" -> "Could not show first-launch banner";
            case "help" -> "Help";
            case "firstLaunchChecklist" -> "First Launch Checklist";
            case "welcome" -> "Welcome to ModernMusicBot.";
            case "beforeFirstRun" -> "Before first run, make sure:";
            case "checkToken" -> "You have a Discord bot token (Discord Developer Portal).";
            case "checkInvite" -> "The bot is invited with required permissions (voice + messages).";
            case "checkInternet" -> "Internet access is available.";
            case "checkWrite" -> "This app can write files in its folder (config + database).";
            case "checkYoutube" -> "Optional for better YouTube reliability: set youtube.poToken and youtube.visitorData.";
            case "javaIncluded" -> "Desktop installers already include Java runtime.";
            case "desktopSearchCancelled" -> "Desktop search cancelled";
            case "desktopQueued" -> "Desktop queued";
            case "desktopQueuedNext" -> "Queued as next";
            case "playerPanelRemoved" -> "Removed player panel from Discord.";
            case "desktopAction" -> "Desktop action";
            case "chooseTrackFor" -> "Choose a track for";
            case "searchResults" -> "Search Results";
            case "couldNotShowSearchChoices" -> "Could not show search choices";
            case "clearConsole" -> "Clear Console";
            case "copySummary" -> "Copy Summary";
            case "queueIndex" -> "Queue #";
            case "queueIndexHint" -> "Track number in queue (1..n)";
            case "queueList" -> "Queue (select item to remove)";
            case "queueRemove" -> "Remove";
            case "queueShuffle" -> "Shuffle";
            case "queueClear" -> "Clear Queue";
            case "queueIndexRequired" -> "Enter a valid queue index (1..n).";
            case "cleanupScope" -> "Cleanup";
            case "cleanupScopeAll" -> "All";
            case "cleanupScopeBotOnly" -> "Bot Only";
            case "toggleOn" -> "On";
            case "toggleOff" -> "Off";
            default -> key;
        };
    }

    private String uiRu(String key) {
        return switch (key) {
            case "appTitle" -> "Панель управления ModernMusicBot";
            case "settings" -> "Настройки";
            case "botToken" -> "Токен бота";
            case "prefix" -> "Префикс";
            case "botLanguage" -> "Язык бота";
            case "controlPanelLanguage" -> "Язык панели управления";
            case "start" -> "Старт";
            case "stop" -> "Стоп";
            case "saveSettings" -> "Сохранить настройки";
            case "console" -> "Консоль";
            case "desktopPlayer" -> "Плеер хоста (Desktop)";
            case "addSong" -> "Добавить песню";
            case "playNext" -> "Играть следующей";
            case "pause" -> "Пауза";
            case "resume" -> "Продолжить";
            case "skip" -> "Пропуск";
            case "refreshLists" -> "Обновить списки";
            case "removePlayer" -> "Убрать плеер из Discord";
            case "clearConsole" -> "Очистить консоль";
            case "copySummary" -> "Копировать сводку";
            case "queueIndex" -> "№ в очереди";
            case "queueIndexHint" -> "Номер трека в очереди (1..n)";
            case "queueList" -> "Очередь (выберите трек для удаления)";
            case "queueRemove" -> "Удалить";
            case "queueShuffle" -> "Перемешать";
            case "queueClear" -> "Очистить очередь";
            case "queueIndexRequired" -> "Введите корректный номер в очереди (1..n).";
            case "cleanupScope" -> "Очистка";
            case "cleanupScopeAll" -> "Все";
            case "cleanupScopeBotOnly" -> "Только бот";
            case "desktopQueuedNext" -> "Добавлено следующей";
            case "botNotRunning" -> "Бот не запущен.";
            case "guild" -> "Сервер";
            case "textChannel" -> "Текстовый канал";
            case "songUrl" -> "Песня / URL";
            case "restartRequired" -> "Перезапустите приложение, чтобы применить язык панели.";
            case "error" -> "Ошибка";
            case "toggleOn" -> "Вкл";
            case "toggleOff" -> "Выкл";
            default -> uiEn(key);
        };
    }

    private String uiLocalizedFallback(String key, String lang) {
        String value = switch (key) {
            case "controlPanelReady" -> byLang(lang, "Control panel ready.", "Կառավարման վահանակը պատրաստ է։", "მართვის პანელი მზადაა.", "İdarəetmə paneli hazırdır.", "Басқару панелі дайын.", "Boshqaruv paneli tayyor.", "Панель керування готова.", "Kontrollzentrum bereit.", "Panel de control listo.", "Pannello di controllo pronto.", "Painel de controle pronto.", "控制面板已就绪。", "コントロールパネルの準備ができました。");
            case "settingsSaved" -> byLang(lang, "Settings saved.", "Կարգավորումները պահպանվել են։", "პარამეტრები შენახულია.", "Ayarlar saxlanıldı.", "Параметрлер сақталды.", "Sozlamalar saqlandi.", "Налаштування збережено.", "Einstellungen gespeichert.", "Configuración guardada.", "Impostazioni salvate.", "Configurações salvas.", "设置已保存。", "設定を保存しました。" );
            case "saveFailed" -> byLang(lang, "Failed to save settings", "Չհաջողվեց պահպանել կարգավորումները", "პარამეტრების შენახვა ვერ მოხერხდა", "Ayarları saxlamaq alınmadı", "Параметрлерді сақтау сәтсіз", "Sozlamalarni saqlash muvaffaqiyatsiz", "Не вдалося зберегти налаштування", "Speichern der Einstellungen fehlgeschlagen", "Error al guardar la configuración", "Salvataggio impostazioni non riuscito", "Falha ao salvar configurações", "保存设置失败", "設定の保存に失敗しました");
            case "startingBot" -> byLang(lang, "Starting bot...", "Բոտը մեկնարկում է...", "ბოტი იწყება...", "Bot işə salınır...", "Бот іске қосылуда...", "Bot ishga tushirilmoqda...", "Бот запускається...", "Bot wird gestartet...", "Iniciando bot...", "Avvio del bot...", "Iniciando bot...", "正在启动机器人...", "ボットを起動しています...");
            case "startFailed" -> byLang(lang, "Start failed", "Գործարկումը ձախողվեց", "გაშვება ვერ მოხერხდა", "Başlatma uğursuz oldu", "Іске қосу сәтсіз", "Ishga tushirish muvaffaqiyatsiz", "Запуск не вдався", "Start fehlgeschlagen", "Error al iniciar", "Avvio non riuscito", "Falha ao iniciar", "启动失败", "起動に失敗しました");
            case "selectGuildChannelSong" -> byLang(lang, "Select guild/channel and enter a song or URL.", "Ընտրեք սերվեր/ալիք և մուտքագրեք երգ կամ URL։", "აირჩიეთ სერვერი/არხი და შეიყვანეთ სიმღერა ან URL.", "Server/kanal seçin və mahnı və ya URL daxil edin.", "Сервер/арнаны таңдап, ән не URL енгізіңіз.", "Server/kanalni tanlang va qo‘shiq yoki URL kiriting.", "Виберіть сервер/канал і введіть пісню або URL.", "Server/Kanal wählen und Song oder URL eingeben.", "Selecciona servidor/canal e ingresa canción o URL.", "Seleziona server/canale e inserisci un brano o URL.", "Selecione servidor/canal e insira música ou URL.", "请选择服务器/频道并输入歌曲或 URL。", "サーバー/チャンネルを選択し、曲またはURLを入力してください。");
            case "nothingFound" -> byLang(lang, "Nothing found", "Ոչինչ չի գտնվել", "ვერაფერი მოიძებნა", "Heç nə tapılmadı", "Ештеңе табылмады", "Hech narsa topilmadi", "Нічого не знайдено", "Nichts gefunden", "No se encontró nada", "Nessun risultato", "Nada encontrado", "未找到结果", "見つかりませんでした");
            case "selectGuildTextChannelFirst" -> byLang(lang, "Select guild and text channel first.", "Սկզբում ընտրեք սերվերն ու տեքստային ալիքը։", "ჯერ აირჩიეთ სერვერი და ტექსტური არხი.", "Əvvəlcə server və mətn kanalını seçin.", "Алдымен сервер мен мәтін арнасын таңдаңыз.", "Avval server va matn kanalini tanlang.", "Спочатку виберіть сервер і текстовий канал.", "Zuerst Server und Textkanal auswählen.", "Primero selecciona servidor y canal de texto.", "Seleziona prima server e canale testuale.", "Selecione servidor e canal de texto primeiro.", "请先选择服务器和文字频道。", "先にサーバーとテキストチャンネルを選択してください。");
            case "noConfigFound" -> byLang(lang, "No config found yet. Enter token and prefix, then press Start.", "Կոնֆիգը դեռ չկա։ Մուտքագրեք token և prefix, հետո սեղմեք Start։", "კონფიგი ჯერ არ არის. შეიყვანეთ ტოკენი და პრეფიქსი, შემდეგ დააჭირეთ დაწყებას.", "Hələ konfiq tapılmadı. Token və prefiks daxil edin, sonra Start edin.", "Конфиг әлі табылмады. Токен мен префикс енгізіп, Start басыңыз.", "Hali konfiguratsiya topilmadi. Token va prefiks kiriting, so‘ng Start bosing.", "Конфіг ще не знайдено. Введіть токен і префікс, потім натисніть Старт.", "Noch keine Konfiguration gefunden. Token und Präfix eingeben, dann Start drücken.", "Aún no hay configuración. Ingresa token y prefijo y pulsa Iniciar.", "Configurazione non trovata. Inserisci token e prefisso, poi Avvia.", "Configuração ainda não encontrada. Insira token e prefixo e clique em Iniciar.", "尚未找到配置。请输入令牌和前缀，然后点击启动。", "設定がまだありません。トークンとプレフィックスを入力して開始を押してください。");
            case "tokenCannotBeEmpty" -> byLang(lang, "Token cannot be empty.", "Token-ը չի կարող դատարկ լինել։", "ტოკენი ცარიელი არ შეიძლება იყოს.", "Token boş ola bilməz.", "Токен бос болмауы керек.", "Token bo‘sh bo‘lishi mumkin emas.", "Токен не може бути порожнім.", "Token darf nicht leer sein.", "El token no puede estar vacío.", "Il token non può essere vuoto.", "O token não pode estar vazio.", "令牌不能为空。", "トークンは空にできません。");
            case "restartRequired" -> byLang(lang, "Restart app to apply control panel language change.", "Հավելվածը վերագործարկեք՝ վահանակի լեզուն կիրառելու համար։", "პანელის ენის ცვლილების გამოსაყენებლად გადატვირთეთ აპი.", "Panel dilini tətbiq etmək üçün tətbiqi yenidən başladın.", "Панель тілін қолдану үшін қолданбаны қайта іске қосыңыз.", "Panel tili o‘zgarishini qo‘llash uchun ilovani qayta ishga tushiring.", "Перезапустіть застосунок, щоб застосувати мову панелі.", "App neu starten, um die Sprache des Kontrollpanels anzuwenden.", "Reinicia la app para aplicar el idioma del panel.", "Riavvia l'app per applicare la lingua del pannello.", "Reinicie o app para aplicar o idioma do painel.", "请重启应用以应用控制面板语言更改。", "コントロールパネル言語を適用するにはアプリを再起動してください。");
            case "welcome" -> byLang(lang, "Welcome to ModernMusicBot.", "Բարի գալուստ ModernMusicBot։", "კეთილი იყოს თქვენი მობრძანება ModernMusicBot-ში.", "ModernMusicBot-a xoş gəlmisiniz.", "ModernMusicBot-қа қош келдіңіз.", "ModernMusicBot-ga xush kelibsiz.", "Ласкаво просимо до ModernMusicBot.", "Willkommen bei ModernMusicBot.", "Bienvenido a ModernMusicBot.", "Benvenuto in ModernMusicBot.", "Bem-vindo ao ModernMusicBot.", "欢迎使用 ModernMusicBot。", "ModernMusicBotへようこそ。");
            case "beforeFirstRun" -> byLang(lang, "Before first run, make sure:", "Առաջին մեկնարկից առաջ համոզվեք, որ՝", "პირველ გაშვებამდე დარწმუნდით:", "İlk işə salmadan əvvəl yoxlayın:", "Алғашқы іске қоспас бұрын тексеріңіз:", "Birinchi ishga tushirishdan oldin tekshiring:", "Перед першим запуском переконайтесь:", "Vor dem ersten Start prüfen:", "Antes del primer inicio, asegúrate de:", "Prima del primo avvio, assicurati che:", "Antes da primeira execução, verifique:", "首次启动前请确认：", "初回起動前に次を確認してください：");
            case "checkToken" -> byLang(lang, "You have a Discord bot token (Discord Developer Portal).", "Ունեք Discord bot token (Discord Developer Portal-ում)։", "გაქვთ Discord ბოტის ტოკენი (Discord Developer Portal).", "Discord bot tokeniniz var (Discord Developer Portal).", "Сізде Discord bot токені бар (Discord Developer Portal).", "Sizda Discord bot tokeni bor (Discord Developer Portal).", "У вас є токен Discord-бота (Discord Developer Portal).", "Discord-Bot-Token vorhanden (Discord Developer Portal).", "Tienes un token de bot de Discord (Discord Developer Portal).", "Hai un token bot Discord (Discord Developer Portal).", "Você tem um token de bot do Discord (Discord Developer Portal).", "你已拥有 Discord 机器人令牌（Developer Portal）。", "Discordボットトークン（Developer Portal）を用意してください。" );
            case "checkInvite" -> byLang(lang, "The bot is invited with required permissions (voice + messages).", "Բոտը հրավիրված է անհրաժեշտ իրավունքներով (voice + messages)։", "ბოტი მოწვეულია საჭირო ნებართვებით (voice + messages).", "Bot tələb olunan icazələrlə dəvət olunub (voice + messages).", "Бот қажетті рұқсаттармен шақырылған (voice + messages).", "Bot kerakli ruxsatlar bilan taklif qilingan (voice + messages).", "Бота запрошено з потрібними правами (voice + messages).", "Bot mit nötigen Rechten eingeladen (voice + messages).", "El bot está invitado con permisos requeridos (voz + mensajes).", "Il bot è invitato con i permessi richiesti (voce + messaggi).", "O bot foi convidado com permissões necessárias (voz + mensagens).", "机器人已使用所需权限邀请（语音+消息）。", "ボットを必要権限（音声+メッセージ）で招待済み。" );
            case "checkInternet" -> byLang(lang, "Internet access is available.", "Ինտերնետ կապը հասանելի է։", "ინტერნეტ წვდომა ხელმისაწვდომია.", "İnternet bağlantısı mövcuddur.", "Интернет қолжетімді.", "Internet mavjud.", "Є доступ до інтернету.", "Internetzugang vorhanden.", "Hay acceso a internet.", "Accesso a internet disponibile.", "Acesso à internet disponível.", "网络连接可用。", "インターネット接続が利用可能。" );
            case "checkWrite" -> byLang(lang, "This app can write files in its folder (config + database).", "Հավելվածը կարող է գրել ֆայլեր իր պանակում (config + database)։", "აპს შეუძლია ფაილების ჩაწერა საკუთარ საქაღალდეში (config + database).", "Tətbiq öz qovluğuna fayl yaza bilir (config + database).", "Қолданба өз қалтасына файл жаза алады (config + database).", "Ilova o‘z papkasiga yozishi mumkin (config + database).", "Застосунок може записувати файли у свою папку (config + database).", "App kann Dateien im eigenen Ordner schreiben (config + database).", "La app puede escribir archivos en su carpeta (config + database).", "L'app può scrivere file nella sua cartella (config + database).", "O app pode gravar arquivos na própria pasta (config + database).", "应用可在其目录写入文件（配置+数据库）。", "アプリが自身のフォルダーに書き込めること（config + database）。" );
            case "checkYoutube" -> byLang(lang, "Optional for better YouTube reliability: set youtube.poToken and youtube.visitorData.", "YouTube կայունության համար՝ ցանկության դեպքում լրացրեք youtube.poToken և youtube.visitorData։", "YouTube-ის სტაბილურობისთვის სურვილისამებრ შეავსეთ youtube.poToken და youtube.visitorData.", "YouTube sabitliyi üçün opsional: youtube.poToken və youtube.visitorData təyin edin.", "YouTube тұрақтылығы үшін қалауыңызша youtube.poToken және youtube.visitorData орнатыңыз.", "YouTube barqarorligi uchun ixtiyoriy: youtube.poToken va youtube.visitorData ni kiriting.", "Для стабільнішого YouTube за потреби задайте youtube.poToken і youtube.visitorData.", "Optional für bessere YouTube-Stabilität: youtube.poToken und youtube.visitorData setzen.", "Opcional para mejor estabilidad de YouTube: configurar youtube.poToken y youtube.visitorData.", "Opzionale per maggiore affidabilità YouTube: imposta youtube.poToken e youtube.visitorData.", "Opcional para melhor estabilidade do YouTube: defina youtube.poToken e youtube.visitorData.", "可选：为提高 YouTube 稳定性，设置 youtube.poToken 和 youtube.visitorData。", "YouTube安定性向上のため、必要に応じて youtube.poToken と youtube.visitorData を設定。" );
            case "javaIncluded" -> byLang(lang, "Desktop installers already include Java runtime.", "Desktop տեղադրիչներն արդեն ներառում են Java runtime։", "Desktop ინსტალატორებში Java runtime უკვე შედის.", "Desktop quraşdırıcılarına Java runtime daxildir.", "Desktop орнатқыштарында Java runtime бар.", "Desktop o‘rnatgichlarda Java runtime bor.", "Desktop-інсталятори вже містять Java runtime.", "Desktop-Installer enthalten bereits Java Runtime.", "Los instaladores desktop ya incluyen Java runtime.", "Gli installer desktop includono già Java runtime.", "Os instaladores desktop já incluem Java runtime.", "桌面安装包已包含 Java 运行时。", "デスクトップインストーラーにはJavaランタイムが含まれています。" );
            case "desktopSearchCancelled" -> byLang(lang, "Desktop search cancelled", "Desktop որոնումը չեղարկվեց", "Desktop ძიება გაუქმდა", "Desktop axtarışı ləğv edildi", "Desktop іздеу тоқтатылды", "Desktop qidiruvi bekor qilindi", "Desktop пошук скасовано", "Desktop-Suche abgebrochen", "Búsqueda desktop cancelada", "Ricerca desktop annullata", "Pesquisa desktop cancelada", "桌面搜索已取消", "デスクトップ検索をキャンセルしました");
            case "desktopQueued" -> byLang(lang, "Desktop queued", "Desktop-ից հերթագրվեց", "Desktop-დან დაემატა რიგში", "Desktop-dan növbəyə əlavə edildi", "Desktop-тен кезекке қосылды", "Desktop'dan navbatga qo‘shildi", "Додано в чергу з Desktop", "Von Desktop in Warteschlange", "Agregado a cola desde desktop", "Aggiunto in coda da desktop", "Adicionado à fila pelo desktop", "已从桌面加入队列", "デスクトップからキューに追加");
            case "queueList" -> byLang(lang, "Queue (select item to remove)", "Հերթ (ընտրեք հեռացնելու համար)", "რიგი (ასარჩევად წასაშლელად)", "Növbə (silmək üçün seçin)", "Кезек (өшіру үшін таңдаңыз)", "Navbat (o‘chirish uchun tanlang)", "Черга (виберіть елемент для видалення)", "Warteschlange (Element zum Entfernen wählen)", "Cola (selecciona elemento para eliminar)", "Coda (seleziona elemento da rimuovere)", "Fila (selecione item para remover)", "队列（选择要移除的项目）", "キュー（削除する項目を選択）");
            case "cleanupScope" -> byLang(lang, "Cleanup", "Մաքրում", "გასუფთავება", "Təmizləmə", "Тазалау", "Tozalash", "Очищення", "Bereinigung", "Limpieza", "Pulizia", "Limpeza", "清理", "クリーンアップ");
            case "cleanupScopeAll" -> byLang(lang, "All", "Բոլորը", "ყველა", "Hamısı", "Барлығы", "Hammasi", "Усе", "Alle", "Todo", "Tutto", "Tudo", "全部", "すべて");
            case "cleanupScopeBotOnly" -> byLang(lang, "Bot Only", "Միայն բոտ", "მხოლოდ ბოტი", "Yalnız bot", "Тек бот", "Faqat bot", "Лише бот", "Nur Bot", "Solo bot", "Solo bot", "Apenas bot", "仅机器人", "ボットのみ");
            case "removePlayer" -> byLang(lang, "Remove Player in Discord", "Հեռացնել պլեյերը Discord-ում", "Discord-ში პლეერის წაშლა", "Discord-da pleyeri sil", "Discord-та плеерді жою", "Discord'da pleyerni olib tashlash", "Видалити плеєр у Discord", "Player in Discord entfernen", "Quitar reproductor en Discord", "Rimuovi player su Discord", "Remover player no Discord", "在 Discord 中移除播放器", "Discordでプレーヤーを削除");
            case "playerPanelRemoved" -> byLang(lang, "Removed player panel from Discord.", "Պլեյերի վահանակը հեռացվեց Discord-ից։", "პლეერის პანელი წაიშალა Discord-იდან.", "Pleyer paneli Discord-dan silindi.", "Плеер панелі Discord-тан жойылды.", "Player panel Discord'dan olib tashlandi.", "Панель плеєра видалено з Discord.", "Player-Panel aus Discord entfernt.", "Panel del reproductor eliminado de Discord.", "Pannello player rimosso da Discord.", "Painel do player removido do Discord.", "已从 Discord 移除播放器面板。", "プレーヤーパネルをDiscordから削除しました。");
            case "desktopAction" -> byLang(lang, "Desktop action", "Desktop գործողություն", "Desktop ქმედება", "Desktop əməliyyatı", "Desktop әрекеті", "Desktop amali", "Desktop дія", "Desktop-Aktion", "Acción de desktop", "Azione desktop", "Ação desktop", "桌面操作", "デスクトップ操作");
            case "chooseTrackFor" -> byLang(lang, "Choose a track for", "Ընտրեք թրեք", "აირჩიეთ ტრეკი", "Trek seçin", "Тректі таңдаңыз", "Trekni tanlang", "Оберіть трек для", "Track auswählen für", "Elegir pista para", "Scegli una traccia per", "Escolha uma faixa para", "为以下内容选择曲目", "次の曲を選択");
            case "searchResults" -> byLang(lang, "Search Results", "Որոնման արդյունքներ", "ძიების შედეგები", "Axtarış nəticələri", "Іздеу нәтижелері", "Qidiruv natijalari", "Результати пошуку", "Suchergebnisse", "Resultados de búsqueda", "Risultati di ricerca", "Resultados da pesquisa", "搜索结果", "検索結果");
            case "couldNotShowSearchChoices" -> byLang(lang, "Could not show search choices", "Չհաջողվեց ցուցադրել որոնման տարբերակները", "ძიების ვარიანტების ჩვენება ვერ მოხერხდა", "Axtarış seçimlərini göstərmək olmadı", "Іздеу нұсқаларын көрсету мүмкін болмады", "Qidiruv variantlarini ko‘rsatib bo‘lmadi", "Не вдалося показати варіанти пошуку", "Suchoptionen konnten nicht angezeigt werden", "No se pudieron mostrar opciones de búsqueda", "Impossibile mostrare le opzioni di ricerca", "Não foi possível mostrar opções de pesquisa", "无法显示搜索选项", "検索候補を表示できませんでした");
            case "toggleOn" -> byLang(lang, "On", "Մի", "ჩართ.", "Açıq", "Қосулы", "Yoqilgan", "Увімк.", "An", "Encendido", "On", "Ligado", "开", "オン");
            case "toggleOff" -> byLang(lang, "Off", "Անջ.", "გამორთ.", "Söndür", "Өшірулі", "O‘chirilgan", "Вимк.", "Aus", "Apagado", "Off", "Desligado", "关", "オフ");
            default -> null;
        };

        return value == null ? uiEn(key) : value;
    }

    private String byLang(String lang, String en, String hy, String ka, String az, String kk, String uz, String uk, String de, String es, String it, String pt, String zh, String ja) {
        return switch (lang) {
            case "hy" -> hy;
            case "ka" -> ka;
            case "az" -> az;
            case "kk" -> kk;
            case "uz" -> uz;
            case "uk" -> uk;
            case "de" -> de;
            case "es" -> es;
            case "it" -> it;
            case "pt" -> pt;
            case "zh" -> zh;
            case "ja" -> ja;
            default -> en;
        };
    }

    private String uiHy(String key) { return switch (key) {
        case "appTitle" -> "ModernMusicBot կառավարման վահանակ";
        case "settings" -> "Կարգավորումներ";
        case "botToken" -> "Բոտի թոքեն";
        case "prefix" -> "Նախածանց";
        case "botLanguage" -> "Բոտի լեզու";
        case "controlPanelLanguage" -> "Կառավարման վահանակի լեզու";
        case "start" -> "Սկսել";
        case "stop" -> "Կանգնեցնել";
        case "saveSettings" -> "Պահպանել կարգավորումները";
        case "console" -> "Վահանակ";
        case "desktopPlayer" -> "Հոսթ պլեյեր (Desktop)";
        case "addSong" -> "Ավելացնել երգ";
        case "pause" -> "Դադար";
        case "resume" -> "Շարունակել";
        case "skip" -> "Բաց թողնել";
        case "refreshLists" -> "Թարմացնել ցուցակները";
        case "launchPlayer" -> "Բացել պլեյերը Discord-ում";
        case "botNotRunning" -> "Բոտը գործարկված չէ։";
        case "guild" -> "Սերվեր";
        case "textChannel" -> "Տեքստային ալիք";
        case "songUrl" -> "Երգ / URL";
        case "help" -> "Օգնություն";
        case "error" -> "Սխալ";
        case "firstLaunchChecklist" -> "Առաջին գործարկման ստուգաթերթ";
        default -> uiLocalizedFallback(key, "hy");
    }; }

    private String uiKa(String key) { return switch (key) {
        case "appTitle" -> "ModernMusicBot მართვის პანელი";
        case "settings" -> "პარამეტრები";
        case "botToken" -> "ბოტის ტოკენი";
        case "prefix" -> "პრეფიქსი";
        case "botLanguage" -> "ბოტის ენა";
        case "controlPanelLanguage" -> "მართვის პანელის ენა";
        case "start" -> "დაწყება";
        case "stop" -> "გაჩერება";
        case "saveSettings" -> "პარამეტრების შენახვა";
        case "console" -> "კონსოლი";
        case "desktopPlayer" -> "ჰოსტის პლეერი (Desktop)";
        case "addSong" -> "სიმღერის დამატება";
        case "pause" -> "პაუზა";
        case "resume" -> "გაგრძელება";
        case "skip" -> "გამოტოვება";
        case "refreshLists" -> "სიების განახლება";
        case "launchPlayer" -> "პლეერის გაშვება Discord-ში";
        case "botNotRunning" -> "ბოტი გაშვებული არ არის.";
        case "guild" -> "სერვერი";
        case "textChannel" -> "ტექსტური არხი";
        case "songUrl" -> "სიმღერა / URL";
        case "help" -> "დახმარება";
        case "error" -> "შეცდომა";
        case "firstLaunchChecklist" -> "პირველი გაშვების სია";
        default -> uiLocalizedFallback(key, "ka");
    }; }

    private String uiAz(String key) { return switch (key) {
        case "appTitle" -> "ModernMusicBot idarəetmə paneli";
        case "settings" -> "Ayarlar";
        case "botToken" -> "Bot tokeni";
        case "prefix" -> "Prefiks";
        case "botLanguage" -> "Bot dili";
        case "controlPanelLanguage" -> "İdarəetmə paneli dili";
        case "start" -> "Başlat";
        case "stop" -> "Dayandır";
        case "saveSettings" -> "Ayarları saxla";
        case "console" -> "Konsol";
        case "desktopPlayer" -> "Host pleyer (Desktop)";
        case "addSong" -> "Mahnı əlavə et";
        case "pause" -> "Pauza";
        case "resume" -> "Davam et";
        case "skip" -> "Keç";
        case "refreshLists" -> "Siyahıları yenilə";
        case "launchPlayer" -> "Discord-da pleyeri aç";
        case "botNotRunning" -> "Bot işləmir.";
        case "guild" -> "Server";
        case "textChannel" -> "Mətn kanalı";
        case "songUrl" -> "Mahnı / URL";
        case "help" -> "Kömək";
        case "error" -> "Xəta";
        case "firstLaunchChecklist" -> "İlk açılış yoxlama siyahısı";
        default -> uiLocalizedFallback(key, "az");
    }; }

    private String uiKk(String key) { return switch (key) {
        case "appTitle" -> "ModernMusicBot басқару панелі";
        case "settings" -> "Параметрлер";
        case "botToken" -> "Бот токені";
        case "prefix" -> "Префикс";
        case "botLanguage" -> "Бот тілі";
        case "controlPanelLanguage" -> "Басқару панелінің тілі";
        case "start" -> "Бастау";
        case "stop" -> "Тоқтату";
        case "saveSettings" -> "Параметрлерді сақтау";
        case "console" -> "Консоль";
        case "desktopPlayer" -> "Хост плеері (Desktop)";
        case "addSong" -> "Ән қосу";
        case "pause" -> "Пауза";
        case "resume" -> "Жалғастыру";
        case "skip" -> "Өткізу";
        case "refreshLists" -> "Тізімдерді жаңарту";
        case "launchPlayer" -> "Discord-та плеерді ашу";
        case "botNotRunning" -> "Бот іске қосылмаған.";
        case "guild" -> "Сервер";
        case "textChannel" -> "Мәтін арнасы";
        case "songUrl" -> "Ән / URL";
        case "help" -> "Көмек";
        case "error" -> "Қате";
        case "firstLaunchChecklist" -> "Алғашқы іске қосу тізімі";
        default -> uiLocalizedFallback(key, "kk");
    }; }

    private String uiUz(String key) { return switch (key) {
        case "appTitle" -> "ModernMusicBot boshqaruv paneli";
        case "settings" -> "Sozlamalar";
        case "botToken" -> "Bot tokeni";
        case "prefix" -> "Prefiks";
        case "botLanguage" -> "Bot tili";
        case "controlPanelLanguage" -> "Boshqaruv paneli tili";
        case "start" -> "Boshlash";
        case "stop" -> "Toʻxtatish";
        case "saveSettings" -> "Sozlamalarni saqlash";
        case "console" -> "Konsol";
        case "desktopPlayer" -> "Host pleyeri (Desktop)";
        case "addSong" -> "Qoʻshiq qoʻshish";
        case "pause" -> "Pauza";
        case "resume" -> "Davom ettirish";
        case "skip" -> "Oʻtkazib yuborish";
        case "refreshLists" -> "Roʻyxatlarni yangilash";
        case "launchPlayer" -> "Discord-da pleyerni ochish";
        case "botNotRunning" -> "Bot ishga tushmagan.";
        case "guild" -> "Server";
        case "textChannel" -> "Matn kanali";
        case "songUrl" -> "Qoʻshiq / URL";
        case "help" -> "Yordam";
        case "error" -> "Xatolik";
        case "firstLaunchChecklist" -> "Birinchi ishga tushirish roʻyxati";
        default -> uiLocalizedFallback(key, "uz");
    }; }

    private String uiUk(String key) { return switch (key) {
        case "appTitle" -> "Панель керування ModernMusicBot";
        case "settings" -> "Налаштування";
        case "botToken" -> "Токен бота";
        case "prefix" -> "Префікс";
        case "botLanguage" -> "Мова бота";
        case "controlPanelLanguage" -> "Мова панелі керування";
        case "start" -> "Старт";
        case "stop" -> "Стоп";
        case "saveSettings" -> "Зберегти налаштування";
        case "console" -> "Консоль";
        case "desktopPlayer" -> "Плеєр хоста (Desktop)";
        case "addSong" -> "Додати пісню";
        case "pause" -> "Пауза";
        case "resume" -> "Продовжити";
        case "skip" -> "Пропустити";
        case "refreshLists" -> "Оновити списки";
        case "launchPlayer" -> "Запустити плеєр у Discord";
        case "botNotRunning" -> "Бот не запущено.";
        case "guild" -> "Сервер";
        case "textChannel" -> "Текстовий канал";
        case "songUrl" -> "Пісня / URL";
        case "help" -> "Довідка";
        case "error" -> "Помилка";
        case "firstLaunchChecklist" -> "Чеклист першого запуску";
        default -> uiLocalizedFallback(key, "uk");
    }; }

    private String uiDe(String key) { return switch (key) {
        case "appTitle" -> "ModernMusicBot Kontrollzentrum";
        case "settings" -> "Einstellungen";
        case "botToken" -> "Bot-Token";
        case "prefix" -> "Präfix";
        case "botLanguage" -> "Bot-Sprache";
        case "controlPanelLanguage" -> "Sprache des Kontrollpanels";
        case "start" -> "Start";
        case "stop" -> "Stopp";
        case "saveSettings" -> "Einstellungen speichern";
        case "console" -> "Konsole";
        case "desktopPlayer" -> "Host-Player (Desktop)";
        case "addSong" -> "Song hinzufügen";
        case "pause" -> "Pause";
        case "resume" -> "Fortsetzen";
        case "skip" -> "Überspringen";
        case "refreshLists" -> "Listen aktualisieren";
        case "launchPlayer" -> "Player in Discord starten";
        case "botNotRunning" -> "Bot läuft nicht.";
        case "guild" -> "Server";
        case "textChannel" -> "Textkanal";
        case "songUrl" -> "Song / URL";
        case "help" -> "Hilfe";
        case "error" -> "Fehler";
        case "firstLaunchChecklist" -> "Checkliste für den ersten Start";
        default -> uiLocalizedFallback(key, "de");
    }; }

    private String uiEs(String key) { return switch (key) {
        case "appTitle" -> "Panel de control de ModernMusicBot";
        case "settings" -> "Configuración";
        case "botToken" -> "Token del bot";
        case "prefix" -> "Prefijo";
        case "botLanguage" -> "Idioma del bot";
        case "controlPanelLanguage" -> "Idioma del panel de control";
        case "start" -> "Iniciar";
        case "stop" -> "Detener";
        case "saveSettings" -> "Guardar configuración";
        case "console" -> "Consola";
        case "desktopPlayer" -> "Reproductor host (Desktop)";
        case "addSong" -> "Agregar canción";
        case "pause" -> "Pausa";
        case "resume" -> "Reanudar";
        case "skip" -> "Saltar";
        case "refreshLists" -> "Actualizar listas";
        case "launchPlayer" -> "Lanzar reproductor en Discord";
        case "botNotRunning" -> "El bot no está en ejecución.";
        case "guild" -> "Servidor";
        case "textChannel" -> "Canal de texto";
        case "songUrl" -> "Canción / URL";
        case "help" -> "Ayuda";
        case "error" -> "Error";
        case "firstLaunchChecklist" -> "Lista de verificación del primer inicio";
        default -> uiLocalizedFallback(key, "es");
    }; }

    private String uiIt(String key) { return switch (key) {
        case "appTitle" -> "Pannello di controllo ModernMusicBot";
        case "settings" -> "Impostazioni";
        case "botToken" -> "Token del bot";
        case "prefix" -> "Prefisso";
        case "botLanguage" -> "Lingua del bot";
        case "controlPanelLanguage" -> "Lingua del pannello di controllo";
        case "start" -> "Avvia";
        case "stop" -> "Arresta";
        case "saveSettings" -> "Salva impostazioni";
        case "console" -> "Console";
        case "desktopPlayer" -> "Player host (Desktop)";
        case "addSong" -> "Aggiungi brano";
        case "pause" -> "Pausa";
        case "resume" -> "Riprendi";
        case "skip" -> "Salta";
        case "refreshLists" -> "Aggiorna elenchi";
        case "launchPlayer" -> "Avvia player in Discord";
        case "botNotRunning" -> "Il bot non è in esecuzione.";
        case "guild" -> "Server";
        case "textChannel" -> "Canale testuale";
        case "songUrl" -> "Brano / URL";
        case "help" -> "Aiuto";
        case "error" -> "Errore";
        case "firstLaunchChecklist" -> "Checklist primo avvio";
        default -> uiLocalizedFallback(key, "it");
    }; }

    private String uiPt(String key) { return switch (key) {
        case "appTitle" -> "Painel de controle do ModernMusicBot";
        case "settings" -> "Configurações";
        case "botToken" -> "Token do bot";
        case "prefix" -> "Prefixo";
        case "botLanguage" -> "Idioma do bot";
        case "controlPanelLanguage" -> "Idioma do painel de controle";
        case "start" -> "Iniciar";
        case "stop" -> "Parar";
        case "saveSettings" -> "Salvar configurações";
        case "console" -> "Console";
        case "desktopPlayer" -> "Player host (Desktop)";
        case "addSong" -> "Adicionar música";
        case "pause" -> "Pausar";
        case "resume" -> "Retomar";
        case "skip" -> "Pular";
        case "refreshLists" -> "Atualizar listas";
        case "launchPlayer" -> "Iniciar player no Discord";
        case "botNotRunning" -> "O bot não está em execução.";
        case "guild" -> "Servidor";
        case "textChannel" -> "Canal de texto";
        case "songUrl" -> "Música / URL";
        case "help" -> "Ajuda";
        case "error" -> "Erro";
        case "firstLaunchChecklist" -> "Checklist da primeira execução";
        default -> uiLocalizedFallback(key, "pt");
    }; }

    private String uiZh(String key) { return switch (key) {
        case "appTitle" -> "ModernMusicBot 控制面板";
        case "settings" -> "设置";
        case "botToken" -> "机器人令牌";
        case "prefix" -> "前缀";
        case "botLanguage" -> "机器人语言";
        case "controlPanelLanguage" -> "控制面板语言";
        case "start" -> "启动";
        case "stop" -> "停止";
        case "saveSettings" -> "保存设置";
        case "console" -> "控制台";
        case "desktopPlayer" -> "主机播放器（桌面）";
        case "addSong" -> "添加歌曲";
        case "pause" -> "暂停";
        case "resume" -> "继续";
        case "skip" -> "跳过";
        case "refreshLists" -> "刷新列表";
        case "launchPlayer" -> "在 Discord 中启动播放器";
        case "botNotRunning" -> "机器人未运行。";
        case "guild" -> "服务器";
        case "textChannel" -> "文字频道";
        case "songUrl" -> "歌曲 / URL";
        case "help" -> "帮助";
        case "error" -> "错误";
        case "firstLaunchChecklist" -> "首次启动清单";
        default -> uiLocalizedFallback(key, "zh");
    }; }

    private String uiJa(String key) { return switch (key) {
        case "appTitle" -> "ModernMusicBot コントロールパネル";
        case "settings" -> "設定";
        case "botToken" -> "ボットトークン";
        case "prefix" -> "プレフィックス";
        case "botLanguage" -> "ボット言語";
        case "controlPanelLanguage" -> "コントロールパネル言語";
        case "start" -> "開始";
        case "stop" -> "停止";
        case "saveSettings" -> "設定を保存";
        case "console" -> "コンソール";
        case "desktopPlayer" -> "ホストプレーヤー (Desktop)";
        case "addSong" -> "曲を追加";
        case "pause" -> "一時停止";
        case "resume" -> "再開";
        case "skip" -> "スキップ";
        case "refreshLists" -> "リストを更新";
        case "launchPlayer" -> "Discordでプレーヤーを起動";
        case "botNotRunning" -> "ボットは起動していません。";
        case "guild" -> "サーバー";
        case "textChannel" -> "テキストチャンネル";
        case "songUrl" -> "曲 / URL";
        case "help" -> "ヘルプ";
        case "error" -> "エラー";
        case "firstLaunchChecklist" -> "初回起動チェックリスト";
        default -> uiLocalizedFallback(key, "ja");
    }; }

    private void selectLanguage(String languageCode) {
        LanguageOption option = Arrays.stream(LANGUAGE_OPTIONS)
                .filter(item -> item.code().equalsIgnoreCase(languageCode))
                .findFirst()
                .orElse(LANGUAGE_OPTIONS[0]);
        languageCombo.setSelectedItem(option);
    }

    private void selectControlPanelLanguage(String languageCode) {
        if (controlPanelLanguageCombo == null) {
            return;
        }
        LanguageOption option = Arrays.stream(LANGUAGE_OPTIONS)
                .filter(item -> item.code().equalsIgnoreCase(languageCode))
                .findFirst()
                .orElse(LANGUAGE_OPTIONS[0]);
        controlPanelLanguageCombo.setSelectedItem(option);
    }

    private boolean isSearchQuery(String query) {
        String value = query.trim().toLowerCase();
        return !(value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("www.")
                || value.startsWith("ytsearch:")
                || value.startsWith("ytmsearch:")
                || value.startsWith("scsearch:"));
    }

    private int pickSearchResultIndex(String query, List<BotRuntime.SearchTrackOptionRef> options) {
        AtomicInteger selected = new AtomicInteger(-1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                String[] items = options.stream()
                        .map(option -> option.title())
                        .toArray(String[]::new);
                String choice = (String) JOptionPane.showInputDialog(
                        frame,
                        ui("chooseTrackFor") + ": " + query,
                        ui("searchResults"),
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        items,
                        items[0]
                );
                if (choice == null) {
                    selected.set(-1);
                    return;
                }

                for (int i = 0; i < items.length; i++) {
                    if (items[i].equals(choice)) {
                        selected.set(i);
                        return;
                    }
                }
            });
        } catch (Exception ex) {
            throw new IllegalStateException(ui("couldNotShowSearchChoices") + ": " + ex.getMessage(), ex);
        }

        return selected.get();
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
