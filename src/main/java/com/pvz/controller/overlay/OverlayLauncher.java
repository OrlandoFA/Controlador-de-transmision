package com.pvz.controller.overlay;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overlay JavaFX con 2 ventanas transparentes:
 * - Ticker: barra horizontal arriba
 * - Teams: sidebar vertical a la derecha
 *
 * Cada ventana es arrastrable, sin bordes, always-on-top.
 * Click derecho para cerrar una ventana.
 */
public class OverlayLauncher extends Application {

    private static final Logger logger = LoggerFactory.getLogger(OverlayLauncher.class);

    private static int port = 8080;
    private static boolean launched = false;

    /**
     * Llamar desde Main.java para abrir el overlay.
     * Se ejecuta en un thread separado porque JavaFX bloquea.
     */
    public static void launch(int serverPort) {
        port = serverPort;
        if (launched) {
            logger.warn("Overlay ya fue lanzado");
            return;
        }
        launched = true;

        Thread fxThread = new Thread(() -> {
            try {
                Application.launch(OverlayLauncher.class);
            } catch (Exception e) {
                logger.error("Error lanzando overlay JavaFX: {}", e.getMessage());
            }
        });
        fxThread.setDaemon(true);
        fxThread.setName("overlay-fx");
        fxThread.start();
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);

        String baseUrl = "http://localhost:" + port + "/overlay";

        // ── Ventana 1: TICKER (arriba, horizontal) ──
        createWindow(
                baseUrl + "?view=ticker",
                "PvZ Ticker",
                960, 52,       // ancho x alto
                0, 0,          // posición x, y
                true           // arrastrable
        );

        // ── Ventana 2: TEAMS (derecha, vertical) ──
        createWindow(
                baseUrl + "?view=teams",
                "PvZ Teams",
                250, 550,      // ancho x alto
                710, 52,       // posición x, y
                true           // arrastrable
        );

        logger.info("🖥️ Overlay JavaFX abierto (2 ventanas)");
    }

    private void createWindow(String url, String title, int width, int height,
                              int posX, int posY, boolean draggable) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle(title);
        stage.setAlwaysOnTop(true);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(posX);
        stage.setY(posY);

        WebView webView = new WebView();
        webView.setPrefSize(width, height);
        webView.setContextMenuEnabled(false);

        // Fondo transparente del WebView
        webView.setStyle("-fx-background-color: transparent;");

        WebEngine engine = webView.getEngine();

        // Hacer el fondo de la página transparente
        engine.setOnStatusChanged(e -> {
            try {
                engine.executeScript(
                        "document.body.style.background='transparent';" +
                                "document.documentElement.style.background='transparent';"
                );
            } catch (Exception ignored) {}
        });

        // Cuando carga, hacer transparente
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    engine.executeScript(
                            "document.body.style.background='transparent';" +
                                    "document.documentElement.style.background='transparent';"
                    );
                } catch (Exception ignored) {}
            }
        });

        engine.load(url);

        StackPane root = new StackPane(webView);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, width, height);
        scene.setFill(Color.TRANSPARENT);

        // ── Drag para mover ──
        if (draggable) {
            final double[] dragDelta = new double[2];

            root.setOnMousePressed(e -> {
                if (e.isPrimaryButtonDown()) {
                    dragDelta[0] = e.getScreenX() - stage.getX();
                    dragDelta[1] = e.getScreenY() - stage.getY();
                }
            });

            root.setOnMouseDragged(e -> {
                if (e.isPrimaryButtonDown()) {
                    stage.setX(e.getScreenX() - dragDelta[0]);
                    stage.setY(e.getScreenY() - dragDelta[1]);
                }
            });

            // Click derecho para cerrar
            root.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    stage.close();
                }
            });
        }

        stage.setScene(scene);
        stage.show();
    }
}