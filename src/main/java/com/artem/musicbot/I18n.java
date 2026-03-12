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
            Map.entry("usage.remove", "Usage: {0}remove <index>"),
            Map.entry("usage.seek", "Usage: {0}seek <seconds>"),
            Map.entry("usage.autoplay", "Usage: {0}autoplay <on|off>"),
            Map.entry("usage.setcommandchannel", "Usage: {0}setcommandchannel <#channel|channelId|off>"),
            Map.entry("usage.setblockedrole", "Usage: {0}setblockedrole <@role|roleId|off>"),
            Map.entry("join.voice", "Join a voice channel first."),
            Map.entry("join.serverMuted", "Warning: I am server-muted in this voice channel. Unmute me in Discord to hear audio."),
            Map.entry("join.voice.desktop", "No active voice channel found. Join a voice channel in Discord first, then add songs from the app."),
            Map.entry("join.voice.desktop.connected", "Desktop auto-connected to voice channel: {0}"),
            Map.entry("loading", "Loading: {0}"),
            Map.entry("queued", "Queued: {0}"),
            Map.entry("queued.playlist", "Queued from playlist: {0}"),
            Map.entry("playlist.empty", "Playlist is empty."),
            Map.entry("nothing.found", "Nothing found for: {0}"),
            Map.entry("load.failed", "Load failed: {0}"),
            Map.entry("queue.empty", "Queue is empty."),
            Map.entry("queue.removed", "Removed track #{0} from queue."),
            Map.entry("queue.invalidIndex", "Invalid queue index."),
            Map.entry("queue.cleared", "Cleared {0} queued track(s)."),
            Map.entry("queue.shuffled", "Shuffled {0} queued track(s)."),
            Map.entry("skip.now", "Skipped. Now playing: {0}"),
            Map.entry("play.none", "Nothing is playing right now."),
            Map.entry("pause.already", "Playback is already paused."),
            Map.entry("pause.done", "Paused playback."),
            Map.entry("resume.already", "Playback is already running."),
            Map.entry("resume.done", "Resumed playback."),
            Map.entry("stop.done", "Stopped playback and left the voice channel."),
            Map.entry("loop.track", "Loop mode set to: track."),
            Map.entry("loop.queue", "Loop mode set to: queue."),
            Map.entry("loop.off", "Loop mode set to: off."),
            Map.entry("seek.nonNegative", "Seek must be >= 0."),
            Map.entry("seek.done", "Seeked to {0}s."),
            Map.entry("autoplay.enabled", "Autoplay enabled."),
            Map.entry("autoplay.disabled", "Autoplay disabled."),
            Map.entry("autoplay.queued", "Autoplay queued: {0}"),
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
            Map.entry("help", "Commands:\n{0}play <url or search>\n{0}skip\n{0}pause\n{0}resume\n{0}stop\n{0}leave\n{0}volume <0-200>\n{0}bass <0-5>\n{0}queue\n{0}player\n{0}debugaudio\n{0}help"),
            Map.entry("earrape.hostOnly", "EarRape is host-only and available only in the desktop Control Panel."),
            Map.entry("earrape.refuse", "I can't add an earrape mode. Use {0}volume and {0}bass instead."),
            Map.entry("player.posted", "Player panel posted."),
            Map.entry("player.title", "Music player controls"),
            Map.entry("player.hint", "Use buttons below or commands with prefix {0}."),
                Map.entry("player.footer", "Commands stay in English. Example: {0}play <song>"),
                Map.entry("player.status", "Status"),
                Map.entry("player.track", "Track"),
                Map.entry("player.voice", "Voice channel"),
                Map.entry("player.volume", "Volume"),
                Map.entry("player.bass", "Bass"),
                Map.entry("player.queuePreview", "Queue preview"),
                Map.entry("player.state.idle", "Idle"),
                Map.entry("player.state.playing", "Playing"),
                Map.entry("player.state.paused", "Paused"),
                Map.entry("player.none", "Nothing playing"),
                Map.entry("player.notConnected", "Not connected"),
                Map.entry("player.queueEmpty", "Queue is empty"),
            Map.entry("player.pause", "Pause"),
            Map.entry("player.resume", "Resume"),
            Map.entry("player.skip", "Skip"),
            Map.entry("player.stop", "Stop"),
            Map.entry("player.queue", "Queue"),
                Map.entry("player.refresh", "Refresh"),
            Map.entry("player.volup", "Vol +"),
                Map.entry("player.voldown", "Vol -"),
                Map.entry("player.bassup", "Bass +"),
                Map.entry("player.bassdown", "Bass -"),
                Map.entry("player.bassreset", "Bass 0"),
                Map.entry("player.qremove", "Remove Next"),
                Map.entry("player.qclear", "Clear Queue"),
            Map.entry("prefix.invalid", "Prefix must be 1-3 characters."),
            Map.entry("prefix.updated", "Prefix updated to {0}"),
            Map.entry("language.updated", "Language updated to {0}"),
            Map.entry("permission.dj", "You need the DJ role (or admin rights) for this command."),
            Map.entry("permission.admin", "Admin permission required."),
            Map.entry("blocked.role", "Your role is blocked from using bot commands."),
            Map.entry("restricted.commands", "Commands are restricted to <#{0}>."),
                Map.entry("restricted.buttons", "Buttons are restricted to <#{0}>."),
                Map.entry("setdj.slashOnly", "Use slash command /setdj to select a role."),
                Map.entry("dj.role.set", "DJ role set to @{0}"),
                Map.entry("restriction.command.disabled", "Command-channel restriction disabled."),
                Map.entry("restriction.command.provideChannel", "Provide a text channel or set off=true."),
                Map.entry("restriction.commands.enabled", "Commands are now restricted to <#{0}>."),
                Map.entry("restriction.blockedRole.disabled", "Blocked-role restriction disabled."),
                Map.entry("restriction.blockedRole.enabled", "Role <@&{0}> is now blocked from bot commands."),
                Map.entry("restriction.blockedRole.provideRole", "Provide a role or set off=true."),
                Map.entry("restriction.blockedRole.name.enabled", "Role @{0} is now blocked from bot commands."),
                Map.entry("role.required", "Role is required."),
                    Map.entry("disconnect.idle", "No playable track was loaded, so I left the voice channel."),
                    Map.entry("search.expired", "This search selection expired."),
                    Map.entry("search.owner.only.choose", "Only the user who started this search can choose a result."),
                    Map.entry("search.owner.only.cancel", "Only the user who started this search can cancel it."),
                    Map.entry("search.invalid", "Invalid search result selection."),
                    Map.entry("search.cancelled", "Search cancelled."),
                    Map.entry("search.selected", "Selected: {0}")
    );

    private static final Map<String, String> RU = Map.ofEntries(
            Map.entry("status.waiting", "Ждет музыку"),
            Map.entry("status.playing", "Поет {0}"),
            Map.entry("usage.play", "Использование: {0}play <ссылка или поиск>"),
            Map.entry("usage.volume", "Использование: {0}volume <0-200>"),
            Map.entry("usage.bass", "Использование: {0}bass <0-5>"),
            Map.entry("usage.remove", "Использование: {0}remove <index>"),
            Map.entry("usage.seek", "Использование: {0}seek <seconds>"),
            Map.entry("usage.autoplay", "Использование: {0}autoplay <on|off>"),
            Map.entry("usage.setcommandchannel", "Использование: {0}setcommandchannel <#channel|channelId|off>"),
            Map.entry("usage.setblockedrole", "Использование: {0}setblockedrole <@role|roleId|off>"),
            Map.entry("join.voice", "Сначала зайдите в голосовой канал."),
            Map.entry("join.serverMuted", "Внимание: бот заглушен на сервере в этом голосовом канале. Снимите mute в Discord, чтобы слышать звук."),
            Map.entry("join.voice.desktop", "Не найден активный голосовой канал. Сначала зайдите в голосовой канал в Discord, затем добавляйте песни из приложения."),
            Map.entry("join.voice.desktop.connected", "Desktop автоматически подключен к голосовому каналу: {0}"),
            Map.entry("loading", "Загружаю: {0}"),
            Map.entry("queued", "Добавлено в очередь: {0}"),
            Map.entry("queued.playlist", "Добавлено из плейлиста: {0}"),
            Map.entry("playlist.empty", "Плейлист пуст."),
            Map.entry("nothing.found", "Ничего не найдено для: {0}"),
            Map.entry("load.failed", "Ошибка загрузки: {0}"),
            Map.entry("queue.empty", "Очередь пуста."),
            Map.entry("queue.removed", "Трек #{0} удален из очереди."),
            Map.entry("queue.invalidIndex", "Неверный индекс в очереди."),
            Map.entry("queue.cleared", "Удалено треков из очереди: {0}."),
            Map.entry("queue.shuffled", "Перемешано треков в очереди: {0}."),
            Map.entry("skip.now", "Пропущено. Сейчас играет: {0}"),
            Map.entry("play.none", "Сейчас ничего не играет."),
            Map.entry("pause.already", "Воспроизведение уже на паузе."),
            Map.entry("pause.done", "Воспроизведение приостановлено."),
            Map.entry("resume.already", "Воспроизведение уже идет."),
            Map.entry("resume.done", "Воспроизведение продолжено."),
            Map.entry("stop.done", "Остановил музыку и вышел из голосового канала."),
            Map.entry("loop.track", "Режим повтора: трек."),
            Map.entry("loop.queue", "Режим повтора: очередь."),
            Map.entry("loop.off", "Режим повтора: выключен."),
            Map.entry("seek.nonNegative", "Позиция должна быть >= 0."),
            Map.entry("seek.done", "Перемотано на {0}с."),
            Map.entry("autoplay.enabled", "Автовоспроизведение включено."),
            Map.entry("autoplay.disabled", "Автовоспроизведение выключено."),
            Map.entry("autoplay.queued", "Автовоспроизведение добавило: {0}"),
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
            Map.entry("help", "Команды:\n{0}play <ссылка или поиск>\n{0}skip\n{0}pause\n{0}resume\n{0}stop\n{0}leave\n{0}volume <0-200>\n{0}bass <0-5>\n{0}queue\n{0}player\n{0}debugaudio\n{0}help"),
            Map.entry("earrape.hostOnly", "EarRape доступен только хосту и только в desktop Control Panel."),
            Map.entry("earrape.refuse", "Я не могу добавить режим earrape. Используйте {0}volume и {0}bass."),
            Map.entry("player.posted", "Панель плеера отправлена."),
            Map.entry("player.title", "Панель управления музыкой"),
            Map.entry("player.hint", "Используйте кнопки ниже или команды с префиксом {0}."),
                Map.entry("player.footer", "Команды остаются на английском. Пример: {0}play <song>"),
                Map.entry("player.status", "Статус"),
                Map.entry("player.track", "Трек"),
                Map.entry("player.voice", "Голосовой канал"),
                Map.entry("player.volume", "Громкость"),
                Map.entry("player.bass", "Бас"),
                Map.entry("player.queuePreview", "Очередь"),
                Map.entry("player.state.idle", "Ожидание"),
                Map.entry("player.state.playing", "Играет"),
                Map.entry("player.state.paused", "Пауза"),
                Map.entry("player.none", "Сейчас ничего не играет"),
                Map.entry("player.notConnected", "Не подключен"),
                Map.entry("player.queueEmpty", "Очередь пуста"),
            Map.entry("player.pause", "Пауза"),
            Map.entry("player.resume", "Продолжить"),
            Map.entry("player.skip", "Пропуск"),
            Map.entry("player.stop", "Стоп"),
            Map.entry("player.queue", "Очередь"),
                Map.entry("player.refresh", "Обновить"),
            Map.entry("player.volup", "Громк +"),
                    Map.entry("player.voldown", "Громк -"),
                    Map.entry("player.bassup", "Бас +"),
                    Map.entry("player.bassdown", "Бас -"),
                        Map.entry("player.bassreset", "Бас 0"),
                        Map.entry("player.qremove", "Убрать след."),
                        Map.entry("player.qclear", "Очистить очередь"),
                    Map.entry("prefix.invalid", "Префикс должен быть длиной 1-3 символа."),
                    Map.entry("prefix.updated", "Префикс обновлен: {0}"),
                    Map.entry("language.updated", "Язык обновлен: {0}"),
                    Map.entry("permission.dj", "Для этой команды нужна роль DJ (или права администратора)."),
                    Map.entry("permission.admin", "Нужны права администратора."),
                    Map.entry("blocked.role", "Ваша роль заблокирована для команд бота."),
                    Map.entry("restricted.commands", "Команды разрешены только в канале <#{0}>."),
                        Map.entry("restricted.buttons", "Кнопки разрешены только в канале <#{0}>."),
                        Map.entry("setdj.slashOnly", "Используйте slash-команду /setdj для выбора роли."),
                        Map.entry("dj.role.set", "DJ роль установлена: @{0}"),
                        Map.entry("restriction.command.disabled", "Ограничение командного канала отключено."),
                        Map.entry("restriction.command.provideChannel", "Укажите текстовый канал или off=true."),
                        Map.entry("restriction.commands.enabled", "Команды теперь разрешены только в канале <#{0}>."),
                        Map.entry("restriction.blockedRole.disabled", "Ограничение заблокированной роли отключено."),
                        Map.entry("restriction.blockedRole.enabled", "Роль <@&{0}> теперь заблокирована для команд бота."),
                        Map.entry("restriction.blockedRole.provideRole", "Укажите роль или off=true."),
                        Map.entry("restriction.blockedRole.name.enabled", "Роль @{0} теперь заблокирована для команд бота."),
                        Map.entry("role.required", "Нужно указать роль."),
                            Map.entry("disconnect.idle", "Не удалось загрузить ни одного трека, поэтому я вышел из голосового канала."),
                            Map.entry("search.expired", "Выбор результата поиска истек."),
                            Map.entry("search.owner.only.choose", "Только пользователь, который начал поиск, может выбрать результат."),
                            Map.entry("search.owner.only.cancel", "Только пользователь, который начал поиск, может отменить выбор."),
                            Map.entry("search.invalid", "Неверный выбор результата поиска."),
                            Map.entry("search.cancelled", "Поиск отменен."),
                            Map.entry("search.selected", "Выбрано: {0}")
    );

                private static final Map<String, String> HY = Map.ofEntries(
                    Map.entry("status.waiting", "Երաժշտության է սպասում"),
                    Map.entry("status.playing", "Երգում է {0}"),
                    Map.entry("player.title", "Երաժշտական նվագարկիչ"),
                    Map.entry("player.hint", "Օգտագործեք ներքևի կոճակները կամ հրամաններ {0} նախածանցով։"),
                    Map.entry("player.footer", "Հրամանները մնում են անգլերեն. Օրինակ՝ {0}play <song>"),
                    Map.entry("player.status", "Կարգավիճակ"),
                    Map.entry("player.track", "Թրեք"),
                    Map.entry("player.voice", "Ձայնային ալիք"),
                    Map.entry("player.volume", "Ձայն"),
                    Map.entry("player.bass", "Բաս"),
                    Map.entry("player.queuePreview", "Հերթ"),
                    Map.entry("player.state.idle", "Սպասում"),
                    Map.entry("player.state.playing", "Նվագարկվում է"),
                    Map.entry("player.state.paused", "Դադար"),
                    Map.entry("player.none", "Ոչինչ չի նվագարկվում"),
                    Map.entry("player.notConnected", "Միացված չէ"),
                    Map.entry("player.queueEmpty", "Հերթը դատարկ է"),
                    Map.entry("player.pause", "Դադար"),
                    Map.entry("player.resume", "Շարունակել"),
                    Map.entry("player.skip", "Բաց թողնել"),
                    Map.entry("player.stop", "Կանգնեցնել"),
                    Map.entry("player.refresh", "Թարմացնել"),
                    Map.entry("player.volup", "Ձայն +"),
                    Map.entry("player.voldown", "Ձայն -"),
                    Map.entry("player.bassup", "Բաս +"),
                    Map.entry("player.bassdown", "Բաս -"),
                    Map.entry("player.bassreset", "Բաս 0")
                );

                private static final Map<String, String> KA = Map.ofEntries(
                    Map.entry("status.waiting", "მუსიკას ელოდება"),
                    Map.entry("status.playing", "მღერის {0}"),
                    Map.entry("player.title", "მუსიკის პლეერი"),
                    Map.entry("player.hint", "გამოიყენეთ ქვემოთ მოცემული ღილაკები ან ბრძანებები პრეფიქსით {0}."),
                    Map.entry("player.footer", "ბრძანებები რჩება ინგლისურად. მაგალითი: {0}play <song>"),
                    Map.entry("player.status", "სტატუსი"),
                    Map.entry("player.track", "ტრეკი"),
                    Map.entry("player.voice", "ხმოვანი არხი"),
                    Map.entry("player.volume", "ხმა"),
                    Map.entry("player.bass", "ბასი"),
                    Map.entry("player.queuePreview", "რიგი"),
                    Map.entry("player.state.idle", "ლოდინი"),
                    Map.entry("player.state.playing", "უკრავს"),
                    Map.entry("player.state.paused", "პაუზა"),
                    Map.entry("player.none", "ახლა არაფერი უკრავს"),
                    Map.entry("player.notConnected", "არ არის დაკავშირებული"),
                    Map.entry("player.queueEmpty", "რიგი ცარიელია"),
                    Map.entry("player.pause", "პაუზა"),
                    Map.entry("player.resume", "გაგრძელება"),
                    Map.entry("player.skip", "გამოტოვება"),
                    Map.entry("player.stop", "გაჩერება"),
                    Map.entry("player.refresh", "განახლება"),
                    Map.entry("player.volup", "ხმა +"),
                    Map.entry("player.voldown", "ხმა -"),
                    Map.entry("player.bassup", "ბასი +"),
                    Map.entry("player.bassdown", "ბასი -"),
                    Map.entry("player.bassreset", "ბასი 0")
                );

                private static final Map<String, String> AZ = Map.ofEntries(
                    Map.entry("status.waiting", "Musiqi gözləyir"),
                    Map.entry("status.playing", "Oxuyur {0}"),
                    Map.entry("player.title", "Musiqi pleyeri"),
                    Map.entry("player.hint", "Aşağıdakı düymələri və ya {0} prefiksi ilə əmrləri istifadə edin."),
                    Map.entry("player.footer", "Əmrlər ingilis dilində qalır. Nümunə: {0}play <song>"),
                    Map.entry("player.status", "Status"),
                    Map.entry("player.track", "Trek"),
                    Map.entry("player.voice", "Səs kanalı"),
                    Map.entry("player.volume", "Səs"),
                    Map.entry("player.bass", "Bas"),
                    Map.entry("player.queuePreview", "Növbə"),
                    Map.entry("player.state.idle", "Gözləmə"),
                    Map.entry("player.state.playing", "Oxunur"),
                    Map.entry("player.state.paused", "Pauza"),
                    Map.entry("player.none", "Heç nə oxunmur"),
                    Map.entry("player.notConnected", "Qoşulmayıb"),
                    Map.entry("player.queueEmpty", "Növbə boşdur"),
                    Map.entry("player.pause", "Pauza"),
                    Map.entry("player.resume", "Davam et"),
                    Map.entry("player.skip", "Keç"),
                    Map.entry("player.stop", "Dayandır"),
                    Map.entry("player.refresh", "Yenilə"),
                    Map.entry("player.volup", "Səs +"),
                    Map.entry("player.voldown", "Səs -"),
                    Map.entry("player.bassup", "Bas +"),
                    Map.entry("player.bassdown", "Bas -"),
                    Map.entry("player.bassreset", "Bas 0")
                );

                private static final Map<String, String> KK = Map.ofEntries(
                    Map.entry("status.waiting", "Музыканы күтіп тұр"),
                    Map.entry("status.playing", "Ән айтып тұр {0}"),
                    Map.entry("player.title", "Музыка ойнатқышы"),
                    Map.entry("player.hint", "Төмендегі батырмаларды немесе {0} префиксі бар командаларды қолданыңыз."),
                    Map.entry("player.footer", "Командалар ағылшын тілінде қалады. Мысал: {0}play <song>"),
                    Map.entry("player.status", "Күй"),
                    Map.entry("player.track", "Трек"),
                    Map.entry("player.voice", "Дауыс арнасы"),
                    Map.entry("player.volume", "Дыбыс"),
                    Map.entry("player.bass", "Басс"),
                    Map.entry("player.queuePreview", "Кезек"),
                    Map.entry("player.state.idle", "Күту"),
                    Map.entry("player.state.playing", "Ойнатылуда"),
                    Map.entry("player.state.paused", "Пауза"),
                    Map.entry("player.none", "Қазір ештеңе ойнатылмайды"),
                    Map.entry("player.notConnected", "Қосылмаған"),
                    Map.entry("player.queueEmpty", "Кезек бос"),
                    Map.entry("player.pause", "Пауза"),
                    Map.entry("player.resume", "Жалғастыру"),
                    Map.entry("player.skip", "Өткізу"),
                    Map.entry("player.stop", "Тоқтату"),
                    Map.entry("player.refresh", "Жаңарту"),
                    Map.entry("player.volup", "Дыбыс +"),
                    Map.entry("player.voldown", "Дыбыс -"),
                    Map.entry("player.bassup", "Басс +"),
                    Map.entry("player.bassdown", "Басс -"),
                    Map.entry("player.bassreset", "Басс 0")
                );

                private static final Map<String, String> UZ = Map.ofEntries(
                    Map.entry("status.waiting", "Musiqa kutmoqda"),
                    Map.entry("status.playing", "Kuylayapti {0}"),
                    Map.entry("player.title", "Musiqa pleyeri"),
                    Map.entry("player.hint", "Quyidagi tugmalarni yoki {0} prefiksi bilan buyruqlarni ishlating."),
                    Map.entry("player.footer", "Buyruqlar ingliz tilida qoladi. Misol: {0}play <song>"),
                    Map.entry("player.status", "Holat"),
                    Map.entry("player.track", "Trek"),
                    Map.entry("player.voice", "Ovoz kanali"),
                    Map.entry("player.volume", "Ovoz"),
                    Map.entry("player.bass", "Bas"),
                    Map.entry("player.queuePreview", "Navbat"),
                    Map.entry("player.state.idle", "Kutish"),
                    Map.entry("player.state.playing", "Ijro etilmoqda"),
                    Map.entry("player.state.paused", "Pauza"),
                    Map.entry("player.none", "Hozir hech narsa ijro etilmayapti"),
                    Map.entry("player.notConnected", "Ulanmagan"),
                    Map.entry("player.queueEmpty", "Navbat bo‘sh"),
                    Map.entry("player.pause", "Pauza"),
                    Map.entry("player.resume", "Davom ettirish"),
                    Map.entry("player.skip", "O‘tkazib yuborish"),
                    Map.entry("player.stop", "To‘xtatish"),
                    Map.entry("player.refresh", "Yangilash"),
                    Map.entry("player.volup", "Ovoz +"),
                    Map.entry("player.voldown", "Ovoz -"),
                    Map.entry("player.bassup", "Bas +"),
                    Map.entry("player.bassdown", "Bas -"),
                    Map.entry("player.bassreset", "Bas 0")
                );

                private static final Map<String, String> UK = Map.ofEntries(
                    Map.entry("status.waiting", "Чекає на музику"),
                    Map.entry("status.playing", "Співає {0}"),
                    Map.entry("player.title", "Музичний плеєр"),
                    Map.entry("player.hint", "Використовуйте кнопки нижче або команди з префіксом {0}."),
                    Map.entry("player.footer", "Команди залишаються англійською. Приклад: {0}play <song>"),
                    Map.entry("player.status", "Статус"),
                    Map.entry("player.track", "Трек"),
                    Map.entry("player.voice", "Голосовий канал"),
                    Map.entry("player.volume", "Гучність"),
                    Map.entry("player.bass", "Бас"),
                    Map.entry("player.queuePreview", "Черга"),
                    Map.entry("player.state.idle", "Очікування"),
                    Map.entry("player.state.playing", "Відтворюється"),
                    Map.entry("player.state.paused", "Пауза"),
                    Map.entry("player.none", "Зараз нічого не грає"),
                    Map.entry("player.notConnected", "Не підключено"),
                    Map.entry("player.queueEmpty", "Черга порожня"),
                    Map.entry("player.pause", "Пауза"),
                    Map.entry("player.resume", "Продовжити"),
                    Map.entry("player.skip", "Пропустити"),
                    Map.entry("player.stop", "Стоп"),
                    Map.entry("player.refresh", "Оновити"),
                    Map.entry("player.volup", "Гучн +"),
                    Map.entry("player.voldown", "Гучн -"),
                    Map.entry("player.bassup", "Бас +"),
                    Map.entry("player.bassdown", "Бас -"),
                    Map.entry("player.bassreset", "Бас 0")
                );

                private static final Map<String, String> DE = Map.ofEntries(
                    Map.entry("status.waiting", "Wartet auf Musik"),
                    Map.entry("status.playing", "Singt {0}"),
                    Map.entry("player.title", "Musik-Player"),
                    Map.entry("player.hint", "Nutze die Buttons unten oder Befehle mit dem Präfix {0}."),
                    Map.entry("player.footer", "Befehle bleiben auf Englisch. Beispiel: {0}play <song>"),
                    Map.entry("player.status", "Status"),
                    Map.entry("player.track", "Track"),
                    Map.entry("player.voice", "Sprachkanal"),
                    Map.entry("player.volume", "Lautstärke"),
                    Map.entry("player.bass", "Bass"),
                    Map.entry("player.queuePreview", "Warteschlange"),
                    Map.entry("player.state.idle", "Leerlauf"),
                    Map.entry("player.state.playing", "Wiedergabe"),
                    Map.entry("player.state.paused", "Pausiert"),
                    Map.entry("player.none", "Es wird nichts abgespielt"),
                    Map.entry("player.notConnected", "Nicht verbunden"),
                    Map.entry("player.queueEmpty", "Warteschlange ist leer"),
                    Map.entry("player.pause", "Pause"),
                    Map.entry("player.resume", "Fortsetzen"),
                    Map.entry("player.skip", "Überspringen"),
                    Map.entry("player.stop", "Stopp"),
                    Map.entry("player.refresh", "Aktualisieren"),
                    Map.entry("player.volup", "Laut +"),
                    Map.entry("player.voldown", "Laut -"),
                    Map.entry("player.bassup", "Bass +"),
                    Map.entry("player.bassdown", "Bass -"),
                    Map.entry("player.bassreset", "Bass 0")
                );

                private static final Map<String, String> ES = Map.ofEntries(
                    Map.entry("status.waiting", "Esperando música"),
                    Map.entry("status.playing", "Cantando {0}"),
                    Map.entry("player.title", "Reproductor de música"),
                    Map.entry("player.hint", "Usa los botones de abajo o comandos con el prefijo {0}."),
                    Map.entry("player.footer", "Los comandos siguen en inglés. Ejemplo: {0}play <song>"),
                    Map.entry("player.status", "Estado"),
                    Map.entry("player.track", "Pista"),
                    Map.entry("player.voice", "Canal de voz"),
                    Map.entry("player.volume", "Volumen"),
                    Map.entry("player.bass", "Bajos"),
                    Map.entry("player.queuePreview", "Cola"),
                    Map.entry("player.state.idle", "En espera"),
                    Map.entry("player.state.playing", "Reproduciendo"),
                    Map.entry("player.state.paused", "Pausado"),
                    Map.entry("player.none", "No hay nada sonando"),
                    Map.entry("player.notConnected", "No conectado"),
                    Map.entry("player.queueEmpty", "La cola está vacía"),
                    Map.entry("player.pause", "Pausa"),
                    Map.entry("player.resume", "Continuar"),
                    Map.entry("player.skip", "Saltar"),
                    Map.entry("player.stop", "Detener"),
                    Map.entry("player.refresh", "Actualizar"),
                    Map.entry("player.volup", "Vol +"),
                    Map.entry("player.voldown", "Vol -"),
                    Map.entry("player.bassup", "Bajos +"),
                    Map.entry("player.bassdown", "Bajos -"),
                    Map.entry("player.bassreset", "Bajos 0")
                );

                private static final Map<String, String> IT = Map.ofEntries(
                    Map.entry("status.waiting", "In attesa di musica"),
                    Map.entry("status.playing", "Sta cantando {0}"),
                    Map.entry("player.title", "Lettore musicale"),
                    Map.entry("player.hint", "Usa i pulsanti sotto o i comandi con prefisso {0}."),
                    Map.entry("player.footer", "I comandi restano in inglese. Esempio: {0}play <song>"),
                    Map.entry("player.status", "Stato"),
                    Map.entry("player.track", "Traccia"),
                    Map.entry("player.voice", "Canale vocale"),
                    Map.entry("player.volume", "Volume"),
                    Map.entry("player.bass", "Bassi"),
                    Map.entry("player.queuePreview", "Coda"),
                    Map.entry("player.state.idle", "In attesa"),
                    Map.entry("player.state.playing", "In riproduzione"),
                    Map.entry("player.state.paused", "In pausa"),
                    Map.entry("player.none", "Nessun brano in riproduzione"),
                    Map.entry("player.notConnected", "Non connesso"),
                    Map.entry("player.queueEmpty", "La coda è vuota"),
                    Map.entry("player.pause", "Pausa"),
                    Map.entry("player.resume", "Riprendi"),
                    Map.entry("player.skip", "Salta"),
                    Map.entry("player.stop", "Stop"),
                    Map.entry("player.refresh", "Aggiorna"),
                    Map.entry("player.volup", "Vol +"),
                    Map.entry("player.voldown", "Vol -"),
                    Map.entry("player.bassup", "Bassi +"),
                    Map.entry("player.bassdown", "Bassi -"),
                    Map.entry("player.bassreset", "Bassi 0")
                );

                private static final Map<String, String> PT = Map.ofEntries(
                    Map.entry("status.waiting", "Aguardando música"),
                    Map.entry("status.playing", "Cantando {0}"),
                    Map.entry("player.title", "Reprodutor de música"),
                    Map.entry("player.hint", "Use os botões abaixo ou comandos com o prefixo {0}."),
                    Map.entry("player.footer", "Os comandos continuam em inglês. Exemplo: {0}play <song>"),
                    Map.entry("player.status", "Status"),
                    Map.entry("player.track", "Faixa"),
                    Map.entry("player.voice", "Canal de voz"),
                    Map.entry("player.volume", "Volume"),
                    Map.entry("player.bass", "Graves"),
                    Map.entry("player.queuePreview", "Fila"),
                    Map.entry("player.state.idle", "Inativo"),
                    Map.entry("player.state.playing", "Tocando"),
                    Map.entry("player.state.paused", "Pausado"),
                    Map.entry("player.none", "Nada tocando"),
                    Map.entry("player.notConnected", "Não conectado"),
                    Map.entry("player.queueEmpty", "A fila está vazia"),
                    Map.entry("player.pause", "Pausar"),
                    Map.entry("player.resume", "Retomar"),
                    Map.entry("player.skip", "Pular"),
                    Map.entry("player.stop", "Parar"),
                    Map.entry("player.refresh", "Atualizar"),
                    Map.entry("player.volup", "Vol +"),
                    Map.entry("player.voldown", "Vol -"),
                    Map.entry("player.bassup", "Graves +"),
                    Map.entry("player.bassdown", "Graves -"),
                    Map.entry("player.bassreset", "Graves 0")
                );

                private static final Map<String, String> ZH = Map.ofEntries(
                    Map.entry("status.waiting", "等待音乐"),
                    Map.entry("status.playing", "正在唱 {0}"),
                    Map.entry("player.title", "音乐播放器"),
                    Map.entry("player.hint", "使用下方按钮或带前缀 {0} 的命令。"),
                    Map.entry("player.footer", "命令仍为英文。示例：{0}play <song>"),
                    Map.entry("player.status", "状态"),
                    Map.entry("player.track", "曲目"),
                    Map.entry("player.voice", "语音频道"),
                    Map.entry("player.volume", "音量"),
                    Map.entry("player.bass", "低音"),
                    Map.entry("player.queuePreview", "队列"),
                    Map.entry("player.state.idle", "空闲"),
                    Map.entry("player.state.playing", "播放中"),
                    Map.entry("player.state.paused", "已暂停"),
                    Map.entry("player.none", "当前没有播放"),
                    Map.entry("player.notConnected", "未连接"),
                    Map.entry("player.queueEmpty", "队列为空"),
                    Map.entry("player.pause", "暂停"),
                    Map.entry("player.resume", "继续"),
                    Map.entry("player.skip", "跳过"),
                    Map.entry("player.stop", "停止"),
                    Map.entry("player.refresh", "刷新"),
                    Map.entry("player.volup", "音量 +"),
                    Map.entry("player.voldown", "音量 -"),
                    Map.entry("player.bassup", "低音 +"),
                    Map.entry("player.bassdown", "低音 -"),
                    Map.entry("player.bassreset", "低音 0")
                );

                private static final Map<String, String> JA = Map.ofEntries(
                    Map.entry("status.waiting", "音楽を待っています"),
                    Map.entry("status.playing", "{0} を歌っています"),
                    Map.entry("player.title", "音楽プレーヤー"),
                    Map.entry("player.hint", "下のボタン、またはプレフィックス {0} のコマンドを使ってください。"),
                    Map.entry("player.footer", "コマンドは英語のままです。例: {0}play <song>"),
                    Map.entry("player.status", "状態"),
                    Map.entry("player.track", "曲"),
                    Map.entry("player.voice", "ボイスチャンネル"),
                    Map.entry("player.volume", "音量"),
                    Map.entry("player.bass", "低音"),
                    Map.entry("player.queuePreview", "キュー"),
                    Map.entry("player.state.idle", "待機中"),
                    Map.entry("player.state.playing", "再生中"),
                    Map.entry("player.state.paused", "一時停止"),
                    Map.entry("player.none", "現在再生中の曲はありません"),
                    Map.entry("player.notConnected", "未接続"),
                    Map.entry("player.queueEmpty", "キューは空です"),
                    Map.entry("player.pause", "一時停止"),
                    Map.entry("player.resume", "再開"),
                    Map.entry("player.skip", "スキップ"),
                    Map.entry("player.stop", "停止"),
                    Map.entry("player.refresh", "更新"),
                    Map.entry("player.volup", "音量 +"),
                    Map.entry("player.voldown", "音量 -"),
                    Map.entry("player.bassup", "低音 +"),
                    Map.entry("player.bassdown", "低音 -"),
                    Map.entry("player.bassreset", "低音 0")
                );

    private final Language language;

    public I18n(String languageCode) {
        this.language = Language.from(languageCode);
    }

    public String code() {
        return language.code();
    }

    public String t(String key, Object... args) {
        Map<String, String> selected = switch (language) {
            case EN -> EN;
            case RU -> RU;
            case HY -> HY;
            case KA -> KA;
            case AZ -> AZ;
            case KK -> KK;
            case UZ -> UZ;
            case UK -> UK;
            case DE -> DE;
            case ES -> ES;
            case IT -> IT;
            case PT -> PT;
            case ZH -> ZH;
            case JA -> JA;
        };

        String pattern;
        if (language == Language.EN) {
            pattern = EN.getOrDefault(key, key);
        } else if (language == Language.RU) {
            pattern = RU.getOrDefault(key, EN.getOrDefault(key, key));
        } else {
            pattern = selected.getOrDefault(key, EN.getOrDefault(key, RU.getOrDefault(key, key)));
        }
        return MessageFormat.format(pattern, args);
    }
}
