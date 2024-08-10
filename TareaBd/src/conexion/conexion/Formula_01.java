package conexion;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class Formula_01 {
    private Connection conn;
    private JFrame frame;
    private JComboBox<String> comboBox;
    private JTable table;
    private DefaultTableModel tableModel;

    public Formula_01() {
        //conexion a la base de datos postgre
        connectDB();

        createGUI();
    }
        //url, usuario y contra del postgres
    private void connectDB() {
        try {
            String url = "jdbc:postgresql://localhost:5432/postgres";
            String user = "postgres";
            String password = "123";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Conexión establecida con PostgreSQL.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame("Tabla de resultados de conductores");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 450);

        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        comboPanel.setBackground(Color.YELLOW);

        comboBox = new JComboBox<>();
        populateComboBox();
        comboBox.setPreferredSize(new Dimension(60, 25)); 
        comboBox.setMaximumSize(new Dimension(60, 25)); 

        comboBox.addActionListener(e -> {
            updateTable();
        });

        comboPanel.add(comboBox);

        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 150)); 

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.YELLOW); 
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        frame.getContentPane().setBackground(Color.YELLOW); 
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(comboPanel, BorderLayout.NORTH); 
        frame.getContentPane().add(tablePanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void populateComboBox() {
        comboBox.addItem("Año");
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

            Vector<String> columnNames = new Vector<>();
            columnNames.add("Driver name");
            columnNames.add("Wins");
            columnNames.add("Total Points");
            columnNames.add("Rank");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("nombre_completo"));
                row.add(rs.getInt("carreras_ganadas"));
                row.add(rs.getInt("total_points"));
                row.add(rs.getInt("rank"));
                data.add(row);
            }

            tableModel.setDataVector(data, columnNames);

            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(JLabel.CENTER);
            table.setDefaultRenderer(Object.class, centerRenderer);

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Formula_01::new);
    }
}
