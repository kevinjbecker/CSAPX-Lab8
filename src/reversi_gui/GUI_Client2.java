package reversi_gui;

import javafx.application.Application;

import java.util.*;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import reversi.ReversiException;
import reversi2.Board;
import reversi2.NetworkClient;

/**
 * This application is the UI for Reversi.
 *
 * @author Kevin Becker
 */
public class GUI_Client2 extends Application implements Observer {

    /**
     * Connection to network interface to server
     */
    private NetworkClient serverConn;

    /**
     * The model that our client holds
     */
    private Board model;

    /**
     * Where the command line parameters will be stored once the application
     * is launched.
     */
    private Map< String, String > params = null;

    /**
     * Look up a named command line parameter (format "--name=value")
     * @param name the string after the "--"
     * @return the value after the "="
     * @throws ReversiException if name not found on command line
     */
    private String getParamNamed( String name ) throws ReversiException {
        if ( this.params == null ) {
            this.params = super.getParameters().getNamed();
        }
        if ( !params.containsKey( name ) ) {
            throw new ReversiException(
                    "Parameter '--" + name + "=xxx' missing."
            );
        }
        else {
            return params.get( name );
        }
    }

    /**
     *
     */
    @Override
    public void init()
    {
        try {
            // gets the two desired parameters that we need
            String host = getParamNamed("host");
            int port = Integer.parseInt(getParamNamed("port"));
            // creates our model
            this.model = new Board();
            // sets the serverConn to be a new NetworkClient with host, port and model
            this.serverConn = new NetworkClient(host, port, model);
            // initializes our model
            this.model.initializeGame();
        }
        catch (ReversiException re)
        {
            System.err.print("Reversi - " + re.getMessage());
        }
    }

    /**
     *
     */
    private void refresh()
    {

    }

    /**
     *
     * @param t
     * @param o
     */
    public void update(Observable t, Object o)
    {
        assert t == this.model: "Update from non-model Observable";

        this.refresh();
    }

    public void start( Stage mainStage ) {
        BorderPane pane = new BorderPane();
        GridPane gp = new GridPane();

        for(int i = 0; i <= 3; i++)
        {
            for(int j = 0; j <= 3; j++)
            {
                String id = i + ", " + j;
                Image imageDecline = new Image(getClass().getResourceAsStream("empty.jpg"));
                Button button = new Button();
                button.setOnAction( (EventAction) -> System.out.println(id) );
                button.setGraphic(new ImageView(imageDecline));
                gp.add(button, i, j);
            }
        }

        pane.setCenter(gp);
        pane.setBottom(new Label("dolor sit amet."));

        mainStage.setScene(new Scene(pane));
        mainStage.show();
    }

    /**
     * GUI is closing, so close the network connection. Server will
     * get the message.
     */
    @Override
    public void stop()
    {
        this.serverConn.close();
    }

    /**
     * Launch the JavaFX GUI.
     *
     * @param args not used, here, but named arguments are passed to the GUI.
     *             <code>--host=<i>hostname</i> --port=<i>portnum</i></code>
     */
    public static void main( String[] args ) {
        Application.launch( args );
    }

}
