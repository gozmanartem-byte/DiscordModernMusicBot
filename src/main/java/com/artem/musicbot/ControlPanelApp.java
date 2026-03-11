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
import javax.swing.WindowConstants;

public class ControlPanelApp {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Path CONFIG_PATH = Path.of("ModernMusicBot.properties");
    private static final Path APP_HOME_PATH = Path.of(System.getProperty("user.home"), ".modernmusicbot");
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

        frame.add(top, BorderLayout.NORTH);
        frame.add(buttons, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);

        scrollPane.setPreferredSize(new Dimension(760, 320));
        frame.setMinimumSize(new Dimension(820, 560));

        loadConfigIntoFields();
        wireActions();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        log("Control panel ready.");
        showFirstLaunchBannerIfNeeded();

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
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
                    runtime.start(CONFIG_PATH, true, this::log);
                    SwingUtilities.invokeLater(() -> {
                        stopButton.setEnabled(true);
                        startButton.setEnabled(false);
                    });
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
            });
        }));
    }

    private void loadConfigIntoFields() {
        if (!Files.exists(CONFIG_PATH)) {
            log("No config found yet. Enter token and prefix, then press Start.");
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
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
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
            }
        }

        props.setProperty("bot.token", token);
        props.setProperty("bot.prefix", prefix);
        props.setProperty("bot.language", language);
        props.putIfAbsent("youtube.poToken", "");
        props.putIfAbsent("youtube.visitorData", "");

        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "ModernMusicBot settings");
        }
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
}
