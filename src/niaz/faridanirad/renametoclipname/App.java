package niaz.faridanirad.renametoclipname;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class App extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("sceneBuilder.fxml"));

		Scene scene = new Scene(root, 300, 275, Color.BLACK);
		primaryStage.setTitle("Hello, Rename");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
}
