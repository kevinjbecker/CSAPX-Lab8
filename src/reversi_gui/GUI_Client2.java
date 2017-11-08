package reversi_gui;

import java.util.*;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
    /** Connection to network interface to server */
    private NetworkClient serverConn;

    /** The model that our client holds */
    private Board model;

    /** Where the command line parameters will be stored once the application is launched. */
    private Map< String, String > params = null;

    /** A Label that tells the user how many moves are remaining in the game. */
    private Label movesRemaining = new Label();
    /** A Label that tells the user whose move it currently is. */
    private Label currentMove = new Label();
    /** A Label that tells the user the status of the game (USUALLY RUNNING). */
    private Label gameStatus = new Label("Running");

    /** The GridPane that our tiles will be held in. Consider this the board game. */
    private GridPane gp;

    /** The image that is used for an empty place on the game board (no users) */
    private Image empty = new Image(getClass().getResourceAsStream("empty.jpg"));
    /** The image that is used for a location that has player 1 facing up */
    private Image p1 = new Image(getClass().getResourceAsStream("othelloP1.jpg"));
    /** The image that is used for a location that has player 2 facing up */
    private Image p2 = new Image(getClass().getResourceAsStream("othelloP2.jpg"));

    /** The Font that is used for the Labels on the bottom */
    private Font bottomFont = new Font(20);
    /** The Font that is used for the Label on the top */
    private Font topFont = Font.font("Arial", FontWeight.BOLD,30);


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
     * Initializes the client before a build of the GUI.
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

    /**
     * Constructs our GUI and then displays it at the end
     *
     * @param mainStage the stage that we are showing our GUI upon.
     */
    @Override
    public void start( Stage mainStage )
    {
        // our main viewport
        BorderPane rootPane = new BorderPane();

        // builds the button plane
        this.gp = buildButtonGridPane(this.model.getNRows(), this.model.getNCols());

        // immediately update the plane so that the center pieces aren't empty
        updatePieceImages();

        // disable the spaces until its our turn at which point we can enable them (disabled at first so we don't have
        // any erroneous clicking by the user.
        disableSpaces();

        // sets the center of our BorderPane to be the GridPane
        rootPane.setCenter(this.gp);

        // builds and sets our bottom HBox to the necessary labels (don't need to keep the HBox accessible)
        rootPane.setBottom(buildBottomLabelHBox());

        // builds and sets our top HBox to the necessary labels
        rootPane.setTop(buildTopLabelHBox());

        // refreshes as soon as we begin so that the labels say the correct things
        refresh();

        // add ourselves to the model's observer list
        this.model.addObserver(this);


        // now we need to finalize the creation of the stage and scene.
        mainStage.setScene(new Scene(rootPane));
        // sets the title of the window to be Reversi
        mainStage.setTitle("Reversi");
        // adds an icon to the window
        mainStage.getIcons().add(new Image(getClass().getResourceAsStream("othello2.png")));
        // sets it so that the window can't be resized (makes it so that everything is right)
        mainStage.setResizable(false);

        // we have now completed building our GUI, we can show it
        mainStage.show();

    }

    /**
     * GUI is closing, so close the network connection. Server will get the message.
     */
    @Override
    public void stop() throws Exception
    {
        super.stop();
        this.serverConn.close();
    }

    /**
     * Builds the button GridPane to the dimensions of our playing field.
     *
     * @param totalRows the number of rows of buttons that we need.
     * @param totalCols The number of columns of buttons that we need.
     */
    private GridPane buildButtonGridPane(int totalRows, int totalCols)
    {
        // an empty GridPane that will eventually be returned
        GridPane gp = new GridPane();

        // sets in place all of the buttons that we need
        for(int col = 0; col < totalCols; ++col)
        {
            for(int row = 0; row < totalRows; ++row)
            {
                // makes a new button with no text
                Button btn = new Button();

                /* < attributes > */
                // sets the ID so that we can get its row and column on its action
                btn.setId(col + " " + row);

                /* < styles > */
                // makes the graphic for the middle pieces their corresponding players
                btn.setGraphic(new ImageView(empty));
                // sets the buttons so they have rectangular corners (rather than rounded, makes it look uniform)
                btn.setStyle("-fx-background-radius: 0em; ");

                /* < three event listeners > */
                // adds an event to the button so that a move is checked
                btn.setOnMouseClicked( (event) -> checkMove(btn) );
                // adds an event so that on mouse-over, the piece brightens a bit
                btn.setOnMouseEntered( (event) -> brightenButton(btn));
                // adds an event so that on mouse-exit, the piece returns to default brightness
                btn.setOnMouseExited( (event) -> normalizeButton(btn));

                // adds the button to the GridPane
                gp.add(btn, col, row);
            }
        }

        // return the newly created GridPane
        return gp;
    }

    /**
     * Builds our top label HBox.
     *
     * @return The HBox that contains the labels that go on the top of the GUI.
     */
    private HBox buildTopLabelHBox()
    {
        // makes two spacers for the bottom HBox
        Region spacer1 = new Region();
        Region spacer2 = new Region();

        // sets the attributes of the spacers
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // sets the text to the correct size in the label
        currentMove.setFont(topFont);

        // returns a new HBox (we don't need to specifically save it for access)
        return new HBox(spacer1, currentMove, spacer2);
    }

    /**
     * Builds our bottom label HBox.
     *
     * @return The HBox that contains the labels that go on the bottom of the GUI.
     */
    private HBox buildBottomLabelHBox()
    {
        // makes a spacer for the bottom HBox
        Region spacer = new Region();

        // sets the attributes of the spacer
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // sets the text to the correct size in the labels
        movesRemaining.setFont(bottomFont);
        gameStatus.setFont(bottomFont);

        // set the game status to green so that it says "Running" in green
        gameStatus.setTextFill(Color.web("#24b749"));

        // returns a new HBox (we don't need to specifically save it for access)
        return new HBox(movesRemaining, spacer, gameStatus);
    }

    /**
     * The method that gets called by the Board Observable when it is time to trigger an update.
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
     * The refresh method that actually refreshes our client GUI so that it reflects the current status of the game.
     */
    private void refresh()
    {
        // update's the turn label to say whose turn it is
        updateTurn();

        // updates the label that says how many moves are left
        updateMovesLeft();

        // updates the images of the pieces
        updatePieceImages();

        // if the game is over, we need to deal with that now
        if(this.model.getStatus() != Board.Status.NOT_OVER)
        {
            // run the gameOver(Board.Status) method to finalize our finished game (i.e. change the Labels to say the
            // correct stuff).
            gameOver(this.model.getStatus());
        }
    }

    /**
     * Update the top label that tells the player whose turn it is.
     */
    private void updateTurn()
    {
        if (this.model.isMyTurn())
        {
            enableSpaces();
            javafx.application.Platform.runLater(() ->
                    currentMove.setText("It's your turn"));
        }
        else
        {
            disableSpaces();
            javafx.application.Platform.runLater(() ->
                    currentMove.setText("Wait your turn"));
        }
    }

    /**
     * Checks to make sure the move that was just requested is valid or not.
     *
     * @param btn The button that was clicked by the user.
     */
    private void checkMove(Button btn)
    {
        int row = getButtonRow(btn);
        int col = getButtonColumn(btn);

        // if its my turn
        if(this.model.isMyTurn())
        {
            // check to make sure its a valid move
            if (this.model.isValidMove(row, col))
            {
                // then send it using the controller
                this.serverConn.sendMove(row, col);
            }
            else
            {
                // otherwise tell the player it was an invalid move
                javafx.application.Platform.runLater(() ->
                        currentMove.setText("INVALID MOVE"));
            }
        }
        else
        {
            // if by some way they are able to make a move while the buttons are disabled
            // really make sure they know it isn't their turn
            javafx.application.Platform.runLater(() ->
                    currentMove.setText("WAIT YOUR TURN!") );
        }
    }

    /**
     * Creates the effect of the button being slightly brighter when the mouse enters its field.
     *
     * @param btn The button in which should now be a bit brighter.
     */
    private void brightenButton(Button btn)
    {
        // set a filter to make the image 20% brighter (so we know which one the mouse is over)
        ColorAdjust bright = new ColorAdjust();
        bright.setBrightness(0.3);

        // determine the image we need
        int row = getButtonRow(btn);
        int col = getButtonColumn(btn);

        // creates a new button ImageView
        ImageView newBtn = getImageView(row, col);

        // set the effect to the new button
        newBtn.setEffect(bright);
        // set the image we have just created
        javafx.application.Platform.runLater(() ->
                btn.setGraphic(newBtn));
    }

    /**
     * Removes any sort of brightness on a button once the mouse exits its field.
     *
     * @param btn The button to remove the brightness from.
     */
    private void normalizeButton(Button btn)
    {
        // determine the image we need
        int row = getButtonRow(btn);
        int col = getButtonColumn(btn);

        // creates a new button ImageView
        ImageView btnReset = getImageView(row, col);

        javafx.application.Platform.runLater(() ->
                btn.setGraphic(btnReset));
    }

    /**
     * Returns a new ImageView based on the player at row, col.
     *
     * @param row The row to search in.
     * @param col The column to search in.
     *
     * @return A new ImageView of the correct type.
     */
    private ImageView getImageView(int row, int col)
    {

        switch(this.model.getContents(row, col))
        {
            case PLAYER_ONE:
                return new ImageView(p1);
            case PLAYER_TWO:
                return new ImageView(p2);
            default:
                // NONE by default
                return new ImageView(empty);
        }
    }

    /**
     * Gets the row of a button in the GridPane.
     *
     * @param btn The button we are searching for.
     *
     * @return The row that the button btn is found in.
     */
    private int getButtonRow(Button btn)
    {
        // splits the location of the button based on the ID of it
        return Integer.parseInt(btn.getId().split(" ")[1]);
    }

    /**
     * Gets the column of a button in the GridPane.
     *
     * @param btn The button we are searching for.
     *
     * @return The column that the button btn is found in.
     */
    private int getButtonColumn(Button btn)
    {
        // gets the column the button is in
        return Integer.parseInt(btn.getId().split(" ")[0]);
    }

    /**
     * Updates the label that contains the number of moves remaining.
     */
    private void updateMovesLeft()
    {
        // gets the number of moves left (used for ternary to get plurality correct)
        int movesLeft = this.model.getMovesLeft();
        javafx.application.Platform.runLater(() ->
                movesRemaining.setText("Moves left: " + movesLeft));
    }

    /**
     * Updates the images so that the move that was just made is accurately shown on the GUI.
     */
    private void updatePieceImages()
    {
        // updating the images on the pieces
        // needed every time we refresh (game over or not)
        for(Node child: this.gp.getChildren())
        {
            // the only children are buttons, but just as a fail-safe we check that here
            if(child instanceof Button)
            {
                //cast child as a Button
                Button btn = (Button) child;

                int row = getButtonRow(btn);
                int col = getButtonColumn(btn);

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

    /**
     * This method is called when the game is over. It updates all of the labels and then nothing else happens.
     *
     * @param status The status that was passed to the client as the way the game ended.
     */
    private void gameOver(Board.Status status)
    {
        // the message that will be put in the place of the currentMove label (the (not) your turn thing)
        String msg;
        // the color that the currentMove label will be changed to
        Color color;

        // determine what the status was
        switch(status)
        {
            case I_WON:
                // the game was a win
                msg = "Game over. You won!";
                color = Color.web("#24b749");
                break;
            case I_LOST:
                // the game was a loss
                msg = "Game over. You lost!";
                color = Color.web("#b73131");
                break;
            case TIE:
                // the game was a tie
                msg = "Game over. You tied!";
                color = Color.web("#2689af");
                break;
            default:
                // the default is an error
                msg = "ERROR!";
                color = Color.web("#b70000");
        }

        // sets the message on the currentMove
        javafx.application.Platform.runLater(() ->
                currentMove.setText(msg));

        // sets the color of the currentMove to be the right color
        javafx.application.Platform.runLater(() ->
                currentMove.setTextFill(color));

        // sets the gameStatus to say that the game is over
        javafx.application.Platform.runLater(() ->
                gameStatus.setText("Stopped"));

        // sets the color of gameStatus to be red so that the user knows the game was stop
        javafx.application.Platform.runLater(() ->
                gameStatus.setTextFill(Color.web("#b73131")));
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
     * Makes all of the valid spaces and filled in spaces enabled.
     */
    private void enableSpaces()
    {
        // goes through each child in the GridPane
        for(Node child: this.gp.getChildren())
        {
            // just a check to make sure the button is still a button (it's just a fail-safe measure)
            if(child instanceof Button)
            {
                // cast child as a button
                Button btn = (Button) child;

                int row = getButtonRow(btn);
                int col = getButtonColumn(btn);

                // if its a valid spot to place the object, or it has anything other than a NONE move, it is enabled
                if(this.model.isValidMove(row, col) || this.model.getContents(row, col) != Board.Move.NONE)
                {
                    // this enables it when the scheduler wants to run it
                    javafx.application.Platform.runLater(() ->
                            child.setDisable(false));
                }
            }
        }
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
