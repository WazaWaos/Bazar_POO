package pe.edu.upeu.project_final_gp1.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pe.edu.upeu.project_final_gp1.modelo.UserSession;

import java.io.IOException;
import java.net.URL;

public class MainController {
    @FXML
    private StackPane contentArea;
    @FXML
    private Label lblUsuario;

    @FXML
    public void initialize() {
        if (UserSession.getInstance().getUserLogger() != null) {
            lblUsuario.setText("Bienvenido: " + UserSession.getInstance().getUserLogger().getNombre());
        }
        showHome();
    }
    @FXML
    private void showHome() {
        loadView("home-view.fxml");
    }

    @FXML
    private void showProductos() {
        loadView("productos-view.fxml");
    }

    @FXML
    private void showVentas() {
        loadView("ventas-view.fxml");
    }

    @FXML
    private void showDeudores() {
        loadView("deudores-view.fxml");
    }

    @FXML
    private void showFlujoCaja() {
        loadView("flujo-view.fxml");
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().cleanSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pe/edu/upeu/project_final_gp1/views/login-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Iniciar sesión | Bazar 1");
            stage.setMaximized(false);
            stage.setWidth(900);
            stage.setHeight(590);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo cerrar la sesión");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadView(String fxml) {
        String ruta = "/pe/edu/upeu/project_final_gp1/views/" + fxml;
        try {
            URL resource = getClass().getResource(ruta);
            if (resource == null) {
                ruta = "/pe/edu/upeu/project_final_gp1/views/" + fxml;
                resource = getClass().getResource(ruta);
            }
            if (resource == null) {
                throw new IOException("No existe el recurso " + fxml);
            }
            Parent view = new FXMLLoader(resource).load();
            contentArea.getChildren().setAll(view);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No se pudo abrir el módulo");
            alert.setHeaderText("Error al cargar la vista");
            alert.setContentText("Vista: " + fxml + "\n" +
                    (e.getMessage() == null ? e.toString() : e.getMessage()));
            alert.showAndWait();
        }
    }
}
