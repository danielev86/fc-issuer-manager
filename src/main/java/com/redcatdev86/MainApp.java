package com.redcatdev86;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.PrimerDark;
import com.redcatdev86.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());        MainView view = new MainView();
        Scene scene = new Scene(view.build(stage), 900, 560);

        scene.getStylesheets().add(
                getClass().getResource("/styles/fc-theme.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}