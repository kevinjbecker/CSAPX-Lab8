package reversi_gui;

import java.util.*;

import javafx.application.Application;
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

    /** A Label that tells the user how many moves are remaining in the game. */
    private Label movesRemaining = new Label("INITIALIZING...");
    /** A Label that tells the user whose move it currently is. */
    private Label currentMove = new Label("NOT YOUR TURN");
    /** A Label that tells the user the status of the game (USUALLY ON). */
    private Label gameStatus = new Label("RUNNING");

    /** The GridPane that our tiles will be held in. Consider this the board game. */
    private GridPane gp;

    /** The image that is used for an empty place on the game board (no users) */
    private Image empty = new Image(getClass().getResourceAsStream("empty.jpg"));
    /** The image that is used for a location that has player 1 facing up */
    private Image p1 = new Image(getClass().getResourceAsStream("othelloP1.jpg"));
    /** The image that is used for a location that has player 2 facing up */
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
        this.gp = new GridPane();
        // builds the button plane
        this.gp = buildButtonGridPane(this.model.getNRows(), this.model.getNCols());
        // immediately update the plane so that the center pieces aren't empty
        updatePieceImages();
        // disable the spaces until its our turn at which point we can enable them (disabled at first so we don't have
        // any erroneous clicking by the user.
        disableSpaces();
        // sets the center of our BorderPane to be the GridPane
        pane.setCenter(this.gp);


        // builds and sets our bottom HBox to the labels (don't need to keep the HBox accessible)
        pane.setBottom(buildLabelHBox());


        // now we need to finalize the creation of the stage and scene.
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
                        currentMove.setText("INVALID MOVE."));
            }
        }
        else
        {
            // if by some way they are able to make a move while the buttons are disabled
            // really make sure they know it isn't their turn
            javafx.application.Platform.runLater(() ->
                    currentMove.setTextFill(Color.web("#FF0000")) );
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
        // updateTurn()
        if (this.model.isMyTurn())
        {
            enableSpaces();
            javafx.application.Platform.runLater(() ->
                    currentMove.setText("YOUR TURN"));
        }
        else
        {
            disableSpaces();
            javafx.application.Platform.runLater(() ->
                    currentMove.setText("NOT YOUR TURN"));
        }

        // gets the number of moves left (used for ternary to get plurality correct)
        int movesLeft = this.model.getMovesLeft();
        // updateMovesLeft()
        javafx.application.Platform.runLater(() ->
                movesRemaining.setText(movesLeft + (movesLeft != 0 ? " moves left": " moves left")));

        // updates the images of the pieces
        updatePieceImages();

        // if the game is over, we need to deal with that now
        if(this.model.getStatus() != Board.Status.NOT_OVER)
        {
            // determine which message we need to show the user because we're at the end case
            switch (this.model.getStatus())
            {
                case ERROR:
                    gameOver("ERROR");
                    break;
                case I_WON:
                    gameOver("You won!");
                    break;
                case I_LOST:
                    gameOver("You lost!");
                    break;
                case TIE:
                    gameOver("You tied!");
                    break;
            }
        }
    }

    private void updatePieceImages()
    {
        // updating the images on the pieces
        // needed every time we refresh (game over or not)
        for(Node child: this.gp.getChildren())
        {
            // gets the row and the column of the child
            int row = GridPane.getRowIndex(child);
            int col = GridPane.getColumnIndex(child);

            // the only children are buttons, but just as a fail-safe we check that here
            if(child instanceof Button)
            {
                //cast c as a Button
                Button btn = (Button) child;

                // we only need to worry about changing the images to p1 or p2, nothing causes the plane to
                // switch a piece from occupied to empty.
                if (this.model.getContents(row, col) == Board.Move.PLAYER_ONE)
                    javafx.application.Platform.runLater(() ->
                            btn.setGraphic(new ImageView(p1)));
                if (this.model.getContents(row, col) == Board.Move.PLAYER_TWO)
                    javafx.application.Platform.runLater(() ->
                            btn.setGraphic(new ImageView(p2)));
            }
        }
    }


    //TODO: These boys
    private void gameOver(String msg)
    {
        // set the message on the gameStatus
        javafx.application.Platform.runLater(() ->
                gameStatus.setText(msg));
        // set the currentMove to say that the game is over
        javafx.application.Platform.runLater(() ->
                currentMove.setText("Game over. Window can be closed."));
    }

    private GridPane buildButtonGridPane(int totalRows, int totalCols)
    {
        GridPane gp = new GridPane();
        // sets in place all of the buttons that we need
        for(int col = 0; col < totalCols; ++col)
        {
            for(int row = 0; row < totalRows; ++row)
            {
                // makes a new button with no text
                Button btn = new Button();
                // sets the ID so that we can get its row and column on its action
                btn.setId(col + " " + row);
                // makes the graphic for the middle pieces their corresponding players
                btn.setGraphic(new ImageView(empty));
                // adds an event to the button
                btn.setOnMouseClicked( (event) -> checkMove(btn) );
                // adds the button to the GridPane
                gp.add(btn, col, row);
            }
        }

        // return the newly created GridPane
        return gp;
    }

    private HBox buildLabelHBox()
    {
        // makes two spacers for the bottom HBox
        Region spacer1 = new Region();
        Region spacer2 = new Region();

        // sets the attributes of the spacers
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // creates and returns a new labels HBox
        return new HBox(movesRemaining, spacer1, currentMove, spacer2, gameStatus);
    }

    /**
     * Disables all spaces so that no interaction can happen (used to prevent the player whose turn it isn't from
     * interacting)
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
