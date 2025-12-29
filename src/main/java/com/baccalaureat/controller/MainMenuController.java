package com.baccalaureat.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {
    @FXML
    private Button startSoloButton;

    @FXML
    private void handleStartSolo(ActionEvent event) throws IOException {
        Stage stage = (Stage) startSoloButton.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/com/baccalaureat/GameView.fxml"));
        stage.setScene(new Scene(root, 900, 700));
        stage.show();
    }
}
