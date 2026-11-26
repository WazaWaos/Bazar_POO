package pe.edu.upeu.project_final_gp1.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pe.edu.upeu.project_final_gp1.implementacion.UsuarioDaoImpl;
import pe.edu.upeu.project_final_gp1.interfaces.IUsuarioDao;
import pe.edu.upeu.project_final_gp1.modelo.UserSession;
import pe.edu.upeu.project_final_gp1.modelo.Usuario;

import java.io.IOException;

public class UsuarioController {
    @FXML
    private TextField txtUser;
    @FXML private PasswordField txtPassword;
    private final IUsuarioDao usuarioDao = new UsuarioDaoImpl();

    @FXML
    private void handleLogin() {
        String user = txtUser.getText().trim();
        String pass = txtPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            aviso("Campos vacíos", "Ingrese su usuario y contraseña.", Alert.AlertType.WARNING);
            return;
        }

        Usuario usuario = usuarioDao.login(user, pass);

        if (usuario != null) {
            UserSession.getInstance().setUserLogger(usuario);

            aviso("Acceso concedido", "Bienvenido, " + user, Alert.AlertType.INFORMATION);

            navigateToHome();

        } else {
            aviso("Acceso denegado", "Usuario o contraseña incorrectos.", Alert.AlertType.ERROR);
            txtPassword.clear();
            txtPassword.requestFocus();
        }
    }

    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/pe/edu/upeu/project_final_gp1/views/main-view.fxml"
            ));

            Parent root = loader.load();

            Stage stage = (Stage) txtUser.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Bazar 1 | Sistema de Gestión");
            stage.setFullScreenExitHint("");
            stage.show();
            stage.setFullScreen(true);

        } catch (IOException e) {
            aviso("Error", "No se pudo cargar el sistema: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void aviso(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}