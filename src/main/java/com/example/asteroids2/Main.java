package com.example.asteroids2;

import com.example.asteroids2.ConstantVar.Level;
import com.example.asteroids2.Flyingobject.*;
import com.example.asteroids2.MenuUi.ButtonMenu;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.example.asteroids2.ConstantVar.Level.*;
import static com.example.asteroids2.ConstantVar.Size.*;
import static com.example.asteroids2.ConstantVar.SpeedRate.*;


public class Main extends Application {
    protected Pane pane;

    protected Scene scene;

    protected double startTime;

    protected Text score;

    protected int points;
    protected Map<KeyCode, Boolean> pressedKeys = new HashMap<>();
    public static int WIDTH = 600;
    public static int HEIGHT = 600;

    protected Ship ship;

    protected List<Asteroid> asteroids = new ArrayList<>();

    protected List<Alien> aliens = new ArrayList<>();

    protected List<Projectile> projectiles = new ArrayList<>();


    protected List<Projectile> alienProjectiles = new ArrayList<>();


    //record the last firing time, to control firing frequency
    protected double lastFireTime;

    protected double lastDestroyedTime;

    protected double alienLastFireTime;
    protected double lastThrustTime;
    protected String currentLevel = "1";

    protected static final int SHIP_FIRE_INTERVAL = 500; // ms
    protected static final int ALIEN_FIRE_INTERVAL = 5000; // ms
    protected static final int SHIP_THRUST_INTERVAL = 3000; // ms
    protected static final int SHIP_INVINCIBLE_TIME = 3000; // ms
    protected static final int SHIP_PROJECTILE_EXIST_TIME = 3000;// ms
    protected static final int ALIEN_PROJECTILE_EXIST_TIME = 1000;// ms
    protected static final int ALIEN_EXISTING_MAXIMUM = 3;
    protected static final int SHIP_PROJECTILE_EXIST_MAXIMUM = 5;
    protected static final double CREATED_PROBABILITY = 0.001;


    protected Random random = new Random();

    protected Scene mainMenuScene;
    private Pane pauseMenuPane;

    private StackPane pauseMenuContainer;

    private StackPane gameOverContainer;

    private StackPane highScoreContainer;
    private boolean isPaused = false;

    private StackPane root;
    private Label infoLabel;

    private String[][] scores;

    private Pane highscorePane;

