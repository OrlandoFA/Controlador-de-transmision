package com.pvz.controller.games.pvz;

import com.pvz.controller.games.GameController;
import com.pvz.controller.memory.TrainerExecutor;
import com.pvz.controller.model.GameCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PvZGameController - Conecta la interface GameController con tu TrainerExecutor existente.
 *
 * Team A = Plantas (plantan plantas)
 * Team B = Zombies (spawnean zombies)
 * Recurso = Sol
 */
public class PvZGameController implements GameController {

    private static final Logger logger = LoggerFactory.getLogger(PvZGameController.class);

    @Override
    public String getGameName() {
        return "🌻 Plants vs Zombies";
    }

    @Override
    public String getProcessName() {
        return "popcapgame1.exe";
    }

    @Override
    public boolean connect() {
        GameCommand cmd = new GameCommand();
        cmd.setCommand("info");
        cmd.setUser("system");

        TrainerExecutor.ExecutionResult result = TrainerExecutor.execute(cmd);
        if (result.isSuccess()) {
            logger.info("✅ PvZ conectado: {}", result.getMessage());
            return true;
        } else {
            logger.warn("⚠️ PvZ no disponible: {}", result.getMessage());
            return false;
        }
    }

    @Override
    public void disconnect() {
        TrainerExecutor.shutdown();
        logger.info("PvZ desconectado");
    }

    @Override
    public boolean isConnected() {
        GameCommand cmd = new GameCommand();
        cmd.setCommand("info");
        cmd.setUser("system");
        return TrainerExecutor.execute(cmd).isSuccess();
    }

    @Override
    public ActionResult onTeamAAction(String type, String row, int col, String user) {
        // Team A = Plantas → plantar
        GameCommand cmd = new GameCommand();
        cmd.setCommand("plant");
        cmd.setType(type);
        cmd.setRow(row);
        cmd.setCol(col);
        cmd.setUser(user);

        if (!cmd.isValid()) {
            return ActionResult.fail("❌ Comando inválido: plant " + type + " " + row + col);
        }

        TrainerExecutor.ExecutionResult result = TrainerExecutor.execute(cmd);
        return new ActionResult(result.isSuccess(), result.getMessage());
    }

    @Override
    public ActionResult onTeamBAction(String type, int count, String row, String user) {
        // Team B = Zombies → spawn
        GameCommand cmd = new GameCommand();
        cmd.setCommand("spawn_zombie");
        cmd.setType(type);
        cmd.setCount(Math.min(count, 5));
        cmd.setUser(user);

        if (row != null && !row.isEmpty()) {
            cmd.setRow(row);
        }

        if (!cmd.isValid()) {
            return ActionResult.fail("❌ Comando inválido: spawn " + type + " x" + count);
        }

        TrainerExecutor.ExecutionResult result = TrainerExecutor.execute(cmd);
        return new ActionResult(result.isSuccess(), result.getMessage());
    }

    @Override
    public ActionResult onLikeBonus(int sunAmount, String user) {
        GameCommand cmd = new GameCommand();
        cmd.setCommand("sun");
        cmd.setCount(sunAmount);
        cmd.setUser(user);

        TrainerExecutor.ExecutionResult result = TrainerExecutor.execute(cmd);
        return new ActionResult(result.isSuccess(), result.getMessage());
    }

    @Override
    public String getStatusInfo() {
        GameCommand cmd = new GameCommand();
        cmd.setCommand("info");
        cmd.setUser("system");

        TrainerExecutor.ExecutionResult result = TrainerExecutor.execute(cmd);
        return result.isSuccess() ? result.getMessage() : "❌ No conectado a PvZ";
    }
}