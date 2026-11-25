package pe.edu.upeu.project_final_gp1.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pe.edu.upeu.project_final_gp1.implementacion.DeudorDaoImpl;
import pe.edu.upeu.project_final_gp1.interfaces.IDeudorDao;
import pe.edu.upeu.project_final_gp1.modelo.Deudor;
import pe.edu.upeu.project_final_gp1.modelo.PagoDeuda;
import pe.edu.upeu.project_final_gp1.utils.CurrencyCellFactory;

import java.sql.SQLException;

public class DeudorController {
    @FXML
    private TextField txtNombre, txtDni, txtDeuda, txtMontoOperacion, txtBuscar;

    @FXML
    private TableView<Deudor> tableDeudores;

    @FXML
    private TableColumn<Deudor, String> colNombre;

    @FXML
    private TableColumn<Deudor, Number> colDni, colDeuda;

    @FXML
    private TableView<PagoDeuda> tablePagos;

    @FXML
    private TableColumn<PagoDeuda, String> colPagoFecha, colPagoNombre;

    @FXML
    private TableColumn<PagoDeuda, Number> colPagoDni, colPagoMonto;

    private final IDeudorDao dao = new DeudorDaoImpl();

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(c -> c.getValue().nombreProperty());
        colDni.setCellValueFactory(c -> c.getValue().dniProperty());
        colDeuda.setCellValueFactory(c -> c.getValue().deudaProperty());
        colDeuda.setCellFactory(CurrencyCellFactory.create());

        colPagoFecha.setCellValueFactory(c -> c.getValue().fechaProperty());
        colPagoNombre.setCellValueFactory(c -> c.getValue().nombreProperty());
        colPagoDni.setCellValueFactory(c -> c.getValue().dniProperty());
        colPagoMonto.setCellValueFactory(c -> c.getValue().montoProperty());
        colPagoMonto.setCellFactory(CurrencyCellFactory.create());

        tableDeudores.getSelectionModel().selectedItemProperty().addListener((obs, old, d) -> {
            if (d != null) {
                txtNombre.setText(d.getNombre());
                txtDni.setText(String.valueOf(d.getDni()));
                txtDeuda.setText(String.format(java.util.Locale.US, "%.2f", d.getDeuda()));
                txtDni.setDisable(true);
            } else if (old != null) {
                txtNombre.clear();
                txtDni.clear();
                txtDeuda.clear();
                txtDni.setDisable(false);
            }
        });

        txtBuscar.textProperty().addListener((obs, old, value) -> cargar(value));

        cargar("");
        cargarPagos();
    }

    @FXML
    private void handleGuardar() {
        Deudor seleccionado = tableDeudores.getSelectionModel().getSelectedItem();

        try {
            String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
            String dniTexto = txtDni.getText() == null ? "" : txtDni.getText().trim();

            if (nombre.isBlank())
                throw new IllegalArgumentException("El nombre del deudor es obligatorio.");

            if (dniTexto.isBlank())
                throw new IllegalArgumentException("El DNI del deudor es obligatorio.");

            int dni;

            try {
                dni = Integer.parseInt(dniTexto);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El DNI debe contener solamente números.");
            }

            if (dni <= 0)
                throw new IllegalArgumentException("Ingresa un DNI válido.");

            double deuda;

            try {
                deuda = Double.parseDouble(txtDeuda.getText().trim().replace(',', '.'));
            } catch (Exception e) {
                throw new IllegalArgumentException("Ingresa una deuda válida.");
            }

            if (!Double.isFinite(deuda) || deuda < 0)
                throw new IllegalArgumentException("La deuda debe ser un número válido y no negativo.");

            deuda = Math.round(deuda * 100.0) / 100.0;

            if (seleccionado == null) {
                dao.registrar(new Deudor(0, nombre, dni, deuda));
                aviso("Deudor registrado correctamente.", Alert.AlertType.INFORMATION);
            } else {
                seleccionado.setNombre(nombre);
                seleccionado.setDeuda(deuda);
                dao.actualizar(seleccionado);

                aviso(deuda <= 0.005
                                ? "La deuda quedó cancelada y el registro se retiró de la lista."
                                : "Datos del deudor actualizados.",
                        Alert.AlertType.INFORMATION);
            }

            cargar(txtBuscar.getText());
            cargarPagos();
            limpiar();

        } catch (IllegalArgumentException | SQLException e) {
            aviso(mensaje(e), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAumentarDeuda() {
        Deudor d = tableDeudores.getSelectionModel().getSelectedItem();

        if (d == null) {
            aviso("Selecciona un deudor de la tabla.", Alert.AlertType.WARNING);
            return;
        }

        try {
            double monto = leerMontoOperacion();
            dao.aumentarDeuda(d.getIdCliente(), monto);
            cargar(txtBuscar.getText());
            limpiar();
            aviso("Deuda incrementada correctamente.", Alert.AlertType.INFORMATION);

        } catch (IllegalArgumentException | SQLException e) {
            aviso(mensaje(e), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleRegistrarPago() {
        Deudor d = tableDeudores.getSelectionModel().getSelectedItem();

        if (d == null) {
            aviso("Selecciona el deudor que realizará el pago.", Alert.AlertType.WARNING);
            return;
        }

        try {
            double monto = leerMontoOperacion();
            dao.registrarPago(d.getIdCliente(), monto);
            cargar(txtBuscar.getText());
            cargarPagos();
            limpiar();
            aviso("Pago registrado y agregado al historial de pagos.", Alert.AlertType.INFORMATION);

        } catch (IllegalArgumentException | SQLException e) {
            aviso(mensaje(e), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleLimpiar() {
        limpiar();
    }

    @FXML
    private void handleBuscar() {
        cargar(txtBuscar.getText());
    }

    private double leerMontoOperacion() {
        try {
            return Double.parseDouble(txtMontoOperacion.getText().trim().replace(',', '.'));
        } catch (Exception e) {
            throw new IllegalArgumentException("Ingresa el monto de la operación.");
        }
    }

    private void cargar(String filtro) {
        try {
            tableDeudores.setItems(FXCollections.observableArrayList(
                    filtro == null || filtro.isBlank() ? dao.listAll() : dao.buscar(filtro)
            ));
        } catch (SQLException e) {
            aviso("No se pudo cargar la lista de deudores: " + mensaje(e), Alert.AlertType.ERROR);
        }
    }

    private void cargarPagos() {
        try {
            tablePagos.setItems(FXCollections.observableArrayList(dao.listarPagos()));
        } catch (SQLException e) {
            aviso("No se pudo cargar el historial de pagos: " + mensaje(e), Alert.AlertType.ERROR);
        }
    }

    private void limpiar() {
        tableDeudores.getSelectionModel().clearSelection();
        txtNombre.clear();
        txtDni.clear();
        txtDeuda.clear();
        txtMontoOperacion.clear();
        txtDni.setDisable(false);
    }

    private String mensaje(Exception e) {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    private void aviso(String texto, Alert.AlertType tipo) {
        Alert a = new Alert(tipo);
        Stage stage = (Stage) txtBuscar.getScene().getWindow();
        a.initOwner(stage);
        a.initModality(Modality.WINDOW_MODAL);
        a.setTitle("Gestión de deudores");
        a.setHeaderText(null);
        a.setContentText(texto);
        a.showAndWait();
    }
}