    // Load the custom font
    Font customFont = Font.loadFont(new FileInputStream("src/main/resources/imageAndFont/Roboto-BoldItalic.ttf"), 18);
    public Main() throws FileNotFoundException {
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        //Load the background image
        ImageView backgroundView = ButtonMenu.getBackgroundView("/imageAndFont/box.png", 400, 300);

        // Create the "Start Game" button with the custom image
        Button startBtn = getStartButton(primaryStage);

        // Create the "Quit Game" button with the custom image
        Button quitBtn = ButtonMenu.getQuitBtn(primaryStage, customFont);

        // Create the "Quit Game" button
        Button highScoreBtn = new Button("High Score List");
        highScoreBtn.setFont(customFont);

        // Create the main menu layout
        VBox mainMenuLayout = new VBox(20, startBtn, quitBtn, highScoreBtn);
        mainMenuLayout.setAlignment(Pos.CENTER);

        // Create a StackPane to hold the background and menu layout
        root = new StackPane();
        root.getChildren().addAll(backgroundView, mainMenuLayout);

        // Create the main menu scene
        mainMenuScene = new Scene(root, 400, 300);
        //stage > scene > pane
        //game pane
        primaryStage.setTitle("Game Title");
        primaryStage.setScene(mainMenuScene);
        primaryStage.show();

        highScoreBtn.setOnAction(e -> {
            try {
                highScoreMenu();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void highScoreMenu() throws IOException {
        // Create the game over pane
        ImageView highScoreBg = ButtonMenu.getBackgroundView("/imageAndFont/mainGameBackground.jpg", 400, 300);

        highscorePane = new Pane();

        highScoreContainer = new StackPane();

        containerSet(highScoreContainer, highScoreBg, highscorePane);
        System.out.println(root);

        root.getChildren().add(highScoreContainer);

        Button mainMenuBtn = ButtonMenu.getMenuBtn(customFont, root, highScoreContainer);

        highscorePane.getChildren().addAll(mainMenuBtn);

        highscorePane.setPrefSize(350, 250);

        // Create a ListView to display the high scores
        ListView<String> highScoreListView = new ListView<>();

        // Read the file
        readFile();
        ObservableList<String> highScores;

        if (scores == null) {
            highScores = FXCollections.observableArrayList("no scores");
        } else {
            // sort the scores
            sortTheScores();

            System.out.println(scores.length);
            List<String> scoresList = new ArrayList<>();
            if(scores.length < 6) {
                for(int i = 0; i <scores.length; i++){
                    scoresList.add("No."+(i+1) + "-" +scores[i][0] +"-"+ scores[i][1]);
                }
                // Add some test high scores to the list
                highScores = FXCollections.observableArrayList(scoresList);
            }else {
                for(int i = 0; i < 6; i++){
                    scoresList.add("No."+(i+1) + "-" +scores[i][0] +"-"+ scores[i][1]);
                }
                highScores = FXCollections.observableArrayList(scoresList);
            }




        }



        highScoreListView.setItems(highScores);

        // Set the size and position of the list view
        highScoreListView.setLayoutX(120);
        highScoreListView.setLayoutY(60);
        highScoreListView.setPrefSize(150, 130);

        highscorePane.getChildren().add(highScoreListView);

    }

    private void sortTheScores() {
        Arrays.sort(scores, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                return Integer.parseInt(o2[1])-Integer.parseInt(o1[1]);
            }
        });
    }

    private void readFile() throws IOException {
        FileReader highScoreFile = new FileReader("src/main/java/com/example/asteroids2/highScores.txt");
        BufferedReader bufferedReader = new BufferedReader(highScoreFile);
        String line;
        while ((line = bufferedReader.readLine())!= null){
            String[] tokens = line.split(" ");
            scores = new String[tokens.length / 2][2];
            for( int i = 0, j= 0; i < tokens.length; i += 2, j++){
                scores[j][0] = tokens[i];
                scores[j][1] = tokens[i+1];
            }
        }
    }

    private void containerSet(StackPane container, ImageView imgBg, Pane pane) {
        container.getChildren().addAll(imgBg, pane);
        container.setLayoutX(pane.getLayoutX());
        container.setLayoutY(pane.getLayoutY());
    }


    private Pane getPane(ImageView backgroundView) {
        Pane gameOverPane = new Pane();

        gameOverPane.setPrefSize(350, 250);
        gameOverPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-border-color: white; -fx-border-width: 2;");
        gameOverPane.setLayoutX((pane.getPrefWidth() - gameOverPane.getPrefWidth()) / 2);
        gameOverPane.setLayoutY((pane.getPrefHeight() - gameOverPane.getPrefHeight()) / 2);

        // Create a StackPane to hold the background and pause menu pane
        gameOverContainer = new StackPane();
        containerSet(gameOverContainer, backgroundView, gameOverPane);

        pane.getChildren().add(gameOverContainer);
        return gameOverPane;
    }

    private Button getStartButton(Stage primaryStage) {
        Button startBtn = new Button("Start Game");
        startBtn.setFont(customFont);
        startBtn.setOnAction(e -> {
            mainGameScene(primaryStage);
            AnimationTimer getAnimationTimer = getAnimationTimer(primaryStage);
            getAnimationTimer.start();

            primaryStage.show();

            // Detect ESC key press event
            scene.addEventHandler(KeyEvent.KEY_PRESSED, e3 -> {
                if (e3.getCode() == KeyCode.ESCAPE) {
                    if (!isPaused) {
                        getAnimationTimer.stop();
                        isPaused = true;
                        // Show pause menu
                        showPauseMenu(primaryStage, getAnimationTimer);
                    } else {
                        pane.getChildren().remove(pauseMenuPane);
                        pane.getChildren().remove(pauseMenuContainer);
                        isPaused = false;
                        getAnimationTimer.start();
                    }
                }
            });
        });
        return startBtn;
    }

    private  void mainGameScene(Stage primaryStage){
        ImageView backgroundView = ButtonMenu.getBackgroundView("/imageAndFont/mainGameBackground.jpg", WIDTH, HEIGHT);
        ImageView boxView = ButtonMenu.getBackgroundView("/imageAndFont/box.png", WIDTH, HEIGHT);

        // Code to start the game goes here
        pane = new Pane();
        pane.setPrefSize(WIDTH, HEIGHT);
        //record the start time of this game
        startTime = System.currentTimeMillis();
        //add roles to pane
        addRoles(pane);

        // Create a StackPane to hold the background and game pane
        Pane mainGamePane = new StackPane();
        mainGamePane.getChildren().addAll(backgroundView, pane);

        infoLabel = new Label("Score: "+ points +"  \nLife: 3  \nLevel: 1");
        // Set the font color to white
        infoLabel.setTextFill(Color.WHITE);
        infoLabel.setFont(customFont);

        pane.getChildren().add(infoLabel);


        scene = new Scene(mainGamePane);
        primaryStage.setTitle("Asteroids Game");
        primaryStage.setScene(scene);
    }


    private void showPauseMenu(Stage primaryStage, AnimationTimer getAnimationTimer) {

        ImageView backgroundView = ButtonMenu.getBackgroundView("/imageAndFont/box.png", 300, 200);

        pauseMenuPane = new Pane();
        pauseMenuPane.setPrefSize(300, 200);
        pauseMenuPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-border-color: white; -fx-border-width: 2;");
        pauseMenuPane.setLayoutX((pane.getPrefWidth() - pauseMenuPane.getPrefWidth()) / 2);
        pauseMenuPane.setLayoutY((pane.getPrefHeight() - pauseMenuPane.getPrefHeight()) / 2);

        // Create a StackPane to hold the background and pause menu pane
        pauseMenuContainer = new StackPane();
        containerSet(pauseMenuContainer, backgroundView, pauseMenuPane);

        pane.getChildren().add(pauseMenuContainer);

        Button resumeBtn = getResumeBtn(getAnimationTimer);

        pauseMenuPane.getChildren().add(resumeBtn);

        Button quitGameBtn = ButtonMenu.getQuitGameBtn(primaryStage, customFont, pauseMenuPane);

        pauseMenuPane.getChildren().add(quitGameBtn);

        pane.getChildren().add(pauseMenuPane);
    }


    private Button getResumeBtn(AnimationTimer getAnimationTimer) {
        Button resumeBtn = new Button("Resume Game");
        resumeBtn.setFont(customFont);
        resumeBtn.setOnAction(e -> {
            pane.getChildren().remove(pauseMenuPane);
            pane.getChildren().remove(pauseMenuContainer);
            isPaused = false;
            getAnimationTimer.start();
        });
        resumeBtn.setLayoutX((pauseMenuPane.getPrefWidth() - resumeBtn.getWidth()) / 3.5);
        resumeBtn.setLayoutY((pauseMenuPane.getPrefHeight() - resumeBtn.getHeight()) / 4);
        return resumeBtn;
    }

    private int getHighScore() {
        return points;
    }

    private void gameOver(Stage primaryStage, AnimationTimer animationTimer) {

        ImageView backgroundView = ButtonMenu.getBackgroundView("/imageAndFont/box.png", 350, 250);

        // Stop the animation timer
        animationTimer.stop();

        // Create the game over pane
        Pane gameOverPane = getPane(backgroundView);

        // Create the "Restart" button
        Button restartBtn = getRestartBtn(primaryStage, gameOverPane);

        // Create the "Back to Main Menu" button
        Button mainMenuBtn = ButtonMenu.getMainMenuBtn(primaryStage, gameOverPane, customFont,pane,mainMenuScene);

        // Add buttons to the game over pane
        gameOverPane.getChildren().addAll(restartBtn, mainMenuBtn);

        // gameOverText
        Text gameOverText = new Text("Game  Over");
        gameOverText.setFont(Font.font("Verdana", FontWeight.BOLD, 30));
        gameOverText.setLayoutX(75);
        gameOverText.setLayoutY(60);
        gameOverText.setFill(Color.WHITE);
        gameOverPane.getChildren().add(gameOverText);

        // scoreText
        Text scoreText = new Text("Your Score: " + points);
        scoreText.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        scoreText.setLayoutX(110);
        scoreText.setLayoutY(95);
        scoreText.setFill(Color.WHITE);
        gameOverPane.getChildren().add(scoreText);

        // code to ask the player to enter a name
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Game Over");
            dialog.setHeaderText("Enter your name:");
            dialog.setContentText("Name:");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String name = result.get();
                // record the score with the name
                if(name != "") recordNameScore(name.replace(" ", "_"));
            }
        });

