package com.artem.musicbot;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

public final class I18n {
    public enum Language {
        EN("en"),
        RU("ru"),
        HY("hy"),
        KA("ka"),
        AZ("az"),
        KK("kk"),
        UZ("uz"),
        UK("uk"),
        DE("de"),
        ES("es"),
        IT("it"),
        PT("pt"),
        ZH("zh"),
        JA("ja");

        private final String code;

        Language(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public static Language from(String raw) {
            if (raw == null || raw.isBlank()) {
                return EN;
            }

            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "ru", "russian" -> RU;
                case "hy", "armenian" -> HY;
                case "ka", "georgian" -> KA;
                case "az", "azerbaijani", "azerbaijanian" -> AZ;
                case "kk", "kazakh" -> KK;
                case "uz", "uzbek" -> UZ;
                case "uk", "ukrainian" -> UK;
                case "de", "german" -> DE;
                case "es", "spanish" -> ES;
                case "it", "italian" -> IT;
                case "pt", "portuguese", "potuguise" -> PT;
                case "zh", "chinese", "chinise" -> ZH;
                case "ja", "japanese" -> JA;
                default -> EN;
            };
        }
    }

    private static final Map<String, String> EN = Map.ofEntries(
            Map.entry("status.waiting", "Waiting for music"),
            Map.entry("status.playing", "Singing {0}"),
            Map.entry("usage.play", "Usage: {0}play <url or search>"),
            Map.entry("usage.volume", "Usage: {0}volume <0-200>"),
            Map.entry("usage.bass", "Usage: {0}bass <0-5>"),
            Map.entry("join.voice", "Join a voice channel first."),
            Map.entry("loading", "Loading: {0}"),
            Map.entry("queued", "Queued: {0}"),
            Map.entry("queued.playlist", "Queued from playlist: {0}"),
            Map.entry("playlist.empty", "Playlist is empty."),
            Map.entry("nothing.found", "Nothing found for: {0}"),
            Map.entry("load.failed", "Load failed: {0}"),
            Map.entry("queue.empty", "Queue is empty."),
            Map.entry("skip.now", "Skipped. Now playing: {0}"),
            Map.entry("play.none", "Nothing is playing right now."),
            Map.entry("pause.already", "Playback is already paused."),
            Map.entry("pause.done", "Paused playback."),
            Map.entry("resume.already", "Playback is already running."),
            Map.entry("resume.done", "Resumed playback."),
            Map.entry("stop.done", "Stopped playback and left the voice channel."),
            Map.entry("volume.range", "Volume must be between 0 and 200."),
            Map.entry("volume.set", "Volume set to {0}%."),
            Map.entry("volume.current", "Current volume: {0}%"),
            Map.entry("bass.range", "Bass level must be between 0 and 5."),
            Map.entry("bass.set", "Bass boost set to {0}."),
            Map.entry("bass.current", "Current bass boost: {0}."),
            Map.entry("reset.done", "Audio reset to normal (volume 100%, bass 0)."),
            Map.entry("loud.done", "Loud preset enabled (volume 200%, bass 5). Use {0}normal to reset."),
            Map.entry("now.playing", "Now playing: {0}"),
            Map.entry("upcoming", "Upcoming:"),
            Map.entry("help", "Commands:\n{0}play <url or search>\n{0}skip\n{0}pause\n{0}resume\n{0}stop\n{0}leave\n{0}volume <0-200>\n{0}bass <0-5>\n{0}loud\n{0}normal\n{0}queue\n{0}player\n{0}debugaudio\n{0}help"),
            Map.entry("earrape.refuse", "I can't add an earrape mode. Use {0}volume and {0}bass instead."),
            Map.entry("player.posted", "Player panel posted."),
            Map.entry("player.title", "Music player controls"),
            Map.entry("player.hint", "Use buttons below or commands with prefix {0}."),
            Map.entry("player.pause", "Pause"),
            Map.entry("player.resume", "Resume"),
            Map.entry("player.skip", "Skip"),
            Map.entry("player.stop", "Stop"),
            Map.entry("player.queue", "Queue"),
            Map.entry("player.volup", "Vol +"),
            Map.entry("player.voldown", "Vol -")
    );

