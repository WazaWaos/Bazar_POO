package pe.edu.upeu.project_final_gp1.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pe.edu.upeu.project_final_gp1.implementacion.DeudorDaoImpl;
import pe.edu.upeu.project_final_gp1.interfaces.IDeudorDao;
import pe.edu.upeu.project_final_gp1.modelo.Deudor;
import pe.edu.upeu.project_final_gp1.modelo.MovimientoDeuda;
import pe.edu.upeu.project_final_gp1.utils.CurrencyCellFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class DeudorController {
    private static final String PENDIENTE = "Deuda Pendiente";
    private static final String PAGADA = "Deuda Pagada";

    @FXML private TextField txtNombre, txtDni, txtTelefono, txtDeuda, txtDetalle, txtMontoOperacion, txtBuscar;
    @FXML private TableView<Deudor> tableDeudores;
    @FXML private TableColumn<Deudor, String> colNombre, colDni, colTelefono, colEstado;
    @FXML private TableColumn<Deudor, Number> colDeuda;
    @FXML private VBox boxDetalle;
    @FXML private Label lblTituloDetalle, lblTotalDeuda, lblEstadoDetalle;
    @FXML private TableView<MovimientoDeuda> tableIngresos, tablePagos;
    @FXML private TableColumn<MovimientoDeuda, String> colIngFecha, colIngDetalle, colPagoFecha;
    @FXML private TableColumn<MovimientoDeuda, Number> colIngMonto, colPagoMonto;
    private final IDeudorDao dao = new DeudorDaoImpl();

    @FXML public void initialize() {
        colNombre.setCellValueFactory(c -> c.getValue().nombreProperty());
        colDni.setCellValueFactory(c -> c.getValue().dniProperty());
        colTelefono.setCellValueFactory(c -> c.getValue().telefonoProperty());
        colDeuda.setCellValueFactory(c -> c.getValue().deudaProperty());
        colDeuda.setCellFactory(CurrencyCellFactory.create());
        // El estado se calcula solo a partir del saldo: cuando llega a cero pasa a "Deuda Pagada".
        colEstado.setCellValueFactory(c -> Bindings.createStringBinding(() -> estado(c.getValue().getDeuda()), c.getValue().deudaProperty()));

        colIngFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().fecha()));
        colIngDetalle.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().detalle()));
        colIngMonto.setCellValueFactory(c -> new ReadOnlyDoubleWrapper(c.getValue().monto()));
        colIngMonto.setCellFactory(CurrencyCellFactory.create());
        colPagoFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().fecha()));
        colPagoMonto.setCellValueFactory(c -> new ReadOnlyDoubleWrapper(c.getValue().monto()));
        colPagoMonto.setCellFactory(CurrencyCellFactory.create());

        tableDeudores.getSelectionModel().selectedItemProperty().addListener((obs, old, d) -> {
            if (d != null) {
                txtNombre.setText(d.getNombre()); txtDni.setText(d.getDni()); txtTelefono.setText(d.getTelefono()); txtDeuda.setText(String.format(Locale.US, "%.2f", d.getDeuda()));
                txtDni.setDisable(true); txtDeuda.setDisable(true);
                mostrarDetalle(d);
            } else if (old != null) {
                limpiar();
            }
        });
        txtBuscar.textProperty().addListener((obs, old, value) -> cargar(value));
        cargar("");
    }

    @FXML private void handleGuardar() {
        Deudor seleccionado = tableDeudores.getSelectionModel().getSelectedItem();
        try {
            String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
            String dni = txtDni.getText() == null ? "" : txtDni.getText().trim();
            String telefono = txtTelefono.getText() == null ? "" : txtTelefono.getText().trim();
            if (nombre.isBlank()) throw new IllegalArgumentException("El nombre del deudor es obligatorio.");
            if (dni.isBlank()) throw new IllegalArgumentException("El DNI del deudor es obligatorio.");
            if (seleccionado == null) {
                double deuda;
                try { deuda = Double.parseDouble(txtDeuda.getText().trim().replace(',', '.')); }
                catch (Exception e) { throw new IllegalArgumentException("Ingresa una deuda válida."); }
                if (!Double.isFinite(deuda) || deuda < 0) throw new IllegalArgumentException("La deuda debe ser un número válido y no negativo.");
                deuda = Math.round(deuda * 100.0) / 100.0;
                dao.registrar(new Deudor(0, nombre, dni, telefono, deuda), txtDetalle.getText());
                aviso("Deudor registrado correctamente.", Alert.AlertType.INFORMATION);
                cargar(txtBuscar.getText()); limpiar();
            } else {
                // La deuda no se edita aquí: se calcula con los ingresos y pagos del cliente.
                seleccionado.setNombre(nombre); seleccionado.setTelefono(telefono);
                dao.actualizar(seleccionado);
                aviso("Datos del cliente actualizados.", Alert.AlertType.INFORMATION);
                recargarYSeleccionar(seleccionado.getIdDeudor());
            }
        } catch (IllegalArgumentException | SQLException e) { aviso(mensaje(e), Alert.AlertType.ERROR); }
    }

    @FXML private void handleAumentarDeuda() {
        Deudor d = tableDeudores.getSelectionModel().getSelectedItem();
        if (d == null) { aviso("Selecciona un deudor de la tabla.", Alert.AlertType.WARNING); return; }
        try {
            double monto = leerMontoOperacion();
            dao.aumentarDeuda(d.getIdDeudor(), monto, txtDetalle.getText());
            recargarYSeleccionar(d.getIdDeudor()); aviso("Deuda incrementada correctamente.", Alert.AlertType.INFORMATION);
        } catch (IllegalArgumentException | SQLException e) { aviso(mensaje(e), Alert.AlertType.ERROR); }
    }

    @FXML private void handleRegistrarPago() {
        Deudor d = tableDeudores.getSelectionModel().getSelectedItem();
        if (d == null) { aviso("Selecciona el deudor que realizará el pago.", Alert.AlertType.WARNING); return; }
        try {
            double monto = leerMontoOperacion(); dao.registrarPago(d.getIdDeudor(), monto);
            recargarYSeleccionar(d.getIdDeudor());
            aviso("Pago registrado. El cobro se incluirá en el flujo de caja de hoy.", Alert.AlertType.INFORMATION);
        } catch (IllegalArgumentException | SQLException e) { aviso(mensaje(e), Alert.AlertType.ERROR); }
    }

    @FXML private void handleLimpiar() { limpiar(); }
    @FXML private void handleBuscar() { cargar(txtBuscar.getText()); }

    private double leerMontoOperacion() {
        try { return Double.parseDouble(txtMontoOperacion.getText().trim().replace(',', '.')); }
        catch (Exception e) { throw new IllegalArgumentException("Ingresa el monto de la operación."); }
    }

    /** Muestra las dos tablas del cliente y el resumen (total = ingresos - pagos). */
    private void mostrarDetalle(Deudor d) {
        try {
            List<MovimientoDeuda> ingresos = dao.listarIngresos(d.getIdDeudor());
            List<MovimientoDeuda> pagos = dao.listarPagos(d.getIdDeudor());
            tableIngresos.setItems(FXCollections.observableArrayList(ingresos));
            tablePagos.setItems(FXCollections.observableArrayList(pagos));
            double total = Math.round((suma(ingresos) - suma(pagos)) * 100.0) / 100.0;
            if (total < 0.005) total = 0;
            lblTituloDetalle.setText("Movimientos de " + d.getNombre() + "  ·  DNI " + d.getDni());
            lblTotalDeuda.setText(String.format(Locale.US, "S/ %.2f", total));
            String estado = estado(total);
            lblEstadoDetalle.setText(estado);
            lblEstadoDetalle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + (total > 0.005 ? "#C0392B" : "#1E8449") + ";");
            boxDetalle.setManaged(true); boxDetalle.setVisible(true);
        } catch (SQLException e) { aviso("No se pudo cargar el detalle del cliente: " + mensaje(e), Alert.AlertType.ERROR); }
    }
    private void ocultarDetalle() {
        boxDetalle.setVisible(false); boxDetalle.setManaged(false);
        tableIngresos.getItems().clear(); tablePagos.getItems().clear();
    }
    private static double suma(List<MovimientoDeuda> lista) { double t = 0; for (MovimientoDeuda m : lista) t += m.monto(); return t; }
    private static String estado(double deuda) { return deuda > 0.005 ? PENDIENTE : PAGADA; }

    private void cargar(String filtro) {
        try { tableDeudores.setItems(FXCollections.observableArrayList(filtro == null || filtro.isBlank() ? dao.listAll() : dao.buscar(filtro))); }
        catch (SQLException e) { aviso("No se pudo cargar la lista de deudores: " + mensaje(e), Alert.AlertType.ERROR); }
    }
    /** Refresca la lista y vuelve a seleccionar al cliente para que sus tablas y su resumen se actualicen. */
    private void recargarYSeleccionar(int idDeudor) {
        cargar(txtBuscar.getText());
        txtMontoOperacion.clear(); txtDetalle.clear();
        for (Deudor d : tableDeudores.getItems()) {
            if (d.getIdDeudor() == idDeudor) { tableDeudores.getSelectionModel().select(d); return; }
        }
        limpiar();
    }
    private void limpiar() {
        tableDeudores.getSelectionModel().clearSelection(); txtNombre.clear(); txtDni.clear(); txtTelefono.clear(); txtDeuda.clear(); txtDetalle.clear(); txtMontoOperacion.clear();
        txtDni.setDisable(false); txtDeuda.setDisable(false);
        ocultarDetalle();
    }
    private String mensaje(Exception e) { return e.getMessage() == null ? e.toString() : e.getMessage(); }
    private void aviso(String texto, Alert.AlertType tipo) { Alert a = new Alert(tipo); a.setTitle("Gestión de deudores"); a.setHeaderText(null); a.setContentText(texto); a.showAndWait(); }
}
