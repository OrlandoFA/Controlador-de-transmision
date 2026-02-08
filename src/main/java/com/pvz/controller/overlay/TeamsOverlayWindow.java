package com.pvz.controller.overlay;

import com.pvz.controller.tiktok.TeamManager;
import com.pvz.controller.tiktok.TeamManager.Team;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ventana overlay independiente para mostrar equipos + ticker de instrucciones.
 * Sin bordes, arrastrable, always on top.
 */
public class TeamsOverlayWindow extends JFrame {

    private final TeamManager teamManager;
    private final TeamsPanel teamsPanel;
    private Timer refreshTimer;

    private Point dragOffset;

    // Colores
    private static final Color BG_DARK = new Color(0, 0, 0, 190);
    private static final Color BORDER_BROWN = new Color(74, 55, 40);
    private static final Color HEADER_BG = new Color(58, 37, 16);
    private static final Color HEADER_GOLD = new Color(255, 215, 0);
    private static final Color PLANTAS_HEADER_BG = new Color(45, 107, 30);
    private static final Color PLANTAS_TEXT = new Color(168, 230, 160);
    private static final Color PLANTAS_ACCENT = new Color(127, 255, 0);
    private static final Color ZOMBIES_HEADER_BG = new Color(90, 26, 94);
    private static final Color ZOMBIES_TEXT = new Color(216, 168, 224);
    private static final Color ZOMBIES_ACCENT = new Color(218, 112, 214);
    private static final Color FOOTER_BG = new Color(42, 24, 8);
    private static final Color BADGE_BG = new Color(255, 255, 255, 38);
    private static final Color SEPARATOR = new Color(74, 55, 40);
    private static final Color ROW_ALT = new Color(255, 255, 255, 8);
    private static final Color TICKER_BG = new Color(30, 15, 5);
    private static final Color TICKER_TEXT = new Color(255, 200, 60);
    private static final Color TICKER_HIGHLIGHT = new Color(255, 100, 100);
    private static final Color TICKER_GREEN = new Color(100, 255, 100);
    private static final Color TICKER_PURPLE = new Color(200, 140, 255);

    private static final int WIDTH = 320;
    private static final int HEIGHT = 560;

    // Ticker de instrucciones
    private static final String[] TICKER_MESSAGES = {
            "GRATIS >> Escribe P o Z para unirte a un equipo!",
            "PLANTAS >> Escribe A1-F9 para plantar un Lanzaguisantes!",
            "ZOMBIES >> Escribe A-F para enviar un Zombie!",
            "Rosa / TikTok >> Plantas: Peashooter | Zombies: Normal",
            "Corazon >> Plantas: Sunflower | Zombies: Normal",
            "Finger Heart >> Plantas: Snow Pea | Zombies: Conehead",
            "Perfume >> Plantas: Repeater | Zombies: Conehead",
            "Dona >> Plantas: Wall-nut | Zombies: Conehead",
            "Gorra >> Plantas: Cherry Bomb | Zombies: Buckethead",
            "Mano Corazon >> Plantas: Squash | Zombies: Buckethead",
            "Love you >> Plantas: Jalapeno | Zombies: Buckethead",
            "Lentes >> Plantas: Torchwood | Zombies: Football",
            "Paleta >> Plantas: Melon-pult | Zombies: Football",
            "Oso >> Plantas: Gatling Pea | Zombies: Gargantuar",
            "GG / Lion >> Plantas: Cob Cannon | Zombies: GIGA!!",
            "FOLLOW >> +100 Sol para el equipo!",
            "COMPARTIR >> +200 Sol para el equipo!",
            "LIKES >> Cada 5 min se activa planta o zombie aleatorio!",
    };