    private static final Map<String, String> RU = Map.ofEntries(
            Map.entry("status.waiting", "Ждет музыку"),
            Map.entry("status.playing", "Поет {0}"),
            Map.entry("usage.play", "Использование: {0}play <ссылка или поиск>"),
            Map.entry("usage.volume", "Использование: {0}volume <0-200>"),
            Map.entry("usage.bass", "Использование: {0}bass <0-5>"),
            Map.entry("join.voice", "Сначала зайдите в голосовой канал."),
            Map.entry("loading", "Загружаю: {0}"),
            Map.entry("queued", "Добавлено в очередь: {0}"),
            Map.entry("queued.playlist", "Добавлено из плейлиста: {0}"),
            Map.entry("playlist.empty", "Плейлист пуст."),
            Map.entry("nothing.found", "Ничего не найдено для: {0}"),
            Map.entry("load.failed", "Ошибка загрузки: {0}"),
            Map.entry("queue.empty", "Очередь пуста."),
            Map.entry("skip.now", "Пропущено. Сейчас играет: {0}"),
            Map.entry("play.none", "Сейчас ничего не играет."),
            Map.entry("pause.already", "Воспроизведение уже на паузе."),
            Map.entry("pause.done", "Воспроизведение приостановлено."),
            Map.entry("resume.already", "Воспроизведение уже идет."),
            Map.entry("resume.done", "Воспроизведение продолжено."),
            Map.entry("stop.done", "Остановил музыку и вышел из голосового канала."),
            Map.entry("volume.range", "Громкость должна быть от 0 до 200."),
            Map.entry("volume.set", "Громкость установлена на {0}%."),
            Map.entry("volume.current", "Текущая громкость: {0}%."),
            Map.entry("bass.range", "Бас должен быть от 0 до 5."),
            Map.entry("bass.set", "Усиление баса: {0}."),
            Map.entry("bass.current", "Текущий бас: {0}."),
            Map.entry("reset.done", "Звук сброшен к обычному режиму (громкость 100%, бас 0)."),
            Map.entry("loud.done", "Включен громкий режим (громкость 200%, бас 5). Используйте {0}normal для сброса."),
            Map.entry("now.playing", "Сейчас играет: {0}"),
            Map.entry("upcoming", "Далее:"),
            Map.entry("help", "Команды:\n{0}play <ссылка или поиск>\n{0}skip\n{0}pause\n{0}resume\n{0}stop\n{0}leave\n{0}volume <0-200>\n{0}bass <0-5>\n{0}loud\n{0}normal\n{0}queue\n{0}player\n{0}debugaudio\n{0}help"),
            Map.entry("earrape.refuse", "Я не могу добавить режим earrape. Используйте {0}volume и {0}bass."),
            Map.entry("player.posted", "Панель плеера отправлена."),
            Map.entry("player.title", "Панель управления музыкой"),
            Map.entry("player.hint", "Используйте кнопки ниже или команды с префиксом {0}."),
            Map.entry("player.pause", "Пауза"),
            Map.entry("player.resume", "Продолжить"),
            Map.entry("player.skip", "Пропуск"),
            Map.entry("player.stop", "Стоп"),
            Map.entry("player.queue", "Очередь"),
            Map.entry("player.volup", "Громк +"),
            Map.entry("player.voldown", "Громк -")
    );

    private final Language language;

    public I18n(String languageCode) {
        this.language = Language.from(languageCode);
    }

    public String code() {
        return language.code();
    }

    public String t(String key, Object... args) {
        String pattern = switch (language) {
            case RU -> RU.getOrDefault(key, EN.getOrDefault(key, key));
            default -> EN.getOrDefault(key, key);
        };
        return MessageFormat.format(pattern, args);
    }
}
