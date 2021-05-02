package com.cookieso.zeiger;

import com.cookieso.zeiger.ui.NumberField;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;

public class Main extends Application implements Runnable {

    private Stage stage;
    private GridPane settings;
    private Canvas canvasPointer;
    private Canvas canvasSine;
    private double height = 500;
    private double width = 800;

    private Button timeManager;
    Slider timeSlider;

    private boolean running = false;

    private double time = 0;
    private boolean isTimeRunning = false;
    private int frequency = 20;
    private SinePoint[] sineVoltage;
    private double coordinateOffset = 0;
    private int sineStartOffset = 0;
    private int sineHeight = 100;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Wechselstrom Simulation");
        setupScene(stage);
        stage.setOnCloseRequest(event -> {
            running = false;
            stage.close();
            System.exit(0);
        });
        stage.show();
        sineVoltage = calcSine(sineHeight);
        this.start();
    }

    private void setupScene(Stage stage) {
        VBox layout = new VBox();
        Scene scene = new Scene(layout, width, height);
        layout.setPadding(new Insets(10));

        HBox top = new HBox();
        settings = new GridPane();

        timeSlider = new Slider();

        settings.setAlignment(Pos.TOP_LEFT);
        settings.setPadding(new Insets(10, 25, 10, 25));
        settings.setHgap(10);
        settings.setVgap(10);
        settings.setMinSize(width-(height*0.4)-50, height*0.4);

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

        Label frequencyLabel = new Label("Frequenz");

        NumberField frequencyField = new NumberField(Integer.toString(frequency));
        frequencyField.setPrefSize(60, 10);
        frequencyField.setOnAction(event -> {
            resetSine();
            frequency = Integer.parseInt(frequencyField.getText());
            sineVoltage = calcSine(sineHeight);
        });

        Label phaseOffsetLabel = new Label("Phasenverschiebung");

        NumberField phaseOffsetField = new NumberField(Integer.toString(sineStartOffset));
        phaseOffsetField.setOnAction(event -> {
            resetSine();
            sineStartOffset = Integer.parseInt(phaseOffsetField.getText());
            sineVoltage = calcSine(sineHeight);
        });

        Label maxVoltageLabel = new Label("û");

        NumberField maxVoltageText = new NumberField(Integer.toString(sineHeight));
        maxVoltageText.setOnAction(event -> {
            resetSine();
            int maxSine = Integer.parseInt(maxVoltageText.getText());
            sineHeight = Math.min(maxSine, 190);
            sineHeight = maxSine > 0 ? sineHeight : 1;
            maxVoltageText.setText(Integer.toString(sineHeight));
            sineVoltage = calcSine(sineHeight);
        });

        timeSlider.setMin(0);
        timeSlider.setBlockIncrement(10);
        timeSlider.setMax(360);
        timeSlider.setValue(0);
        timeSlider.setShowTickLabels(true);
        timeSlider.setShowTickMarks(true);
        timeSlider.setPrefSize(300, 20);
        timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> time = (double) newValue);

        settings.add(timeManager, 0, 0, 2, 1);
        settings.add(timeSlider, 0, 1);
        settings.add(frequencyLabel, 0, 2);
        settings.add(frequencyField, 1, 2);
        settings.add(phaseOffsetLabel, 0, 3);
        settings.add(phaseOffsetField, 1, 3);
        settings.add(maxVoltageLabel, 0, 4);
        settings.add(maxVoltageText, 1, 4);

        top.getChildren().add(settings);
        canvasPointer = new Canvas(height*0.4, height*0.4);
        top.getChildren().add(canvasPointer);

        canvasSine = new Canvas(width, height*0.6);

        layout.getChildren().add(top);
        layout.getChildren().add(canvasSine);
        stage.setScene(scene);
    }

    private void resetSine() {
        isTimeRunning = false;
        timeManager.setText("Start");
        timeSlider.setValue(0);
        time=0;
    }

    private void start() {
        if (running) return;
        running = true;
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
                        timeSlider.setValue(time);
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

        double cPD = height*0.4;

        settings.setMinSize(width-(cPD)-50, cPD);

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

        g.setFill(Color.gray(0.956));
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

        g.setFill(Color.gray(0.956));
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        g.setStroke(Color.gray(0));
        g.setFill(Color.gray(0));

        // x-Axis
        double heightByTwo = canvasHeight/2;
        g.strokeLine(10, heightByTwo, canvasWidth, heightByTwo);
        // y-Axis
        g.strokeLine(20, canvasHeight/80, 20, canvasHeight-(canvasHeight/80));

        renderMarkers(g, heightByTwo);
        renderSineVoltage(g, canvasHeight, canvasWidth);
    }

    private void renderMarkers(GraphicsContext g, double height) {
        for (double i = 20; i<361; i+=20) {
            g.strokeLine(i*5, height+2, i*5, height-2);
        }
    }

    private void renderPointer(GraphicsContext g, double height) {
        double mid = height/2;
        g.setStroke(Color.rgb(0, 123, 255));
        g.strokeLine(mid, mid, Math.cos(time/360.0*frequency)*102+mid, -Math.sin(time/360.0*frequency)*102+mid);
        g.setStroke(Color.gray(0));
    }

    private void renderSineVoltage(GraphicsContext g, double height, double width) {
        double periodTime = 70.5;
        double half = height/2;
        SinePoint pBefore = null;
        if (sineVoltage != null) {
            g.setStroke(Color.rgb(0, 123, 255));
            for (SinePoint p : sineVoltage) {
                if (p.getX() == periodTime) {
                    System.out.println("Draw");
                    double x = p.getX()/(360/width)+20;
                    g.setStroke(Color.rgb(0, 150, 110));

                    g.strokeText("T", x-3, half-4);
                    g.strokeLine(x, half-2, x, half+2);

                    g.setStroke(Color.rgb(0, 123, 255));
                }

                if (pBefore != null && p.getX() <= time) {
                    g.strokeLine(pBefore.getX()*5+20+coordinateOffset, pBefore.getY()*(height/400)+half, p.getX()*5+20+coordinateOffset, p.getY()*(height/400)+half);
                }
                pBefore = p;
            }
        }
    }

    private SinePoint[] calcSine(double size) {
        ArrayList<SinePoint> points = new ArrayList<>();
        for (double i = 0; i < 360; i+=0.1) {
            points.add(new SinePoint(i, Math.sin(i/360.0*frequency + (sineStartOffset/10.0))*size*(-1)));
        }
        return points.toArray(new SinePoint[0]);
    }
}
