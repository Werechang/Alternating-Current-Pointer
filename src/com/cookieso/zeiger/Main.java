package com.cookieso.zeiger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;

import java.util.ArrayList;

public class Main extends Application implements Runnable {

    private Stage stage;
    private Scene scene;
    private VBox layout;
    private HBox top;
    private GridPane settings;
    private Canvas canvasPointer;
    private Canvas canvasSine;
    private double height = 500;
    private double width = 800;

    private Button timeManager;

    private boolean running = false;

    private double time = 0;
    private boolean isTimeRunning = false;
    private double frequency = 50;
    private Point2D[] sineVoltage;
    private Point2D[] pointer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        stage.setTitle("Wechselstrom Simulation");
        setupScene(stage);
        stage.setOnCloseRequest(event -> {
            running = false;
            stage.close();
            System.exit(0);
        });
        stage.show();
        this.start();
    }

    private void setupScene(Stage stage) {
        layout = new VBox();
        scene = new Scene(layout, width, height);
        layout.setPadding(new Insets(10));

        top = new HBox();
        settings = new GridPane();

        settings.setAlignment(Pos.TOP_LEFT);
        settings.setPadding(new Insets(10, 25, 10, 25));
        settings.setHgap(5);
        settings.setVgap(5);
        settings.setMinSize(width-(height*0.4), height*0.4);

        timeManager = new Button("Start");
        timeManager.setPrefSize(60, 10);
        timeManager.setOnAction(event -> {
            if (!isTimeRunning) {
                if (time>=360) time=0;
                timeManager.setText("Stop");
                isTimeRunning = true;
            } else {
                timeManager.setText("Start");
                isTimeRunning = false;
            }
        });

        settings.add(timeManager, 0, 0, 2, 1);

        top.getChildren().add(settings);
        canvasPointer = new Canvas(height*0.4, height*0.4);
        top.getChildren().add(canvasPointer);

        canvasSine = new Canvas(width, height*0.6);

        layout.getChildren().add(top);
        layout.getChildren().add(canvasSine);
        stage.setScene(scene);
    }

    private void start() {
        if (running) return;
        running = true;
        sineVoltage = calcSine(100);
        pointer = calcPointer(100);
        new Thread(this).start();
        System.out.println("Starting...");
    }

    @Override
    public void run() {
        System.out.println("Running...");
        long lastTime = System.nanoTime();
        long timer = System.currentTimeMillis();
        double ns = 1000000000.0/60;
        double delta = 0;
        int frames = 0;

        while (running) {
            long now = System.nanoTime();
            delta += ((now - lastTime) / ns);
            lastTime = now;
            while (delta >= 1) {
                delta--;
                Platform.runLater(() -> {
                    updateDimensions();
                    render();
                });
                frames++;
                if (isTimeRunning) {
                    if (time<361) {
                        time++;
                    } else {
                        Platform.runLater(() -> timeManager.setText("Start"));
                        isTimeRunning = false;
                    }
                }
            }

            if(System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                int finalFrames = frames;
                Platform.runLater(() -> this.stage.setTitle("Wechselstrom Simulation | " + finalFrames + " fps"));
                frames = 0;
            }
        }
    }

    private void updateDimensions() {
        width = stage.getWidth();
        height = stage.getHeight();

        settings.setMinSize(width-(height*0.4), height*0.4);

        double cPD = height*0.4;
        canvasPointer.setHeight(cPD);
        canvasPointer.setWidth(cPD);

        canvasSine.setHeight(height*0.6);
        canvasSine.setWidth(width);
    }

    private void render() {
        GraphicsContext gP = canvasPointer.getGraphicsContext2D();
        GraphicsContext gS = canvasSine.getGraphicsContext2D();
        renderPointerGrid(gP);
        renderSineGrid(gS);
    }

    private void renderPointerGrid(GraphicsContext g) {
        double canvasHeight = canvasPointer.getHeight();
        double canvasWidth = canvasPointer.getWidth();

        g.setFill(Color.gray(0.8));
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        g.setStroke(Color.gray(0));
        g.setFill(Color.gray(0));

        if (canvasHeight <= canvasWidth) {
            g.strokeOval(5, 5, canvasHeight-10, canvasHeight-10);
        } else {
            g.strokeOval(5, 5, canvasWidth-10, canvasWidth-10);
        }

        renderPointer(g, canvasHeight);
    }

    private void renderSineGrid(GraphicsContext g) {
        double canvasHeight = canvasSine.getHeight();
        double canvasWidth = canvasSine.getWidth();

        g.setFill(Color.gray(0.8));
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        g.setStroke(Color.gray(0));
        g.setFill(Color.gray(0));

        // x-Axis
        g.strokeLine(10, canvasHeight/2, canvasWidth, canvasHeight/2);
        // y-Axis
        g.strokeLine(20, canvasHeight/80, 20, canvasHeight-(canvasHeight/80));

        renderSineVoltage(g, canvasHeight, canvasWidth);
    }

    private void renderPointer(GraphicsContext g, double height) {
        double mid = height/2;
        for (Point2D p : pointer) {
            g.strokeLine(mid, mid, p.getX()+mid, p.getY()+mid);
            System.out.println();
        }
    }

    private void renderSineVoltage(GraphicsContext g, double height, double width) {
        double periodTime = 70;
        double half = height/2;
        Point2D pBefore = null;
        if (sineVoltage != null) {
            g.setStroke(Color.rgb(0, 123, 255));
            for (Point2D p : sineVoltage) {
                if (p.getX() == periodTime) {
                    double x = p.getX()/(360/width)+20;
                    g.setStroke(Color.rgb(0, 150, 110));

                    g.strokeText("T", x-3, half-4);
                    g.strokeLine(x, half-2, x, half+2);

                    g.setStroke(Color.rgb(0, 123, 255));
                }

                if (pBefore != null && p.getX() <= time) {
                    g.strokeLine(pBefore.getX()/(360/width)+20, pBefore.getY()*(height/400)+half, p.getX()/(360/width)+20, p.getY()*(height/400)+half);
                }
                pBefore = p;
            }
        }
    }

    private Point2D[] calcSine(double size) {
        ArrayList<Point2D> points = new ArrayList<>();
        for (int i = 0; i < 360; i++) {
            points.add(new Point2D(i, Math.sin(i/360.0*frequency)*size*(-1)));
        }
        return points.toArray(new Point2D[0]);
    }

    private Point2D[] calcPointer(double size) {
        ArrayList<Point2D> points = new ArrayList<>();
        for (int i = 0; i < 360; i++) {
            points.add(new Point2D(-Math.cos(i/360.0*frequency)*size, -Math.sin(i/360.0*frequency)*size));
        }
        return points.toArray(new Point2D[0]);
    }
}
