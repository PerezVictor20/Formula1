package Interface;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class Formula_1 {
    private Connection conn;
    private JFrame frame;
    private JComboBox<String> comboBox;
    private JTable table;
    private DefaultTableModel tableModel;

    public Formula_1() {
        // Establecer conexión a la base de datos PostgreSQL
        connectDB();

        // Crear la interfaz gráfica
        createGUI();
    }

    private void connectDB() {
        try {
            String url = "jdbc:postgresql://localhost:5433/formula1";
            String user = "postgres";
            String password = "1234";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Conexión establecida con PostgreSQL.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(550, 550);

        // Centrar el título de la ventana
        centerTitle(frame, "Tabla de corredores de fórmula 1");

        // Panel para el combo box y la etiqueta
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel label = new JLabel("Año:");
        comboBox = new JComboBox<>();
        comboBox.setPreferredSize(new Dimension(150, 25)); // Ajustar tamaño del combo box
        populateComboBox();
        comboBox.addActionListener(e -> {
            // Cuando se seleccione un año, actualizar la tabla de corredores
            updateTable();
        });
        comboPanel.add(label);
        comboPanel.add(comboBox);

        // Tabla para mostrar los datos de corredores y carreras
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Centrar el contenido de las celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        frame.getContentPane().add(comboPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void populateComboBox() {
        comboBox.addItem("Seleccione un año");
        for (int year = 1950; year <= 2018; year++) {
            comboBox.addItem(String.valueOf(year));
        }
    }

    private void updateTable() {
        try {
            String selectedYear = (String) comboBox.getSelectedItem();
            String query;
            PreparedStatement pstmt;

            if ("All Years".equals(selectedYear)) {
                query = "SELECT DISTINCT ON (d.driver_id, r.year) d.driver_id, " +
                        "d.forename || ' ' || d.surname AS nombre_completo, " +
                        "r.year AS year, " +
                        "(SELECT COUNT(*) FROM driver_standings ds INNER JOIN races r2 ON ds.race_id = r2.race_id " +
                        "WHERE ds.driver_id = d.driver_id AND r2.year = r.year AND ds.position = 1) AS carreras_ganadas, " +
                        "(SELECT SUM(ds.points) FROM driver_standings ds INNER JOIN races r2 ON ds.race_id = r2.race_id " +
                        "WHERE ds.driver_id = d.driver_id AND r2.year = r.year) AS total_points, " +
                        "(SELECT ds.position FROM driver_standings ds INNER JOIN races r2 ON ds.race_id = r2.race_id " +
                        "WHERE ds.driver_id = d.driver_id AND r2.year = r.year ORDER BY ds.position LIMIT 1) AS rank " +
                        "FROM drivers d " +
                        "JOIN driver_standings ds ON d.driver_id = ds.driver_id " +
                        "JOIN races r ON ds.race_id = r.race_id " +
                        "ORDER BY d.driver_id, r.year, r.date";

                pstmt = conn.prepareStatement(query);
            } else {
                query = "SELECT DISTINCT ON (d.driver_id) d.driver_id, " +
                        "d.forename || ' ' || d.surname AS nombre_completo, " +
                        "(SELECT COUNT(*) FROM driver_standings ds INNER JOIN races r ON ds.race_id = r.race_id " +
                        "WHERE ds.driver_id = d.driver_id AND r.year = ? AND ds.position = 1) AS carreras_ganadas, " +
                        "(SELECT SUM(ds.points) FROM driver_standings ds INNER JOIN races r ON ds.race_id = r.race_id " +
                        "WHERE ds.driver_id = d.driver_id AND r.year = ?) AS total_points, " +
                        "(SELECT ds.position FROM driver_standings ds INNER JOIN races r ON ds.race_id = r.race_id " +
                        "WHERE ds.driver_id = d.driver_id AND r.year = ? ORDER BY ds.position LIMIT 1) AS rank " +
                        "FROM drivers d " +
                        "JOIN driver_standings ds ON d.driver_id = ds.driver_id " +
                        "JOIN races r ON ds.race_id = r.race_id " +
                        "WHERE r.year = ? " +
                        "ORDER BY d.driver_id, r.date";

                pstmt = conn.prepareStatement(query);
                pstmt.setInt(1, Integer.parseInt(selectedYear));
                pstmt.setInt(2, Integer.parseInt(selectedYear));
                pstmt.setInt(3, Integer.parseInt(selectedYear));
                pstmt.setInt(4, Integer.parseInt(selectedYear));
            }

            ResultSet rs = pstmt.executeQuery();

            // Obtener columnas
            Vector<String> columnNames = new Vector<>();
            columnNames.add("Driver name");
            columnNames.add("Wins");
            columnNames.add("Total Points");
            columnNames.add("Rank");

            // Obtener filas
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("nombre_completo"));
                row.add(rs.getInt("carreras_ganadas"));
                row.add(rs.getInt("total_points"));
                row.add(rs.getInt("rank"));
                data.add(row);
            }

            // Actualizar modelo de la tabla
            tableModel.setDataVector(data, columnNames);

            // Centrar el contenido de las celdas
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(JLabel.CENTER);
            table.setDefaultRenderer(Object.class, centerRenderer);

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void centerTitle(JFrame frame, String title) {
        Font font = new Font("Serif", Font.BOLD, 14);
        FontMetrics metrics = frame.getFontMetrics(font);
        int frameWidth = frame.getWidth();
        int titleWidth = metrics.stringWidth(title);
        int padding = (frameWidth - titleWidth) / 2;
        StringBuilder paddedTitle = new StringBuilder(title);
        for (int i = 0; i < padding / 8; i++) {
            paddedTitle.insert(0, " ");
        }
        frame.setTitle(paddedTitle.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Formula_1::new);
    }
}