    public TeamsOverlayWindow(TeamManager teamManager) {
        this.teamManager = teamManager;

        setTitle("PvZ Teams Overlay");
        setUndecorated(true);
        setAlwaysOnTop(true);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));

        teamsPanel = new TeamsPanel();
        setContentPane(teamsPanel);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset != null) {
                    Point location = getLocation();
                    setLocation(
                            location.x + e.getX() - dragOffset.x,
                            location.y + e.getY() - dragOffset.y
                    );
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    close();
                }
            }
        });

        refreshTimer = new Timer(3000, e -> teamsPanel.repaint());
        refreshTimer.start();
    }

    public void close() {
        refreshTimer.stop();
        dispose();
    }

    // ═══════════════════════════════════════════════════════════
    // PANEL PRINCIPAL
    // ═══════════════════════════════════════════════════════════

    class TeamsPanel extends JPanel {

        private int scrollOffsetPlantas = 0;
        private int scrollOffsetZombies = 0;
        private int tickerOffset = 0;
        private int currentTickerMsg = 0;
        private int tickerPause = 0;
        private Timer animTimer;

        TeamsPanel() {
            setOpaque(false);

            animTimer = new Timer(50, e -> {
                scrollOffsetPlantas++;
                scrollOffsetZombies++;

                // Ticker animation
                if (tickerPause > 0) {
                    tickerPause--;
                } else {
                    tickerOffset += 2;
                }

                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();
            int r = 16;

            // ── Fondo ──
            g2.setColor(BG_DARK);
            g2.fillRoundRect(0, 0, w, h, r, r);

            // ── Borde ──
            g2.setColor(BORDER_BROWN);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(1, 1, w - 3, h - 3, r, r);

            // ── Header "EQUIPOS" ──
            int headerH = 38;
            g2.setColor(HEADER_BG);
            g2.fillRoundRect(2, 2, w - 4, headerH, r, r);
            g2.fillRect(2, headerH - 5, w - 4, 10);

            g2.setColor(BORDER_BROWN);
            g2.fillRect(2, headerH, w - 4, 3);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
            g2.setColor(HEADER_GOLD);
            String title = "EQUIPOS";
            FontMetrics fm = g2.getFontMetrics();
            int titleX = (w - fm.stringWidth(title)) / 2;
            g2.drawString(title, titleX, headerH - 12);

            // Espadas decorativas
            drawSword(g2, titleX - 22, headerH - 24, false);
            drawSword(g2, titleX + fm.stringWidth(title) + 6, headerH - 24, true);

            int y = headerH + 3;

            // ── Obtener datos ──
            List<PlayerEntry> plantas = new ArrayList<>();
            List<PlayerEntry> zombies = new ArrayList<>();
            collectPlayers(plantas, zombies);

            int halfW = (w - 3) / 2;
            int teamHeaderH = 30;

            // ── Team Plantas header ──
            g2.setColor(PLANTAS_HEADER_BG);
            g2.fillRect(2, y, halfW, teamHeaderH);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(PLANTAS_ACCENT);
            drawPlantIcon(g2, 8, y + 7, 14);
            g2.drawString("PLANTAS", 26, y + 20);

            drawBadge(g2, String.valueOf(plantas.size()), 2 + halfW - 35, y + 6, PLANTAS_ACCENT);

            // ── Team Zombies header ──
            int zx = 2 + halfW + 3;
            g2.setColor(ZOMBIES_HEADER_BG);
            g2.fillRect(zx, y, halfW, teamHeaderH);

            g2.setColor(ZOMBIES_ACCENT);
            drawZombieIcon(g2, zx + 6, y + 7, 14);
            g2.drawString("ZOMBIES", zx + 24, y + 20);

            drawBadge(g2, String.valueOf(zombies.size()), zx + halfW - 35, y + 6, ZOMBIES_ACCENT);

            // ── Separador vertical ──
            g2.setColor(SEPARATOR);
            g2.fillRect(2 + halfW, y, 3, h - y - 90);

            // ── Separador bajo headers ──
            y += teamHeaderH;
            g2.setColor(new Color(255, 255, 255, 25));
            g2.fillRect(2, y, w - 4, 1);
            y += 1;

            // ── Área de nombres ──
            int tickerAreaH = 62;
            int listAreaH = h - y - tickerAreaH - 2;
            int rowH = 26;

            Shape oldClip = g2.getClip();

            g2.setClip(2, y, halfW, listAreaH);
            drawPlayerList(g2, plantas, 2, y, halfW, listAreaH, rowH,
                    PLANTAS_TEXT, true, scrollOffsetPlantas);

            g2.setClip(zx, y, halfW, listAreaH);
            drawPlayerList(g2, zombies, zx, y, halfW, listAreaH, rowH,
                    ZOMBIES_TEXT, false, scrollOffsetZombies);

            g2.setClip(oldClip);

            // ── Ticker de instrucciones ──
            int tickerY = h - tickerAreaH;
            drawTicker(g2, 2, tickerY, w - 4, tickerAreaH, r);

            g2.dispose();
        }

        // ── ICONOS CUSTOM ──

        private void drawPlantIcon(Graphics2D g2, int x, int y, int size) {
            Color old = g2.getColor();
            // Tallo
            g2.setColor(new Color(60, 140, 30));
            g2.fillRect(x + size / 2 - 1, y + size / 2, 3, size / 2);
            // Cabeza (hoja)
            g2.setColor(new Color(80, 200, 50));
            g2.fillOval(x + 1, y, size - 2, size / 2 + 3);
            // Detalle
            g2.setColor(new Color(120, 240, 80));
            g2.fillOval(x + 3, y + 2, size / 3, size / 4);
            g2.setColor(old);
        }

        private void drawZombieIcon(Graphics2D g2, int x, int y, int size) {
            Color old = g2.getColor();
            // Cabeza
            g2.setColor(new Color(120, 160, 100));
            g2.fillOval(x + 2, y, size - 4, size - 2);
            // Ojos
            g2.setColor(new Color(200, 50, 50));
            g2.fillOval(x + 4, y + 3, 3, 3);
            g2.fillOval(x + size - 8, y + 3, 3, 3);
            // Boca
            g2.setColor(new Color(80, 40, 40));
            g2.drawLine(x + 4, y + size - 5, x + size - 5, y + size - 5);
            g2.setColor(old);
        }

        private void drawSword(Graphics2D g2, int x, int y, boolean flip) {
            Color old = g2.getColor();
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(2));

            int dir = flip ? -1 : 1;
            // Hoja
            g2.setColor(new Color(200, 200, 220));
            g2.drawLine(x + (flip ? 14 : 0), y + 4, x + (flip ? 0 : 14), y + 18);
            // Guardia
            g2.setColor(HEADER_GOLD);
            g2.drawLine(x + (flip ? 2 : 5), y + 15, x + (flip ? 12 : 9), y + 15);
            // Mango
            g2.setColor(new Color(139, 90, 43));
            g2.drawLine(x + 7, y + 16, x + 7, y + 22);

            g2.setStroke(oldStroke);
            g2.setColor(old);
        }

        private void drawLightning(Graphics2D g2, int x, int y, Color color) {
            Color old = g2.getColor();
            g2.setColor(color);
            int[] xp = {x + 4, x + 2, x + 5, x + 3, x + 7, x + 5, x + 8};
            int[] yp = {y, y + 4, y + 4, y + 8, y + 4, y + 4, y};
            g2.fillPolygon(xp, yp, 7);
            g2.setColor(old);
        }

        // ── PLAYER LIST ──

        private void drawPlayerList(Graphics2D g2, List<PlayerEntry> players,
                                    int x, int y, int width, int areaH, int rowH,
                                    Color textColor, boolean isPlants, int scrollOffset) {
            if (players.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                g2.setColor(new Color(255, 255, 255, 75));
                g2.drawString("Esperando...", x + 10, y + 30);
                return;
            }

            int totalH = players.size() * rowH;
            boolean needsScroll = totalH > areaH;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            if (!needsScroll) {
                for (int i = 0; i < players.size(); i++) {
                    int rowY = y + i * rowH;
                    drawPlayerRow(g2, players.get(i), x, rowY, width, rowH, textColor, isPlants, i);
                }
            } else {
                int offset = scrollOffset % totalH;
                for (int pass = 0; pass < 2; pass++) {
                    for (int i = 0; i < players.size(); i++) {
                        int rowY = y + (pass * totalH) + (i * rowH) - offset;
                        if (rowY + rowH < y || rowY > y + areaH) continue;
                        drawPlayerRow(g2, players.get(i), x, rowY, width, rowH, textColor, isPlants, i);
                    }
                }
            }
        }

        private void drawPlayerRow(Graphics2D g2, PlayerEntry player,
                                   int x, int rowY, int width, int rowH,
                                   Color textColor, boolean isPlants, int index) {
            if (index % 2 == 1) {
                g2.setColor(ROW_ALT);
                g2.fillRect(x, rowY, width, rowH);
            }

            // Icono mini
            if (isPlants) {
                drawPlantIcon(g2, x + 4, rowY + 5, 12);
            } else {
                drawZombieIcon(g2, x + 4, rowY + 5, 12);
            }

            // Nombre
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(textColor);
            String display = player.nickname;

            FontMetrics fm = g2.getFontMetrics();
            int maxTextW = width - 55;
            if (fm.stringWidth(display) > maxTextW) {
                while (fm.stringWidth(display + "..") > maxTextW && display.length() > 3) {
                    display = display.substring(0, display.length() - 1);
                }
                display += "..";
            }

            g2.drawString(display, x + 20, rowY + rowH - 8);

            // Acciones con rayo
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(new Color(255, 200, 60, 200));
            String acts = String.valueOf(player.actions);
            int actW = g2.getFontMetrics().stringWidth(acts);
            g2.drawString(acts, x + width - actW - 16, rowY + rowH - 9);
            drawLightning(g2, x + width - 14, rowY + rowH - 16,
                    new Color(255, 200, 60, 200));
        }

        private void drawBadge(Graphics2D g2, String text, int x, int y, Color color) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int bw = fm.stringWidth(text) + 12;
            int bh = 18;

            g2.setColor(BADGE_BG);
            g2.fillRoundRect(x, y, bw, bh, 10, 10);

            g2.setColor(color);
            g2.drawString(text, x + 6, y + 13);
        }

        // ── TICKER DE INSTRUCCIONES ──

        private void drawTicker(Graphics2D g2, int x, int y, int width, int height, int r) {
            // Separador superior
            g2.setColor(BORDER_BROWN);
            g2.fillRect(x, y, width, 3);

            // Fondo
            g2.setColor(TICKER_BG);
            g2.fillRoundRect(x, y + 3, width, height - 3, r, r);
            g2.fillRect(x, y + 3, width, 10);

            // Label "MENU"
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(new Color(255, 200, 60, 120));
            g2.drawString("COMO JUGAR", x + (width - g2.getFontMetrics().stringWidth("COMO JUGAR")) / 2, y + 15);

            // Línea decorativa
            g2.setColor(new Color(74, 55, 40, 150));
            g2.fillRect(x + 8, y + 19, width - 16, 1);

            // Mensaje actual
            Shape oldClip = g2.getClip();
            g2.setClip(x + 4, y + 22, width - 8, height - 25);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();

            String msg = TICKER_MESSAGES[currentTickerMsg];
            int msgWidth = fm.stringWidth(msg);

            // Scroll horizontal
            int textX;
            if (msgWidth <= width - 16) {
                // Cabe sin scroll → centrado
                textX = x + (width - msgWidth) / 2;
                tickerPause = 0;
            } else {
                // Scroll
                textX = x + width - 8 - tickerOffset;

                // Si ya salió por la izquierda, siguiente mensaje
                if (textX + msgWidth < x) {
                    currentTickerMsg = (currentTickerMsg + 1) % TICKER_MESSAGES.length;
                    tickerOffset = 0;
                    tickerPause = 40; // pausa de 2 seg antes de empezar
                    msg = TICKER_MESSAGES[currentTickerMsg];
                    msgWidth = fm.stringWidth(msg);
                    textX = x + (width - msgWidth) / 2;
                }
            }

            // Colorear según tipo
            int separatorIdx = msg.indexOf(">>");
            if (separatorIdx > 0) {
                String prefix = msg.substring(0, separatorIdx).trim();
                String rest = msg.substring(separatorIdx + 2).trim();

                // Color del prefijo
                Color prefixColor;
                if (prefix.contains("PLANTAS") || prefix.contains("Rosa") || prefix.contains("Corazon")
                        || prefix.contains("Finger") || prefix.contains("Perfume") || prefix.contains("Dona")
                        || prefix.contains("Gorra") || prefix.contains("Mano") || prefix.contains("Love")
                        || prefix.contains("Lentes") || prefix.contains("Paleta") || prefix.contains("Oso")
                        || prefix.contains("GG") || prefix.contains("FOLLOW") || prefix.contains("COMPARTIR")
                        || prefix.contains("LIKES")) {
                    prefixColor = TICKER_HIGHLIGHT;
                } else if (prefix.equals("GRATIS")) {
                    prefixColor = TICKER_GREEN;
                } else {
                    prefixColor = TICKER_HIGHLIGHT;
                }

                g2.setColor(prefixColor);
                g2.drawString(prefix + " ", textX, y + 38);

                int prefixW = fm.stringWidth(prefix + " >> ");
                g2.setColor(TICKER_TEXT);
                g2.drawString(rest, textX + prefixW, y + 38);
            } else {
                g2.setColor(TICKER_TEXT);
                g2.drawString(msg, textX, y + 38);
            }

            // Auto-avance si cabe (cada 3 seg)
            if (msgWidth <= width - 16) {
                tickerOffset++;
                if (tickerOffset > 60) {
                    currentTickerMsg = (currentTickerMsg + 1) % TICKER_MESSAGES.length;
                    tickerOffset = 0;
                    tickerPause = 10;
                }
            }

            g2.setClip(oldClip);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DATOS
    // ═══════════════════════════════════════════════════════════

    record PlayerEntry(String nickname, int actions) {}

    private void collectPlayers(List<PlayerEntry> plantas, List<PlayerEntry> zombies) {
        try {
            Map<String, TeamManager.PlayerInfo> players = teamManager.getPlayersSnapshot();

            for (var entry : players.entrySet()) {
                TeamManager.PlayerInfo info = entry.getValue();
                PlayerEntry pe = new PlayerEntry(
                        info.nickname != null ? info.nickname : entry.getKey(),
                        info.actions
                );
                if (info.team == Team.PLANTAS) plantas.add(pe);
                else zombies.add(pe);
            }

            plantas.sort((a, b) -> Integer.compare(b.actions, a.actions));
            zombies.sort((a, b) -> Integer.compare(b.actions, a.actions));

        } catch (Exception e) {
            // Silenciar
        }
    }
}