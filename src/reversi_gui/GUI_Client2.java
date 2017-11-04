package reversi_gui;

import javafx.application.Application;

import java.util.*;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
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

    private Label remaining = new Label("GAME INITIALIZING...");
    private Label move = new Label("WAITING");
    private Label status = new Label("RUNNING");

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
        System.out.println("refreshing");
//        if ( !this.model.isMyTurn() ) {
//            this.userOut.println( this.model );
//            this.userOut.println( this.model.getMovesLeft() + " moves left." );
//            Board.Status status = this.model.getStatus();
//            switch ( status )
//            {
//                case ERROR:
//                    //this.userOut.println( status );
//                    this.endGame();
//                    break;
//                case I_WON:
//                    this.userOut.println( "You won. Yay!" );
//                    this.endGame();
//                    break;
//                case I_LOST:
//                    this.userOut.println( "You lost. Boo!" );
//                    this.endGame();
//                    break;
//                case TIE:
//                    this.userOut.println( "Tie game. Meh." );
//                    this.endGame();
//                    break;
//                default:
//                    this.userOut.println();
//            }
//        }
//        else {
//            boolean done = false;
//            do {
//                // this.userOut.print("type move as row◻︎column: ");
//                this.userOut.flush();
//                int row = this.userIn.nextInt();
//                int col = this.userIn.nextInt();
//                if (this.model.isValidMove(row, col)) {
//                    this.userOut.println(this.userIn.nextLine());
//                    this.serverConn.sendMove(row, col);
//                    done = true;
//                }
//            } while (!done);
//        }
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
        // sets the alignments of the labels
        remaining.setTextAlignment(TextAlignment.LEFT);
        move.setTextAlignment(TextAlignment.CENTER);
        status.setTextAlignment(TextAlignment.RIGHT);
        // adds the labels to an HBox
        FlowPane labels = new FlowPane(remaining, move, status);


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

        // sets the GridPane to the bottom of the BorderPane
        pane.setCenter(gp);
        // sets the status HBox to the bottom of the BorderPane
        pane.setBottom(labels);

        // sets the scene as a new scene of the pane
        mainStage.setScene(new Scene(pane));

        // we have now completed building our GUI, we can show
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
