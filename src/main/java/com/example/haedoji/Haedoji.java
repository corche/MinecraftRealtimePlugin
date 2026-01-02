package com.example.haedoji;
import net.kyori.adventure.text.minimessage.MiniMessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;

public class Haedoji extends JavaPlugin {
    // 시간 포맷 설정 (예: 14:30:05)
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void onEnable() {
        getLogger().info("Haedoji 플러그인이 시작되었습니다!");
        startSyncTask();
    }

    private void startSyncTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long calculatedTime = calculateMinecraftTime();
                String timeString = LocalTime.now().format(timeFormatter);

                applyTimeToWorlds(calculatedTime);
                updateTabList(timeString);
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    /**
     * 현실 시간을 마인크래프트 틱(0~24000)으로 변환하는 로직 (메서드 추출)
     */
    private long calculateMinecraftTime() {
        LocalTime now = LocalTime.now();
        double hour = now.getHour() + (now.getMinute() / 60.0) + (now.getSecond() / 3600.0);

        // 기준점 설정 (시간 단위)
        double realSunrise = 7.66;  // 07:40
        double realSunset = 18.83; // 18:50 (6시 50분)

        // 마크 기준점 (틱 단위)
        double mcSunrise = 22900; // 일출 시작
        double mcSunset = 13000;  // 일몰 완료 (해가 완전히 지는 틱)

        double finalTick;

        if (hour >= realSunrise && hour < realSunset) {
            // 1. 낮 시간대 (07:40 ~ 18:50) -> 마크의 22900틱부터 다음날 13000틱까지 흐르게 함
            // 마크 낮의 총 길이 계산 (22900 -> 24000 -> 13000 = 총 14100틱 흐름)
            double realDayLength = realSunset - realSunrise;
            double mcDayLength = 14100;

            finalTick = mcSunrise + ((hour - realSunrise) / realDayLength) * mcDayLength;
        } else {
            // 2. 밤 시간대 (18:50 ~ 다음날 07:40) -> 나머지 9900틱을 분배
            double realNightLength = 24 - (realSunset - realSunrise);
            double mcNightLength = 9900;

            double timePassed;
            if (hour >= realSunset) {
                timePassed = hour - realSunset;
            } else {
                timePassed = (24 - realSunset) + hour;
            }

            finalTick = mcSunset + (timePassed / realNightLength) * mcNightLength;
        }

        return (long) (finalTick % 24000);
    }

    /**
     * 계산된 시간을 월드에 적용하는 로직
     */
    private void applyTimeToWorlds(long time) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {

                world.setTime(time);
            }
        }
    }

    private void updateTabList(String timeString) {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();

        // Adventure API를 사용하여 색상이 들어간 텍스트 생성
//        Component header = Component.text("HaeDoJi Server")
//                .color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD)
//                .append(Component.newline());
//
//        Component footer = Component.newline()
//                .append(Component.text("실제 시간: "))
//                .append(Component.text(timeString).color(NamedTextColor.GREEN))
//                .append(Component.newline()) // 줄바꿈 추가
//                .append(Component.text("접속자 수: ").color(NamedTextColor.DARK_GRAY))
//                .append(Component.text(onlinePlayers).color(NamedTextColor.GRAY))
//                .append(Component.text(" / " + maxPlayers).color(NamedTextColor.DARK_GRAY));
//
//        for (Player player : Bukkit.getOnlinePlayers()) {
//            player.sendPlayerListHeaderAndFooter(header, footer);
//        }

        // MiniMessage 인스턴스 생성
        MiniMessage mm = MiniMessage.miniMessage();

        Component header = mm.deserialize(
                "<newline><gray>  === </gray><gradient:#DB5A2B:#0059FF><bold>2026 HaeDoji Server</bold></gradient><gray> ===  </gray><newline>"
        );

        Component footer = mm.deserialize(
                "<newline>" +
                        "<gray>시간: <green>" + timeString + "<newline>" +
                        "<gray>접속자: <gray>" + onlinePlayers + "</gray><dark_gray>/" + maxPlayers + "<newline>"
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(header, footer);
        }
    }
}