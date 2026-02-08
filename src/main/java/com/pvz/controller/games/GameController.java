package com.pvz.controller.games;

/**
 * Interface que todo juego debe implementar para ser controlado via TikTok LIVE.
 *
 * El flujo es:
 *   1. TikTok detecta un evento (gift, like, follow)
 *   2. El TeamManager determina el equipo del viewer
 *   3. Se llama al método correspondiente del GameController activo
 */
public interface GameController {

    /** Nombre del juego para mostrar en consola */
    String getGameName();

    /** Nombre del proceso de Windows (para futura auto-detección) */
    String getProcessName();

    /**
     * Inicializar conexión con el juego
     * @return true si conectó exitosamente
     */
    boolean connect();

    /** Desconectar del juego */
    void disconnect();

    /** @return true si está conectado al juego */
    boolean isConnected();

    /**
     * Acción del Team Plantas (o equivalente "bueno")
     * @param type tipo de entidad a colocar (ej: "pea", "snow")
     * @param row fila (letra A-F o número)
     * @param col columna (1-9)
     * @param user usuario de TikTok que ejecuta
     * @return resultado de la acción
     */
    ActionResult onTeamAAction(String type, String row, int col, String user);

    /**
     * Acción del Team Zombies (o equivalente "malo")
     * @param type tipo de entidad a spawnear (ej: "normal", "buckethead")
     * @param count cantidad
     * @param row fila (puede ser null para aleatorio)
     * @param user usuario de TikTok que ejecuta
     * @return resultado de la acción
     */
    ActionResult onTeamBAction(String type, int count, String row, String user);

    /**
     * Acción de likes (bonus general)
     * @param sunAmount cantidad de recurso a agregar
     * @param user usuario origen
     * @return resultado
     */
    ActionResult onLikeBonus(int sunAmount, String user);

    /**
     * Obtener info del estado actual del juego
     */
    String getStatusInfo();

    // ═══════════════════════════════════════════════════════════
    // RESULTADO DE ACCIÓN
    // ═══════════════════════════════════════════════════════════

    record ActionResult(boolean success, String message) {
        public static ActionResult ok(String message) {
            return new ActionResult(true, message);
        }
        public static ActionResult fail(String message) {
            return new ActionResult(false, message);
        }
    }
}