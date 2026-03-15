package net.pixeldreamstudios.cthulib.util.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.pixeldreamstudios.cthulib.config.PromoMessageConfig;

import java.util.List;

public class PromoMessageHandler {

    public static void onJoinServer() {
        Minecraft mc = Minecraft.getInstance();
        ServerData server = mc.getCurrentServer();

        if (server == null) return;

        PromoMessageConfig config = PromoMessageConfig.get();
        String serverIp = server.ip.toLowerCase();

        for (PromoMessageConfig.ServerPromo promo : config.serverPromos) {
            boolean matches = promo.servers.stream()
                    .anyMatch(s -> serverIp.contains(s.toLowerCase()));

            if (matches) {
                Component message = buildMessage(promo.messageParts);
                mc.player.sendSystemMessage(message);
                break;
            }
        }
    }

    private static Component buildMessage(List<PromoMessageConfig.MessagePart> parts) {
        if (parts.isEmpty()) return Component.empty();

        Component message = Component.empty();

        for (PromoMessageConfig.MessagePart part : parts) {
            Style style = Style.EMPTY;

            if (! part.color.isEmpty()) {
                style = style.withColor(ChatFormatting.getByName(part.color));
            }

            if (part.bold) {
                style = style.withBold(true);
            }

            if (part.italic) {
                style = style.withItalic(true);
            }

            if (part.underlined) {
                style = style.withUnderlined(true);
            }

            if (!part.clickAction.isEmpty() && !part.clickValue.isEmpty()) {
                if (part.clickAction.equals("open_url")) {
                    style = style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, part.clickValue));
                } else if (part.clickAction.equals("run_command")) {
                    style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, part.clickValue));
                } else if (part.clickAction.equals("suggest_command")) {
                    style = style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, part.clickValue));
                }
            }

            message = message.copy().append(Component.literal(part.text).withStyle(style));
        }

        return message;
    }
}