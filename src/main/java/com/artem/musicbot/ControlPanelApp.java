package com.artem.musicbot;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.RadialGradientPaint;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.AlphaComposite;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.ButtonModel;
import javax.swing.AbstractButton;

import com.formdev.flatlaf.FlatDarkLaf;

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
    private static final ThemeOption[] THEME_OPTIONS = {
        new ThemeOption("Neon Dark", "neon"),
        new ThemeOption("Midnight", "midnight"),
        new ThemeOption("Nebula", "nebula"),
        new ThemeOption("Graphite", "graphite"),
        new ThemeOption("Twilight", "twilight"),
        new ThemeOption("Pearl Light", "pearl"),
        new ThemeOption("Sky Light", "sky"),
        new ThemeOption("Dawn Light", "dawn"),
        new ThemeOption("Frost Light", "frost")
    };
    private static final CornerStyleOption[] CORNER_STYLE_OPTIONS = {
        new CornerStyleOption("Rounded", "rounded"),
        new CornerStyleOption("Square", "square")
    };

    private final BotRuntime runtime = new BotRuntime();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService realtimeWorker = Executors.newSingleThreadExecutor();
    private final boolean macUi = System.getProperty("os.name", "").toLowerCase().contains("mac");

    private JFrame frame;
    private JPanel rootPanel;
    private JPanel centerCardPanel;
    private CardLayout centerCardLayout;
    private AnimatedPanel headerWrap;
    private AnimatedPanel sidebarWrap;
    private AnimatedPanel centerWrap;
    private AnimatedPanel rightWrap;
    private JLabel headerLogoLabel;
    private SplashOverlay splashOverlay;
    private Timer splashTimer;
    private JPanel rightRail;
    private JScrollPane rightRailScroll;
    private JPanel rightQuickAddCard;
    private JPanel rightSearchCard;
    private JPanel rightQueueCard;
    private JPanel rightQuickActionsCard;
    private JPanel rightSettingsHintCard;
    private JPanel rightConsoleHintCard;
    private String activeNav = "home";
    private JLabel statusPill;
    private JLabel statusMessageLabel;
    private Timer statusMessageTimer;
    private JLabel guildStatusLabel;
    private JLabel channelStatusLabel;
    private JLabel nowPlayingTitle;
    private JLabel nowPlayingSubtitle;
    private JLabel nowPlayingHint;
    private JLabel progressTimeLabel;
    private JSlider progressSlider;
    private WaveformPanel waveformPanel;
    private DiscPanel heroDisc;
    private DiscPanel homeDisc;
    private String pendingNowPlayingTitle;
    private long pendingNowPlayingUntilMillis;
    private long lastProgressUiUpdateMillis;
    private boolean lastPlayingState;
    private JButton navHomeButton;
    private JButton navPlayerButton;
    private JButton navQueueButton;
    private JButton navSearchButton;
    private JButton navSettingsButton;
    private JButton navConsoleButton;
    private JPasswordField tokenField;
    private JTextField prefixField;
    private JComboBox<LanguageOption> languageCombo;
    private JComboBox<LanguageOption> controlPanelLanguageCombo;
    private JComboBox<ThemeOption> themeCombo;
    private JComboBox<CornerStyleOption> cornerStyleCombo;
    private JTextArea console;
    private JButton startButton;
    private JButton stopButton;
    private JButton saveButton;
    private JButton homeStartButton;
    private JButton homeStopButton;
    private JButton toggleConsoleButton;
    private JButton clearConsoleButton;
    private JComboBox<GuildOption> guildCombo;
    private JComboBox<ChannelOption> channelCombo;
    private JTextField addSongField;
    private JButton addSongButton;
    private JButton playNextButton;
    private JButton pauseButton;
    private JButton resumeButton;
    private JButton skipButton;
    private JButton stopPlaybackButton;
    private JSlider volumeSlider;
    private JLabel volumeValueLabel;
    private JButton volumeResetButton;
    private JSlider bassSlider;
    private JLabel bassValueLabel;
    private JButton bassResetButton;
    private JList<String> searchResultsList;
    private DefaultListModel<String> searchResultsModel;
    private List<BotRuntime.SearchTrackOptionRef> searchResultsOptions = List.of();
    private String lastSearchQuery;
    private JTextField removeIndexField;
    private JButton removeQueueButton;
    private JButton shuffleQueueButton;
    private JButton clearQueueButton;
    private JButton cleanupScopeButton;
    private JButton earRapeToggleButton;
    private boolean earRapeEnabled;
    private JButton refreshDesktopButton;
    private JButton launchPlayerButton;
    private JList<String> queueList;
    private DefaultListModel<String> queueListModel;
    private JScrollPane consoleScrollPane;
    private boolean consoleVisible;
    private Timer desktopRefreshTimer;
    private Timer waveformRefreshTimer;
    private String controlPanelLanguageCode = "en";
    private String controlPanelThemeCode = "neon";
    private String controlPanelCornerStyleCode = macUi ? "rounded" : "square";
    private Rectangle restoreBounds;
    private boolean restoreBoundsNext;
    private boolean windowListenerInstalled;
    private Color brandAccent = new Color(64, 217, 255);
    private Color brandAccentAlt = new Color(164, 88, 255);
    private Color brandGlow = new Color(86, 160, 255);
    private Color brandText = new Color(232, 239, 255);
    private Color subtleText = new Color(168, 176, 204);
    private Color brandBackground = new Color(10, 12, 24);
    private Color brandBackgroundAlt = new Color(20, 18, 44);
    private Color brandPanel = new Color(20, 26, 42);
    private Color brandPanelAlt = new Color(26, 32, 50);
    private Color brandBorder = new Color(60, 78, 120);
    private Color glassFill = new Color(20, 28, 45, 200);
    private Color glassBorder = new Color(90, 120, 170, 150);
    private Color sidebarBackground = new Color(12, 16, 30);
    private int barArc = 18;
    private int cardArc = 18;
    private int heroArc = 26;
    private int controlsArc = 22;
    private int welcomeArc = 26;
    private int buttonBorderArc = 18;
    private int navSelectedArc = 14;
    private int inputBorderArc = 12;
    private static final String ROUND_ARC_KEY = "mm.roundArc";
    private static final String ROUND_BORDER_KEY = "mm.roundBorder";
    private static final String ROUND_FILL_KEY = "mm.roundFill";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ControlPanelApp().show());
    }

    private void show() {
        loadControlPanelLanguagePreference();
        loadControlPanelThemePreference();
        loadControlPanelCornerStylePreference();
        setupLookAndFeel();
        applyBrandTheme();
        frame = new JFrame(ui("appTitle"));
        applyAppIcon();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        rootPanel = new GradientPanel(brandBackground, brandBackgroundAlt);
        rootPanel.setLayout(new BorderLayout(18, 18));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        frame.setContentPane(rootPanel);

        if (isDesktopOnboardingEnabled()) {
            frame.setJMenuBar(buildMenuBar());
        }

        initDesktopComponents();

        JPanel sidebar = buildSidebar();
        sidebarWrap = new AnimatedPanel(sidebar);
        JPanel mainArea = buildMainArea();
        rootPanel.add(sidebarWrap, BorderLayout.WEST);
        rootPanel.add(mainArea, BorderLayout.CENTER);
        setStartStopEnabled(false);

        if (isDesktopOnboardingEnabled()) {
            frame.setSize(1400, 920);
            frame.setMinimumSize(new Dimension(1220, 820));
        } else {
            frame.setSize(1200, 840);
            frame.setMinimumSize(new Dimension(1040, 740));
        }

        loadConfigIntoFields();
        wireActions();
        wireKeyboardShortcuts();
        if (restoreBoundsNext && restoreBounds != null) {
            frame.setBounds(restoreBounds);
            restoreBoundsNext = false;
            restoreBounds = null;
        } else {
            frame.setLocationRelativeTo(null);
        }
        prepareIntroLayout();
        frame.setVisible(true);
        startStartupAnimation();
        startWaveformRefresh();
        setConsoleVisible(true);
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
                if (waveformRefreshTimer != null) {
                    waveformRefreshTimer.stop();
                }
                realtimeWorker.shutdownNow();
                worker.shutdownNow();
                runtime.stop(ControlPanelApp.this::log);
            }
        });
        windowListenerInstalled = true;
    }

    private void startWaveformRefresh() {
        if (waveformPanel == null) {
            return;
        }
        if (waveformRefreshTimer != null) {
            waveformRefreshTimer.stop();
        }
        waveformRefreshTimer = new Timer(33, ignored -> refreshWaveformLevels());
        waveformRefreshTimer.start();
    }

    private void refreshWaveformLevels() {
        if (waveformPanel == null) {
            return;
        }
        Long guildId = selectedGuildId();
        if (guildId == null || !runtime.isRunning()) {
            waveformPanel.setLevels(null, false);
            if (heroDisc != null) {
                heroDisc.setState(runtime.isRunning(), false);
            }
            if (homeDisc != null) {
                homeDisc.setState(runtime.isRunning(), false);
            }
            updateProgressFromSnapshot(new MusicController.NowPlayingSnapshot("none", "idle", 0L, 0L));
            return;
        }
        MusicController.NowPlayingSnapshot snapshot = runtime.nowPlayingSnapshotForGuild(guildId);
        boolean playing = "playing".equalsIgnoreCase(snapshot.state());
        lastPlayingState = playing;
        float[] levels = runtime.visualizerLevelsForGuild(guildId);
        waveformPanel.setLevels(levels, playing);
        if (heroDisc != null) {
            heroDisc.setState(runtime.isRunning(), playing);
        }
        if (homeDisc != null) {
            homeDisc.setState(runtime.isRunning(), playing);
        }
        long now = System.currentTimeMillis();
        if (now - lastProgressUiUpdateMillis >= 200L) {
            updateProgressFromSnapshot(snapshot);
            lastProgressUiUpdateMillis = now;
        }
    }

    private void updateProgressFromSnapshot(MusicController.NowPlayingSnapshot snapshot) {
        if (progressTimeLabel == null || progressSlider == null) {
            return;
        }
        long duration = snapshot.durationMs();
        long position = snapshot.positionMs();
        if (duration <= 0 || "none".equalsIgnoreCase(snapshot.title())) {
            progressTimeLabel.setText("0:00 / 0:00");
            progressSlider.setValue(0);
            return;
        }
        position = Math.max(0L, Math.min(position, duration));
        progressTimeLabel.setText(formatTime(position) + " / " + formatTime(duration));
        int percent = (int) Math.min(100, Math.max(0, (position * 100) / duration));
        progressSlider.setValue(percent);
    }

    private void initDesktopComponents() {
        boolean roundedControls = useRoundedControls();
        tokenField = roundedControls ? new RoundedPasswordField() : new JPasswordField();
        prefixField = roundedControls ? new RoundedTextField("!") : new JTextField("!");
        languageCombo = roundedControls ? new RoundedComboBox<>(LANGUAGE_OPTIONS) : new JComboBox<>(LANGUAGE_OPTIONS);
        languageCombo.setSelectedIndex(0);
        controlPanelLanguageCombo = roundedControls ? new RoundedComboBox<>(LANGUAGE_OPTIONS) : new JComboBox<>(LANGUAGE_OPTIONS);
        controlPanelLanguageCombo.setSelectedIndex(0);
        themeCombo = roundedControls ? new RoundedComboBox<>(THEME_OPTIONS) : new JComboBox<>(THEME_OPTIONS);
        themeCombo.setSelectedIndex(0);
        cornerStyleCombo = roundedControls ? new RoundedComboBox<>(CORNER_STYLE_OPTIONS) : new JComboBox<>(CORNER_STYLE_OPTIONS);
        cornerStyleCombo.setSelectedIndex(0);
        styleInputField(tokenField, 38);
        styleInputField(prefixField, 36);
        styleCombo(languageCombo, 36);
        styleCombo(controlPanelLanguageCombo, 36);
        styleCombo(themeCombo, 36);
        styleCombo(cornerStyleCombo, 36);
        controlPanelLanguageCombo.addActionListener(ignored -> setEarRapeEnabled(earRapeEnabled));

        startButton = new JButton(ui("start"));
        stopButton = new JButton(ui("stop"));
        saveButton = new JButton(ui("saveSettings"));
        toggleConsoleButton = new JButton();
        clearConsoleButton = new JButton(ui("clearConsole"));
        stylePrimaryButton(startButton);
        styleSecondaryButton(stopButton);
        stylePrimaryButton(saveButton);
        styleSecondaryButton(toggleConsoleButton);
        styleSecondaryButton(clearConsoleButton);
        stopButton.setEnabled(false);

        console = new JTextArea();
        console.setEditable(false);
        console.setBackground(brandPanelAlt);
        console.setForeground(brandText);
        consoleScrollPane = new JScrollPane(console);
        consoleScrollPane.setBorder(BorderFactory.createEmptyBorder());
        consoleScrollPane.getViewport().setOpaque(false);
        consoleScrollPane.setOpaque(false);

        guildCombo = roundedControls ? new RoundedComboBox<>() : new JComboBox<>();
        channelCombo = roundedControls ? new RoundedComboBox<>() : new JComboBox<>();
        styleCombo(guildCombo, 34);
        styleCombo(channelCombo, 34);

        addSongField = roundedControls ? new RoundedTextField() : new JTextField();
        addSongField.putClientProperty("JTextField.placeholderText", ui("songPlaceholder"));
        styleInputField(addSongField, 38);
        addSongButton = new JButton(ui("addSong"));
        playNextButton = new JButton(ui("playNext"));
        stylePrimaryButton(addSongButton);
        styleSecondaryButton(playNextButton);

        pauseButton = new JButton(ui("pause"));
        resumeButton = new JButton(ui("play"));
        skipButton = new JButton(ui("skip"));
        stopPlaybackButton = new JButton(ui("stop"));
        stylePlaybackButton(pauseButton, false);
        stylePlaybackButton(resumeButton, true);
        stylePlaybackButton(skipButton, false);
        stylePlaybackButton(stopPlaybackButton, false);

        removeIndexField = roundedControls ? new RoundedTextField(4) : new JTextField(4);
        removeIndexField.setToolTipText(ui("queueIndexHint"));
        styleInputField(removeIndexField, 30);
        removeQueueButton = new JButton(ui("queueRemove"));
        shuffleQueueButton = new JButton(ui("queueShuffle"));
        clearQueueButton = new JButton(ui("queueClear"));
        styleSecondaryButton(removeQueueButton);
        styleSecondaryButton(shuffleQueueButton);
        styleSecondaryButton(clearQueueButton);

        volumeSlider = new JSlider(0, 200, 100);
        volumeSlider.setPaintTicks(false);
        volumeSlider.setMajorTickSpacing(50);
        volumeSlider.setMinorTickSpacing(10);
        volumeSlider.putClientProperty("JSlider.isFilled", true);
        volumeSlider.setOpaque(false);
        volumeValueLabel = new JLabel(ui("volume") + ": 100%");
        volumeValueLabel.setForeground(subtleText);
        volumeResetButton = new JButton(ui("reset"));
        styleSecondaryButton(volumeResetButton);

        bassSlider = new JSlider(0, 5, 0);
        bassSlider.setPaintTicks(false);
        bassSlider.setMajorTickSpacing(1);
        bassSlider.setMinorTickSpacing(1);
        bassSlider.putClientProperty("JSlider.isFilled", true);
        bassSlider.setOpaque(false);
        bassValueLabel = new JLabel(ui("bass") + ": 0");
        bassValueLabel.setForeground(subtleText);
        bassResetButton = new JButton(ui("reset"));
        styleSecondaryButton(bassResetButton);

        searchResultsModel = new DefaultListModel<>();
        searchResultsList = new JList<>(searchResultsModel);
        searchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultsList.setVisibleRowCount(8);
        searchResultsList.setCellRenderer(new NeonListCellRenderer());
        styleList(searchResultsList);
        searchResultsList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    enqueueSelectedSearchResult(false);
                }
            }
        });

        cleanupScopeButton = new JButton();
        styleSecondaryButton(cleanupScopeButton);
        setCleanupScopeButtonState(false, false);
        earRapeToggleButton = new JButton();
        styleSecondaryButton(earRapeToggleButton);
        setEarRapeEnabled(false);
        refreshDesktopButton = new JButton(ui("refreshLists"));
        launchPlayerButton = new JButton();
        styleSecondaryButton(refreshDesktopButton);
        stylePrimaryButton(launchPlayerButton);
        setPlayerPanelToggleButtonState(false, false);

        queueListModel = new DefaultListModel<>();
        queueList = new JList<>(queueListModel);
        queueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queueList.setVisibleRowCount(12);
        queueList.setCellRenderer(new NeonListCellRenderer());
        styleList(queueList);
        queueList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selected = queueList.getSelectedIndex();
                if (selected >= 0) {
                    removeIndexField.setText(String.valueOf(selected + 1));
                }
            }
        });

        setDesktopControlsEnabled(false);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new javax.swing.BoxLayout(sidebar, javax.swing.BoxLayout.Y_AXIS));
        sidebar.setBackground(sidebarBackground);
        sidebar.setOpaque(true);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                roundedLineBorder(new Color(18, 26, 40), 1, cardArc),
                BorderFactory.createEmptyBorder(16, 14, 16, 14)
        ));

        JLabel titleLabel = new JLabel("ModernMusicBot");
        titleLabel.setForeground(brandText);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        JLabel subtitleLabel = new JLabel(ui("controlPanel"));
        subtitleLabel.setForeground(subtleText);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12f));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new javax.swing.BoxLayout(brand, javax.swing.BoxLayout.Y_AXIS));
        brand.add(titleLabel);
        brand.add(subtitleLabel);

        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(20));

        navHomeButton = createNavButton(ui("navHome"), "home");
        navPlayerButton = createNavButton(ui("navPlayer"), "player");
        navQueueButton = createNavButton(ui("navQueue"), "queue");
        navSearchButton = createNavButton(ui("navSearch"), "search");
        navSettingsButton = createNavButton(ui("navSettings"), "settings");
        navConsoleButton = createNavButton(ui("navConsole"), "console");

        sidebar.add(navHomeButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navPlayerButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navQueueButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navSearchButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navSettingsButton);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navConsoleButton);

        sidebar.add(Box.createVerticalGlue());

        setActiveNav("home");
        return sidebar;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout(16, 16));
        main.setOpaque(false);
        headerWrap = new AnimatedPanel(buildTopBar());
        main.add(headerWrap, BorderLayout.NORTH);

        centerCardLayout = new CardLayout();
        centerCardPanel = new JPanel(centerCardLayout);
        centerCardPanel.setOpaque(false);
        centerCardPanel.add(buildHomeView(), "home");
        centerCardPanel.add(buildPlayerView(), "player");
        centerCardPanel.add(buildSettingsView(), "settings");
        centerCardPanel.add(buildConsoleView(), "console");

        JPanel rightContainer = new JPanel(new BorderLayout());
        rightContainer.setOpaque(false);
        rightContainer.setPreferredSize(new Dimension(380, 0));
        rightRailScroll = wrapRightRail(buildRightRail());
        rightContainer.add(rightRailScroll, BorderLayout.CENTER);

        centerWrap = new AnimatedPanel(centerCardPanel);
        rightWrap = new AnimatedPanel(rightContainer);
        main.add(centerWrap, BorderLayout.CENTER);
        main.add(rightWrap, BorderLayout.EAST);
        switchView("home");
        return main;
    }

    private JPanel buildTopBar() {
        GlassPanel bar = new GlassPanel(glassFill, glassBorder, barArc);
        bar.setLayout(new BorderLayout(12, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel title = new JLabel(ui("appTitleShort"));
        title.setForeground(brandText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        left.add(title);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        center.setOpaque(false);
        statusPill = useRoundedControls() ? new RoundedLabel() : new JLabel();
        statusPill.setText(ui("statusOffline"));
        styleStatusPill(statusPill, false);
        guildStatusLabel = new JLabel(ui("guild") + ":");
        guildStatusLabel.setForeground(subtleText);
        channelStatusLabel = new JLabel(ui("textChannel") + ":");
        channelStatusLabel.setForeground(subtleText);
        center.add(statusPill);
        if (isDesktopOnboardingEnabled()) {
            center.add(guildStatusLabel);
            center.add(guildCombo);
            center.add(channelStatusLabel);
            center.add(channelCombo);
        }

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        statusMessageLabel = new JLabel();
        statusMessageLabel.setForeground(subtleText);
        statusMessageLabel.setFont(statusMessageLabel.getFont().deriveFont(Font.PLAIN, 12f));
        right.add(statusMessageLabel);
        JButton settingsQuick = new JButton(ui("navSettings"));
        styleSecondaryButton(settingsQuick);
        settingsQuick.addActionListener(e -> switchView("settings"));
        if (isDesktopOnboardingEnabled()) {
            styleSecondaryButton(refreshDesktopButton);
            stylePrimaryButton(launchPlayerButton);
            right.add(refreshDesktopButton);
            right.add(launchPlayerButton);
        }
        right.add(settingsQuick);

        bar.add(left, BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildPlayerView() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));

        GlassPanel hero = new GlassPanel(glassFill, glassBorder, heroArc);
        hero.setLayout(new BorderLayout(16, 16));
        hero.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        heroDisc = new DiscPanel(brandAccent, brandAccentAlt);
        heroDisc.setPreferredSize(new Dimension(160, 160));

        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new javax.swing.BoxLayout(heroText, javax.swing.BoxLayout.Y_AXIS));
        nowPlayingTitle = new JLabel(ui("nowPlayingPlaceholder"));
        nowPlayingTitle.setFont(nowPlayingTitle.getFont().deriveFont(Font.BOLD, 26f));
        nowPlayingTitle.setForeground(brandText);
        nowPlayingSubtitle = new JLabel(ui("playerIdle"));
        nowPlayingSubtitle.setFont(nowPlayingSubtitle.getFont().deriveFont(Font.PLAIN, 14f));
        nowPlayingSubtitle.setForeground(subtleText);
        nowPlayingHint = new JLabel(ui("playerHint"));
        nowPlayingHint.setFont(nowPlayingHint.getFont().deriveFont(Font.PLAIN, 12f));
        nowPlayingHint.setForeground(subtleText);

        heroText.add(nowPlayingTitle);
        heroText.add(Box.createVerticalStrut(6));
        heroText.add(nowPlayingSubtitle);
        heroText.add(Box.createVerticalStrut(8));
        heroText.add(nowPlayingHint);

        JPanel heroCenter = new JPanel(new BorderLayout(16, 0));
        heroCenter.setOpaque(false);
        heroCenter.add(heroDisc, BorderLayout.WEST);
        heroCenter.add(heroText, BorderLayout.CENTER);

        JPanel waveformWrap = new JPanel(new BorderLayout());
        waveformWrap.setOpaque(false);
        waveformWrap.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        waveformPanel = new WaveformPanel(brandAccent, brandAccentAlt);
        waveformWrap.add(waveformPanel, BorderLayout.CENTER);

        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setEnabled(false);
        progressSlider.putClientProperty("JSlider.isFilled", true);
        progressSlider.setPaintTicks(false);
        progressSlider.setOpaque(false);
        progressTimeLabel = new JLabel("0:00 / 0:00");
        progressTimeLabel.setForeground(subtleText);
        progressTimeLabel.setFont(progressTimeLabel.getFont().deriveFont(Font.PLAIN, 12f));

        JPanel progressRow = new JPanel(new BorderLayout(10, 0));
        progressRow.setOpaque(false);
        progressRow.add(progressSlider, BorderLayout.CENTER);
        progressRow.add(progressTimeLabel, BorderLayout.EAST);

        JPanel heroBottom = new JPanel();
        heroBottom.setOpaque(false);
        heroBottom.setLayout(new javax.swing.BoxLayout(heroBottom, javax.swing.BoxLayout.Y_AXIS));
        heroBottom.add(waveformWrap);
        heroBottom.add(Box.createVerticalStrut(12));
        heroBottom.add(progressRow);

        hero.add(heroCenter, BorderLayout.CENTER);
        hero.add(heroBottom, BorderLayout.SOUTH);

        GlassPanel controls = new GlassPanel(glassFill, glassBorder, controlsArc);
        controls.setLayout(new BorderLayout(16, 12));
        controls.setBorder(BorderFactory.createEmptyBorder(16, 20, 18, 20));

        JPanel mainControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        mainControls.setOpaque(false);
        mainControls.add(pauseButton);
        mainControls.add(resumeButton);
        mainControls.add(skipButton);
        mainControls.add(stopPlaybackButton);

        JPanel sliders = new JPanel(new GridBagLayout());
        sliders.setOpaque(false);
        GridBagConstraints s = new GridBagConstraints();
        s.insets = new Insets(4, 6, 4, 6);
        s.anchor = GridBagConstraints.WEST;
        s.fill = GridBagConstraints.HORIZONTAL;
        s.weightx = 0.0;

        s.gridx = 0;
        s.gridy = 0;
        JLabel volumeLabel = new JLabel(ui("volume"));
        volumeLabel.setForeground(subtleText);
        sliders.add(volumeLabel, s);
        s.gridx = 1;
        s.weightx = 1.0;
        sliders.add(volumeSlider, s);
        s.gridx = 2;
        s.weightx = 0.0;
        sliders.add(volumeValueLabel, s);
        s.gridx = 3;
        sliders.add(volumeResetButton, s);

        s.gridx = 0;
        s.gridy = 1;
        s.weightx = 0.0;
        JLabel bassLabel = new JLabel(ui("bass"));
        bassLabel.setForeground(subtleText);
        sliders.add(bassLabel, s);
        s.gridx = 1;
        s.weightx = 1.0;
        sliders.add(bassSlider, s);
        s.gridx = 2;
        s.weightx = 0.0;
        sliders.add(bassValueLabel, s);
        s.gridx = 3;
        sliders.add(bassResetButton, s);

        s.gridx = 0;
        s.gridy = 2;
        s.weightx = 0.0;
        JLabel overdriveLabel = new JLabel("EarRape");
        overdriveLabel.setForeground(subtleText);
        sliders.add(overdriveLabel, s);
        s.gridx = 1;
        s.weightx = 1.0;
        s.gridwidth = 3;
        sliders.add(earRapeToggleButton, s);
        s.gridwidth = 1;

        controls.add(mainControls, BorderLayout.NORTH);
        controls.add(sliders, BorderLayout.CENTER);

        panel.add(hero);
        panel.add(Box.createVerticalStrut(16));
        panel.add(controls);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildHomeView() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));

        GlassPanel welcome = new GlassPanel(glassFill, glassBorder, welcomeArc);
        welcome.setLayout(new BorderLayout(16, 16));
        welcome.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        homeDisc = new DiscPanel(brandAccent, brandAccentAlt);
        homeDisc.setPreferredSize(new Dimension(160, 160));
        homeDisc.setState(runtime.isRunning(), lastPlayingState);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel(ui("homeTitle"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(brandText);
        JLabel subtitle = new JLabel(ui("homeSubtitle"));
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        subtitle.setForeground(subtleText);
        JLabel hint = new JLabel(ui("homeHint"));
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 12f));
        hint.setForeground(subtleText);
        text.add(title);
        text.add(Box.createVerticalStrut(6));
        text.add(subtitle);
        text.add(Box.createVerticalStrut(8));
        text.add(hint);

        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setOpaque(false);
        center.add(homeDisc, BorderLayout.WEST);
        center.add(text, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        homeStartButton = new JButton(ui("start"));
        homeStopButton = new JButton(ui("stop"));
        stylePrimaryButton(homeStartButton);
        styleSecondaryButton(homeStopButton);
        actions.add(homeStartButton);
        actions.add(homeStopButton);

        welcome.add(center, BorderLayout.CENTER);
        welcome.add(actions, BorderLayout.SOUTH);

        panel.add(welcome);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildSettingsView() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));

        GlassPanel settingsCard = createGlassCard(ui("settings"));
        settingsCard.add(buildSettingsForm(), BorderLayout.CENTER);
        JPanel settingsActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        settingsActions.setOpaque(false);
        settingsActions.add(saveButton);
        settingsCard.add(settingsActions, BorderLayout.SOUTH);

        GlassPanel controlCard = createGlassCard(ui("botControl"));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(startButton);
        actions.add(stopButton);
        controlCard.add(actions, BorderLayout.CENTER);

        panel.add(settingsCard);
        panel.add(Box.createVerticalStrut(14));
        panel.add(controlCard);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildConsoleView() {
        GlassPanel consoleCard = createGlassCard(ui("console"));
        consoleCard.setLayout(new BorderLayout(0, 12));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(toggleConsoleButton);
        actions.add(clearConsoleButton);

        consoleCard.add(actions, BorderLayout.NORTH);
        consoleCard.add(consoleScrollPane, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(consoleCard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRightRail() {
        rightRail = new JPanel();
        rightRail.setOpaque(false);
        rightRail.setLayout(new javax.swing.BoxLayout(rightRail, javax.swing.BoxLayout.Y_AXIS));

        rightQuickAddCard = buildQuickAddCard();
        rightSearchCard = buildSearchCard();
        rightQueueCard = buildQueueCard();
        rightQuickActionsCard = buildQuickActionsCard();
        rightSettingsHintCard = null;
        rightConsoleHintCard = buildHintCard(ui("console"), ui("consoleHint"));

        renderRightRail("home");
        return rightRail;
    }

    private JScrollPane wrapRightRail(JPanel rail) {
        JScrollPane scroll = new JScrollPane(rail);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildSettingsForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 6, 8, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        JLabel tokenLabel = new JLabel(ui("botToken") + ":");
        tokenLabel.setForeground(subtleText);
        form.add(tokenLabel, c);

        c.gridx = 1;
        c.weightx = 1.0;
        form.add(tokenField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        JLabel prefixLabel = new JLabel(ui("prefix") + ":");
        prefixLabel.setForeground(subtleText);
        form.add(prefixLabel, c);

        c.gridx = 1;
        c.weightx = 1.0;
        form.add(prefixField, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        JLabel botLanguageLabel = new JLabel(ui("botLanguage") + ":");
        botLanguageLabel.setForeground(subtleText);
        form.add(botLanguageLabel, c);

        c.gridx = 1;
        c.weightx = 1.0;
        form.add(languageCombo, c);

        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0;
        JLabel panelLanguageLabel = new JLabel(ui("controlPanelLanguage") + ":");
        panelLanguageLabel.setForeground(subtleText);
        form.add(panelLanguageLabel, c);

        c.gridx = 1;
        c.weightx = 1.0;
        form.add(controlPanelLanguageCombo, c);

        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0;
        JLabel themeLabel = new JLabel(ui("theme") + ":");
        themeLabel.setForeground(subtleText);
        form.add(themeLabel, c);

        c.gridx = 1;
        c.weightx = 1.0;
        form.add(themeCombo, c);

        c.gridx = 0;
        c.gridy = 5;
        c.weightx = 0;
        JLabel cornerStyleLabel = new JLabel(ui("cornerStyle") + ":");
        cornerStyleLabel.setForeground(subtleText);
        form.add(cornerStyleLabel, c);

        c.gridx = 1;
        c.weightx = 1.0;
        form.add(cornerStyleCombo, c);
        return form;
    }

    private JPanel buildQuickAddCard() {
        GlassPanel card = createGlassCard(ui("songUrl"));
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        row.add(addSongField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(addSongButton);
        buttons.add(playNextButton);
        row.add(buttons, c);

        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSearchCard() {
        GlassPanel card = createGlassCard(ui("searchResultsPanel"));
        JScrollPane scroll = new JScrollPane(searchResultsList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(320, 220));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildQueueCard() {
        GlassPanel card = createGlassCard(ui("queueList"));
        JScrollPane scroll = new JScrollPane(queueList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(320, 240));
        card.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JLabel queueIndexLabel = new JLabel(ui("queueIndex"));
        queueIndexLabel.setForeground(subtleText);
        actions.add(queueIndexLabel);
        actions.add(removeIndexField);
        actions.add(removeQueueButton);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildQuickActionsCard() {
        GlassPanel card = createGlassCard(ui("quickActions"));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(shuffleQueueButton);
        actions.add(clearQueueButton);
        actions.add(cleanupScopeButton);
        card.add(actions, BorderLayout.CENTER);
        return card;
    }

    private JButton createNavButton(String title, String view) {
        JButton button = new JButton(title);
        button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12.5f));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        button.setForeground(subtleText);
        if (useRoundedControls()) {
            applyMacRoundedButton(button, null, navSelectedArc, new Insets(10, 12, 10, 12));
            updateMacRoundedButton(button, new Color(0, 0, 0, 0), null, navSelectedArc);
        }
        button.addActionListener(e -> switchView(view));
        return button;
    }

    private void switchView(String view) {
        activeNav = view;
        String centerView;
        if ("home".equals(view)) {
            centerView = "home";
        } else if ("player".equals(view)) {
            centerView = "player";
        } else if ("settings".equals(view)) {
            centerView = "settings";
        } else if ("console".equals(view)) {
            centerView = "console";
        } else {
            centerView = "player";
        }
        if (centerCardLayout != null) {
            centerCardLayout.show(centerCardPanel, centerView);
        }
        renderRightRail(view);
        setActiveNav(view);
    }

    private void setActiveNav(String view) {
        setNavSelected(navHomeButton, "home".equals(view));
        setNavSelected(navPlayerButton, "player".equals(view));
        setNavSelected(navQueueButton, "queue".equals(view));
        setNavSelected(navSearchButton, "search".equals(view));
        setNavSelected(navSettingsButton, "settings".equals(view));
        setNavSelected(navConsoleButton, "console".equals(view));
    }

    private void setNavSelected(JButton button, boolean selected) {
        if (button == null) {
            return;
        }
        if (selected) {
            Color navFill = mixColor(brandPanelAlt, brandAccent, isLightThemePalette() ? 0.18f : 0.26f);
            Color navBorder = tintColor(brandAccent, isLightThemePalette() ? 0.22f : 0.35f);
            button.setBackground(navFill);
            button.setForeground(readableTextOn(navFill));
            if (useRoundedControls()) {
                updateMacRoundedButton(button, button.getBackground(), navBorder, navSelectedArc);
                button.setBorder(BorderFactory.createEmptyBorder(9, 11, 9, 11));
                button.setOpaque(false);
                button.setContentAreaFilled(false);
            } else {
                button.setOpaque(true);
                button.setContentAreaFilled(true);
                button.setBorder(BorderFactory.createCompoundBorder(
                        roundedLineBorder(navBorder, 1, navSelectedArc),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)
                ));
            }
        } else {
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setForeground(subtleText);
            button.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            if (useRoundedControls()) {
                updateMacRoundedButton(button, new Color(0, 0, 0, 0), null, navSelectedArc);
            }
        }
    }

    private void renderRightRail(String view) {
        if (rightRail == null) {
            return;
        }
        rightRail.removeAll();
        boolean showHome = "home".equals(view) || "player".equals(view);
        boolean showQueue = "queue".equals(view);
        boolean showSearch = "search".equals(view);
        boolean showSettings = "settings".equals(view);
        boolean showConsole = "console".equals(view);

        if (showHome || showSearch) {
            addRightSection(rightQuickAddCard);
            addRightSection(rightSearchCard);
        }
        if (showHome || showQueue) {
            addRightSection(rightQueueCard);
        }
        if (showHome) {
            addRightSection(rightQuickActionsCard);
        }
        if (showSettings) {
            addRightSection(rightSettingsHintCard);
        }
        if (showConsole) {
            addRightSection(rightConsoleHintCard);
        }
        rightRail.add(Box.createVerticalGlue());
        rightRail.revalidate();
        rightRail.repaint();
        if (rightRailScroll != null) {
            rightRailScroll.getViewport().setViewPosition(new Point(0, 0));
        }
    }

    private void addRightSection(JPanel card) {
        if (card == null) {
            return;
        }
        rightRail.add(card);
        rightRail.add(Box.createVerticalStrut(12));
    }

    private GlassPanel buildHintCard(String title, String hintText) {
        GlassPanel card = createGlassCard(title);
        JLabel hint = new JLabel(hintText);
        hint.setForeground(subtleText);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 12f));
        card.add(hint, BorderLayout.CENTER);
        return card;
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
                showStatusMessage(ui("settingsSaved"));
            } catch (Exception ex) {
                showError(ui("saveFailed") + ": " + ex.getMessage());
            }
        });

        java.awt.event.ActionListener startAction = e -> {
            setStartStopEnabled(true);
            worker.submit(() -> {
                try {
                    saveConfigFromFields();
                    log(ui("startingBot"));
                    runtime.start(resolveConfigPath(), true, this::log);
                    SwingUtilities.invokeLater(() -> {
                        setStartStopEnabled(true);
                        setDesktopControlsEnabled(true);
                        refreshCleanupScopeButtonAsync();
                        refreshPlayerPanelToggleButtonAsync();
                        setConnectionStatus(true);
                    });
                    refreshDesktopSelectorsAsync();
                    refreshPlayerSummaryAsync();
                } catch (Exception ex) {
                    log(ui("startFailed") + ": " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> setStartStopEnabled(false));
                }
            });
        };

        java.awt.event.ActionListener stopAction = e -> worker.submit(() -> {
            runtime.stop(this::log);
            SwingUtilities.invokeLater(() -> {
                setStartStopEnabled(false);
                setEarRapeEnabled(false);
                setDesktopControlsEnabled(false);
                setCleanupScopeButtonState(false, false);
                setPlayerPanelToggleButtonState(false, false);
                setConnectionStatus(false);
                pendingNowPlayingTitle = null;
                pendingNowPlayingUntilMillis = 0L;
            });
        });

        startButton.addActionListener(startAction);
        stopButton.addActionListener(stopAction);
        if (homeStartButton != null) {
            homeStartButton.addActionListener(startAction);
        }
        if (homeStopButton != null) {
            homeStopButton.addActionListener(stopAction);
        }

        clearConsoleButton.addActionListener(e -> console.setText(""));
        toggleConsoleButton.addActionListener(e -> setConsoleVisible(!consoleVisible));

        if (isDesktopOnboardingEnabled()) {
            guildCombo.addActionListener(ignored -> refreshChannelsAsync());
            guildCombo.addActionListener(ignored -> refreshPlayerSummaryAsync());
            guildCombo.addActionListener(ignored -> refreshPlayerPanelToggleButtonAsync());
            guildCombo.addActionListener(ignored -> setEarRapeEnabled(false));

            addSongButton.addActionListener(ignored -> submitDesktopEnqueue(false));
            playNextButton.addActionListener(ignored -> submitDesktopEnqueue(true));

            addSongField.addActionListener(ignored -> addSongButton.doClick());

            pauseButton.addActionListener(ignored -> desktopControlAsync("pause", BotRuntime::pauseFromDesktop));
            resumeButton.addActionListener(ignored -> desktopControlAsync("resume", BotRuntime::resumeFromDesktop));
            skipButton.addActionListener(ignored -> desktopControlAsync("skip", BotRuntime::skipFromDesktop));
            stopPlaybackButton.addActionListener(ignored -> desktopControlAsync("stop", BotRuntime::stopFromDesktop));
            volumeSlider.addChangeListener(event -> {
                if (volumeSlider.getValueIsAdjusting()) {
                    updateVolumeLabel(volumeSlider.getValue());
                    return;
                }
                int value = volumeSlider.getValue();
                updateVolumeLabel(value);
                worker.submit(() -> {
                    Long guildId = selectedGuildId();
                    if (guildId == null) {
                        SwingUtilities.invokeLater(() -> showError(ui("selectGuildTextChannelFirst")));
                        return;
                    }
                    try {
                        runtime.setVolumeFromDesktop(guildId, value);
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
                    }
                });
            });
            volumeResetButton.addActionListener(ignored -> {
                volumeSlider.setValue(100);
                updateVolumeLabel(100);
                worker.submit(() -> {
                    Long guildId = selectedGuildId();
                    if (guildId == null) {
                        SwingUtilities.invokeLater(() -> showError(ui("selectGuildTextChannelFirst")));
                        return;
                    }
                    try {
                        runtime.setVolumeFromDesktop(guildId, 100);
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
                    }
                });
            });
            bassSlider.addChangeListener(event -> {
                if (bassSlider.getValueIsAdjusting()) {
                    updateBassLabel(bassSlider.getValue());
                    return;
                }
                int value = bassSlider.getValue();
                updateBassLabel(value);
                worker.submit(() -> {
                    Long guildId = selectedGuildId();
                    if (guildId == null) {
                        SwingUtilities.invokeLater(() -> showError(ui("selectGuildTextChannelFirst")));
                        return;
                    }
                    try {
                        runtime.setBassFromDesktop(guildId, value);
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
                    }
                });
            });
            bassResetButton.addActionListener(ignored -> {
                bassSlider.setValue(0);
                updateBassLabel(0);
                worker.submit(() -> {
                    Long guildId = selectedGuildId();
                    if (guildId == null) {
                        SwingUtilities.invokeLater(() -> showError(ui("selectGuildTextChannelFirst")));
                        return;
                    }
                    try {
                        runtime.setBassFromDesktop(guildId, 0);
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
                    }
                });
            });
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
                    boolean panelExists = runtime.hasPlayerPanelForGuild(guildId);
                    if (panelExists) {
                        runtime.removePlayerPanelFromDesktop(guildId, channelId);
                        log(ui("playerPanelRemoved"));
                        setPlayerPanelToggleButtonState(true, false);
                    } else {
                        runtime.launchPlayerPanelFromDesktop(guildId, channelId);
                        log(ui("playerPanelPosted"));
                        setPlayerPanelToggleButtonState(true, true);
                    }
                    refreshPlayerPanelToggleButtonAsync();
                    refreshPlayerSummaryAsync();
                    CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(this::refreshPlayerPanelToggleButtonAsync);
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
                }
            }));
        }
    }

    private JPanel buildDesktopPlayerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(1080, 820));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        boolean roundedControls = useRoundedControls();
        guildCombo = roundedControls ? new RoundedComboBox<>() : new JComboBox<>();
        channelCombo = roundedControls ? new RoundedComboBox<>() : new JComboBox<>();
        addSongField = roundedControls ? new RoundedTextField() : new JTextField();
        addSongButton = new JButton(ui("addSong"));
        playNextButton = new JButton(ui("playNext"));
        stylePrimaryButton(addSongButton);
        styleSecondaryButton(playNextButton);
        pauseButton = new JButton(ui("pause"));
        resumeButton = new JButton(ui("resume"));
        skipButton = new JButton(ui("skip"));
        stopPlaybackButton = new JButton(ui("stop"));
        removeIndexField = roundedControls ? new RoundedTextField(4) : new JTextField(4);
        removeIndexField.setToolTipText(ui("queueIndexHint"));
        removeQueueButton = new JButton(ui("queueRemove"));
        shuffleQueueButton = new JButton(ui("queueShuffle"));
        clearQueueButton = new JButton(ui("queueClear"));
        volumeSlider = new JSlider(0, 200, 100);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setMajorTickSpacing(50);
        volumeSlider.setMinorTickSpacing(10);
        volumeValueLabel = new JLabel(ui("volume") + ": 100%");
        volumeValueLabel.setForeground(subtleText);
        volumeResetButton = new JButton(ui("reset"));
        styleSecondaryButton(volumeResetButton);
        bassSlider = new JSlider(0, 5, 0);
        bassSlider.setPaintTicks(true);
        bassSlider.setMajorTickSpacing(1);
        bassSlider.setMinorTickSpacing(1);
        bassValueLabel = new JLabel(ui("bass") + ": 0");
        bassValueLabel.setForeground(subtleText);
        bassResetButton = new JButton(ui("reset"));
        styleSecondaryButton(bassResetButton);
        searchResultsModel = new DefaultListModel<>();
        searchResultsList = new JList<>(searchResultsModel);
        searchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultsList.setVisibleRowCount(6);
        searchResultsList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    enqueueSelectedSearchResult(false);
                }
            }
        });
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
        launchPlayerButton = new JButton();
        styleSecondaryButton(refreshDesktopButton);
        stylePrimaryButton(launchPlayerButton);
        setPlayerPanelToggleButtonState(false, false);
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
        controls.setOpaque(false);
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

        JPanel sliders = new JPanel(new GridBagLayout());
        sliders.setOpaque(false);
        GridBagConstraints s = new GridBagConstraints();
        s.insets = new Insets(4, 6, 4, 6);
        s.anchor = GridBagConstraints.WEST;
        s.fill = GridBagConstraints.HORIZONTAL;
        s.weightx = 0.0;

        s.gridx = 0;
        s.gridy = 0;
        sliders.add(new JLabel(ui("volume") + ":"), s);
        s.gridx = 1;
        s.weightx = 1.0;
        sliders.add(volumeSlider, s);
        s.gridx = 2;
        s.weightx = 0.0;
        sliders.add(volumeValueLabel, s);
        s.gridx = 3;
        sliders.add(volumeResetButton, s);

        s.gridx = 0;
        s.gridy = 1;
        sliders.add(new JLabel(ui("bass") + ":"), s);
        s.gridx = 1;
        s.weightx = 1.0;
        sliders.add(bassSlider, s);
        s.gridx = 2;
        s.weightx = 0.0;
        sliders.add(bassValueLabel, s);
        s.gridx = 3;
        sliders.add(bassResetButton, s);

        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sliders, c);

        c.gridy = 5;
        c.gridx = 0;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.3;
        JScrollPane searchScroll = new JScrollPane(searchResultsList);
        searchScroll.setBorder(BorderFactory.createTitledBorder(ui("searchResultsPanel")));
        searchScroll.setPreferredSize(new Dimension(860, 140));
        panel.add(searchScroll, c);

        c.gridy = 6;
        c.gridx = 0;
        c.weighty = 0.7;
        c.gridwidth = 4;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.BOTH;
        JScrollPane queueScroll = new JScrollPane(queueList);
        queueScroll.setBorder(BorderFactory.createTitledBorder(ui("queueList")));
        queueScroll.setPreferredSize(new Dimension(860, 260));
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
        volumeSlider.setEnabled(enabled);
        bassSlider.setEnabled(enabled);
        volumeResetButton.setEnabled(enabled);
        bassResetButton.setEnabled(enabled);
        searchResultsList.setEnabled(enabled);
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

    private void refreshPlayerPanelToggleButtonAsync() {
        worker.submit(() -> {
            Long guildId = selectedGuildId();
            boolean hasPanel = guildId != null && runtime.hasPlayerPanelForGuild(guildId);
            setPlayerPanelToggleButtonState(runtime.isRunning(), hasPanel);
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

    private void setPlayerPanelToggleButtonState(boolean enabled, boolean panelExists) {
        SwingUtilities.invokeLater(() -> {
            if (launchPlayerButton == null) {
                return;
            }
            launchPlayerButton.setEnabled(enabled);
            launchPlayerButton.setText(ui(panelExists ? "removePlayer" : "showPlayer"));
        });
    }

    private void setEarRapeEnabled(boolean enabled) {
        earRapeEnabled = enabled;
        if (earRapeToggleButton != null) {
            earRapeToggleButton.setText("EarRape: " + uiForLanguage(enabled ? "toggleOn" : "toggleOff", currentControlPanelLanguage()));
            earRapeToggleButton.setBackground(enabled ? brandAccentAlt : brandPanelAlt);
            earRapeToggleButton.setForeground(enabled ? new Color(245, 247, 255) : brandText);
        }
    }

    private String currentControlPanelLanguage() {
        if (controlPanelLanguageCombo != null) {
            LanguageOption selected = (LanguageOption) controlPanelLanguageCombo.getSelectedItem();
            if (selected != null && selected.code() != null && !selected.code().isBlank()) {
                return selected.code().trim().toLowerCase();
            }
        }
        return controlPanelLanguageCode == null ? "en" : controlPanelLanguageCode.toLowerCase();
    }

    private String uiForLanguage(String key, String lang) {
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

    private void setConsoleVisible(boolean visible) {
        consoleVisible = visible;
        if (consoleScrollPane != null) {
            consoleScrollPane.setVisible(visible);
        }
        if (toggleConsoleButton != null) {
            toggleConsoleButton.setText(ui(visible ? "hideConsole" : "showConsole"));
        }
        if (frame != null) {
            frame.revalidate();
            frame.repaint();
        }
    }

    private void setConnectionStatus(boolean running) {
        if (statusPill == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            styleStatusPill(statusPill, running);
            if (heroDisc != null) {
                heroDisc.setState(running, lastPlayingState);
            }
            if (homeDisc != null) {
                homeDisc.setState(running, lastPlayingState);
            }
        });
    }

    private void showStatusMessage(String message) {
        if (statusMessageLabel == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            statusMessageLabel.setText(message);
            if (statusMessageTimer != null && statusMessageTimer.isRunning()) {
                statusMessageTimer.stop();
            }
            statusMessageTimer = new Timer(3500, e -> statusMessageLabel.setText(""));
            statusMessageTimer.setRepeats(false);
            statusMessageTimer.start();
        });
    }

    private void setStartStopEnabled(boolean running) {
        boolean startEnabled = !running;
        boolean stopEnabled = running;
        if (startButton != null) {
            startButton.setEnabled(startEnabled);
        }
        if (stopButton != null) {
            stopButton.setEnabled(stopEnabled);
        }
        if (homeStartButton != null) {
            homeStartButton.setEnabled(startEnabled);
        }
        if (homeStopButton != null) {
            homeStopButton.setEnabled(stopEnabled);
        }
    }

    private void styleStatusPill(JLabel label, boolean running) {
        label.setText(ui(running ? "statusOnline" : "statusOffline"));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11.5f));
        Color fill = running ? brandAccent : brandPanelAlt;
        Color border = running ? tintColor(brandAccent, 0.35f) : brandBorder;
        label.setForeground(readableTextOn(fill));
        label.setBackground(fill);
        if (useRoundedControls() && label instanceof RoundedLabel rounded) {
            rounded.setRoundStyle(label.getBackground(), border, 999);
            label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            label.setOpaque(false);
        } else {
            label.setOpaque(true);
            label.setBorder(BorderFactory.createCompoundBorder(
                    roundedLineBorder(border, 1, buttonBorderArc),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
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
        String theme = controlPanelThemeCode == null ? "neon" : controlPanelThemeCode.toLowerCase();
        switch (theme) {
            case "midnight" -> {
                brandAccent = new Color(96, 160, 255);
                brandAccentAlt = new Color(138, 100, 255);
                brandGlow = new Color(90, 140, 220);
                brandText = new Color(232, 239, 255);
                subtleText = new Color(156, 166, 196);
                brandBackground = new Color(8, 10, 20);
                brandBackgroundAlt = new Color(14, 16, 28);
                brandPanel = new Color(16, 20, 34);
                brandPanelAlt = new Color(20, 24, 40);
                brandBorder = new Color(50, 68, 110);
                glassFill = new Color(16, 22, 38, 200);
                glassBorder = new Color(80, 110, 160, 150);
                sidebarBackground = new Color(10, 14, 26);
            }
            case "nebula" -> {
                brandAccent = new Color(112, 180, 255);
                brandAccentAlt = new Color(188, 112, 255);
                brandGlow = new Color(110, 160, 255);
                brandText = new Color(236, 240, 255);
                subtleText = new Color(168, 172, 210);
                brandBackground = new Color(12, 10, 26);
                brandBackgroundAlt = new Color(22, 18, 40);
                brandPanel = new Color(22, 20, 40);
                brandPanelAlt = new Color(28, 26, 48);
                brandBorder = new Color(70, 84, 130);
                glassFill = new Color(24, 22, 44, 200);
                glassBorder = new Color(110, 120, 180, 150);
                sidebarBackground = new Color(14, 12, 28);
            }
            case "graphite" -> {
                brandAccent = new Color(96, 190, 255);
                brandAccentAlt = new Color(120, 140, 255);
                brandGlow = new Color(98, 150, 220);
                brandText = new Color(232, 236, 244);
                subtleText = new Color(160, 168, 190);
                brandBackground = new Color(12, 14, 18);
                brandBackgroundAlt = new Color(18, 20, 26);
                brandPanel = new Color(20, 22, 30);
                brandPanelAlt = new Color(26, 28, 36);
                brandBorder = new Color(60, 70, 90);
                glassFill = new Color(22, 24, 34, 200);
                glassBorder = new Color(90, 105, 135, 150);
                sidebarBackground = new Color(14, 16, 22);
            }
            case "twilight" -> {
                brandAccent = new Color(110, 190, 255);
                brandAccentAlt = new Color(168, 120, 255);
                brandGlow = new Color(130, 170, 230);
                brandText = new Color(236, 240, 255);
                subtleText = new Color(184, 190, 212);
                brandBackground = new Color(26, 30, 44);
                brandBackgroundAlt = new Color(34, 40, 58);
                brandPanel = new Color(32, 38, 56);
                brandPanelAlt = new Color(40, 46, 66);
                brandBorder = new Color(90, 102, 140);
                glassFill = new Color(36, 42, 62, 200);
                glassBorder = new Color(120, 132, 170, 150);
                sidebarBackground = new Color(22, 26, 40);
            }
            case "pearl" -> {
                brandAccent = new Color(74, 160, 255);
                brandAccentAlt = new Color(176, 120, 255);
                brandGlow = new Color(120, 180, 255);
                brandText = new Color(28, 34, 52);
                subtleText = new Color(86, 98, 128);
                brandBackground = new Color(232, 237, 247);
                brandBackgroundAlt = new Color(214, 224, 240);
                brandPanel = new Color(244, 247, 253);
                brandPanelAlt = new Color(230, 236, 248);
                brandBorder = new Color(174, 190, 220);
                glassFill = new Color(246, 249, 255, 210);
                glassBorder = new Color(170, 188, 220, 150);
                sidebarBackground = new Color(222, 230, 243);
            }
            case "sky" -> {
                brandAccent = new Color(64, 170, 255);
                brandAccentAlt = new Color(120, 140, 255);
                brandGlow = new Color(120, 190, 255);
                brandText = new Color(24, 34, 50);
                subtleText = new Color(80, 94, 122);
                brandBackground = new Color(224, 235, 248);
                brandBackgroundAlt = new Color(206, 222, 242);
                brandPanel = new Color(238, 245, 252);
                brandPanelAlt = new Color(226, 236, 250);
                brandBorder = new Color(170, 188, 214);
                glassFill = new Color(240, 246, 255, 210);
                glassBorder = new Color(168, 186, 214, 150);
                sidebarBackground = new Color(214, 226, 243);
            }
            case "dawn" -> {
                brandAccent = new Color(255, 168, 88);
                brandAccentAlt = new Color(255, 122, 180);
                brandGlow = new Color(255, 180, 120);
                brandText = new Color(42, 30, 32);
                subtleText = new Color(100, 86, 90);
                brandBackground = new Color(246, 236, 228);
                brandBackgroundAlt = new Color(236, 224, 214);
                brandPanel = new Color(252, 244, 238);
                brandPanelAlt = new Color(242, 232, 224);
                brandBorder = new Color(200, 184, 172);
                glassFill = new Color(252, 245, 238, 210);
                glassBorder = new Color(196, 178, 170, 150);
                sidebarBackground = new Color(238, 228, 218);
            }
            case "frost" -> {
                brandAccent = new Color(96, 176, 255);
                brandAccentAlt = new Color(140, 160, 220);
                brandGlow = new Color(130, 190, 255);
                brandText = new Color(32, 40, 52);
                subtleText = new Color(90, 102, 128);
                brandBackground = new Color(228, 234, 244);
                brandBackgroundAlt = new Color(210, 220, 236);
                brandPanel = new Color(240, 245, 252);
                brandPanelAlt = new Color(226, 234, 248);
                brandBorder = new Color(176, 188, 210);
                glassFill = new Color(238, 244, 252, 210);
                glassBorder = new Color(170, 186, 210, 150);
                sidebarBackground = new Color(216, 226, 240);
            }
            default -> {
                brandAccent = new Color(64, 217, 255);
                brandAccentAlt = new Color(164, 88, 255);
                brandGlow = new Color(86, 160, 255);
                brandText = new Color(232, 239, 255);
                subtleText = new Color(168, 176, 204);
                brandBackground = new Color(10, 12, 24);
                brandBackgroundAlt = new Color(18, 16, 34);
                brandPanel = new Color(18, 24, 38);
                brandPanelAlt = new Color(24, 30, 46);
                brandBorder = new Color(60, 78, 120);
                glassFill = new Color(20, 28, 45, 200);
                glassBorder = new Color(90, 120, 170, 150);
                sidebarBackground = new Color(12, 16, 30);
            }
        }

        UIManager.put("Panel.background", brandBackground);
        UIManager.put("OptionPane.background", brandPanel);
        UIManager.put("Label.foreground", brandText);
        UIManager.put("Button.background", brandPanelAlt);
        UIManager.put("Button.foreground", brandText);
        UIManager.put("TextField.background", brandPanelAlt);
        UIManager.put("TextField.foreground", brandText);
        UIManager.put("TextField.caretForeground", brandAccent);
        UIManager.put("PasswordField.background", brandPanelAlt);
        UIManager.put("PasswordField.foreground", brandText);
        UIManager.put("PasswordField.caretForeground", brandAccent);
        UIManager.put("ComboBox.background", brandPanelAlt);
        UIManager.put("ComboBox.foreground", brandText);
        UIManager.put("TextArea.background", new Color(12, 16, 24));
        UIManager.put("TextArea.foreground", brandText);
        UIManager.put("TextArea.caretForeground", brandAccent);
        UIManager.put("TitledBorder.titleColor", brandText);
        UIManager.put("control", brandBackground);
        UIManager.put("ScrollPane.background", brandBackground);
        UIManager.put("List.background", new Color(14, 20, 32));
        UIManager.put("List.foreground", brandText);
        UIManager.put("List.selectionBackground", new Color(34, 86, 140));
        UIManager.put("List.selectionForeground", new Color(242, 248, 255));
        UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Button.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("PasswordField.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("ComboBox.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("TextArea.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("List.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("Separator.foreground", brandBorder);
        UIManager.put("Separator.background", brandBackground);
        UIManager.put("ScrollBar.thumb", new Color(70, 90, 130));
        UIManager.put("ScrollBar.track", brandBackground);
        UIManager.put("Slider.trackColor", new Color(32, 40, 60));
        UIManager.put("Slider.trackValueColor", brandAccent);
        UIManager.put("Slider.thumbColor", brandAccent);
        UIManager.put("Slider.focusColor", brandAccent);
        UIManager.put("Slider.thumbBorderColor", brandGlow);
    }

    private void setupLookAndFeel() {
        boolean roundedControls = useRoundedControls();
        try {
            FlatDarkLaf.setup();
            UIManager.put("Component.arc", roundedControls ? 18 : 12);
            UIManager.put("Button.arc", roundedControls ? 18 : 12);
            UIManager.put("TextComponent.arc", roundedControls ? 12 : 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Component.focusWidth", 0);
            UIManager.put("Button.focusWidth", 0);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("TitlePane.unifiedBackground", true);
            UIManager.put("TitlePane.background", new Color(12, 16, 30));
        } catch (Exception ignored) {
        }
        if (macUi) {
            barArc = 24;
            cardArc = 24;
            heroArc = 30;
            controlsArc = 24;
            welcomeArc = 30;
        } else {
            barArc = 18;
            cardArc = 18;
            heroArc = 26;
            controlsArc = 22;
            welcomeArc = 26;
        }
        if (roundedControls) {
            buttonBorderArc = 999;
            navSelectedArc = 999;
            inputBorderArc = 999;
        } else {
            buttonBorderArc = 18;
            navSelectedArc = 14;
            inputBorderArc = 12;
        }
    }

    private boolean useRoundedControls() {
        String style = controlPanelCornerStyleCode == null ? "" : controlPanelCornerStyleCode.trim().toLowerCase();
        if (style.isBlank()) {
            return macUi;
        }
        return "rounded".equals(style);
    }

    private JPanel createCard(String title) {
        return createGlassCard(title);
    }

    private GlassPanel createGlassCard(String title) {
        GlassPanel card = new GlassPanel(glassFill, glassBorder, cardArc);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        if (title != null && !title.isBlank()) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13.5f));
            titleLabel.setForeground(brandText);
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(titleLabel, BorderLayout.WEST);
            JSeparator separator = new JSeparator();
            separator.setForeground(brandBorder);
            separator.setBackground(brandBorder);

            JPanel headerWrap = new JPanel(new BorderLayout(0, 6));
            headerWrap.setOpaque(false);
            headerWrap.add(header, BorderLayout.NORTH);
            headerWrap.add(separator, BorderLayout.SOUTH);
            card.add(headerWrap, BorderLayout.NORTH);
        }
        return card;
    }

    private void styleInputField(JTextField field, int height) {
        field.setPreferredSize(new Dimension(240, height));
        field.setBackground(brandPanelAlt);
        field.setForeground(brandText);
        field.setCaretColor(brandAccent);
        if (useRoundedControls() && field instanceof RoundedField rounded) {
            rounded.setRoundStyle(brandPanelAlt, brandBorder, 999);
            field.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        } else {
            field.setOpaque(true);
            field.setBorder(BorderFactory.createCompoundBorder(
                    roundedLineBorder(brandBorder, 1, inputBorderArc),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
        }
    }

    private void styleCombo(JComboBox<?> combo, int height) {
        combo.setPreferredSize(new Dimension(240, height));
        combo.setBackground(brandPanelAlt);
        combo.setForeground(brandText);
        if (useRoundedControls() && combo instanceof RoundedComboBox<?> rounded) {
            rounded.setRoundStyle(brandPanelAlt, brandBorder, 999);
            combo.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 6));
        } else {
            combo.setOpaque(true);
            combo.setBorder(roundedLineBorder(brandBorder, 1, inputBorderArc));
        }
    }

    private void prepareIntroLayout() {
        // Keep all cards visible; animation uses window fade-in only.
    }

    private void startIntroAnimation() {
        if (frame == null) {
            return;
        }

        boolean canFade = supportsWindowOpacity();
        if (canFade) {
            try {
                frame.setOpacity(0f);
            } catch (UnsupportedOperationException ignored) {
                canFade = false;
            }
        }

        if (!canFade) {
            return;
        }

        final float[] alpha = {0f};
        Timer fade = new Timer(16, null);
        fade.addActionListener(e -> {
            float value = Math.min(1f, alpha[0] + 0.08f);
            try {
                frame.setOpacity(value);
            } catch (UnsupportedOperationException ignored) {
                fade.stop();
                return;
            }
            alpha[0] = value;
            if (value >= 1f) {
                fade.stop();
            }
        });
        fade.start();
    }

    private void startStartupAnimation() {
        if (frame == null) {
            return;
        }

        setInitialPanelAnimationState();
        splashOverlay = new SplashOverlay();
        frame.setGlassPane(splashOverlay);
        splashOverlay.setVisible(true);

        final long start = System.currentTimeMillis();
        final int startupMillis = 1800;
        final int readyMillis = 2000;
        final int totalMillis = 2600;

        splashTimer = new Timer(16, null);
        splashTimer.setCoalesce(false);
        splashTimer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - start;
            float progress = Math.min(1f, elapsed / (float) startupMillis);
            boolean connected = elapsed >= startupMillis;
            float transition = elapsed <= readyMillis ? 0f : Math.min(1f, (elapsed - readyMillis) / (float) (totalMillis - readyMillis));

            splashOverlay.update(progress, connected, transition);
            applyPanelAnimationState(transition);

            if (elapsed >= totalMillis) {
                splashTimer.stop();
                applyPanelAnimationState(1f);
                splashOverlay.setVisible(false);
            }
        });
        splashTimer.start();
    }

    private void setInitialPanelAnimationState() {
        if (headerWrap != null) {
            headerWrap.setAnimation(0f, 0, -10);
        }
        if (sidebarWrap != null) {
            sidebarWrap.setAnimation(0f, -20, 0);
        }
        if (centerWrap != null) {
            centerWrap.setAnimation(0f, 0, 14);
        }
        if (rightWrap != null) {
            rightWrap.setAnimation(0f, 20, 0);
        }
    }

    private void applyPanelAnimationState(float rawProgress) {
        float t = easeInOut(rawProgress);
        if (headerWrap != null) {
            headerWrap.setAnimation(t, 0, (int) (-10 * (1 - t)));
        }
        if (sidebarWrap != null) {
            sidebarWrap.setAnimation(t, (int) (-20 * (1 - t)), 0);
        }
        if (centerWrap != null) {
            centerWrap.setAnimation(t, 0, (int) (14 * (1 - t)));
        }
        if (rightWrap != null) {
            rightWrap.setAnimation(t, (int) (20 * (1 - t)), 0);
        }
    }

    private float easeInOut(float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return clamped * clamped * (3f - 2f * clamped);
    }

    private boolean supportsWindowOpacity() {
        try {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            return device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        } catch (Exception ignored) {
            return false;
        }
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

    private Color mixColor(Color a, Color b, float t) {
        float mix = Math.max(0.0f, Math.min(1.0f, t));
        int r = (int) (a.getRed() * (1.0f - mix) + b.getRed() * mix);
        int g = (int) (a.getGreen() * (1.0f - mix) + b.getGreen() * mix);
        int bb = (int) (a.getBlue() * (1.0f - mix) + b.getBlue() * mix);
        return new Color(r, g, bb);
    }

    private void styleList(JList<?> list) {
        list.setBackground(brandPanelAlt);
        list.setForeground(brandText);
        list.setSelectionBackground(mixColor(brandPanelAlt, brandAccent, isLightThemePalette() ? 0.25f : 0.32f));
        list.setSelectionForeground(readableTextOn(list.getSelectionBackground()));
    }

    private boolean isLightThemePalette() {
        return luminance(brandBackground) > 0.62f;
    }

    private float luminance(Color color) {
        if (color == null) {
            return 0f;
        }
        return (0.2126f * color.getRed() + 0.7152f * color.getGreen() + 0.0722f * color.getBlue()) / 255f;
    }

    private Color readableTextOn(Color background) {
        return luminance(background) > 0.6f ? new Color(28, 32, 44) : new Color(242, 248, 255);
    }

    private Border roundedLineBorder(Color color, int thickness, int arc) {
        return BorderFactory.createLineBorder(color, thickness, true);
    }

    private void applyMacRoundedButton(JButton button, Color borderColor, int arc, Insets padding) {
        if (!useRoundedControls()) {
            return;
        }
        if (!(button.getUI() instanceof RoundedButtonUI)) {
            button.setUI(new RoundedButtonUI());
        }
        button.putClientProperty(ROUND_ARC_KEY, arc);
        button.putClientProperty(ROUND_BORDER_KEY, borderColor);
        button.putClientProperty(ROUND_FILL_KEY, null);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(
                padding.top, padding.left, padding.bottom, padding.right
        ));
    }

    private void updateMacRoundedButton(JButton button, Color fill, Color border, int arc) {
        if (!useRoundedControls() || button == null) {
            return;
        }
        if (!(button.getUI() instanceof RoundedButtonUI)) {
            button.setUI(new RoundedButtonUI());
        }
        button.putClientProperty(ROUND_ARC_KEY, arc);
        button.putClientProperty(ROUND_BORDER_KEY, border);
        button.putClientProperty(ROUND_FILL_KEY, fill);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.repaint();
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(brandAccent);
        button.setForeground(new Color(8, 14, 24));
        if (useRoundedControls()) {
            applyMacRoundedButton(button, tintColor(brandAccent, 0.35f), buttonBorderArc, new Insets(7, 16, 7, 16));
            button.setOpaque(false);
        } else {
            button.setOpaque(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    roundedLineBorder(tintColor(brandAccent, 0.35f), 1, buttonBorderArc),
                    BorderFactory.createEmptyBorder(7, 16, 7, 16)
            ));
        }
        button.setFocusPainted(false);
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(brandPanelAlt);
        button.setForeground(brandText);
        if (useRoundedControls()) {
            applyMacRoundedButton(button, brandBorder, buttonBorderArc, new Insets(7, 14, 7, 14));
            button.setOpaque(false);
        } else {
            button.setOpaque(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    roundedLineBorder(brandBorder, 1, buttonBorderArc),
                    BorderFactory.createEmptyBorder(7, 14, 7, 14)
            ));
        }
        button.setFocusPainted(false);
    }

    private void stylePlaybackButton(JButton button, boolean primary) {
        if (primary) {
            button.setBackground(brandAccent);
            button.setForeground(new Color(8, 14, 24));
            if (useRoundedControls()) {
                applyMacRoundedButton(button, tintColor(brandAccent, 0.25f), buttonBorderArc, new Insets(9, 18, 9, 18));
                button.setOpaque(false);
            } else {
                button.setOpaque(true);
                button.setBorder(BorderFactory.createCompoundBorder(
                        roundedLineBorder(tintColor(brandAccent, 0.25f), 1, buttonBorderArc),
                        BorderFactory.createEmptyBorder(9, 18, 9, 18)
                ));
            }
            button.setPreferredSize(new Dimension(130, 44));
        } else {
            button.setBackground(new Color(26, 34, 54));
            button.setForeground(brandText);
            if (useRoundedControls()) {
                applyMacRoundedButton(button, brandBorder, buttonBorderArc, new Insets(8, 16, 8, 16));
                button.setOpaque(false);
            } else {
                button.setOpaque(true);
                button.setBorder(BorderFactory.createCompoundBorder(
                        roundedLineBorder(brandBorder, 1, buttonBorderArc),
                        BorderFactory.createEmptyBorder(8, 16, 8, 16)
                ));
            }
            button.setPreferredSize(new Dimension(110, 40));
        }
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13.5f));
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
                refreshPlayerPanelToggleButtonAsync();
            });
        });
    }

    private void refreshPlayerSummaryAsync() {
        if (!isDesktopOnboardingEnabled()) {
            return;
        }
        worker.submit(() -> {
            Long guildId = selectedGuildId();
            List<String> queue = guildId == null ? List.of() : runtime.playerQueueForGuild(guildId);
            MusicController.NowPlayingSnapshot nowPlaying = guildId == null
                    ? new MusicController.NowPlayingSnapshot("none", "idle", 0L, 0L)
                    : runtime.nowPlayingSnapshotForGuild(guildId);
            int volume = guildId == null ? 100 : runtime.currentVolumeForGuild(guildId);
            int bass = guildId == null ? 0 : runtime.currentBassForGuild(guildId);
            boolean hasPanel = guildId != null && runtime.hasPlayerPanelForGuild(guildId);
            boolean playing = runtime.isRunning() && "playing".equalsIgnoreCase(nowPlaying.state());
            boolean pendingVisible = pendingNowPlayingTitle != null
                    && System.currentTimeMillis() < pendingNowPlayingUntilMillis
                    && "none".equalsIgnoreCase(nowPlaying.title());
            String nowPlayingTitleText = "none".equalsIgnoreCase(nowPlaying.title())
                    ? (pendingVisible ? pendingNowPlayingTitle : ui("nowPlayingPlaceholder"))
                    : nowPlaying.title();
            int queueCount = queue.size();
            String subtitle = queueCount == 0 && "none".equalsIgnoreCase(nowPlaying.title()) && !pendingVisible
                    ? ui("playerIdle")
                    : ui("queueCount") + ": " + (queueCount + ("none".equalsIgnoreCase(nowPlaying.title()) && !pendingVisible ? 0 : 1));
            String hint = queueCount > 0
                    ? ui("upNext") + ": " + queue.get(0)
                    : ui("playerHint");
            SwingUtilities.invokeLater(() -> {
                setPlayerPanelToggleButtonState(runtime.isRunning(), hasPanel);
                if (queueListModel != null) {
                    queueListModel.clear();
                    int index = 1;
                    if (!"none".equalsIgnoreCase(nowPlaying.title()) || pendingVisible) {
                        queueListModel.addElement("▶ " + nowPlayingTitleText);
                        index++;
                    }
                    for (int i = 0; i < queue.size(); i++) {
                        queueListModel.addElement(index++ + ". " + queue.get(i));
                    }
                }
                if (!"none".equalsIgnoreCase(nowPlaying.title())) {
                    pendingNowPlayingTitle = null;
                    pendingNowPlayingUntilMillis = 0L;
                }
                if (nowPlayingTitle != null) {
                    nowPlayingTitle.setText(nowPlayingTitleText);
                }
                if (nowPlayingSubtitle != null) {
                    nowPlayingSubtitle.setText(subtitle);
                }
                if (nowPlayingHint != null) {
                    nowPlayingHint.setText(hint);
                }
                if (progressTimeLabel != null) {
                    progressTimeLabel.setText(formatTime(nowPlaying.positionMs()) + " / " + formatTime(nowPlaying.durationMs()));
                }
                if (progressSlider != null && nowPlaying.durationMs() > 0) {
                    int percent = (int) Math.min(100, Math.max(0, (nowPlaying.positionMs() * 100) / nowPlaying.durationMs()));
                    progressSlider.setValue(percent);
                }
                if (volumeSlider != null && !volumeSlider.getValueIsAdjusting()) {
                    volumeSlider.setValue(volume);
                    updateVolumeLabel(volume);
                }
                if (bassSlider != null && !bassSlider.getValueIsAdjusting()) {
                    bassSlider.setValue(bass);
                    updateBassLabel(bass);
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
                    boolean hasSelection = query.equalsIgnoreCase(lastSearchQuery)
                            && searchResultsList != null
                            && searchResultsList.getSelectedIndex() >= 0
                            && searchResultsList.getSelectedIndex() < searchResultsOptions.size();
                    if (!hasSelection) {
                        List<BotRuntime.SearchTrackOptionRef> options = runtime.searchTracksFromDesktop(query, 10);
                        if (options.isEmpty()) {
                            SwingUtilities.invokeLater(() -> showError(ui("nothingFound") + ": " + query));
                            return;
                        }

                        lastSearchQuery = query;
                        searchResultsOptions = options;
                        SwingUtilities.invokeLater(() -> {
                            searchResultsModel.clear();
                            for (int i = 0; i < options.size(); i++) {
                                searchResultsModel.addElement((i + 1) + ". " + options.get(i).title());
                            }
                            if (!searchResultsModel.isEmpty()) {
                                searchResultsList.setSelectedIndex(0);
                            }
                        });
                        return;
                    }

                    int selected = searchResultsList.getSelectedIndex();
                    enqueueQuery = searchResultsOptions.get(selected).uri();
                }

                if (playNext) {
                    runtime.addSongNextFromDesktop(guildId, channelId, enqueueQuery);
                    log(ui("desktopQueuedNext") + ": " + query);
                } else {
                    runtime.addSongFromDesktop(guildId, channelId, enqueueQuery);
                    log(ui("desktopQueued") + ": " + query);
                }

                pendingNowPlayingTitle = query;
                pendingNowPlayingUntilMillis = System.currentTimeMillis() + 15000L;
                SwingUtilities.invokeLater(() -> addSongField.setText(""));
                refreshPlayerSummaryAsync();
                CompletableFuture.delayedExecutor(600, TimeUnit.MILLISECONDS).execute(this::refreshPlayerSummaryAsync);
                CompletableFuture.delayedExecutor(1800, TimeUnit.MILLISECONDS).execute(this::refreshPlayerSummaryAsync);
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
        String themeCode = props.getProperty("app.theme", controlPanelThemeCode).trim();
        String cornerStyleCode = props.getProperty("app.cornerStyle", controlPanelCornerStyleCode).trim();
        selectLanguage(languageCode);
        selectControlPanelLanguage(controlLanguageCode);
        selectTheme(themeCode);
        selectCornerStyle(cornerStyleCode);
    }

    private void saveConfigFromFields() throws IOException {
        Path configPath = resolveConfigPath();
        String token = new String(tokenField.getPassword()).trim();
        String prefix = prefixField.getText().trim();
        LanguageOption selected = (LanguageOption) languageCombo.getSelectedItem();
        String language = selected == null ? "en" : selected.code();
        LanguageOption selectedControl = (LanguageOption) controlPanelLanguageCombo.getSelectedItem();
        String controlLanguage = selectedControl == null ? "en" : selectedControl.code();
        ThemeOption selectedTheme = themeCombo == null ? null : (ThemeOption) themeCombo.getSelectedItem();
        String theme = selectedTheme == null ? controlPanelThemeCode : selectedTheme.code();
        CornerStyleOption selectedCorner = cornerStyleCombo == null ? null : (CornerStyleOption) cornerStyleCombo.getSelectedItem();
        String cornerStyle = selectedCorner == null ? controlPanelCornerStyleCode : selectedCorner.code();

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
        props.setProperty("app.theme", theme);
        props.setProperty("app.cornerStyle", cornerStyle);
        props.putIfAbsent("youtube.poToken", "");
        props.putIfAbsent("youtube.visitorData", "");

        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "ModernMusicBot settings");
        }

        log(ui("configSavedTo") + ": " + configPath.toAbsolutePath());
        if (!controlLanguage.equalsIgnoreCase(controlPanelLanguageCode)
                || !theme.equalsIgnoreCase(controlPanelThemeCode)
                || !cornerStyle.equalsIgnoreCase(controlPanelCornerStyleCode)) {
            SwingUtilities.invokeLater(this::reloadUiAfterSettingsChange);
        }
    }

    private void reloadUiAfterSettingsChange() {
        rebuildUiInPlace();
    }

    private void rebuildUiInPlace() {
        if (frame == null) {
            show();
            return;
        }
        Rectangle bounds = frame.getBounds();
        if (desktopRefreshTimer != null) {
            desktopRefreshTimer.stop();
        }
        if (waveformRefreshTimer != null) {
            waveformRefreshTimer.stop();
        }
        if (splashTimer != null) {
            splashTimer.stop();
        }
        loadControlPanelLanguagePreference();
        loadControlPanelThemePreference();
        loadControlPanelCornerStylePreference();
        setupLookAndFeel();
        applyBrandTheme();

        rootPanel = new GradientPanel(brandBackground, brandBackgroundAlt);
        rootPanel.setLayout(new BorderLayout(18, 18));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        frame.setContentPane(rootPanel);

        if (isDesktopOnboardingEnabled()) {
            frame.setJMenuBar(buildMenuBar());
        } else {
            frame.setJMenuBar(null);
        }

        initDesktopComponents();

        JPanel sidebar = buildSidebar();
        sidebarWrap = new AnimatedPanel(sidebar);
        JPanel mainArea = buildMainArea();
        rootPanel.add(sidebarWrap, BorderLayout.WEST);
        rootPanel.add(mainArea, BorderLayout.CENTER);
        setStartStopEnabled(false);

        if (isDesktopOnboardingEnabled()) {
            frame.setMinimumSize(new Dimension(1220, 820));
        } else {
            frame.setMinimumSize(new Dimension(1040, 740));
        }

        loadConfigIntoFields();
        wireActions();
        wireKeyboardShortcuts();
        frame.setBounds(bounds);
        prepareIntroLayout();
        frame.revalidate();
        frame.repaint();
        startStartupAnimation();
        startWaveformRefresh();
        setConsoleVisible(true);
        log(ui("controlPanelReady"));

        if (isDesktopOnboardingEnabled()) {
            desktopRefreshTimer = new Timer(3000, ignored -> refreshPlayerSummaryAsync());
            desktopRefreshTimer.start();
            refreshDesktopSelectorsAsync();
        }

        if (!windowListenerInstalled) {
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    if (desktopRefreshTimer != null) {
                        desktopRefreshTimer.stop();
                    }
                    if (waveformRefreshTimer != null) {
                        waveformRefreshTimer.stop();
                    }
                    realtimeWorker.shutdownNow();
                    worker.shutdownNow();
                    runtime.stop(ControlPanelApp.this::log);
                }
            });
            windowListenerInstalled = true;
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

    private void updateVolumeLabel(int value) {
        if (volumeValueLabel != null) {
            volumeValueLabel.setText(ui("volume") + ": " + value + "%");
            volumeValueLabel.setForeground(subtleText);
        }
    }

    private void updateBassLabel(int value) {
        if (bassValueLabel != null) {
            bassValueLabel.setText(ui("bass") + ": " + value);
            bassValueLabel.setForeground(subtleText);
        }
    }

    private String formatTime(long ms) {
        if (ms <= 0) {
            return "0:00";
        }
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    private void enqueueSelectedSearchResult(boolean playNext) {
        worker.submit(() -> {
            Long guildId = selectedGuildId();
            Long channelId = selectedChannelId();
            if (guildId == null || channelId == null) {
                SwingUtilities.invokeLater(() -> showError(ui("selectGuildTextChannelFirst")));
                return;
            }

            int index = searchResultsList == null ? -1 : searchResultsList.getSelectedIndex();
            if (index < 0 || index >= searchResultsOptions.size()) {
                SwingUtilities.invokeLater(() -> showError(ui("selectSearchResult")));
                return;
            }

            String query = searchResultsOptions.get(index).uri();
            String title = searchResultsOptions.get(index).title();
            try {
                if (playNext) {
                    runtime.addSongNextFromDesktop(guildId, channelId, query);
                } else {
                    runtime.addSongFromDesktop(guildId, channelId, query);
                }
                pendingNowPlayingTitle = title;
                pendingNowPlayingUntilMillis = System.currentTimeMillis() + 15000L;
                refreshPlayerSummaryAsync();
                CompletableFuture.delayedExecutor(600, TimeUnit.MILLISECONDS).execute(this::refreshPlayerSummaryAsync);
                CompletableFuture.delayedExecutor(1800, TimeUnit.MILLISECONDS).execute(this::refreshPlayerSummaryAsync);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> showError(ex.getMessage()));
            }
        });
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

    private void loadControlPanelThemePreference() {
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            controlPanelThemeCode = "neon";
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
            controlPanelThemeCode = props.getProperty("app.theme", "neon").trim().toLowerCase();
        } catch (Exception ignored) {
            controlPanelThemeCode = "neon";
        }
    }

    private void loadControlPanelCornerStylePreference() {
        Path configPath = resolveConfigPath();
        controlPanelCornerStyleCode = macUi ? "rounded" : "square";
        if (!Files.exists(configPath)) {
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
            String style = props
                    .getProperty("app.cornerStyle", controlPanelCornerStyleCode)
                    .trim()
                    .toLowerCase();
            if (!"rounded".equals(style) && !"square".equals(style)) {
                style = macUi ? "rounded" : "square";
            }
            controlPanelCornerStyleCode = style;
        } catch (Exception ignored) {
            controlPanelCornerStyleCode = macUi ? "rounded" : "square";
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
            case "appTitleShort" -> "ModernMusicBot";
            case "controlPanel" -> "Control Panel";
            case "navHome" -> "Home";
            case "navPlayer" -> "Player";
            case "navQueue" -> "Queue";
            case "navSearch" -> "Search";
            case "navSettings" -> "Settings";
            case "navConsole" -> "Console";
            case "sidebarHint" -> "Neon host dashboard";
            case "statusOnline" -> "Online";
            case "statusOffline" -> "Offline";
            case "booting" -> "Booting";
            case "bootSubtitle" -> "Discord music services initializing";
            case "loadingModules" -> "Loading modules";
            case "loadingDetail" -> "Audio - Queue - Discord API";
            case "bootPrep" -> "Preparing player session and restoring dashboard...";
            case "playerIdle" -> "Waiting for a track";
            case "playerHint" -> "Add a song or URL to start playback";
            case "nowPlayingPlaceholder" -> "Nothing playing yet";
            case "queueCount" -> "Queue";
            case "upNext" -> "Up next";
            case "homeTitle" -> "Ready to play";
            case "homeSubtitle" -> "Connect your guild and drop in a track";
            case "homeHint" -> "Start the bot, then head to Player for controls";
            case "settingsHint" -> "Theme and setup options are in the center panel.";
            case "consoleHint" -> "Use the console view to watch logs in real time.";
            case "botControl" -> "Bot Control";
            case "quickActions" -> "Quick Actions";
            case "songPlaceholder" -> "Paste URL or search for a track";
            case "play" -> "Play";
            case "overdrive" -> "Overdrive";
            case "settings" -> "Settings";
            case "botToken" -> "Bot Token";
            case "prefix" -> "Prefix";
            case "botLanguage" -> "Bot Language";
            case "controlPanelLanguage" -> "Control Panel Language";
            case "theme" -> "Theme";
            case "cornerStyle" -> "Corner style";
            case "start" -> "Start";
            case "stop" -> "Stop";
            case "saveSettings" -> "Save Settings";
            case "showConsole" -> "Show Console";
            case "hideConsole" -> "Hide Console";
            case "console" -> "Console";
            case "desktopPlayer" -> "Host Player (Desktop)";
            case "addSong" -> "Add Song";
            case "playNext" -> "Play Next";
            case "pause" -> "Pause";
            case "resume" -> "Resume";
            case "skip" -> "Skip";
            case "volume" -> "Volume";
            case "bass" -> "Bass";
            case "reset" -> "Reset";
            case "searchResultsPanel" -> "Search Results (double-click to add)";
            case "selectSearchResult" -> "Select a search result first.";
            case "refreshLists" -> "Refresh Lists";
            case "removePlayer" -> "Remove Player in Discord";
            case "showPlayer" -> "Show Player in Discord";
            case "botNotRunning" -> "Bot is not running.";
            case "guild" -> "Guild";
            case "textChannel" -> "Text channel";
            case "songUrl" -> "Song / URL";
            case "restartRequired" -> "Restart app to apply control panel changes.";
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
            case "playerPanelPosted" -> "Posted player panel to Discord.";
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
            case "appTitleShort" -> "ModernMusicBot";
            case "controlPanel" -> "Панель управления";
            case "navHome" -> "Главная";
            case "navPlayer" -> "Плеер";
            case "navQueue" -> "Очередь";
            case "navSearch" -> "Поиск";
            case "navSettings" -> "Настройки";
            case "navConsole" -> "Консоль";
            case "sidebarHint" -> "Неоновая панель";
            case "statusOnline" -> "Онлайн";
            case "statusOffline" -> "Оффлайн";
            case "booting" -> "Загрузка";
            case "bootSubtitle" -> "Инициализация музыкальных сервисов Discord";
            case "loadingModules" -> "Загрузка модулей";
            case "loadingDetail" -> "Аудио - Очередь - Discord API";
            case "bootPrep" -> "Подготовка плеера и восстановление панели...";
            case "playerIdle" -> "Ожидание трека";
            case "playerHint" -> "Добавьте песню или URL для старта";
            case "nowPlayingPlaceholder" -> "Пока ничего не играет";
            case "queueCount" -> "Очередь";
            case "upNext" -> "Далее";
            case "homeTitle" -> "Готово к запуску";
            case "homeSubtitle" -> "Подключите сервер и добавьте трек";
            case "homeHint" -> "Запустите бота, затем откройте Плеер";
            case "settingsHint" -> "Тема и настройки — в центральной панели.";
            case "consoleHint" -> "Откройте консоль для логов.";
            case "botControl" -> "Управление ботом";
            case "quickActions" -> "Быстрые действия";
            case "songPlaceholder" -> "Вставьте URL или введите запрос";
            case "play" -> "Играть";
            case "overdrive" -> "Перегруз";
            case "settings" -> "Настройки";
            case "botToken" -> "Токен бота";
            case "prefix" -> "Префикс";
            case "botLanguage" -> "Язык бота";
            case "controlPanelLanguage" -> "Язык панели управления";
            case "theme" -> "Тема";
            case "cornerStyle" -> "Стиль углов";
            case "start" -> "Старт";
            case "stop" -> "Стоп";
            case "saveSettings" -> "Сохранить настройки";
            case "showConsole" -> "Показать консоль";
            case "hideConsole" -> "Скрыть консоль";
            case "console" -> "Консоль";
            case "desktopPlayer" -> "Плеер хоста (Desktop)";
            case "addSong" -> "Добавить песню";
            case "playNext" -> "Играть следующей";
            case "pause" -> "Пауза";
            case "resume" -> "Продолжить";
            case "skip" -> "Пропуск";
            case "volume" -> "Громкость";
            case "bass" -> "Бас";
            case "reset" -> "Сброс";
            case "searchResultsPanel" -> "Результаты поиска (двойной клик — добавить)";
            case "selectSearchResult" -> "Сначала выберите результат поиска.";
            case "refreshLists" -> "Обновить списки";
            case "removePlayer" -> "Убрать плеер из Discord";
            case "showPlayer" -> "Показать плеер в Discord";
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
            case "restartRequired" -> "Перезапустите приложение, чтобы применить изменения панели.";
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
            case "restartRequired" -> byLang(lang, "Restart app to apply control panel changes.", "Վահանակի փոփոխությունները կիրառելու համար վերագործարկեք հավելվածը։", "პანელის ცვლილებების გამოსაყენებლად გადატვირთეთ აპი.", "Panel dəyişikliklərini tətbiq etmək üçün tətbiqi yenidən başladın.", "Панель өзгерістерін қолдану үшін қолданбаны қайта іске қосыңыз.", "Panel o‘zgarishlarini qo‘llash uchun ilovani qayta ishga tushiring.", "Перезапустіть застосунок, щоб застосувати зміни панелі.", "App neu starten, um die Änderungen am Kontrollpanel anzuwenden.", "Reinicia la app para aplicar los cambios del panel.", "Riavvia l'app per applicare le modifiche del pannello.", "Reinicie o app para aplicar mudanças do painel.", "请重启应用以应用控制面板更改。", "コントロールパネルの変更を適用するにはアプリを再起動してください。");
            case "welcome" -> byLang(lang, "Welcome to ModernMusicBot.", "Բարի գալուստ ModernMusicBot։", "კეთილი იყოს თქვენი მობრძანება ModernMusicBot-ში.", "ModernMusicBot-a xoş gəlmisiniz.", "ModernMusicBot-қа қош келдіңіз.", "ModernMusicBot-ga xush kelibsiz.", "Ласкаво просимо до ModernMusicBot.", "Willkommen bei ModernMusicBot.", "Bienvenido a ModernMusicBot.", "Benvenuto in ModernMusicBot.", "Bem-vindo ao ModernMusicBot.", "欢迎使用 ModernMusicBot。", "ModernMusicBotへようこそ。");
            case "beforeFirstRun" -> byLang(lang, "Before first run, make sure:", "Առաջին մեկնարկից առաջ համոզվեք, որ՝", "პირველ გაშვებამდე დარწმუნდით:", "İlk işə salmadan əvvəl yoxlayın:", "Алғашқы іске қоспас бұрын тексеріңіз:", "Birinchi ishga tushirishdan oldin tekshiring:", "Перед першим запуском переконайтесь:", "Vor dem ersten Start prüfen:", "Antes del primer inicio, asegúrate de:", "Prima del primo avvio, assicurati che:", "Antes da primeira execução, verifique:", "首次启动前请确认：", "初回起動前に次を確認してください：");
            case "checkToken" -> byLang(lang, "You have a Discord bot token (Discord Developer Portal).", "Ունեք Discord bot token (Discord Developer Portal-ում)։", "გაქვთ Discord ბოტის ტოკენი (Discord Developer Portal).", "Discord bot tokeniniz var (Discord Developer Portal).", "Сізде Discord bot токені бар (Discord Developer Portal).", "Sizda Discord bot tokeni bor (Discord Developer Portal).", "У вас є токен Discord-бота (Discord Developer Portal).", "Discord-Bot-Token vorhanden (Discord Developer Portal).", "Tienes un token de bot de Discord (Discord Developer Portal).", "Hai un token bot Discord (Discord Developer Portal).", "Você tem um token de bot do Discord (Discord Developer Portal).", "你已拥有 Discord 机器人令牌（Developer Portal）。", "Discordボットトークン（Developer Portal）を用意してください。" );
            case "checkInvite" -> byLang(lang, "The bot is invited with required permissions (voice + messages).", "Բոտը հրավիրված է անհրաժեշտ իրավունքներով (voice + messages)։", "ბოტი მოწვეულია საჭირო ნებართვებით (voice + messages).", "Bot tələb olunan icazələrlə dəvət olunub (voice + messages).", "Бот қажетті рұқсаттармен шақырылған (voice + messages).", "Bot kerakli ruxsatlar bilan taklif qilingan (voice + messages).", "Бота запрошено з потрібними правами (voice + messages).", "Bot mit nötigen Rechten eingeladen (voice + messages).", "El bot está invitado con permisos requeridos (voz + mensajes).", "Il bot è invitato con i permessi richiesti (voce + messaggi).", "O bot foi convidado com permissões necessárias (voz + mensagens).", "机器人已使用所需权限邀请（语音+消息）。", "ボットを必要権限（音声+メッセージ）で招待済み。" );
            case "checkInternet" -> byLang(lang, "Internet access is available.", "Ինտերնետ կապը հասանելի է։", "ინტერნეტ წვდომა ხელმისაწვდომია.", "İnternet bağlantısı mövcuddur.", "Интернет қолжетімді.", "Internet mavjud.", "Є доступ до інтернету.", "Internetzugang vorhanden.", "Hay acceso a internet.", "Accesso a internet disponibile.", "Acesso à internet disponível.", "网络连接可用。", "インターネット接続が利用可能。" );
            case "checkWrite" -> byLang(lang, "This app can write files in its folder (config + database).", "Հավելվածը կարող է գրել ֆայլեր իր պանակում (config + database)։", "აპს შეუძლია ფაილების ჩაწერა საკუთარ საქაღალდეში (config + database).", "Tətbiq öz qovluğuna fayl yaza bilir (config + database).", "Қолданба өз қалтасына файл жаза алады (config + database).", "Ilova o‘z papkasiga yozishi mumkin (config + database).", "Застосунок може записувати файли у свою папку (config + database).", "App kann Dateien im eigenen Ordner schreiben (config + database).", "La app puede escribir archivos en su carpeta (config + database).", "L'app può scrivere file nella sua cartella (config + database).", "O app pode gravar arquivos na própria pasta (config + database).", "应用可在其目录写入文件（配置+数据库）。", "アプリが自身のフォルダーに書き込めること（config + database）。" );
            case "checkYoutube" -> byLang(lang, "Optional for better YouTube reliability: set youtube.poToken and youtube.visitorData.", "YouTube կայունության համար՝ ցանկության դեպքում լրացրեք youtube.poToken և youtube.visitorData։", "YouTube-ის სტაბილურობისთვის სურვილისამებრ შეავსეთ youtube.poToken და youtube.visitorData.", "YouTube sabitliyi üçün opsional: youtube.poToken və youtube.visitorData təyin edin.", "YouTube тұрақтылығы үшін қалауыңызша youtube.poToken және youtube.visitorData орнатыңыз.", "YouTube barqarorligi uchun ixtiyoriy: youtube.poToken va youtube.visitorData ni kiriting.", "Для стабільнішого YouTube за потреби задайте youtube.poToken і youtube.visitorData.", "Optional für bessere YouTube-Stabilität: youtube.poToken und youtube.visitorData setzen.", "Opcional para mejor estabilidad de YouTube: configurar youtube.poToken y youtube.visitorData.", "Opzionale per maggiore affidabilità YouTube: imposta youtube.poToken e youtube.visitorData.", "Opcional para melhor estabilidade do YouTube: defina youtube.poToken e youtube.visitorData.", "可选：为提高 YouTube 稳定性，设置 youtube.poToken 和 youtube.visitorData。", "YouTube安定性向上のため、必要に応じて youtube.poToken と youtube.visitorData を設定。" );
            case "theme" -> byLang(lang, "Theme", "Թեմա", "თემა", "Mövzu", "Тақырып", "Mavzu", "Тема", "Thema", "Tema", "Tema", "Tema", "主题", "テーマ");
            case "cornerStyle" -> byLang(lang, "Corner style", "Անկյունների ոճը", "კუთხეების სტილი", "Künc stili", "Бұрыш стилі", "Burchak uslubi", "Стиль кутів", "Eckenstil", "Estilo de esquinas", "Stile angoli", "Estilo de cantos", "角样式", "角のスタイル");
            case "settingsHint" -> byLang(lang, "Theme and setup options are in the center panel.", "Թեման և կարգավորումները գտնվում են կենտրոնական վահանակում։", "თემა და პარამეტრები ცენტრალურ პანელშია.", "Tema və ayarlar mərkəzi paneldədir.", "Тақырып пен баптаулар орталық панельде.", "Mavzu va sozlamalar markaziy panelda.", "Тема та налаштування — в центральній панелі.", "Thema und Einstellungen befinden sich im mittleren Bereich.", "Tema y ajustes están en el panel central.", "Tema e impostazioni sono nel pannello centrale.", "Tema e configurações ficam no painel central.", "主题和设置在中间面板。", "テーマと設定は中央パネルにあります。");
            case "consoleHint" -> byLang(lang, "Use the console view to watch logs in real time.", "Օգտագործեք կոնսոլը՝ լոգերը տեսնելու համար։", "გამოიყენეთ კონსოლი ლოგების实时 դիտմանთვის.", "Konsoldan istifadə edib jurnalları real vaxtda izləyin.", "Консоль арқылы журналдарды нақты уақытта қараңыз.", "Konsoldan foydalанып, loglarni real vaqtda kuzating.", "Використовуйте консоль для перегляду логів у реальному часі.", "Konsole öffnen, um Logs in Echtzeit zu sehen.", "Usa la consola para ver logs en tiempo real.", "Usa la console per vedere i log in tempo reale.", "Use o console para ver logs em tempo real.", "使用控制台实时查看日志。", "コンソールでログをリアルタイムに確認できます。");
            case "javaIncluded" -> byLang(lang, "Desktop installers already include Java runtime.", "Desktop տեղադրիչներն արդեն ներառում են Java runtime։", "Desktop ინსტალატორებში Java runtime უკვე შედის.", "Desktop quraşdırıcılarına Java runtime daxildir.", "Desktop орнатқыштарында Java runtime бар.", "Desktop o‘rnatgichlarda Java runtime bor.", "Desktop-інсталятори вже містять Java runtime.", "Desktop-Installer enthalten bereits Java Runtime.", "Los instaladores desktop ya incluyen Java runtime.", "Gli installer desktop includono già Java runtime.", "Os instaladores desktop já incluem Java runtime.", "桌面安装包已包含 Java 运行时。", "デスクトップインストーラーにはJavaランタイムが含まれています。" );
            case "desktopSearchCancelled" -> byLang(lang, "Desktop search cancelled", "Desktop որոնումը չեղարկվեց", "Desktop ძიება გაუქმდა", "Desktop axtarışı ləğv edildi", "Desktop іздеу тоқтатылды", "Desktop qidiruvi bekor qilindi", "Desktop пошук скасовано", "Desktop-Suche abgebrochen", "Búsqueda desktop cancelada", "Ricerca desktop annullata", "Pesquisa desktop cancelada", "桌面搜索已取消", "デスクトップ検索をキャンセルしました");
            case "desktopQueued" -> byLang(lang, "Desktop queued", "Desktop-ից հերթագրվեց", "Desktop-დან დაემატა რიგში", "Desktop-dan növbəyə əlavə edildi", "Desktop-тен кезекке қосылды", "Desktop'dan navbatga qo‘shildi", "Додано в чергу з Desktop", "Von Desktop in Warteschlange", "Agregado a cola desde desktop", "Aggiunto in coda da desktop", "Adicionado à fila pelo desktop", "已从桌面加入队列", "デスクトップからキューに追加");
            case "queueList" -> byLang(lang, "Queue (select item to remove)", "Հերթ (ընտրեք հեռացնելու համար)", "რიგი (ასარჩევად წასაშლელად)", "Növbə (silmək üçün seçin)", "Кезек (өшіру үшін таңдаңыз)", "Navbat (o‘chirish uchun tanlang)", "Черга (виберіть елемент для видалення)", "Warteschlange (Element zum Entfernen wählen)", "Cola (selecciona elemento para eliminar)", "Coda (seleziona elemento da rimuovere)", "Fila (selecione item para remover)", "队列（选择要移除的项目）", "キュー（削除する項目を選択）");
            case "cleanupScope" -> byLang(lang, "Cleanup", "Մաքրում", "გასუფთავება", "Təmizləmə", "Тазалау", "Tozalash", "Очищення", "Bereinigung", "Limpieza", "Pulizia", "Limpeza", "清理", "クリーンアップ");
            case "cleanupScopeAll" -> byLang(lang, "All", "Բոլորը", "ყველა", "Hamısı", "Барлығы", "Hammasi", "Усе", "Alle", "Todo", "Tutto", "Tudo", "全部", "すべて");
            case "cleanupScopeBotOnly" -> byLang(lang, "Bot Only", "Միայն բոտ", "მხოლოდ ბოტი", "Yalnız bot", "Тек бот", "Faqat bot", "Лише бот", "Nur Bot", "Solo bot", "Solo bot", "Apenas bot", "仅机器人", "ボットのみ");
            case "removePlayer" -> byLang(lang, "Remove Player in Discord", "Հեռացնել պլեյերը Discord-ում", "Discord-ში პლეერის წაშლა", "Discord-da pleyeri sil", "Discord-та плеерді жою", "Discord'da pleyerni olib tashlash", "Видалити плеєр у Discord", "Player in Discord entfernen", "Quitar reproductor en Discord", "Rimuovi player su Discord", "Remover player no Discord", "在 Discord 中移除播放器", "Discordでプレーヤーを削除");
            case "showPlayer" -> byLang(lang, "Show Player in Discord", "Ցույց տալ պլեյերը Discord-ում", "Discord-ში პლეერის ჩვენება", "Discord-da pleyeri göstər", "Discord-та плеерді көрсету", "Discord'da pleyerni ko‘rsatish", "Показати плеєр у Discord", "Player in Discord anzeigen", "Mostrar reproductor en Discord", "Mostra player su Discord", "Mostrar player no Discord", "在 Discord 中显示播放器", "Discordでプレーヤーを表示");
            case "playerPanelPosted" -> byLang(lang, "Posted player panel to Discord.", "Պլեյերի վահանակը ուղարկվեց Discord։", "პლეერის პანელი გაიგზავნა Discord-ში.", "Pleyer paneli Discord-a göndərildi.", "Плеер панелі Discord-қа жіберілді.", "Player panel Discord'ga yuborildi.", "Панель плеєра відправлено в Discord.", "Player-Panel in Discord gepostet.", "Panel del reproductor enviado a Discord.", "Pannello player inviato su Discord.", "Painel do player enviado ao Discord.", "播放器面板已发送到 Discord。", "プレーヤーパネルをDiscordに送信しました。");
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
        setEarRapeEnabled(earRapeEnabled);
    }

    private void selectTheme(String themeCode) {
        if (themeCombo == null) {
            return;
        }
        ThemeOption option = Arrays.stream(THEME_OPTIONS)
                .filter(item -> item.code().equalsIgnoreCase(themeCode))
                .findFirst()
                .orElse(THEME_OPTIONS[0]);
        themeCombo.setSelectedItem(option);
    }

    private void selectCornerStyle(String cornerStyleCode) {
        if (cornerStyleCombo == null) {
            return;
        }
        CornerStyleOption option = Arrays.stream(CORNER_STYLE_OPTIONS)
                .filter(item -> item.code().equalsIgnoreCase(cornerStyleCode))
                .findFirst()
                .orElse(CORNER_STYLE_OPTIONS[0]);
        cornerStyleCombo.setSelectedItem(option);
    }

    private boolean isSearchQuery(String query) {
        String value = query == null ? "" : query.trim().toLowerCase();
        if (value.isEmpty()) {
            return false;
        }
        return !(value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("www.")
                || value.startsWith("ytsearch:")
                || value.startsWith("ytmsearch:")
                || value.startsWith("scsearch:"));
    }

    private int pickSearchResultIndex(String query, List<BotRuntime.SearchTrackOptionRef> options) {
        try {
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
                return -1;
            }

            for (int i = 0; i < items.length; i++) {
                if (items[i].equals(choice)) {
                    return i;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ui("couldNotShowSearchChoices") + ": " + ex.getMessage(), ex);
        }

        return -1;
    }

    private class NeonListCellRenderer extends JLabel implements javax.swing.ListCellRenderer<String> {
        private NeonListCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setText(value == null ? "" : value);
            if (isSelected) {
                Color base = brandPanelAlt;
                Color selected = mixColor(base, brandAccent, isLightThemePalette() ? 0.25f : 0.32f);
                setBackground(selected);
                setForeground(readableTextOn(selected));
            } else {
                setBackground(brandPanelAlt);
                setForeground(brandText);
            }
            return this;
        }
    }

    private static final class GlassPanel extends JPanel {
        private final Color fill;
        private final Color border;
        private final int arc;

        private GlassPanel(Color fill, Color border, int arc) {
            this.fill = fill;
            this.border = border;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, width, height, arc, arc);
            if (border != null) {
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
            }
            g2.dispose();
        }
    }

    private static final class GradientPanel extends JPanel {
        private final Color start;
        private final Color end;

        private GradientPanel(Color start, Color end) {
            this.start = start;
            this.end = end;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(0, 0, start, getWidth(), getHeight(), end);
            g2.setPaint(paint);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private interface RoundedField {
        void setRoundStyle(Color fill, Color border, int arc);
    }

    private static final class RoundedLabel extends JLabel {
        private Color fill;
        private Color border;
        private int arc = 18;

        private void setRoundStyle(Color fill, Color border, int arc) {
            this.fill = fill;
            this.border = border;
            this.arc = arc;
            setOpaque(false);
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            if (fill != null) {
                int width = getWidth();
                int height = getHeight();
                if (width > 0 && height > 0) {
                    int drawArc = arc >= 999 ? Math.min(width, height) : arc;
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                    RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0.5f, 0.5f,
                            width - 1f, height - 1f, drawArc, drawArc);
                    g2.setColor(fill);
                    g2.fill(shape);
                    if (border != null) {
                        g2.setColor(border);
                        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.draw(shape);
                    }
                    g2.dispose();
                }
            }
            super.paintComponent(g);
        }
    }

    private static final class RoundedTextField extends JTextField implements RoundedField {
        private Color fill;
        private Color border;
        private int arc = 18;

        private RoundedTextField() {
            super();
            setOpaque(false);
        }

        private RoundedTextField(String text) {
            super(text);
            setOpaque(false);
        }

        private RoundedTextField(int columns) {
            super(columns);
            setOpaque(false);
        }

        @Override
        public void setRoundStyle(Color fill, Color border, int arc) {
            this.fill = fill;
            this.border = border;
            this.arc = arc;
            setOpaque(false);
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            paintRoundedFieldBackground(g, this, fill, border, arc);
            super.paintComponent(g);
        }
    }

    private static final class RoundedPasswordField extends JPasswordField implements RoundedField {
        private Color fill;
        private Color border;
        private int arc = 18;

        private RoundedPasswordField() {
            super();
            setOpaque(false);
        }

        @Override
        public void setRoundStyle(Color fill, Color border, int arc) {
            this.fill = fill;
            this.border = border;
            this.arc = arc;
            setOpaque(false);
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            paintRoundedFieldBackground(g, this, fill, border, arc);
            super.paintComponent(g);
        }
    }

    private static final class RoundedComboBox<E> extends JComboBox<E> implements RoundedField {
        private Color fill;
        private Color border;
        private int arc = 18;

        private RoundedComboBox() {
            super();
            setOpaque(false);
        }

        private RoundedComboBox(E[] items) {
            super(items);
            setOpaque(false);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 6));
            setUI(new RoundedComboBoxUI());
            setRenderer(new RoundedComboRenderer());
        }

        @Override
        public void setRoundStyle(Color fill, Color border, int arc) {
            this.fill = fill;
            this.border = border;
            this.arc = arc;
            setOpaque(false);
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            paintRoundedFieldBackground(g, this, fill, border, arc);
            super.paintComponent(g);
        }
    }

    private static final class RoundedComboRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (index < 0) {
                label.setOpaque(false);
            } else {
                label.setOpaque(true);
            }
            label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            return label;
        }
    }

    private static final class RoundedComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            BasicArrowButton button = new RoundedArrowButton();
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusable(false);
            return button;
        }

        @Override
        public void paintCurrentValueBackground(java.awt.Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
            // Background is painted by RoundedComboBox.
        }
    }

    private static final class RoundedArrowButton extends BasicArrowButton {
        private RoundedArrowButton() {
            super(javax.swing.SwingConstants.SOUTH,
                    new Color(0, 0, 0, 0),
                    new Color(0, 0, 0, 0),
                    new Color(212, 224, 245),
                    new Color(0, 0, 0, 0));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
        }

        @Override
        public void paint(java.awt.Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) / 3;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            paintTriangle(g2, x, y, size, javax.swing.SwingConstants.SOUTH, isEnabled());
            g2.dispose();
        }
    }

    private static void paintRoundedFieldBackground(java.awt.Graphics g, JComponent component, Color fill, Color border, int arc) {
        if (fill == null) {
            fill = component.getBackground();
        }
        if (border == null) {
            border = new Color(0, 0, 0, 0);
        }
        if (!component.isEnabled()) {
            fill = applyAlpha(fill, 120);
            border = applyAlpha(border, 120);
        }
        int width = component.getWidth();
        int height = component.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        int drawArc = arc >= 999 ? Math.min(width, height) : arc;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0.5f, 0.5f,
                width - 1f, height - 1f, drawArc, drawArc);
        g2.setColor(fill);
        g2.fill(shape);
        if (border.getAlpha() > 0) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(shape);
        }
        g2.dispose();
    }

    private static Color applyAlpha(Color color, int alpha) {
        if (color == null) {
            return null;
        }
        int clamped = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamped);
    }

    private static final class RoundedButtonUI extends BasicButtonUI {
        @Override
        public void paint(java.awt.Graphics g, JComponent c) {
            if (c instanceof AbstractButton button) {
                paintRoundedBackground(g, button);
            }
            super.paint(g, c);
        }

        private void paintRoundedBackground(java.awt.Graphics g, AbstractButton button) {
            Object arcValue = button.getClientProperty(ROUND_ARC_KEY);
            int arc = arcValue instanceof Integer ? (Integer) arcValue : 18;
            if (arc >= 999) {
                arc = Math.min(button.getWidth(), button.getHeight());
            }
            Color fill = (Color) button.getClientProperty(ROUND_FILL_KEY);
            if (fill == null) {
                fill = button.getBackground();
            }
            Color border = (Color) button.getClientProperty(ROUND_BORDER_KEY);

            ButtonModel model = button.getModel();
            if (!button.isEnabled()) {
                fill = applyAlpha(fill, 120);
                border = applyAlpha(border, 120);
            } else if (model.isPressed()) {
                fill = darken(fill, 0.08f);
            } else if (model.isRollover()) {
                fill = lighten(fill, 0.04f);
            }

            if (fill == null) {
                return;
            }

            int width = button.getWidth();
            int height = button.getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0.5f, 0.5f,
                    width - 1f, height - 1f, arc, arc);
            g2.setColor(fill);
            g2.fill(shape);
            if (border != null && border.getAlpha() > 0) {
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(shape);
            }
            g2.dispose();
        }

        private Color darken(Color color, float amount) {
            if (color == null) {
                return null;
            }
            float clamped = Math.max(0f, Math.min(1f, amount));
            int r = Math.round(color.getRed() * (1f - clamped));
            int g = Math.round(color.getGreen() * (1f - clamped));
            int b = Math.round(color.getBlue() * (1f - clamped));
            return new Color(r, g, b, color.getAlpha());
        }

        private Color lighten(Color color, float amount) {
            if (color == null) {
                return null;
            }
            float clamped = Math.max(0f, Math.min(1f, amount));
            int r = Math.round(color.getRed() + (255 - color.getRed()) * clamped);
            int g = Math.round(color.getGreen() + (255 - color.getGreen()) * clamped);
            int b = Math.round(color.getBlue() + (255 - color.getBlue()) * clamped);
            return new Color(r, g, b, color.getAlpha());
        }

        private Color applyAlpha(Color color, int alpha) {
            if (color == null) {
                return null;
            }
            int clamped = Math.max(0, Math.min(255, alpha));
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamped);
        }
    }

    private static final class WaveformPanel extends JPanel {
        private final Color left;
        private final Color right;
        private static final int BARS = AudioVisualizer.BANDS;
        private final float[] current = new float[BARS];
        private final float[] target = new float[BARS];
        private boolean active = false;

        private WaveformPanel(Color left, Color right) {
            this.left = left;
            this.right = right;
            setOpaque(false);
            setPreferredSize(new Dimension(380, 60));
            Arrays.fill(current, 0.12f);
            Arrays.fill(target, 0.12f);
            Timer timer = new Timer(33, e -> step());
            timer.start();
        }

        private void setLevels(float[] levels, boolean active) {
            this.active = active;
            if (levels == null || levels.length == 0 || !active) {
                Arrays.fill(target, 0.12f);
                return;
            }
            int count = Math.min(BARS, levels.length);
            for (int i = 0; i < count; i++) {
                float value = Math.max(0f, Math.min(1f, levels[i]));
                target[i] = 0.05f + value * 0.95f;
            }
            for (int i = count; i < BARS; i++) {
                target[i] = 0.12f;
            }
        }

        private void step() {
            float idleFloor = 0.08f;
            for (int i = 0; i < BARS; i++) {
                float t = target[i];
                float c = current[i];
                float speed = t > c ? 0.45f : 0.2f;
                float next = c + (t - c) * speed;
                if (!active) {
                    next = Math.max(idleFloor, next * 0.9f);
                }
                current[i] = next;
            }
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int gap = 6;
            int barWidth = Math.max(3, (width - (BARS - 1) * gap) / BARS);
            int x = 0;
            for (int i = 0; i < BARS; i++) {
                float value = current[i];
                int barHeight = (int) (height * (0.18f + value * 0.72f));
                int y = (height - barHeight) / 2;
                g2.setColor(i % 2 == 0 ? left : right);
                g2.fillRoundRect(x, y, barWidth, barHeight, 10, 10);
                x += barWidth + gap;
            }
            g2.dispose();
        }
    }

    private static final class DiscPanel extends JPanel {
        private final Color left;
        private final Color right;
        private boolean active;
        private boolean playing;
        private Timer spinTimer;

        private DiscPanel(Color left, Color right) {
            this.left = left;
            this.right = right;
            setOpaque(false);
        }

        private void setState(boolean active, boolean playing) {
            if (this.active == active && this.playing == playing) {
                return;
            }
            this.active = active;
            this.playing = playing;
            if (playing) {
                if (spinTimer == null) {
                    spinTimer = new Timer(33, e -> repaint());
                }
                spinTimer.start();
            } else if (spinTimer != null) {
                spinTimer.stop();
            }
            repaint();
        }

        private Color blend(Color base, Color target, float amount) {
            int r = Math.round(base.getRed() * (1f - amount) + target.getRed() * amount);
            int g = Math.round(base.getGreen() * (1f - amount) + target.getGreen() * amount);
            int b = Math.round(base.getBlue() * (1f - amount) + target.getBlue() * amount);
            return new Color(r, g, b);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int size = Math.min(getWidth(), getHeight());
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            int padding = 6;
            int discSize = size - padding * 2;
            if (discSize <= 0) {
                g2.dispose();
                return;
            }
            int discX = x + padding;
            int discY = y + padding;

            Color muted = new Color(10, 16, 28);
            Color idleMix = blend(left, muted, 0.65f);
            Color idleMixRight = blend(right, muted, 0.65f);
            Color activeMix = blend(left, muted, 0.35f);
            Color activeMixRight = blend(right, muted, 0.35f);
            Color baseLeft = playing ? left : (active ? activeMix : idleMix);
            Color baseRight = playing ? right : (active ? activeMixRight : idleMixRight);

            float pulse = playing ? (float) (0.5f + 0.5f * Math.sin(System.currentTimeMillis() / 220f)) : 0f;
            int glowAlpha = playing ? (int) (70 + 40 * pulse) : (active ? 55 : 25);
            g2.setColor(new Color(baseLeft.getRed(), baseLeft.getGreen(), baseLeft.getBlue(), glowAlpha));
            int glowInset = 2;
            g2.fillOval(discX - glowInset, discY - glowInset, discSize + glowInset * 2, discSize + glowInset * 2);

            GradientPaint paint = new GradientPaint(discX, discY, baseLeft, discX + discSize, discY + discSize, baseRight);
            g2.setPaint(paint);
            g2.fillOval(discX, discY, discSize, discSize);

            g2.setColor(new Color(255, 255, 255, playing ? 46 : (active ? 30 : 18)));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawOval(discX + 6, discY + 6, discSize - 12, discSize - 12);
            g2.setColor(new Color(0, 0, 0, playing ? 70 : 90));
            g2.fillOval(discX + discSize / 3, discY + discSize / 3, discSize / 3, discSize / 3);

            int ringInset = Math.max(0, 2 - (playing ? Math.round(1.2f * pulse) : 0));
            float ringStroke = 4.5f + (playing ? 1.2f * pulse : 0f);
            int ringSize = discSize - ringInset * 2;
            int ringX = discX + ringInset;
            int ringY = discY + ringInset;
            int cyanAlpha = playing ? (int) (120 + 80 * pulse) : (active ? 90 : 50);
            int violetAlpha = playing ? (int) (100 + 70 * pulse) : (active ? 70 : 40);
            g2.setStroke(new BasicStroke(ringStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(73, 216, 255, cyanAlpha));
            g2.drawOval(ringX, ringY, ringSize, ringSize);

            int innerInset = ringInset + 6;
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(157, 92, 255, violetAlpha));
            g2.drawOval(discX + innerInset, discY + innerInset, discSize - innerInset * 2, discSize - innerInset * 2);

            if (!active) {
                g2.setColor(new Color(255, 255, 255, 16));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(discX + discSize / 4, discY + discSize / 4, discSize / 2, discSize / 2);
            }
            g2.dispose();
        }
    }

    private static final class AnimatedPanel extends JPanel {
        private float alpha = 1f;
        private int offsetX;
        private int offsetY;

        private AnimatedPanel(JComponent content) {
            setOpaque(false);
            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
        }

        private void setAnimation(float alpha, int offsetX, int offsetY) {
            this.alpha = Math.max(0f, Math.min(1f, alpha));
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            repaint();
        }

        @Override
        public void paint(java.awt.Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.translate(offsetX, offsetY);
            super.paint(g2);
            g2.dispose();
        }
    }

    private final class SplashOverlay extends JComponent {
        private float progress;
        private boolean connected;
        private float transition;
        private final long startMillis = System.currentTimeMillis();
        private final float[] nodeX = {0.21f, 0.72f, 0.18f, 0.82f, 0.86f, 0.58f};
        private final float[] nodeY = {0.16f, 0.28f, 0.70f, 0.80f, 0.12f, 0.58f};
        private final float[] nodePhase = {0.2f, 0.8f, 1.3f, 1.7f, 2.1f, 0.5f};
        private Color splashBg1;
        private Color splashBg2;
        private Color splashPanel;
        private Color splashLine;
        private Color splashCyan;
        private Color splashCyanSoft;
        private Color splashViolet;
        private Color splashVioletSoft;
        private Color splashWhite;
        private Color splashMuted;
        private Color splashSuccess;
        private int paletteHash;
        private boolean neonTheme;
        private final Color neonBg1 = new Color(5, 8, 22);
        private final Color neonBg2 = new Color(13, 18, 48);
        private final Color neonPanel = new Color(15, 20, 45, 184);
        private final Color neonLine = new Color(110, 145, 255, 56);
        private final Color neonCyan = new Color(73, 216, 255);
        private final Color neonCyanSoft = new Color(73, 216, 255, 115);
        private final Color neonViolet = new Color(157, 92, 255);
        private final Color neonVioletSoft = new Color(157, 92, 255, 108);
        private final Color neonWhite = new Color(238, 244, 255);
        private final Color neonMuted = new Color(169, 180, 214);
        private final Color neonSuccess = new Color(82, 243, 173);
        private BufferedImage cachedBackground;
        private BufferedImage cachedGrid;
        private int cachedWidth = -1;
        private int cachedHeight = -1;

        private SplashOverlay() {
            setOpaque(false);
            setDoubleBuffered(true);
            syncPalette();
        }

        private void update(float progress, boolean connected, float transition) {
            this.progress = progress;
            this.connected = connected;
            this.transition = transition;
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            syncPalette();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int width = getWidth();
            int height = getHeight();
            long elapsed = System.currentTimeMillis() - startMillis;

            float overlayAlpha = 1f - transition;
            ensureCache(width, height);
            drawBackdrop(g2, width, height, overlayAlpha);
            drawGrid(g2, width, height, overlayAlpha);
            drawNodes(g2, elapsed, overlayAlpha);

            Point target = resolveHeaderLogoCenter();
            int startSize = 220;
            int endSize = 52;
            int centerX = width / 2;
            int centerY = height / 2;

            int iconX = (int) (centerX + (target.x - centerX) * transition);
            int iconY = (int) (centerY + (target.y - centerY) * transition);
            int iconSize = (int) (startSize + (endSize - startSize) * transition);

            drawOrbitalRings(g2, iconX, iconY, iconSize, overlayAlpha);
            drawSoundBars(g2, elapsed, width, height, overlayAlpha);
            drawCore(g2, iconX, iconY, iconSize, overlayAlpha, elapsed);
            drawBrand(g2, centerX, centerY, overlayAlpha);
            drawStatusPill(g2, width, overlayAlpha);
            drawProgressBar(g2, width, height, overlayAlpha);

            g2.dispose();
        }

        private Point resolveHeaderLogoCenter() {
            if (headerLogoLabel == null) {
                return new Point(getWidth() / 2, getHeight() / 2);
            }
            Point p = SwingUtilities.convertPoint(headerLogoLabel, headerLogoLabel.getWidth() / 2, headerLogoLabel.getHeight() / 2, this);
            return new Point(p.x, p.y);
        }

        private void drawStatusPill(Graphics2D g2, int width, float alpha) {
            String text = connected ? ui("statusOnline") : ui("booting");
            String subtitle = ui("bootSubtitle");
            Font bold = getFont().deriveFont(Font.BOLD, 12f);
            Font plain = getFont().deriveFont(Font.PLAIN, 12f);
            g2.setFont(bold);
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g2.setFont(plain);
            int subtitleWidth = g2.getFontMetrics().stringWidth(subtitle);
            int pillWidth = textWidth + subtitleWidth + 52;
            int pillHeight = 30;
            int x = (width - pillWidth) / 2;
            int y = (int) (getHeight() * 0.065f);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(new Color(splashPanel.getRed(), splashPanel.getGreen(), splashPanel.getBlue(), 150));
            g2.fillRoundRect(x, y, pillWidth, pillHeight, 999, 999);
            g2.setColor(new Color(splashLine.getRed(), splashLine.getGreen(), splashLine.getBlue(), 70));
            g2.drawRoundRect(x, y, pillWidth, pillHeight, 999, 999);

            int dotX = x + 12;
            int dotY = y + (pillHeight - 10) / 2;
            g2.setColor(new Color(splashSuccess.getRed(), splashSuccess.getGreen(), splashSuccess.getBlue(), connected ? 255 : 140));
            g2.fillOval(dotX, dotY, 10, 10);
            g2.setColor(new Color(82, 243, 173, connected ? 200 : 80));
            g2.drawOval(dotX, dotY, 10, 10);

            int textX = dotX + 18;
            int textY = y + 19;
            g2.setFont(bold);
            g2.setColor(splashWhite);
            g2.drawString(text, textX, textY);
            g2.setFont(plain);
            g2.setColor(splashMuted);
            g2.drawString(subtitle, textX + textWidth + 10, textY);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawProgressBar(Graphics2D g2, int width, int height, float alpha) {
            int barWidth = Math.min(620, width - 160);
            int barHeight = 8;
            int x = (width - barWidth) / 2;
            int y = (int) (height * 0.875f);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            g2.setColor(splashMuted);
            g2.drawString(ui("loadingModules"), x, y - 14);
            String detail = ui("loadingDetail");
            int detailWidth = g2.getFontMetrics().stringWidth(detail);
            g2.drawString(detail, x + barWidth - detailWidth, y - 14);

            g2.setColor(applyAlpha(splashWhite, 20));
            g2.fillRoundRect(x, y, barWidth, barHeight, 999, 999);
            g2.setColor(applyAlpha(splashLine, 40));
            g2.drawRoundRect(x, y, barWidth, barHeight, 999, 999);

            int fillWidth = (int) (barWidth * progress);
            GradientPaint fill = new GradientPaint(x, y, splashCyan, x + barWidth, y, splashViolet);
            g2.setPaint(fill);
            g2.fillRoundRect(x, y, fillWidth, barHeight, 999, 999);

            g2.setFont(getFont().deriveFont(Font.PLAIN, 13f));
            g2.setColor(splashMuted);
            g2.drawString(ui("bootPrep"), x, y + 24);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawCore(Graphics2D g2, int centerX, int centerY, int size, float alpha, long elapsed) {
            int coreSize = size + 100;
            int coreX = centerX - coreSize / 2;
            int coreY = centerY - coreSize / 2;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            RadialGradientPaint glow1 = new RadialGradientPaint(
                    new Point2D.Float(centerX, centerY - coreSize * 0.1f),
                    coreSize * 0.55f,
                    new float[]{0f, 0.65f, 1f},
                    new Color[]{applyAlpha(splashWhite, 18), applyAlpha(splashCyan, 18), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(glow1);
            g2.fillOval(coreX, coreY, coreSize, coreSize);

            float haloPhase = (float) Math.sin((elapsed / 1000f) * 2.2f) * 0.5f + 0.5f;
            float haloScale = 0.92f + 0.2f * haloPhase;
            int haloSize = (int) (coreSize * haloScale);
            int haloX = centerX - haloSize / 2;
            int haloY = centerY - haloSize / 2;
            g2.setColor(applyAlpha(splashCyan, (int) (alpha * 60)));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(haloX, haloY, haloSize, haloSize);

            drawLogoCard(g2, centerX, centerY, size, alpha, elapsed);

            drawScanLine(g2, coreX, coreY, coreSize, alpha, elapsed);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawLogoCard(Graphics2D g2, int centerX, int centerY, int size, float alpha, long elapsed) {
            int discSize = (int) (size * 0.95f);
            int discX = centerX - discSize / 2;
            int discY = centerY - discSize / 2;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            g2.setColor(new Color(splashCyan.getRed(), splashCyan.getGreen(), splashCyan.getBlue(), 40));
            g2.fillOval(discX - 8, discY - 8, discSize + 16, discSize + 16);

            GradientPaint paint = new GradientPaint(discX, discY, splashCyan, discX + discSize, discY + discSize, splashViolet);
            g2.setPaint(paint);
            g2.fillOval(discX, discY, discSize, discSize);

            g2.setColor(new Color(255, 255, 255, 40));
            g2.drawOval(discX + 6, discY + 6, discSize - 12, discSize - 12);
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(discX + discSize / 3, discY + discSize / 3, discSize / 3, discSize / 3);

            float sweep = (elapsed % 2800L) / 2800f;
            float sweepAngle = sweep * 360f;
            g2.rotate(Math.toRadians(sweepAngle), centerX, centerY);
            GradientPaint sweepPaint = new GradientPaint(discX - discSize, discY - discSize, splashCyanSoft, discX + discSize, discY + discSize, splashVioletSoft);
            g2.setPaint(sweepPaint);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.18f));
            g2.fillOval(discX - 18, discY - 18, discSize + 36, discSize + 36);
            g2.rotate(Math.toRadians(-sweepAngle), centerX, centerY);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawScanLine(Graphics2D g2, int x, int y, int size, float alpha, long elapsed) {
            float scan = (elapsed % 2200L) / 2200f;
            int scanY = y + (int) (scan * size);
            GradientPaint scanPaint = new GradientPaint(x, scanY, new Color(255, 255, 255, 0), x, scanY + 50, new Color(255, 255, 255, 22));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.6f));
            g2.setPaint(scanPaint);
            g2.fillRect(x, scanY, size, 40);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawNodes(Graphics2D g2, long elapsed, float alpha) {
            float baseAlpha = 0.6f * alpha;
            for (int i = 0; i < nodeX.length; i++) {
                float twinkle = 0.4f + 0.6f * (float) Math.abs(Math.sin((elapsed / 1000f) + nodePhase[i]));
                int size = (int) (6 + twinkle * 4);
                int x = (int) (getWidth() * nodeX[i]);
                int y = (int) (getHeight() * nodeY[i]);
                Color nodeColor = tintColor(splashCyan, 0.25f);
                g2.setColor(applyAlpha(nodeColor, (int) (baseAlpha * twinkle * 255)));
                g2.fillOval(x, y, size, size);
            }
        }

        private void drawSoundBars(Graphics2D g2, long elapsed, int width, int height, float alpha) {
            int totalWidth = Math.min(760, width - 160);
            int barHeight = 100;
            int centerY = height / 2;
            int leftX = (width - totalWidth) / 2;
            int rightX = leftX + totalWidth - 180;
            drawBarsBlock(g2, elapsed, leftX, centerY - barHeight / 2, alpha);
            drawBarsBlock(g2, elapsed + 400, rightX, centerY - barHeight / 2, alpha);
        }

        private void drawBarsBlock(Graphics2D g2, long elapsed, int x, int y, float alpha) {
            int bars = 8;
            int gap = 10;
            for (int i = 0; i < bars; i++) {
                float phase = (elapsed / 1000f) + i * 0.3f;
                float scale = 0.55f + 0.6f * (float) Math.abs(Math.sin(phase));
                int height = (int) (24 + scale * 60);
                int barX = x + i * (8 + gap);
                int barY = y + (100 - height) / 2;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                GradientPaint paint = new GradientPaint(barX, barY, splashCyan, barX, barY + height, splashViolet);
                g2.setPaint(paint);
                g2.fillRoundRect(barX, barY, 8, height, 999, 999);
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawBackdrop(Graphics2D g2, int width, int height, float alpha) {
            if (cachedBackground == null) {
                return;
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f + 0.35f * alpha));
            g2.drawImage(cachedBackground, 0, 0, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawGrid(Graphics2D g2, int width, int height, float alpha) {
            if (cachedGrid == null) {
                return;
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f + 0.45f * alpha));
            g2.drawImage(cachedGrid, 0, 0, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawOrbitalRings(Graphics2D g2, int centerX, int centerY, int iconSize, float alpha) {
            float ringAlpha = 0.45f * alpha;
            int baseSize = iconSize + 300;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ringAlpha));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(applyAlpha(tintColor(splashCyan, 0.35f), 40));
            g2.drawOval(centerX - baseSize / 2, centerY - baseSize / 2, baseSize, baseSize);
            g2.setColor(applyAlpha(splashCyan, 35));
            int inner1 = baseSize - 56;
            g2.drawOval(centerX - inner1 / 2, centerY - inner1 / 2, inner1, inner1);
            g2.setColor(applyAlpha(splashViolet, 30));
            int inner2 = baseSize - 120;
            g2.drawOval(centerX - inner2 / 2, centerY - inner2 / 2, inner2, inner2);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawBrand(Graphics2D g2, int centerX, int centerY, float alpha) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            Font titleFont = getFont().deriveFont(Font.BOLD, 42f);
            Font subtitleFont = getFont().deriveFont(Font.PLAIN, 16f);
            String title = ui("appTitleShort");
            String subtitle = ui("controlPanel");
            g2.setFont(titleFont);
            g2.setColor(splashWhite);
            int titleWidth = g2.getFontMetrics().stringWidth(title);
            int y = centerY + 190;
            g2.drawString(title, centerX - titleWidth / 2, y);

            g2.setFont(subtitleFont);
            g2.setColor(splashMuted);
            int subtitleWidth = g2.getFontMetrics().stringWidth(subtitle);
            g2.drawString(subtitle, centerX - subtitleWidth / 2, y + 24);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void ensureCache(int width, int height) {
            if (width <= 0 || height <= 0) {
                return;
            }
            if (cachedBackground != null && cachedGrid != null && width == cachedWidth && height == cachedHeight) {
                return;
            }
            cachedWidth = width;
            cachedHeight = height;
            cachedBackground = buildBackground(width, height);
            cachedGrid = buildGrid(width, height);
        }

        private BufferedImage buildBackground(int width, int height) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Color gradientEnd = neonTheme ? new Color(18, 5, 31) : mixColor(splashBg2, splashViolet, 0.35f);
            LinearGradientPaint base = new LinearGradientPaint(
                    0f, 0f, width, height,
                    new float[]{0f, 0.65f, 1f},
                    new Color[]{splashBg1, splashBg2, gradientEnd}
            );
            g2.setPaint(base);
            g2.fillRect(0, 0, width, height);

            Color glowLeftColor = neonTheme ? new Color(73, 216, 255, 36) : applyAlpha(splashCyan, 36);
            RadialGradientPaint glowLeft = new RadialGradientPaint(
                    new Point2D.Float(width * 0.3f, height * 0.2f),
                    Math.max(width, height) * 0.35f,
                    new float[]{0f, 0.3f, 1f},
                    new Color[]{glowLeftColor, applyAlpha(glowLeftColor, 0), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(glowLeft);
            g2.fillRect(0, 0, width, height);

            Color glowRightColor = neonTheme ? new Color(157, 92, 255, 40) : applyAlpha(splashViolet, 40);
            RadialGradientPaint glowRight = new RadialGradientPaint(
                    new Point2D.Float(width * 0.75f, height * 0.75f),
                    Math.max(width, height) * 0.4f,
                    new float[]{0f, 0.32f, 1f},
                    new Color[]{glowRightColor, applyAlpha(glowRightColor, 0), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(glowRight);
            g2.fillRect(0, 0, width, height);
            g2.dispose();
            return img;
        }

        private BufferedImage buildGrid(int width, int height) {
            int step = 52;
            BufferedImage grid = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = grid.createGraphics();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setColor(new Color(splashLine.getRed(), splashLine.getGreen(), splashLine.getBlue(), 20));
            for (int x = 0; x < width; x += step) {
                gg.drawLine(x, 0, x, height);
            }
            for (int y = 0; y < height; y += step) {
                gg.drawLine(0, y, width, y);
            }

            RadialGradientPaint mask = new RadialGradientPaint(
                    new Point2D.Float(width / 2f, height / 2f),
                    Math.max(width, height) * 0.6f,
                    new float[]{0f, 0.4f, 0.95f, 1f},
                    new Color[]{new Color(0, 0, 0, 255), new Color(0, 0, 0, 255), new Color(0, 0, 0, 0), new Color(0, 0, 0, 0)}
            );
            gg.setComposite(AlphaComposite.DstIn);
            gg.setPaint(mask);
            gg.fillRect(0, 0, width, height);
            gg.dispose();
            return grid;
        }

        private void syncPalette() {
            int hash = 17;
            String theme = controlPanelThemeCode == null ? "neon" : controlPanelThemeCode.toLowerCase();
            hash = 31 * hash + theme.hashCode();
            hash = 31 * hash + brandBackground.getRGB();
            hash = 31 * hash + brandBackgroundAlt.getRGB();
            hash = 31 * hash + brandPanel.getRGB();
            hash = 31 * hash + brandBorder.getRGB();
            hash = 31 * hash + brandAccent.getRGB();
            hash = 31 * hash + brandAccentAlt.getRGB();
            hash = 31 * hash + brandText.getRGB();
            hash = 31 * hash + subtleText.getRGB();
            if (hash == paletteHash) {
                return;
            }
            paletteHash = hash;
            neonTheme = "neon".equals(theme);
            if (neonTheme) {
                splashBg1 = neonBg1;
                splashBg2 = neonBg2;
                splashPanel = neonPanel;
                splashLine = neonLine;
                splashCyan = neonCyan;
                splashCyanSoft = neonCyanSoft;
                splashViolet = neonViolet;
                splashVioletSoft = neonVioletSoft;
                splashWhite = neonWhite;
                splashMuted = neonMuted;
                splashSuccess = neonSuccess;
            } else {
                splashBg1 = brandBackground;
                splashBg2 = brandBackgroundAlt;
                splashPanel = applyAlpha(brandPanel, 184);
                splashLine = applyAlpha(brandBorder, 56);
                splashCyan = brandAccent;
                splashCyanSoft = applyAlpha(brandAccent, 115);
                splashViolet = brandAccentAlt;
                splashVioletSoft = applyAlpha(brandAccentAlt, 108);
                splashWhite = brandText;
                splashMuted = subtleText;
                splashSuccess = new Color(82, 243, 173);
            }
            cachedBackground = null;
            cachedGrid = null;
        }
    }


    private record LanguageOption(String label, String code) {
        @Override
        public String toString() {
            return label + " (" + code + ")";
        }
    }

    private record ThemeOption(String label, String code) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record CornerStyleOption(String label, String code) {
        @Override
        public String toString() {
            return label;
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
