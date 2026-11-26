package pe.edu.upeu.project_final_gp1.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import pe.edu.upeu.project_final_gp1.implementacion.ProductoDaoImpl;
import pe.edu.upeu.project_final_gp1.interfaces.IProductoDao;
import pe.edu.upeu.project_final_gp1.modelo.Producto;
import pe.edu.upeu.project_final_gp1.utils.CurrencyCellFactory;
import pe.edu.upeu.project_final_gp1.utils.Dinero;

import java.sql.SQLException;

public class ProductoController {
    @FXML
    private Label lblId;
    @FXML
    private TextField txtNombre, txtPrecio, txtStock, txtBuscar;
    @FXML
    private TableView<Producto> tableProductos;
    @FXML
    private TableColumn<Producto, Number> colId, colPrecio, colStock;
    @FXML
    private TableColumn<Producto, String> colNombre;

    private final IProductoDao dao = new ProductoDaoImpl();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> c.getValue().idProperty());
        colNombre.setCellValueFactory(c -> c.getValue().nombreProperty());
        colPrecio.setCellValueFactory(c -> c.getValue().precioProperty());
        colPrecio.setCellFactory(CurrencyCellFactory.create());
        colStock.setCellValueFactory(c -> c.getValue().stockProperty());
        tableProductos.getSelectionModel().selectedItemProperty().addListener((obs, old, p) -> {
            if (p != null) {
                lblId.setText(String.valueOf(p.getId()));
                txtNombre.setText(p.getNombre());
                txtPrecio.setText(String.valueOf(p.getPrecio()));
                txtStock.setText(String.valueOf(p.getStock()));
            }
        });
        if (txtBuscar != null) txtBuscar.textProperty().addListener((obs, old, value) -> cargarProductos(value));
        cargarProductos("");
    }

    @FXML
    private void handleRegistrar() {
        try {
            Producto p = leerFormulario();
            dao.insert(p);
            cargarProductos(txtBuscar.getText());
            limpiarFormulario();
            aviso("Registro exitoso",
                    "Producto registrado correctamente.",
                    Alert.AlertType.INFORMATION);
        } catch (IllegalArgumentException | SQLException e) {
            aviso("Error al registrar",
                    mensaje(e),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleActualizar() {
        Producto seleccionado = tableProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            aviso("Advertencia",
                    "Selecciona un producto de la tabla para actualizarlo.",
                    Alert.AlertType.WARNING);
            return;
        }
        try {
            Producto p = leerFormulario();
            p.setId(seleccionado.getId());
            if (!dao.update(p)) throw new SQLException("No se encontró el producto que intentas actualizar.");
            cargarProductos(txtBuscar.getText());
            limpiarFormulario();
            aviso("Actualización exitosa",
                    "Producto actualizado correctamente.",
                    Alert.AlertType.INFORMATION);
        } catch (IllegalArgumentException | SQLException e) {
            aviso("Error al actualizar",
                    mensaje(e),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleEliminar() {
        Producto seleccionado = tableProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            aviso("Advertencia",
                    "Selecciona el producto que deseas eliminar.",
                    Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar el producto '" + seleccionado.getNombre() + "'?", ButtonType.CANCEL, ButtonType.OK);
        confirm.setTitle("Eliminar producto");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            if (!dao.delete(seleccionado.getId())) throw new SQLException("No se encontró el producto.");
            cargarProductos(txtBuscar.getText());
            limpiarFormulario();
            aviso("Eliminación exitosa",
                    "Producto eliminado correctamente.",
                    Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            aviso("Error al eliminar",
                    mensaje(e),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleLimpiar() {
        limpiarFormulario();
    }

    @FXML
    private void handleBuscar() {
        cargarProductos(txtBuscar.getText());
    }

    private Producto leerFormulario() {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        if (nombre.isBlank()) throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        double precio;
        int stock;
        precio = Dinero.parse(txtPrecio.getText(), "Ingresa un precio válido (máximo 2 decimales). Ejemplo: 12.50.");
        try {
            stock = Integer.parseInt(txtStock.getText().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("El stock debe ser un número entero.");
        }
        if (!Double.isFinite(precio) || precio < 0)
            throw new IllegalArgumentException("El precio debe ser un número finito y no negativo.");
        precio = Math.round(precio * 100.0) / 100.0;
        if (stock < 0) throw new IllegalArgumentException("El stock no puede ser negativo.");
        return new Producto(nombre, precio, stock);
    }

    private void cargarProductos(String filtro) {
        try {
            tableProductos.setItems(FXCollections.observableArrayList(filtro == null || filtro.isBlank() ? dao.listAll() : dao.searchByNombre(filtro)));
        } catch (SQLException e) {
            aviso("Error de inventario",
                    "No se pudo cargar el inventario: " + mensaje(e),
                    Alert.AlertType.ERROR);
        }
    }

    private void limpiarFormulario() {
        tableProductos.getSelectionModel().clearSelection();
        lblId.setText("Automático");
        txtNombre.clear();
        txtPrecio.clear();
        txtStock.clear();
    }

    private String mensaje(Exception e) {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }
    private void aviso(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        if (txtBuscar.getScene() != null && txtBuscar.getScene().getWindow() != null) {
            alert.initOwner(txtBuscar.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
        }
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