        // Add the game over pane to the game pane
        pane.getChildren().add(gameOverPane);
    }

    private Button getRestartBtn(Stage primaryStage, Pane gameOverPane) {
        Button restartBtn = new Button("Restart");
        restartBtn.setFont(customFont);
        restartBtn.setOnAction(e -> {
            pane.getChildren().remove(gameOverPane);
            pane.getChildren().remove(gameOverContainer);
            startNewGame(primaryStage);
        });
        restartBtn.setLayoutX(135);
        restartBtn.setLayoutY(125);
        return restartBtn;
    }

    private void startNewGame(Stage primaryStage) {
        // Reset player's life, score, and other game states
        asteroids = new ArrayList<>();
        aliens = new ArrayList<>();
        projectiles = new ArrayList<>();
        alienProjectiles = new ArrayList<>();
        points = 0;
        ship.setLives(3);
//        ship.setMovement(new Point2D(0, 0));

        // Start the game
        mainGameScene(primaryStage);
        AnimationTimer getAnimationTimer = getAnimationTimer(primaryStage);
        getAnimationTimer.start();;
    }


    private void recordNameScore(String name) {
        // Implement the logic to record the player's score
        try {
            File file = new File("src/main/java/com/example/asteroids2/highScores.txt");
            FileWriter writer = new FileWriter(file, true);
            FileReader reader = new FileReader(file);
            int charCount = reader.read();
            if(charCount == -1){
                //save username
                writer.write(name+ " "+ points);
            } else {
                //save username
                writer.write(" " + name+ " "+ points);
            }
            reader.close();
            writer.close();
        }catch (IOException e){
            System.out.println("File is not found");
        }
    }

    private AnimationTimer getAnimationTimer(Stage primaryStage) {
    return new AnimationTimer() {

            //handle function will be automatically called.
            @Override
            public void handle(long now) {

                //Continuous generation of asteroids and aliens
                createAsteroids(pane);
                createAliens(pane);
                //control ship, turning direction, going forward
                controlShip();
                checkInvincibility();

                fire();
                alienFire();
                collision(primaryStage, this);
                moveObjects();
                removeProjectiles();

                showGameInfo();


                //Once a collision occurs, game stops
                asteroids.forEach(asteroid -> {
                    //When a collision occurs，
                    //if the number of deaths of the ship is already greater than or equal to 3, the game will stop.
                    //On the contrary, recreate ship in the center of window
                    try {
                        checkCollisionOfShip(asteroid, primaryStage, this);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                aliens.forEach(alien -> {
                    try {
                       checkCollisionOfShip(alien, primaryStage, this);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        };
}

    private void showGameInfo() {
        infoLabel.setText("Score: "+ points +"  \nLife: "+ship.getLives()+"  \nLevel: ");
    }

    private void checkCollisionOfShip(FlyingObject obj, Stage primaryStage, AnimationTimer getAnimationTimer) throws Exception {
        if (ship.collide(obj) && ship.getLives() == 3 && !ship.isInvincibility()) {
            gameOver(primaryStage, getAnimationTimer);
        } else if (ship.collide(obj) && !ship.isInvincibility()) {
            lastDestroyedTime = System.currentTimeMillis();
            pane.getChildren().remove(obj.getShape());
            obj.setIsALive(false);
            pane.getChildren().remove(ship.getShape());
            //increase the number of death
            int temp = ship.getLives() - 1;
            //change the location ship in the center of window
            ship = new Ship(WIDTH / 2, HEIGHT / 2);
            ship.setInvincibility(true);
            ship.setLives(temp);
            //Change the colour of the reborn ship
            ship.getShape().setFill(Color.web("#FF0000"));
            pane.getChildren().add(ship.getShape());
        }
    }

    private void checkInvincibility(){
        if ((System.currentTimeMillis() - lastDestroyedTime > SHIP_INVINCIBLE_TIME)){
            ship.setInvincibility(false);
            ship.getShape().setFill(Color.WHITE);
        }
    }

    private AtomicInteger getAtomicInteger() {
        return new AtomicInteger();
    }

    private void addRoles(Pane pane) {
        //create ship and add it to pane
        ship = new Ship(WIDTH / 2, HEIGHT / 2);
        pane.getChildren().add(ship.getShape());
        //create some lowest moving asteroids when a new game starts
        Random rnd = new Random();
        for (int i = 0; i < 2; i++) {
            Asteroid asteroid = new Asteroid(rnd.nextInt(WIDTH), rnd.nextInt(HEIGHT),LARGE.setSize(), LOWER.setSpeedRate());
            //This shows that the newly generated asteroid will not be too close to the ship
            if (!asteroid.collide(ship)) {
                asteroids.add(asteroid);
                pane.getChildren().add(asteroid.getShape());
            }
        }
    }

    private void createAsteroids(Pane pane) {
        //if a random number is less 0.005, a new asteroids will be added to the window.
        if (Math.random() < CREATED_PROBABILITY) {
            if ((System.currentTimeMillis() - startTime) <= Level.EASY_TIME.setLevel()) {
                Asteroid asteroid = new Asteroid(WIDTH, HEIGHT, LARGE.setSize(), LOWER.setSpeedRate());
                if (!asteroid.collide(ship)) {
                    asteroids.add(asteroid);
                    pane.getChildren().add(asteroid.getShape());
                }
            } else if ((System.currentTimeMillis() - startTime) > Level.EASY_TIME.setLevel()
                    && (System.currentTimeMillis() - startTime <= MODERATE_TIME.setLevel())) {
                Asteroid asteroid = new Asteroid(WIDTH, HEIGHT, random.nextInt(MEDIUM.setSize(), LARGE.setSize() + 1), NORMAL.setSpeedRate());
                if (!asteroid.collide(ship)) {
                    asteroids.add(asteroid);
                    pane.getChildren().add(asteroid.getShape());
                }
            } else if ((System.currentTimeMillis() - startTime) > HARD_TIME.setLevel()) {
                Asteroid asteroid = new Asteroid(WIDTH, HEIGHT, random.nextInt(SMALL.setSize(), LARGE.setSize() + 1), FASTER.setSpeedRate());
                if (!asteroid.collide(ship)) {
                    asteroids.add(asteroid);
                    pane.getChildren().add(asteroid.getShape());
                }
            }
        }
    }


    private void createAliens(Pane pane) {
        //if a random number is less 0.005, a new aliens will be added to the window.
        if (Math.random() < CREATED_PROBABILITY && (aliens.size() <= ALIEN_EXISTING_MAXIMUM)) {
            if (((System.currentTimeMillis() - startTime) > MODERATE_TIME.setLevel()) &
                    ((System.currentTimeMillis() - startTime) <= HARD_TIME.setLevel())) {
                Alien alien = new Alien(WIDTH, HEIGHT,1);
                if (!alien.collide(ship)) {
                    aliens.add(alien);
                    alien.getShape().setFill(Color.TRANSPARENT);
                    alien.getShape().setStroke(Color.RED);
                    pane.getChildren().add(alien.getShape());
                }
            } else{
                Alien alien = new Alien(WIDTH, HEIGHT,2);
                if (!alien.collide(ship)) {
                    aliens.add(alien);
                    alien.getShape().setFill(Color.TRANSPARENT);
                    alien.getShape().setStroke(Color.RED);
                    pane.getChildren().add(alien.getShape());
                }
            }

        }
    }

    private void controlShip() {
        scene.setOnKeyPressed(event -> {
            pressedKeys.put(event.getCode(), Boolean.TRUE);
        });

        scene.setOnKeyReleased(event -> {
            pressedKeys.put(event.getCode(), Boolean.FALSE);
        });

        if (pressedKeys.getOrDefault(KeyCode.LEFT, false)) {
            ship.turnLeft();
        }

        if (pressedKeys.getOrDefault(KeyCode.RIGHT, false)) {
            ship.turnRight();
        }


        //speed = length / time
        //length = square (x-axis ** 2 + y-axis ** 2)
        //time: The number of times the AnimationTimer refreshes the screen per second
        double speed = Math.sqrt((Math.pow(ship.getMovement().getX(), 2) +
                Math.pow(ship.getMovement().getY(), 2))) * 60 ;
        if (pressedKeys.getOrDefault(KeyCode.UP, false)) {
            //limit the speed of the ship, prevents unlimited increase in speed of ship
            if (speed < 500){
                ship.accelerate();
            }
        }


        //This function is used to slow down the ship, but our project does not need this function.
        if (pressedKeys.getOrDefault(KeyCode.DOWN, false)) {
            if (speed >= 0 ){
                ship.setMovement(new Point2D(0,0));
            }
        }



        if (pressedKeys.getOrDefault(KeyCode.J, false)){
            //per 3 seconds, ship can use one time
            if((System.currentTimeMillis() - lastThrustTime) > SHIP_THRUST_INTERVAL){
                //remove the shape of ship from pane
                pane.getChildren().remove(ship.getShape());
                //using while loop until find a proper position
                ship.thrust(random.nextInt(WIDTH), random.nextInt(HEIGHT));
                while (thrustCheckAsteroid() && thrustCheckAlien()){
                    ship.thrust(random.nextInt(WIDTH), random.nextInt(HEIGHT));
                }
                //only a thrust has successfully occurred, we record the time
                lastThrustTime = System.currentTimeMillis();
                pane.getChildren().add(ship.getShape());
            }
        }

        //slowdown function

        ship.move();
    }

    //The following two functions are designed to detect if a collision has occurred after a thrust
    protected boolean thrustCheckAsteroid(){
        for(Asteroid asteroid : asteroids){
            if (asteroid.collide(ship)){
                return true;
            }
        }
        return false;
    }

    protected boolean thrustCheckAlien(){
        for(Alien alien : aliens){
            if (alien.collide(ship)){
                return true;
            }
        }
        return false;
    }

    private void fire() {
        //only 10 projectiles exist at the same time
        if (pressedKeys.getOrDefault(KeyCode.SPACE, false) && projectiles.size() < SHIP_PROJECTILE_EXIST_MAXIMUM) {
            //fire interval is 500 ms
            if (System.currentTimeMillis() - lastFireTime > SHIP_FIRE_INTERVAL) {
                lastFireTime = System.currentTimeMillis();
                Projectile projectile = new Projectile(
                        (int) ship.getShape().getTranslateX(),
                        (int) ship.getShape().getTranslateY());
                projectile.getShape().setRotate(ship.getShape().getRotate());
                projectiles.add(projectile);

                projectile.accelerate();
                projectile.setMovement(projectile.getMovement().normalize().multiply(3));
                pane.getChildren().add(projectile.getShape());
            }
        }
        //make all projectiles move
        projectiles.forEach(projectile -> projectile.move());
    }

    private void alienFire(){
        //fire interval is 5000 ms
        if (System.currentTimeMillis() - alienLastFireTime > ALIEN_FIRE_INTERVAL) {
            alienLastFireTime = System.currentTimeMillis();
            aliens.forEach(alien -> {
                Projectile alienprojectile = new Projectile(
                        (int) alien.getShape().getTranslateX(),
                        (int) alien.getShape().getTranslateY());
                if (alien.getShape().getTranslateY() > ship.getShape().getTranslateY()){
                    alienprojectile.getShape().setRotate(
                            Math.toDegrees(
                                    Math.atan(
                                            (ship.getShape().getTranslateY() - alien.getShape().getTranslateY())/
                                                    (ship.getShape().getTranslateX() - alien.getShape().getTranslateX())
                                    )));
                }

                alienProjectiles.add(alienprojectile);


                alienprojectile.accelerate();
                alienprojectile.setMovement(alienprojectile.getMovement().normalize().multiply(3));
                alienprojectile.getShape().setFill(Color.RED);
                pane.getChildren().add(alienprojectile.getShape());
            });
        }

        //make all projectiles move
        alienProjectiles.forEach(alienProjectile -> alienProjectile.move());
    }

    //remove projectiles that have timed out
    public void removeProjectiles() {
        //Label projectiles that have timed out
        projectiles.forEach(projectile -> {
            //the projectile will be removed after 5 seconds
            if ((System.currentTimeMillis() - projectile.getCreateTime()) >= SHIP_PROJECTILE_EXIST_TIME) {
                projectile.setIsALive(false);
            }
        });

        alienProjectiles.forEach(alienProjectile -> {
            //the projectile will be removed after 2 seconds
            if ((System.currentTimeMillis() - alienProjectile.getCreateTime()) >= ALIEN_PROJECTILE_EXIST_TIME) {
                alienProjectile.setIsALive(false);
            }
        });
    }

    public void removeObjects(){
        //if a projectile and asteroid, both of them should be deleted from the projectiles and asteroids list, respectively.
        //remove all dead projectiles and asteroids from the window
        projectiles.stream()
                .filter(projectile -> !projectile.getIsALive())  //the statement returns a stream which includes the dead projectiles
                .forEach(projectile -> pane.getChildren().remove(projectile.getShape()));//remove the shape of  every dead projectile
        alienProjectiles.stream()
                .filter(alienProjectile -> !alienProjectile.getIsALive())  //the statement returns a stream which includes the dead projectiles
                .forEach(alienProjectile -> pane.getChildren().remove(alienProjectile.getShape()));
        asteroids.stream()
                .filter(asteroid -> !asteroid.getIsALive())
                .forEach(asteroid -> {
                    pane.getChildren().remove(asteroid.getShape());
                    //For each asteroid destroyed, the player gains ten points
                    //bonus score can be set up in 'addAndGet' by change the parameter.
                    if (asteroid.getSize() == SMALL.setSize()){
                        points += 250;
                    } else if (asteroid.getSize() == MEDIUM.setSize()) {
                        points += 500;
                    }else if(asteroid.getSize() == LARGE.setSize()){
                        points += 1000;
                    }
                });
        aliens.stream()
                .filter(alien -> !alien.getIsALive())
                .forEach(alien -> {
                    pane.getChildren().remove(alien.getShape());
                    points += 2000;
                });
        showGameInfo();
        //Remove all projectiles and asteroids that collide
        projectiles.removeAll(projectiles.stream()
                .filter(projectile -> !projectile.getIsALive())
                .collect(Collectors.toList()));
        alienProjectiles.removeAll(alienProjectiles.stream()
                .filter(alienProjectile -> !alienProjectile.getIsALive())
                .collect(Collectors.toList()));
        asteroids.removeAll(asteroids.stream()
                .filter(asteroid -> !asteroid.getIsALive())
                .collect(Collectors.toList()));
        aliens.removeAll(aliens.stream()
                .filter(alien -> !alien.getIsALive())
                .collect(Collectors.toList()));
    }

    public void collision(Stage primaryStage, AnimationTimer getAnimationTimer) {
        //Label asteroids and projectiles as dead if they collide
        projectiles.forEach(projectile -> {
            //check projectiles and asteroids
            asteroids.forEach(asteroid -> {
                if (projectile.collide(asteroid)) {
                    //record the number of death of each object
                    projectile.setLives(0);
                    asteroid.setLives(0);
                    //set the status of objects which occurs collision is false
                    //it mean that the status objects marked as false should be deleted
                    projectile.setIsALive(false);
                    asteroid.setIsALive(false);
                }
            });
            //check projectiles and aliens
            aliens.forEach(alien -> {
                if (projectile.collide(alien)){
                    projectile.setLives(0);
                    alien.setLives(0);
                    //set the status of objects which occurs collision is false
                    //it mean that the status objects marked as false should be deleted
                    projectile.setIsALive(false);
                    alien.setIsALive(false);
                }
            });
        });

        alienProjectiles.forEach(alienProjectile -> {
            try {
                checkCollisionOfShip(alienProjectile, primaryStage, getAnimationTimer);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        //If a larger asteroid is destroyed, then three smaller asteroids are created in the same place
        asteroids.addAll(createSmallerAsteroid());
        //delete the hit objects and the projectile
        removeObjects();
    }

    private ArrayList<Asteroid> createSmallerAsteroid() {
        ArrayList<Asteroid> smallerAsteroids = new ArrayList<>();
        asteroids.stream()
                .filter(asteroid -> !asteroid.getIsALive())
                .forEach(asteroid -> {
                    if(asteroid.getSize() != SMALL.setSize()){
                        int temp = 0;
                        while (temp<2){
                            Asteroid newAsteroid = new Asteroid(
                                    (int) asteroid.getShape().getTranslateX() + random.nextInt(-10,10),
                                    (int) asteroid.getShape().getTranslateY() + random.nextInt(-10,10),
                                    asteroid.getSize() - 1,asteroid.getSpeedTimes() - 1);
                            if(!newAsteroid.collide(ship)){
                                smallerAsteroids.add(newAsteroid);
                                pane.getChildren().add(newAsteroid.getShape());
                                temp+=1;
                            }
                        }
                    }
                });
        smallerAsteroids.forEach(smallerAsteroid ->
        {
            smallerAsteroid.move();
        });
        return smallerAsteroids;
    }

    private String changeLevel(){
        if (System.currentTimeMillis() - startTime< Level.EASY_TIME.setLevel()){
            return String.valueOf(EASY_LEVEL.setLevel());
        } else if ((System.currentTimeMillis() - startTime > EASY_TIME.setLevel())
                && (System.currentTimeMillis() - startTime <= MODERATE_TIME.setLevel())) {
            return String.valueOf(MODERATE_LEVEL.setLevel());
        } else if (System.currentTimeMillis() - startTime > HARD_TIME.setLevel()) {
            return String.valueOf(HARD_LEVEL.setLevel());
        }
        return String.valueOf(EASY_LEVEL.setLevel());
    }

    //make all asteroids can move
    private void moveObjects() {
        asteroids.forEach(asteroid -> asteroid.move());
        projectiles.forEach(projectile -> projectile.move());
        aliens.forEach(alien -> alien.move());
    }
     
    public static void main(String[] args) {
        launch(args);
    }
}