package com.artem.musicbot;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.audio.factory.DefaultSendFactory;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.nio.file.Path;
import java.util.EnumSet;

public class ModernMusicBot {
    public static void main(String[] args) throws Exception {
        Path configPath = args.length > 0 ? Path.of(args[0]) : Path.of("ModernMusicBot.properties");
        BotConfig config = BotConfig.load(configPath);

        AudioModuleConfig audioModuleConfig = new AudioModuleConfig()
            .withDaveSessionFactory(new JDaveSessionFactory())
            .withAudioSendFactory(new DefaultSendFactory());

        MusicController musicController = new MusicController(config);

        JDABuilder.createDefault(config.token(), EnumSet.of(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.MESSAGE_CONTENT))
            .disableCache(
                CacheFlag.EMOJI,
                CacheFlag.STICKER,
                CacheFlag.SCHEDULED_EVENTS)
                .enableCache(CacheFlag.VOICE_STATE)
                .setMemberCachePolicy(MemberCachePolicy.VOICE)
                .setActivity(Activity.playing("Ждёт музыку"))
                .setStatus(OnlineStatus.ONLINE)
                .setAudioModuleConfig(audioModuleConfig)
                .addEventListeners(new CommandListener(config.prefix(), musicController))
                .build();
    }
}
