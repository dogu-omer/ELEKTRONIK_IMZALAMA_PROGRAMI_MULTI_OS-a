package io.github.doguomer.esign.desktop;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Küçük Swing yardımcıları. */
final class Ui {

    private Ui() {}

    static GridBagConstraints at(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = x == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        c.weightx = x == 1 ? 1 : 0;
        return c;
    }

    /** Metin alanı + "Seç…" düğmesi. */
    static JButton chooser(Component parent, JTextField field, boolean directories) {
        JButton b = new JButton("Seç…");
        b.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(field.getText().isBlank() ? null : Path.of(field.getText()).toFile());
            fc.setFileSelectionMode(directories ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                field.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        return b;
    }

    static Optional<Path> path(JTextField f) {
        String t = f.getText() == null ? "" : f.getText().strip();
        return t.isEmpty() ? Optional.empty() : Optional.of(Path.of(t));
    }

    static void add(JPanel p, int row, String label, Component field, Component extra) {
        p.add(new javax.swing.JLabel(label), at(0, row));
        GridBagConstraints c = at(1, row);
        if (field instanceof javax.swing.JSpinner || field instanceof javax.swing.JComboBox) {
            c.fill = GridBagConstraints.NONE;
        }
        p.add(field, c);
        if (extra != null) {
            p.add(extra, at(2, row));
        }
    }
}
