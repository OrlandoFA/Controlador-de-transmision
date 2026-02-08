package com.pvz.controller.tiktok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * Mapea regalos de TikTok a acciones del juego según el equipo.
 * Gifts no mapeados usan fallback por costo en diamantes.
 */
public class GiftMapper {

    private static final Logger logger = LoggerFactory.getLogger(GiftMapper.class);

    public record GiftAction(String plantType, String zombieType) {}

    private static final Map<Integer, GiftAction> GIFT_MAP = new TreeMap<>();

    private static final String[] TIER_PLANTS  = {"pea", "snow", "cherry", "melon", "gatling", "cob"};
    private static final String[] TIER_ZOMBIES = {"normal", "conehead", "buckethead", "football", "gargantuar", "giga"};
    private static final int[] TIER_MAX_COST   = {5, 49, 99, 499, 999, Integer.MAX_VALUE};

    static {
        // Tier 1: Gratis / Muy baratos (1-5 coins)
        GIFT_MAP.put(5655, new GiftAction("pea", "normal"));        // Rose
        GIFT_MAP.put(5658, new GiftAction("pea", "normal"));        // TikTok
        GIFT_MAP.put(5659, new GiftAction("sunflower", "normal"));  // Heart

        // Tier 2: Baratos (10-49 coins)
        GIFT_MAP.put(5487, new GiftAction("snow", "conehead"));      // Finger Heart
        GIFT_MAP.put(5827, new GiftAction("repeater", "conehead"));   // Perfume
        GIFT_MAP.put(5879, new GiftAction("wallnut", "conehead"));    // Doughnut

        // Tier 3: Medio (50-99 coins)
        GIFT_MAP.put(5885, new GiftAction("cherry", "buckethead"));   // Cap
        GIFT_MAP.put(5895, new GiftAction("squash", "buckethead"));   // Hand Heart
        GIFT_MAP.put(5900, new GiftAction("jalapeno", "buckethead")); // Love you

        // Tier 4: Caros (100-499 coins)
        GIFT_MAP.put(5835, new GiftAction("torch", "football"));      // Sunglasses
        GIFT_MAP.put(5839, new GiftAction("melon", "football"));      // Lollipop
        GIFT_MAP.put(5890, new GiftAction("gatling", "gargantuar"));  // Bear

        // Tier 5: Premium (500+ coins)
        GIFT_MAP.put(5876, new GiftAction("cob", "giga"));           // GG
        GIFT_MAP.put(5845, new GiftAction("winter", "gargantuar"));  // Star
        GIFT_MAP.put(5878, new GiftAction("cob", "giga"));           // Lion
        GIFT_MAP.put(5860, new GiftAction("cob", "giga"));           // Drama Queen
        GIFT_MAP.put(5861, new GiftAction("cob", "giga"));           // Universe
    }

    /**
     * Resuelve un regalo a un tipo de acción según el equipo
     */
    public static String resolve(int giftId, int diamondCost, TeamManager.Team team) {
        GiftAction action = GIFT_MAP.get(giftId);

        if (action != null) {
            return team == TeamManager.Team.PLANTAS ? action.plantType : action.zombieType;
        }

        // Fallback por costo
        for (int i = 0; i < TIER_MAX_COST.length; i++) {
            if (diamondCost <= TIER_MAX_COST[i]) {
                String type = team == TeamManager.Team.PLANTAS ? TIER_PLANTS[i] : TIER_ZOMBIES[i];
                logger.debug("Gift id:{} ({}💎) → fallback tier {} → {}", giftId, diamondCost, i, type);
                return type;
            }
        }

        return team == TeamManager.Team.PLANTAS ? "cob" : "giga";
    }

    /**
     * Guía de mapeo para mostrar en consola
     */
    public static String getGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n🎁 Guía de Regalos → Acciones:\n\n");
        sb.append(String.format("  %-8s │ %-15s │ %-15s%n", "GiftID", "Team 🌱", "Team 🧟"));
        sb.append("  ─────────┼─────────────────┼─────────────────\n");

        for (var entry : GIFT_MAP.entrySet()) {
            sb.append(String.format("  %-8d │ %-15s │ %-15s%n",
                    entry.getKey(), entry.getValue().plantType, entry.getValue().zombieType));
        }

        sb.append("\n  Fallback por 💎: 1-5→pea/normal, 6-49→snow/conehead,");
        sb.append("\n  50-99→cherry/buckethead, 100-499→melon/football,");
        sb.append("\n  500-999→gatling/gargantuar, 1000+→cob/giga\n");

        return sb.toString();
    }
}