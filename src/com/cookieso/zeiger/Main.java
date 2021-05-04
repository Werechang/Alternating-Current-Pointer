package com.cookieso.zeiger;

import com.cookieso.zeiger.ui.NumberField;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;

public class Main extends Application implements Runnable {

    private static final int FPS = 60;

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
    private int sineHeight = 10;

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

        Label frequencyLabel = new Label("Frequenz (Hz)");

        NumberField frequencyField = new NumberField(Integer.toString(frequency));
        frequencyField.setPrefSize(60, 10);
        frequencyField.setOnAction(event -> {
            if (Integer.parseInt(frequencyField.getText()) != frequency) {
                resetSine();
                frequency = Integer.parseInt(frequencyField.getText());
                sineVoltage = calcSine(sineHeight);
            }
        });

        Label phaseOffsetLabel = new Label("Phasenverschiebung (t)");

        NumberField phaseOffsetField = new NumberField(Integer.toString(sineStartOffset));
        phaseOffsetField.setOnAction(event -> {
            if (Integer.parseInt(phaseOffsetField.getText()) != sineStartOffset) {
                resetSine();
                sineStartOffset = Integer.parseInt(phaseOffsetField.getText());
                sineVoltage = calcSine(sineHeight);
            }
        });

        Label maxVoltageLabel = new Label("û (V)");

        NumberField maxVoltageText = new NumberField(Integer.toString(sineHeight));
        maxVoltageText.setOnAction(event -> {
            if (Integer.parseInt(maxVoltageText.getText()) != sineHeight) {
                resetSine();
                int maxSine = Integer.parseInt(maxVoltageText.getText());
                sineHeight = Math.min(maxSine, 19);
                sineHeight = maxSine > -20 ? sineHeight : 1;
                maxVoltageText.setText(Integer.toString(sineHeight));
                sineVoltage = calcSine(sineHeight);
            }
        });

        timeSlider.setMin(0);
        timeSlider.setBlockIncrement(10);
        timeSlider.setMax(360);
        timeSlider.setValue(0);
        timeSlider.setShowTickLabels(true);
        timeSlider.setShowTickMarks(true);
        timeSlider.setPrefSize(300, 20);
        timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> time = (double) Math.round((Double) newValue));

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
        double ns = 1000000000.0/FPS;
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
                    if (time<360) {
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
        double canvasHeightP = canvasPointer.getHeight();
        double canvasWidthP = canvasPointer.getWidth();

        double canvasHeightS = canvasSine.getHeight();
        double canvasWidthS = canvasSine.getWidth();

        GraphicsContext gP = canvasPointer.getGraphicsContext2D();
        GraphicsContext gS = canvasSine.getGraphicsContext2D();
        renderPointerGrid(gP, canvasWidthP, canvasHeightP);
        renderPointer(gP, canvasHeightP);

        renderSineGrid(gS, canvasWidthS, canvasHeightS);
        renderSineVoltage(gS, canvasHeightS, canvasWidthS);
        renderMarkers(gS, canvasHeightS/2);
    }

    private void renderPointerGrid(GraphicsContext g, double canvasWidth, double canvasHeight) {
        g.setFill(Color.gray(0.956));
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        g.setStroke(Color.gray(0));
        g.setFill(Color.gray(0));

        if (canvasHeight <= canvasWidth) {
            g.strokeOval(5, 5, canvasHeight-10, canvasHeight-10);
        } else {
            g.strokeOval(5, 5, canvasWidth-10, canvasWidth-10);
        }
        g.strokeLine(canvasHeight/2, canvasHeight/2, canvasHeight-5, canvasHeight/2);
    }

    private void renderSineGrid(GraphicsContext g, double canvasWidth, double canvasHeight) {
        g.setFill(Color.gray(0.956));
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        g.setStroke(Color.gray(0));
        g.setFill(Color.gray(0));

        // x-Axis
        double heightByTwo = canvasHeight/2;
        g.strokeLine(10, heightByTwo, canvasWidth, heightByTwo);
        // y-Axis
        g.strokeLine(20, canvasHeight/80, 20, canvasHeight-(canvasHeight/80));
    }

    private void renderMarkers(GraphicsContext g, double height) {
        for (int i = 20; i<width && i<=1820; i+=90) {
            g.strokeLine(i, height+2, i, height-2);
            g.strokeText((int) ((i - 20) / 1.8) + " ms", i, height-4);
        }
    }

    private void renderPointer(GraphicsContext g, double height) {
        double mid = height/2;
        g.setStroke(Color.rgb(0, 123, 255));
        double px = Math.cos(time/360.0*frequency*2*Math.PI)*(mid-5)+mid;
        double py = -Math.sin(time/360.0*frequency*2*Math.PI)*(mid-5)+mid;
        g.strokeLine(mid, mid, px, py);

        double arrowWidth = 2;

        double pTriangle1x = Math.cos((time+(arrowWidth/frequency))/360.0*frequency*2*Math.PI)*(mid-10)+mid;
        double pTriangle1y = -Math.sin((time+(arrowWidth/frequency))/360.0*frequency*2*Math.PI)*(mid-10)+mid;

        double pTriangle2x = Math.cos((time-(arrowWidth/frequency))/360.0*frequency*2*Math.PI)*(mid-10)+mid;
        double pTriangle2y = -Math.sin((time-(arrowWidth/frequency))/360.0*frequency*2*Math.PI)*(mid-10)+mid;

        g.setFill(Color.rgb(0, 123, 255));

        double[] arrowX = {pTriangle1x, px, pTriangle2x};
        double[] arrowY = {pTriangle1y, py, pTriangle2y};

        g.fillPolygon(arrowX, arrowY, 3);

        g.setFill(Color.gray(0));
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
        g.setFill(Color.gray(0));
        g.setStroke(Color.gray(0));
    }

    private SinePoint[] calcSine(double size) {
        ArrayList<SinePoint> points = new ArrayList<>();
        for (double i = 0; i < 360; i+=0.01) {
            double y = Math.sin(i/360*frequency*2*Math.PI + (sineStartOffset/10.0))*size*10*(-1);
            points.add(new SinePoint(i, y));
        }
        return points.toArray(new SinePoint[0]);
    }
}
