package com.extremecraft.client.gui.layout;

/**
 * Lightweight grid helper for consistent spacing/alignment.
 */
public final class LayoutGrid {
    private final int originX;
    private final int originY;
    private final int columns;
    private final int rows;
    private final int cellWidth;
    private final int cellHeight;

    public LayoutGrid(int originX, int originY, int width, int height, int columns, int rows) {
        this.originX = originX;
        this.originY = originY;
        this.columns = Math.max(1, columns);
        this.rows = Math.max(1, rows);
        this.cellWidth = Math.max(1, width / this.columns);
        this.cellHeight = Math.max(1, height / this.rows);
    }

    public int cellX(int column) {
        return originX + (Math.max(0, column) * cellWidth);
    }

    public int cellY(int row) {
        return originY + (Math.max(0, row) * cellHeight);
    }

    public PanelRegion region(String id, int column, int row, int columnSpan, int rowSpan) {
        int clampedSpanX = Math.max(1, columnSpan);
        int clampedSpanY = Math.max(1, rowSpan);
        return new PanelRegion(
                id,
                cellX(column),
                cellY(row),
                cellWidth * clampedSpanX,
                cellHeight * clampedSpanY
        );
    }
}
