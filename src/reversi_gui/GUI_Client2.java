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
     *
     */
    private Label remaining = new Label("GAME INITIALIZING...");
    private Label move = new Label("WAITING");
    private Label status = new Label("RUNNING");

    private Image empty = new Image(getClass().getResourceAsStream("empty.jpg"));
    private Image p1 = new Image(getClass().getResourceAsStream("othelloP1.jpg"));
    private Image p2 = new Image(getClass().getResourceAsStream("othelloP2.jpg"));

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
     * Initializes the client before a build of the game board.
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
        if ( !this.model.isMyTurn() ) {

            move.setText(this.model.getMovesLeft() + " moves left.");

            Board.Status status = this.model.getStatus();
            switch (status)
            {
                case ERROR:
                    // change /status/ label to say there was an error
                    this.endGame();
                    break;
                case I_WON:
                    // change the /status/ label to say: Game over. You won!
                    this.endGame();
                    break;
                case I_LOST:
                    // change the /status/ label to say: Game over. You lost!
                    this.endGame();
                    break;
                case TIE:
                    // change the /status/ label to say: Game over. You tied.
                    this.endGame();
                    break;
            }
        }
        else {
            boolean stillMoving = true;
            while (stillMoving) {
//                this.userOut.print("type move as row◻︎column: ");
//                this.userOut.flush();
                int row = 0; //this.userIn.nextInt();
                int col = 1; //this.userIn.nextInt();
                if (this.model.isValidMove(row, col)) {
                    this.serverConn.sendMove(row, col);
                    stillMoving = false;
                }
            }
        }
    }

    /**
     *
     * @param observable
     * @param object
     */
    public void update(Observable observable, Object object)
    {
        // if observable not our model, game crashes with this error
        assert observable == this.model: "Update from non-model Observable";

        // refresh our game since it is our turn
        this.refresh();
    }

    public void start( Stage mainStage ) {
        BorderPane pane = new BorderPane();
        GridPane gp = new GridPane();

        // sets in place all of the buttons that we need
        for(int i = 0; i < this.model.getNCols(); i++)
        {
            for(int j = 0; j < this.model.getNRows(); j++)
            {
                // row and col of the button
                int col = i;
                int row = j;

                // makes a new button
                Button btn = new Button(i + " " + j);
                // makes the graphic for the button
                btn.setGraphic(new ImageView(empty));
                // adds an event to the button
                btn.setOnAction( (EventAction) -> this.serverConn.sendMove(row,col) );
                // adds the button to the GridPane
                gp.add(btn, i, j);
            }
        }

        // sets the GridPane to the bottom of the BorderPane
        pane.setCenter(gp);



        // makes two spacers for the bottom HBox
        Region spacer1 = new Region();
        Region spacer2 = new Region();

        // sets the attributes of the spacers
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // makes a new labels HBox
        HBox labels = new HBox(remaining, spacer1, move, spacer2, status);

        // sets the status HBox to the bottom of the BorderPane
        pane.setBottom(labels);



        // sets the scene as a new scene of the pane
        mainStage.setScene(new Scene(pane));

        // we have now completed building our GUI, we can show
        mainStage.show();

        //this.beginGame();
    }

    private synchronized void beginGame()
    {
        // add ourselves as an observer
        this.model.addObserver(this);

        // refreshes our game
        this.refresh();

        while ( this.model.getStatus() == Board.Status.NOT_OVER )
        {
            try {
                this.wait();
            }
            catch( InterruptedException ie ) { /*sauce*/ }
        }
    }

    /**
     * Ends the game.
     */
    private synchronized void endGame()
    {
        this.notify();
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
