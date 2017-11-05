package reversi_gui;

import javafx.application.Application;

import java.util.*;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import reversi.ReversiException;
import reversi2.Board;
import reversi2.NetworkClient;

/**
 * This application is the UI for Reversi.
 *
 * @author Kevin Becker
 */
public class GUI_Client2 extends Application implements Observer
{
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

    private GridPane gp;

    private Image empty = new Image(getClass().getResourceAsStream("empty.jpg"));
    private Image p1 = new Image(getClass().getResourceAsStream("othelloP1.jpg"));
    private Image p2 = new Image(getClass().getResourceAsStream("othelloP2.jpg"));

    private int numRows;
    private int numCols;

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
    public void init() throws Exception
    {
        super.init();
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

    public void start( Stage mainStage )
    {
        // our main viewport
        BorderPane pane = new BorderPane();
        // the GridPane that the locations will be put on

        //TODO: make this a method, make the middle ones the correct cool boys

        this.gp = new GridPane();

        this.numCols = this.model.getNCols();
        this.numRows = this.model.getNRows();

        // sets in place all of the buttons that we need
        for(int i = 0; i < this.numCols; i++)
        {
            for(int j = 0; j < this.numRows; j++)
            {
                // row and col of the current button
                int col = i;
                int row = j;

                // makes a new button with no text
                Button btn = new Button();
                btn.setId(i + " " + j);
                // makes the graphic for the button
                btn.setGraphic(new ImageView(empty));
                // adds an event to the button
                btn.setOnMouseClicked( (EventAction) -> checkMove(btn) );
                // adds the button to the GridPane
                this.gp.add(btn, i, j);
            }
        }
        disableSpaces();

        // sets the GridPane to the center of the BorderPane
        pane.setCenter(this.gp);

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
        // sets the title of the window to be Reversi
        mainStage.setTitle("Reversi");
        // adds an icon to the window
        mainStage.getIcons().add(new Image(getClass().getResourceAsStream("othello2.png")));
        // we have now completed building our GUI, we can show
        mainStage.show();

        // add ourselves to the model's observer list
        this.model.addObserver(this);
    }

    private void checkMove(Button btn)
    {
        String [] location = btn.getId().split(" ");
        int row = Integer.parseInt(location[1]);
        int col = Integer.parseInt(location[0]);

        if(this.model.isMyTurn())
        {
            if (this.model.isValidMove(row, col))
            {
                this.serverConn.sendMove(row, col);
            }
            else
            {
                javafx.application.Platform.runLater(() ->
                        move.setText("INVALID MOVE."));
            }
        }
        else
        {
            // if by some way they are able to make a move while the buttons are disabled
            // really make sure they know it isn't their turn
            javafx.application.Platform.runLater(() ->
                    move.setTextFill(Color.web("#FF0000")) );
        }
    }

    /**
     *
     * @param observable the observable that we will be checking to see for similarity to our model (it needs to be
     *                   exact so we cn use '==' instead of .equals(Object).
     * @param object a required object from the Observer interface. Not needed for this method.
     */
    @Override
    public void update(Observable observable, Object object)
    {
        // if observable not our model, game crashes with this error
        assert observable == this.model: "Update from non-model Observable";

        // refresh our game since it is our turn
        this.refresh();
    }

    /**
     *
     */
    private void refresh()
    {

        if (this.model.isMyTurn())
        {
            enableSpaces();
            javafx.application.Platform.runLater(() ->
                    move.setText("YOUR MOVE"));
        }
        else
        {
            disableSpaces();
            javafx.application.Platform.runLater(() ->
                    move.setText("NOT YOUR MOVE"));
        }

        javafx.application.Platform.runLater(() ->
                remaining.setText(this.model.getMovesLeft() + " MOVES LEFT"));

        // updating the images on the pieces
        // needed every time we refresh (game over or not)
        for(Node c: this.gp.getChildren())
        {
            int row = GridPane.getRowIndex(c);
            int col = GridPane.getColumnIndex(c);

            // the only children are buttons so we can cast it as such
            Button child = (Button) c;

            if(this.model.getContents(row, col) == Board.Move.PLAYER_ONE)
                javafx.application.Platform.runLater( () ->
                        child.setGraphic(new ImageView(p1)));
            if(this.model.getContents(row, col) == Board.Move.PLAYER_TWO)
                javafx.application.Platform.runLater( () ->
                        child.setGraphic(new ImageView(p2)));
            if(this.model.getContents(row, col) == Board.Move.NONE)
                javafx.application.Platform.runLater( () ->
                        child.setGraphic(new ImageView(empty)));
        }

        if(this.model.getStatus() != Board.Status.NOT_OVER)
        {
            switch (this.model.getStatus())
            {
                case ERROR:
                    break;
                case I_WON:
                    break;
                case I_LOST:
                    break;
                case TIE:
                    break;
            }
        }
    }

    /**
     * Ends the game.
     */
    private void endGame()
    {
        this.notify();
    }

    /**
     * Disables all spaces so that no interaction can happen
     */
    private void disableSpaces()
    {
        for(Node child: this.gp.getChildren())
        {
            javafx.application.Platform.runLater(() ->
                    child.setDisable(true));
        }
    }

    /**
     * Makes all of the valid spaces enabled so only they can be clicked
     */
    private void enableSpaces()
    {
        for(Node child: this.gp.getChildren())
        {
            javafx.application.Platform.runLater(() ->
                    child.setDisable(false));
        }
    }

    /**
     * GUI is closing, so close the network connection. Server will
     * get the message.
     */
    @Override
    public void stop() throws Exception
    {
        super.stop();
        this.serverConn.close();
    }

    /**
     * Launch the JavaFX GUI.
     *
     * @param args not used, here, but named arguments are passed to the GUI.
     *             <code>--host=<i>hostname</i> --port=<i>portnum</i></code>
     */
    public static void main( String[] args )
    {
        Application.launch( args );
    }

}
