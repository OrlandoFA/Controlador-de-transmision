package com.pvz.controller.memory;

import java.util.HashMap;
import java.util.Map;

/**
 * Memory offsets para Plants vs Zombies 1.2.0.1096 GOTY Steam
 * IDs corregidos según PvZ Toolkit (pvz.h)
 */
public class PvZOffsets {

    // ==================== BASE ADDRESSES ====================
    public static final long[] BASE_ADDRESSES = {
            0x731C50,  // PvZ 1.2.0.1096 GOTY Steam (PRINCIPAL)
            0x6A9EC0,  // PvZ Original 1.0.0.1051
            0x6A9F38,  // Otras versiones
    };

    // ==================== STRUCTURE OFFSETS ====================
    public static final int BOARD = 0x868;

    // ==================== BOARD OFFSETS ====================
    public static final int SUN_COUNT = 0x5578;
    public static final int GAME_PAUSED = 0x17C;
    public static final int ZOMBIE_COUNT = 0xAC;
    public static final int GAME_CLOCK = 0x5580;
    public static final int CHALLENGE = 0x178;

    public static final int SCENE = 0x5564;
    public static final int ROW_TYPE = 0x5F0;
    public static final int ADVENTURE_LEVEL = 0x5568;

    // ==================== SCENE TYPES ====================
    public static final int SCENE_DAY = 0;
    public static final int SCENE_NIGHT = 1;
    public static final int SCENE_POOL = 2;
    public static final int SCENE_FOG = 3;
    public static final int SCENE_ROOF = 4;
    public static final int SCENE_MOON = 5;

    // ==================== ROW TYPES ====================
    public static final int ROW_NORMAL = 0;
    public static final int ROW_POOL = 1;
    public static final int ROW_HIGH = 2;

    // ==================== FUNCTION ADDRESSES ====================
    public static final int CALL_PUT_ZOMBIE = 0x0042DCE0;
    public static final int CALL_PUT_ZOMBIE_IN_ROW = 0x00411290;
    public static final int CALL_PUT_PLANT = 0x004105A0;

    // ==================== SCENE HELPERS ====================

    public static int getRowCountForScene(int scene) {
        switch (scene) {
            case SCENE_POOL:
            case SCENE_FOG:
                return 6;
            default:
                return 5;
        }
    }

    public static String getSceneName(int scene) {
        switch (scene) {
            case SCENE_DAY: return "Day";
            case SCENE_NIGHT: return "Night";
            case SCENE_POOL: return "Pool";
            case SCENE_FOG: return "Fog";
            case SCENE_ROOF: return "Roof";
            case SCENE_MOON: return "Moon";
            default: return "Unknown";
        }
    }

    public static boolean hasPool(int scene) {
        return scene == SCENE_POOL || scene == SCENE_FOG;
    }

    // ==================== ROW HELPERS ====================

    public static int rowLetterToIndex(char letter) {
        char upper = Character.toUpperCase(letter);
        if (upper >= 'A' && upper <= 'F') {
            return upper - 'A';
        }
        return -1;
    }

    public static char rowIndexToLetter(int index) {
        if (index >= 0 && index <= 5) {
            return (char) ('A' + index);
        }
        return '?';
    }

    // ==================== ZOMBIE TYPES ====================
    private static final Map<String, Integer> ZOMBIE_NAMES = new HashMap<>();
    private static final Map<Integer, String> ZOMBIE_IDS = new HashMap<>();

    static {
        addZombie(0, "Normal", "normal", "zombie", "regular");
        addZombie(1, "Flag", "flag", "bandera");
        addZombie(2, "Conehead", "cone", "cono", "conehead");
        addZombie(3, "Pole Vaulting", "pole", "pertiga", "polevaulting", "vaulting");
        addZombie(4, "Buckethead", "bucket", "cubeta", "buckethead");
        addZombie(5, "Newspaper", "newspaper", "periodico", "news");
        addZombie(6, "Screen Door", "screendoor", "screen", "door", "puerta");
        addZombie(7, "Football", "football", "futbol", "americano");
        addZombie(8, "Dancing", "dancing", "dancer", "michael", "bailarin");
        addZombie(9, "Backup Dancer", "backup", "dancer_backup", "corista");
        addZombie(10, "Ducky Tube", "ducky", "duck", "tube", "flotador");
        addZombie(11, "Snorkel", "snorkel", "buzo");
        addZombie(12, "Zomboni", "zomboni", "zamboni");
        addZombie(13, "Zombie Bobsled Team", "bobsled", "trineo");
        addZombie(14, "Dolphin Rider", "dolphin", "delfin");
        addZombie(15, "Jack-in-the-Box", "jack", "jackinbox", "caja");
        addZombie(16, "Balloon", "balloon", "globo");
        addZombie(17, "Digger", "digger", "minero", "excavador");
        addZombie(18, "Pogo", "pogo", "saltarin");
        addZombie(19, "Zombie Yeti", "yeti", "abominable");
        addZombie(20, "Bungee", "bungee", "elastico");
        addZombie(21, "Ladder", "ladder", "escalera");
        addZombie(22, "Catapult", "catapult", "catapulta", "basketball");
        addZombie(23, "Gargantuar", "gargantuar", "garga", "gigante");
        addZombie(24, "Imp", "imp", "duende", "pequeño");
        addZombie(25, "Dr. Zomboss", "zomboss", "boss", "doctor");
        addZombie(26, "Peashooter Zombie", "peashooter_zombie", "pea_zombie");
        addZombie(27, "Wall-nut Zombie", "wallnut_zombie", "wall_zombie");
        addZombie(28, "Jalapeno Zombie", "jalapeno_zombie", "jala_zombie");
        addZombie(29, "Gatling Pea Zombie", "gatling_zombie", "gatling_pea_zombie");
        addZombie(30, "Squash Zombie", "squash_zombie");
        addZombie(31, "Tall-nut Zombie", "tallnut_zombie", "tall_zombie");
        addZombie(32, "GigaGargantuar", "giga", "gigagargantuar", "giga_garga");
    }

