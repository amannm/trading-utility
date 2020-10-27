package systems.cauldron.utility.trading;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = new StackPane();
        Scene scene = new Scene(root);
        stage.setTitle("Trading Utility");
        stage.setScene(scene);
        stage.show();
    }
}