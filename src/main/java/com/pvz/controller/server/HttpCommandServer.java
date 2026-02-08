package com.pvz.controller.server;

import com.pvz.controller.config.ControllerConfig;
import com.pvz.controller.handler.CommandRequestHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class HttpCommandServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpCommandServer.class);
    private HttpServer server;

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(ControllerConfig.getPort()), 0);

        // Command endpoint
        server.createContext("/command", new CommandRequestHandler());

        // Health check
        server.createContext("/health", exchange -> {
            String response = "{\"status\":\"ok\"}";
            sendResponse(exchange, 200, response);
        });

        // Status endpoint
        server.createContext("/status", exchange -> {
            String response = String.format(
                    "{\"status\":\"running\",\"port\":%d,\"scriptsDir\":\"%s\",\"localhostOnly\":%b}",
                    ControllerConfig.getPort(),
                    ControllerConfig.getScriptsDir().replace("\\", "\\\\"),
                    ControllerConfig.isLocalhostOnly()
            );
            sendResponse(exchange, 200, response);
        });

        // ── TEAMS JSON ──
        server.createContext("/teams", exchange -> {
            try {
                Path teamsFile = Path.of("data/teams.json");
                if (Files.exists(teamsFile)) {
                    String json = Files.readString(teamsFile, StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    sendResponse(exchange, 200, "{}");
                }
            } catch (Exception e) {
                logger.error("Error sirviendo teams: {}", e.getMessage());
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // ── OVERLAY HTML ──
        server.createContext("/overlay", exchange -> {
            try {
                String html = getOverlayHtml();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                logger.error("Error sirviendo overlay: {}", e.getMessage());
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // Root endpoint
        server.createContext("/", exchange -> {
            String response = "{\"name\":\"PvZ Controller\",\"version\":\"2.0.0\",\"endpoints\":[\"/command\",\"/health\",\"/status\",\"/teams\",\"/overlay\"]}";
            sendResponse(exchange, 200, response);
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        logger.info("HTTP Server started on port {}", ControllerConfig.getPort());
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("HTTP Server stopped");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getOverlayHtml() {
        return """
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>PvZ Overlay</title>
<link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@700;900&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
<style>
* { margin: 0; padding: 0; box-sizing: border-box; }

body {
    background: #1a1208;
    font-family: 'Inter', sans-serif;
    color: #e0d5c0;
    width: 420px;
    min-height: 100vh;
    overflow: hidden;
    padding: 12px;
}

/* ── HEADER ── */
.header {
    text-align: center;
    margin-bottom: 14px;
}
.header .swords {
    font-size: 22px;
    color: #c9a84c;
    letter-spacing: 4px;
}
.header h1 {
    font-family: 'Cinzel', serif;
    font-size: 32px;
    font-weight: 900;
    color: #f5ecd7;
    letter-spacing: 6px;
    text-shadow: 0 2px 8px rgba(0,0,0,0.5);
    margin-top: -2px;
}

/* ── TEAMS CONTAINER ── */
.teams {
    display: flex;
    gap: 10px;
    margin-bottom: 14px;
}

/* ── TEAM CARD ── */
.team-card {
    flex: 1;
    border-radius: 12px;
    border: 2px solid;
    overflow: hidden;
    background: rgba(0,0,0,0.3);
}
.team-card.plantas { border-color: #3d6b1e; }
.team-card.zombies { border-color: #6b1e1e; }

/* Team Header */
.team-header {
    padding: 10px 12px;
    display: flex;
    align-items: center;
    gap: 10px;
    position: relative;
}
.plantas .team-header { background: linear-gradient(135deg, #2d4a15 0%, #1f3310 100%); }
.zombies .team-header { background: linear-gradient(135deg, #4a1515 0%, #331010 100%); }

.team-icon {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
}
.plantas .team-icon { background: rgba(80,160,40,0.3); }
.zombies .team-icon { background: rgba(160,40,40,0.3); }

.team-info h2 {
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 2px;
}
.plantas .team-info h2 { color: #7cc644; }
.zombies .team-info h2 { color: #e85454; }

.team-info .subtitle {
    font-size: 9px;
    opacity: 0.5;
    font-style: italic;
}

.team-total {
    position: absolute;
    right: 10px;
    top: 50%;
    transform: translateY(-50%);
    border-radius: 10px;
    padding: 4px 10px;
    text-align: center;
    min-width: 48px;
}
.plantas .team-total {
    background: rgba(80,160,40,0.2);
    border: 1px solid #3d6b1e;
    color: #7cc644;
}
.zombies .team-total {
    background: rgba(160,40,40,0.2);
    border: 1px solid #6b1e1e;
    color: #e85454;
}
.team-total .number {
    font-size: 18px;
    font-weight: 700;
    line-height: 1;
}
.team-total .label {
    font-size: 7px;
    letter-spacing: 2px;
    opacity: 0.7;
}

/* ── PLAYER LIST ── */
.player-list {
    padding: 4px 6px;
    max-height: 320px;
    overflow: hidden;
}

.player-row {
    display: flex;
    align-items: center;
    padding: 7px 8px;
    border-radius: 8px;
    margin-bottom: 4px;
    border: 1px solid transparent;
    transition: background 0.2s;
}
.player-row:nth-child(odd) {
    background: rgba(255,255,255,0.03);
}
.plantas .player-row { border-color: rgba(80,160,40,0.15); }
.zombies .player-row { border-color: rgba(160,40,40,0.15); }

.player-icon {
    width: 22px;
    height: 22px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    margin-right: 8px;
    flex-shrink: 0;
}
.plantas .player-icon { background: rgba(80,160,40,0.15); }
.zombies .player-icon { background: rgba(160,40,40,0.15); }

.player-name {
    flex: 1;
    font-size: 12px;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.player-actions {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 600;
    flex-shrink: 0;
}
.plantas .player-actions {
    background: rgba(80,160,40,0.15);
    color: #7cc644;
}
.zombies .player-actions {
    background: rgba(160,40,40,0.15);
    color: #e85454;
}

.player-empty {
    text-align: center;
    padding: 20px 8px;
    font-size: 11px;
    opacity: 0.3;
    font-style: italic;
}

/* ── INSTRUCCIONES ── */
.instructions {
    border-top: 2px solid #3a2510;
    padding-top: 10px;
}

.instructions-label {
    text-align: center;
    font-size: 10px;
    letter-spacing: 4px;
    color: #6b5a3e;
    margin-bottom: 8px;
}

/* Tabs */
.tabs {
    display: flex;
    gap: 6px;
    margin-bottom: 10px;
    padding: 0 4px;
}
.tab {
    font-size: 9px;
    font-weight: 600;
    letter-spacing: 1px;
    padding: 4px 10px;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;
    color: #6b5a3e;
}
.tab.active {
    color: #c9a84c;
}
.tab.active::before {
    content: '';
    display: inline-block;
    width: 6px; height: 6px;
    background: #c9a84c;
    border-radius: 50%;
    margin-right: 6px;
}

/* Tab content */
.tab-content { display: none; }
.tab-content.active { display: block; }

/* Progress bar under tabs */
.tab-progress {
    height: 3px;
    background: #2a1a08;
    border-radius: 2px;
    margin-bottom: 10px;
    overflow: hidden;
}
.tab-progress-bar {
    height: 100%;
    border-radius: 2px;
    transition: width 0.3s;
}

/* Instruction cards */
.instr-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    padding: 0 4px;
}
.instr-badge {
    font-size: 8px;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 4px;
    letter-spacing: 1px;
}
.instr-title {
    font-family: 'Cinzel', serif;
    font-size: 14px;
    font-weight: 700;
    color: #f5ecd7;
}
.instr-subtitle {
    font-size: 10px;
    opacity: 0.4;
    margin-left: auto;
}

.steps {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 6px;
}
.step {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    border-radius: 8px;
    border: 1px solid;
}
.step-num {
    width: 22px; height: 22px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 700;
    flex-shrink: 0;
}
.step-text strong {
    font-size: 10px;
    display: block;
    letter-spacing: 1px;
}
.step-text span {
    font-size: 9px;
    opacity: 0.5;
    font-style: italic;
}

/* Tab: Inicio */
.tab-inicio .instr-badge { background: #2d4a15; color: #7cc644; }
.tab-inicio .step { background: rgba(201,168,76,0.05); border-color: rgba(201,168,76,0.2); }
.tab-inicio .step-num { background: rgba(201,168,76,0.15); color: #c9a84c; }
.tab-inicio .step-text strong { color: #c9a84c; }

/* Tab: Equipos */
.tab-equipos .instr-badge { background: #1a3a60; color: #5b9bd5; }
.tab-equipos .step { background: rgba(91,155,213,0.05); border-color: rgba(91,155,213,0.15); }
.tab-equipos .step-num { background: rgba(91,155,213,0.15); color: #5b9bd5; }
.tab-equipos .step-text strong { color: #5b9bd5; }

/* Tab: Regalos */
.tab-regalos .instr-badge { background: #4a1515; color: #e85454; }
.tab-regalos .gift-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 5px; }
.gift-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 8px;
    border-radius: 6px;
    background: rgba(255,255,255,0.03);
    border: 1px solid rgba(201,168,76,0.1);
    font-size: 10px;
}
.gift-item .gift-name { color: #c9a84c; font-weight: 600; flex-shrink: 0; }
.gift-item .gift-arrow { color: #6b5a3e; }
.gift-item .gift-result { color: #7cc644; font-size: 9px; }
.gift-item .gift-result.zombie { color: #e85454; }

/* Animation */
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.player-row { animation: fadeIn 0.3s ease; }

/* Scroll animation for overflow */
.scroll-wrapper {
    position: relative;
    overflow: hidden;
}
.scroll-content {
    transition: transform 0.5s ease;
}
</style>
</head>
<body>

<!-- HEADER -->
<div class="header">
    <div class="swords">⚔</div>
    <h1>EQUIPOS</h1>
</div>

<!-- TEAMS -->
<div class="teams">
    <!-- PLANTAS -->
    <div class="team-card plantas">
        <div class="team-header">
            <div class="team-icon">🌻</div>
            <div class="team-info">
                <h2>PLANTAS</h2>
                <div class="subtitle">Defensores del jardin</div>
            </div>
            <div class="team-total">
                <div class="number" id="plantas-total">0</div>
                <div class="label">TOTAL</div>
            </div>
        </div>
        <div class="player-list" id="plantas-list">
            <div class="player-empty">Esperando jugadores...</div>
        </div>
    </div>

    <!-- ZOMBIES -->
    <div class="team-card zombies">
        <div class="team-header">
            <div class="team-icon">💀</div>
            <div class="team-info">
                <h2>ZOMBIES</h2>
                <div class="subtitle">Horda imparable</div>
            </div>
            <div class="team-total">
                <div class="number" id="zombies-total">0</div>
                <div class="label">TOTAL</div>
            </div>
        </div>
        <div class="player-list" id="zombies-list">
            <div class="player-empty">Esperando jugadores...</div>
        </div>
    </div>
</div>

<!-- INSTRUCCIONES -->
<div class="instructions">
    <div class="instructions-label">─── INSTRUCCIONES ───</div>

    <div class="tabs">
        <div class="tab active" data-tab="inicio">INICIO</div>
        <div class="tab" data-tab="equipos">EQUIPOS</div>
        <div class="tab" data-tab="regalos">REGALOS</div>
    </div>

    <div class="tab-progress">
        <div class="tab-progress-bar" id="tab-progress" style="width:0%; background:#c9a84c;"></div>
    </div>

    <!-- Tab: Inicio -->
    <div class="tab-content tab-inicio active" id="tab-inicio">
        <div class="instr-header">
            <div class="instr-badge">BIENVENIDOS</div>
            <div class="instr-title">COMO FUNCIONA</div>
            <div class="instr-subtitle">Sigue estos pasos</div>
        </div>
        <div class="steps">
            <div class="step"><div class="step-num">1</div><div class="step-text"><strong>ELIGE EQUIPO</strong><span>Escribe P o Z en el chat</span></div></div>
            <div class="step"><div class="step-num">2</div><div class="step-text"><strong>ESCRIBE POSICION</strong><span>Ej: A3, B1, C5</span></div></div>
            <div class="step"><div class="step-num">3</div><div class="step-text"><strong>ACCION GRATIS</strong><span>Planta o zombie basico</span></div></div>
            <div class="step"><div class="step-num">4</div><div class="step-text"><strong>POTENCIA!</strong><span>Regalos = unidades fuertes</span></div></div>
        </div>
    </div>

    <!-- Tab: Equipos -->
    <div class="tab-content tab-equipos" id="tab-equipos">
        <div class="instr-header">
            <div class="instr-badge">EQUIPOS</div>
            <div class="instr-title">COMO JUGAR</div>
        </div>
        <div class="steps">
            <div class="step"><div class="step-num">🌱</div><div class="step-text"><strong>PLANTAS</strong><span>Escribe A1-F9 para plantar</span></div></div>
            <div class="step"><div class="step-num">🧟</div><div class="step-text"><strong>ZOMBIES</strong><span>Escribe A-F para enviar</span></div></div>
            <div class="step"><div class="step-num">❤</div><div class="step-text"><strong>FOLLOW</strong><span>+100 sol para tu equipo</span></div></div>
            <div class="step"><div class="step-num">📢</div><div class="step-text"><strong>COMPARTIR</strong><span>+200 sol para tu equipo</span></div></div>
        </div>
    </div>

    <!-- Tab: Regalos -->
    <div class="tab-content tab-regalos" id="tab-regalos">
        <div class="instr-header">
            <div class="instr-badge">REGALOS</div>
            <div class="instr-title">QUE INVOCAN?</div>
        </div>
        <div class="gift-grid">
            <div class="gift-item"><span class="gift-name">Rosa</span><span class="gift-arrow">→</span><span class="gift-result">Pea</span><span class="gift-arrow">/</span><span class="gift-result zombie">Normal</span></div>
            <div class="gift-item"><span class="gift-name">Corazon</span><span class="gift-arrow">→</span><span class="gift-result">Sunflower</span><span class="gift-arrow">/</span><span class="gift-result zombie">Normal</span></div>
            <div class="gift-item"><span class="gift-name">Perfume</span><span class="gift-arrow">→</span><span class="gift-result">Repeater</span><span class="gift-arrow">/</span><span class="gift-result zombie">Conehead</span></div>
            <div class="gift-item"><span class="gift-name">Dona</span><span class="gift-arrow">→</span><span class="gift-result">Wall-nut</span><span class="gift-arrow">/</span><span class="gift-result zombie">Conehead</span></div>
            <div class="gift-item"><span class="gift-name">Gorra</span><span class="gift-arrow">→</span><span class="gift-result">Cherry</span><span class="gift-arrow">/</span><span class="gift-result zombie">Bucket</span></div>
            <div class="gift-item"><span class="gift-name">Lentes</span><span class="gift-arrow">→</span><span class="gift-result">Torch</span><span class="gift-arrow">/</span><span class="gift-result zombie">Football</span></div>
            <div class="gift-item"><span class="gift-name">Oso</span><span class="gift-arrow">→</span><span class="gift-result">Gatling</span><span class="gift-arrow">/</span><span class="gift-result zombie">Gargantu</span></div>
            <div class="gift-item"><span class="gift-name">GG/Lion</span><span class="gift-arrow">→</span><span class="gift-result">Cob</span><span class="gift-arrow">/</span><span class="gift-result zombie">GIGA!!</span></div>
        </div>
    </div>
</div>

<script>
// ── AUTO-ROTATE TABS ──
const tabs = ['inicio', 'equipos', 'regalos'];
const tabColors = { inicio: '#c9a84c', equipos: '#5b9bd5', regalos: '#e85454' };
let currentTab = 0;
let tabTimer = 0;
const TAB_DURATION = 80; // ticks (80 * 100ms = 8 seg)

function switchTab(idx) {
    currentTab = idx;
    tabTimer = 0;
    document.querySelectorAll('.tab').forEach((t, i) => {
        t.classList.toggle('active', i === idx);
    });
    document.querySelectorAll('.tab-content').forEach((c, i) => {
        c.classList.toggle('active', i === idx);
    });
    document.getElementById('tab-progress').style.background = tabColors[tabs[idx]];
}

setInterval(() => {
    tabTimer++;
    let pct = (tabTimer / TAB_DURATION) * 100;
    document.getElementById('tab-progress').style.width = pct + '%';

    if (tabTimer >= TAB_DURATION) {
        switchTab((currentTab + 1) % tabs.length);
    }
}, 100);

// ── FETCH TEAMS ──
async function updateTeams() {
    try {
        const res = await fetch('/teams');
        const data = await res.json();

        const plantas = [];
        const zombies = [];

        for (const [id, info] of Object.entries(data)) {
            const entry = {
                nickname: info.nickname || id,
                actions: info.actions || 0,
                team: info.team
            };
            if (info.team === 'PLANTAS') plantas.push(entry);
            else zombies.push(entry);
        }

        plantas.sort((a, b) => b.actions - a.actions);
        zombies.sort((a, b) => b.actions - a.actions);

        renderList('plantas-list', plantas, 'plantas');
        renderList('zombies-list', zombies, 'zombies');

        const pTotal = plantas.reduce((s, p) => s + p.actions, 0);
        const zTotal = zombies.reduce((s, p) => s + p.actions, 0);
        document.getElementById('plantas-total').textContent = pTotal;
        document.getElementById('zombies-total').textContent = zTotal;

    } catch (e) {
        console.error('Error fetching teams:', e);
    }
}

function renderList(containerId, players, team) {
    const container = document.getElementById(containerId);
    if (players.length === 0) {
        container.innerHTML = '<div class="player-empty">Esperando jugadores...</div>';
        return;
    }

    const icon = team === 'plantas' ? '🌱' : '💀';
    container.innerHTML = players.map(p => {
        const name = p.nickname.length > 14 ? p.nickname.substring(0, 13) + '..' : p.nickname;
        return '<div class="player-row">' +
            '<div class="player-icon">' + icon + '</div>' +
            '<div class="player-name">' + escapeHtml(name) + '</div>' +
            '<div class="player-actions">💬 ' + p.actions + '</div>' +
            '</div>';
    }).join('');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

updateTeams();
setInterval(updateTeams, 5000);
</script>
</body>
</html>
""";
    }
}