    private static void addZombie(int id, String displayName, String... aliases) {
        ZOMBIE_IDS.put(id, displayName);
        for (String alias : aliases) {
            ZOMBIE_NAMES.put(alias.toLowerCase(), id);
        }
        ZOMBIE_NAMES.put(displayName.toLowerCase(), id);
        ZOMBIE_NAMES.put(String.valueOf(id), id);
    }

    public static int getZombieTypeId(String name) {
        if (name == null || name.isEmpty()) {
            return 0;
        }
        String key = name.toLowerCase().trim();
        if (ZOMBIE_NAMES.containsKey(key)) {
            return ZOMBIE_NAMES.get(key);
        }
        try {
            int id = Integer.parseInt(name);
            if (id >= 0 && id <= 32) {
                return id;
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    public static String getZombieName(int id) {
        return ZOMBIE_IDS.getOrDefault(id, "Unknown");
    }

    // ==================== PLANT TYPES ====================
    // IDs CORREGIDOS según PvZ Toolkit oficial
    // ==================== PLANT TYPES ====================
    private static final Map<String, Integer> PLANT_NAMES = new HashMap<>();
    private static final Map<Integer, String> PLANT_IDS = new HashMap<>();
    private static final Map<Integer, String> PLANT_EMOJIS = new HashMap<>();

    static {
        // ID 0-9
        addPlant(0, "🌱", "Peashooter", "pea", "guisante", "lanzaguisantes");
        addPlant(1, "🌻", "Sunflower", "sun", "girasol", "sunflower");
        addPlant(2, "🍒", "Cherry Bomb", "cherry", "cereza", "cherrybomb");
        addPlant(3, "🥜", "Wall-nut", "wall", "nuez", "wallnut");
        addPlant(4, "🥔", "Potato Mine", "potato", "patata", "mina", "potatomine");
        addPlant(5, "❄️", "Snow Pea", "snow", "nieve", "snowpea");
        addPlant(6, "🦷", "Chomper", "chomper", "carnivora", "mordedor");
        addPlant(7, "🔁", "Repeater", "repeat", "repeater", "repetidor");
        addPlant(8, "🍄", "Puff-shroom", "puff", "puffshroom", "setapequeña");
        addPlant(9, "☀️", "Sun-shroom", "sunshroom", "setasol");

        // ID 10-19
        addPlant(10, "💨", "Fume-shroom", "fume", "fumeshroom", "humo");
        addPlant(11, "🪦", "Grave Buster", "grave", "gravebuster", "tumba", "rompetumbas");
        addPlant(12, "🌀", "Hypno-shroom", "hypno", "hypnoshroom", "hipno");
        addPlant(13, "😰", "Scaredy-shroom", "scaredy", "scaredyshroom", "miedosa");
        addPlant(14, "🧊", "Ice-shroom", "iceshroom", "hielo", "setahielo");
        addPlant(15, "💥", "Doom-shroom", "doom", "doomshroom", "atomica");
        addPlant(16, "🌸", "Lily Pad", "lily", "lilypad", "nenufar");
        addPlant(17, "🎃", "Squash", "squash", "aplastador");
        addPlant(18, "🌺", "Threepeater", "three", "threepeater", "triple");
        addPlant(19, "🌿", "Tangle Kelp", "tangle", "tanglekelp", "alga");

        // ID 20-29
        addPlant(20, "🌶️", "Jalapeno", "jala", "jalapeno", "chile");
        addPlant(21, "📌", "Spikeweed", "spikeweed", "pincho");
        addPlant(22, "🔥", "Torchwood", "torch", "torchwood", "antorcha");
        addPlant(23, "🥜", "Tall-nut", "tall", "tallnut", "nuezalta");
        addPlant(24, "🌊", "Sea-shroom", "sea", "seashroom", "setamar");
        addPlant(25, "🔦", "Plantern", "plantern", "linterna");
        addPlant(26, "🌵", "Cactus", "cactus");
        addPlant(27, "🍀", "Blover", "blover", "trebol");
        addPlant(28, "↔️", "Split Pea", "split", "splitpea", "biguisante");
        addPlant(29, "⭐", "Starfruit", "star", "starfruit", "estrella");

        // ID 30-39 (CORREGIDO - Flower Pot es 33)
        addPlant(30, "🎃", "Pumpkin", "pumpkin", "calabaza");
        addPlant(31, "🧲", "Magnet-shroom", "magnet", "magnetshroom", "iman");
        addPlant(32, "🥬", "Cabbage-pult", "cabbage", "cabbagepult", "col");
        addPlant(33, "🪴", "Flower Pot", "pot", "flowerpot", "maceta");
        addPlant(34, "🌽", "Kernel-pult", "kernel", "kernelpult", "maiz");
        addPlant(35, "☕", "Coffee Bean", "coffee", "coffeebean", "cafe");
        addPlant(36, "🧄", "Garlic", "garlic", "ajo");
        addPlant(37, "☂️", "Umbrella Leaf", "umbrella", "umbrellaleaf", "paraguas");
        addPlant(38, "🌼", "Marigold", "marigold", "calendula");
        addPlant(39, "🍈", "Melon-pult", "melon", "melonpult", "lanzamelon");

        // ID 40-48 (CORREGIDO)
        addPlant(40, "🔫", "Gatling Pea", "gatling", "gatlingpea", "metralleta");
        addPlant(41, "🌻", "Twin Sunflower", "twin", "twinsunflower", "girasoldoble");
        addPlant(42, "☠️", "Gloom-shroom", "gloom", "gloomshroom", "sombria");
        addPlant(43, "🐱", "Cattail", "cattail", "gatuna");
        addPlant(44, "🥶", "Winter Melon", "winter", "wintermelon", "meloninvierno");
        addPlant(45, "💰", "Gold Magnet", "gold", "goldmagnet", "imanoro");
        addPlant(46, "🪨", "Spikerock", "spikerock", "rocapuas");
        addPlant(47, "🌽", "Cob Cannon", "cob", "cobcannon", "mazorca", "cannon");
        addPlant(48, "🎭", "Imitater", "imitater", "imitador");
    }

    private static void addPlant(int id, String emoji, String displayName, String... aliases) {
        PLANT_IDS.put(id, displayName);
        PLANT_EMOJIS.put(id, emoji);
        for (String alias : aliases) {
            PLANT_NAMES.put(alias.toLowerCase(), id);
        }
        PLANT_NAMES.put(displayName.toLowerCase(), id);
        PLANT_NAMES.put(String.valueOf(id), id);
    }

    public static int getPlantTypeId(String name) {
        if (name == null || name.isEmpty()) {
            return -1;
        }
        String key = name.toLowerCase().trim();
        if (PLANT_NAMES.containsKey(key)) {
            return PLANT_NAMES.get(key);
        }
        try {
            int id = Integer.parseInt(name);
            if (id >= 0 && id <= 48) {
                return id;
            }
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    public static String getPlantName(int id) {
        return PLANT_IDS.getOrDefault(id, "Unknown");
    }

    public static String getPlantEmoji(int id) {
        return PLANT_EMOJIS.getOrDefault(id, "🌱");
    }

    // ==================== DISCORD MENUS ====================

    public static String getPlantMenu() {
        StringBuilder sb = new StringBuilder();
        sb.append("🌱 **PLANTAS DISPONIBLES** 🌱\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("**🎯 Ataque:**\n");
        sb.append("🌱 `pea` - Lanzaguisantes | 🔁 `repeat` - Repetidora\n");
        sb.append("🌺 `three` - Tresguisantes | ↔️ `split` - Bíguisante\n");
        sb.append("🔫 `gatling` - Metralleta | ❄️ `snow` - Lanzanieve\n\n");

        sb.append("**☀️ Sol:**\n");
        sb.append("🌻 `sun` - Girasol | 🌻 `twin` - Girasol Doble\n");
        sb.append("☀️ `sunshroom` - Seta Solar | 🌼 `marigold` - Caléndula\n\n");

        sb.append("**🛡️ Defensa:**\n");
        sb.append("🥜 `wall` - Nuez | 🥜 `tall` - Nuez Alta\n");
        sb.append("🎃 `pumpkin` - Calabaza\n\n");

        sb.append("**💥 Explosivas:**\n");
        sb.append("🍒 `cherry` - Cereza | 🥔 `potato` - Patata Mina\n");
        sb.append("🌶️ `jala` - Jalapeño | 💥 `doom` - Seta Atómica\n");
        sb.append("🎃 `squash` - Aplastador\n\n");

        sb.append("**🍄 Setas:**\n");
        sb.append("🍄 `puff` - Seta Pequeña | 💨 `fume` - Seta Humo\n");
        sb.append("🌀 `hypno` - Seta Hipnótica | 😰 `scaredy` - Seta Miedosa\n");
        sb.append("🧊 `iceshroom` - Seta Hielo | ☠️ `gloom` - Seta Sombría\n");
        sb.append("🧲 `magnet` - Seta Magnética | 💰 `gold` - Imán de Oro\n\n");

        sb.append("**🎯 Catapultas:**\n");
        sb.append("🥬 `cabbage` - Lanzacoles | 🌽 `kernel` - Lanzamazorcas\n");
        sb.append("🍈 `melon` - Lanzamelones | 🥶 `winter` - Melón Invernal\n");
        sb.append("🌽 `cob` - Mazorcañón\n\n");

        sb.append("**🌊 Acuáticas:**\n");
        sb.append("🌸 `lily` - Nenúfar | 🌿 `tangle` - Alga\n");
        sb.append("🌊 `sea` - Seta Marina | 🐱 `cattail` - Gatuna\n\n");

        sb.append("**🔧 Utilidad:**\n");
        sb.append("🦷 `chomper` - Planta Carnívora | 🪦 `grave` - Rompetumbas\n");
        sb.append("📌 `spikeweed` - Pincho | 🪨 `spikerock` - Rocapúas\n");
        sb.append("🔥 `torch` - Antorcha | 🔦 `plantern` - Planterna\n");
        sb.append("🌵 `cactus` - Cactus | 🍀 `blover` - Trébol\n");
        sb.append("⭐ `star` - Frutestrella | ☕ `coffee` - Café\n");
        sb.append("🧄 `garlic` - Ajo | ☂️ `umbrella` - Sombrilla\n");
        sb.append("🪴 `pot` - Maceta | 🎭 `imitater` - Imitador\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("**Uso:** `!plant [planta] [fila] [col]`\n");
        sb.append("**Ejemplo:** `!plant sun A 1`");

        return sb.toString();
    }

    public static String getZombieMenu() {
        StringBuilder sb = new StringBuilder();
        sb.append("🧟 **ZOMBIES DISPONIBLES** 🧟\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("**🚶 Básicos:**\n");
        sb.append("🧟 `normal` - Zombie Normal\n");
        sb.append("🚩 `flag` - Abanderado\n");
        sb.append("🔶 `cone` - Con Cono\n");
        sb.append("🪣 `bucket` - Con Cubeta\n\n");

        sb.append("**🏃 Especiales:**\n");
        sb.append("🏃 `pole` - Pértiga\n");
        sb.append("📰 `newspaper` - Periódico\n");
        sb.append("🚪 `screen` - Mosquitero\n");
        sb.append("🏈 `football` - Americano\n\n");

        sb.append("**💀 Peligrosos:**\n");
        sb.append("🎸 `dancing` - Bailarín\n");
        sb.append("🎁 `jack` - Payaso\n");
        sb.append("🎈 `balloon` - Globo\n");
        sb.append("⛏️ `digger` - Minero\n");
        sb.append("🦘 `pogo` - Saltador\n\n");

        sb.append("**🌊 Acuáticos:**\n");
        sb.append("🦆 `ducky` - Flotador\n");
        sb.append("🤿 `snorkel` - Snorkel\n");
        sb.append("🐬 `dolphin` - Delfín\n\n");

        sb.append("**🚗 Vehículos:**\n");
        sb.append("🚗 `zomboni` - Zomboni\n");
        sb.append("🛷 `bobsled` - Trineo\n");
        sb.append("🏀 `catapult` - Catapulta\n\n");

        sb.append("**⚔️ Élite:**\n");
        sb.append("🪜 `ladder` - Escalera\n");
        sb.append("🪂 `bungee` - Bungee\n");
        sb.append("❄️ `yeti` - Yeti\n\n");

        sb.append("**👹 Jefes:**\n");
        sb.append("👹 `gargantuar` - Gargantúa\n");
        sb.append("👿 `imp` - Duendecillo\n");
        sb.append("💀 `giga` - Giga-Gargantúa\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("**Uso:** `!spawn [zombie] [cantidad]`\n");
        sb.append("**Ejemplo:** `!spawn gargantuar 3`");

        return sb.toString();
    }
}