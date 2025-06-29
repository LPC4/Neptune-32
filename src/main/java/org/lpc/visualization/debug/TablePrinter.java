package org.lpc.visualization.debug;

public class TablePrinter {
    private final int nameWidth;
    private final int addrWidth;
    private final int sizeWidth;
    private final String title;

    public TablePrinter(String title, int nameWidth, int addrWidth, int sizeWidth) {
        this.title = title;
        this.nameWidth = nameWidth;
        this.addrWidth = addrWidth;
        this.sizeWidth = sizeWidth;
    }

    public void printHeader(String nameLabel) {
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("=".repeat(90));
        System.out.printf("%" + (90 / 2 + title.length() / 2) + "s\n", title);
        System.out.println("=".repeat(90));

        System.out.printf(
                "%-" + nameWidth + "s | %" + addrWidth + "s | %" + addrWidth + "s | %" + sizeWidth + "s | %s\n",
                nameLabel, "START", "END", "SIZE", "DESCRIPTION"
        );
        System.out.println("-".repeat(90));
    }

    public void printRow(String name, int start, int end, int size, String description) {
        System.out.printf(
                "%-" + nameWidth + "s | 0x%08X | 0x%08X | %" + sizeWidth + "s | %s\n",
                name, start, end, formatSize(size), description
        );
    }

    public void printRow(String name, String start, String end, String size, String description) {
        System.out.printf(
                "%-" + nameWidth + "s | %-10s | %-10s | %" + sizeWidth + "s | %s\n",
                name, start, end, size, description
        );
    }

    public void printFooter() {
        System.out.println("-".repeat(90));
    }

    public void printDoubleFooter() {
        System.out.println("=".repeat(90));
    }

    private String formatSize(int bytes) {
        if (bytes >= 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        if (bytes >= 1024) return String.format("%.1fKB", bytes / 1024.0);
        return bytes + "B";
    }
}
