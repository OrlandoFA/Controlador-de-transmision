package com.pvz.controller.tiktok;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae posiciones del grid de mensajes de chat.
 * Soporta: "A1", "A 1", "a1", "A-1", "B3", etc.
 * Filas: A-F  |  Columnas: 1-9
 */
public class PositionParser {

    private static final Pattern POSITION_REGEX = Pattern.compile("([a-fA-F])\\s*[-]?\\s*([1-9])");

    public record Position(String row, int col, int rowIndex) {
        @Override
        public String toString() {
            return row + col;
        }
    }

    /**
     * Extrae una posición de un texto
     * @return Position o null si no se encontró
     */
    public static Position parse(String text) {
        if (text == null || text.isEmpty()) return null;

        Matcher matcher = POSITION_REGEX.matcher(text);
        if (!matcher.find()) return null;

        String row = matcher.group(1).toUpperCase();
        int col = Integer.parseInt(matcher.group(2));
        int rowIndex = row.charAt(0) - 'A';

        return new Position(row, col, rowIndex);
    }
}