/*
 * Copyright (c) 2018 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.fx.anglepicker;

import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.util.StringConverter;

import java.util.Locale;


/**
 * User: hansolo
 * Date: 07.11.18
 * Time: 12:30
 */
@DefaultProperty("children")
public class AnglePicker extends Region {
    private static final double                   PREFERRED_WIDTH  = 63;
    private static final double                   PREFERRED_HEIGHT = 63;
    private static final double                   MINIMUM_WIDTH    = 20;
    private static final double                   MINIMUM_HEIGHT   = 20;
    private static final double                   MAXIMUM_WIDTH    = 1024;
    private static final double                   MAXIMUM_HEIGHT   = 1024;
    private              double                   size;
    private              double                   width;
    private              double                   height;
    private              Circle                   background;
    private              Circle                   foreground;
    private              Rectangle                indicator;
    private              Text                     text;
    private              Pane                     pane;
    private              Rotate                   rotate;
    private              double                   _angle;
    private              DoubleProperty           angle;
    private              Paint                    _backgroundPaint;
    private              ObjectProperty<Paint>    backgroundPaint;
    private              Paint                    _foregroundPaint;
    private              ObjectProperty<Paint>    foregroundPaint;
    private              ObjectProperty<Paint>    indicatorPaint;
    private              Paint                    _indicatorPaint;
    private              Paint                    _textPaint;
    private              ObjectProperty<Paint>    textPaint;
    private              InnerShadow              innerShadow;
    private              TextField                textField;
    private              StringConverter<Double>  converter;
    private              EventHandler<MouseEvent> mouseFilter;


    // ******************** Constructors **************************************
    public AnglePicker() {
        getStylesheets().add(AnglePicker.class.getResource("angle-picker.css").toExternalForm());

        rotate           = new Rotate();

        _angle           = 0;
        _backgroundPaint = Color.rgb(32, 32, 32);
        _foregroundPaint = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                                              new Stop(0.0, Color.rgb(61, 61, 61)),
                                              new Stop(0.5, Color.rgb(50, 50, 50)),
                                              new Stop(1.0, Color.rgb(42, 42, 42)));
        _indicatorPaint  = Color.rgb(159, 159, 159);
        _textPaint       = Color.rgb(230, 230, 230);

        converter = new StringConverter<Double>() {
            @Override public String toString(final Double number) {
                return String.format(Locale.US, "%.1f", number);
            }

            @Override public Double fromString(final String string) {
                if (null == string && string.isEmpty()) { return null; }
                String numberString = string.replace("\\n", "").replace("\u00b0", "");
                if (numberString.matches("\\d{0,3}([\\.]\\d{0,1})?")) {
                    return Double.valueOf(numberString);
                } else {
                    return 0d;
                }
            }
        };

        mouseFilter      = evt -> {
            EventType<? extends MouseEvent> type = evt.getEventType();
            if (type.equals(MouseEvent.MOUSE_DRAGGED)) {
                double angle = getAngleFromXY(evt.getX() + size * 0.5, evt.getY() + size * 0.5, size * 0.5, size * 0.5, 0);
                setAngle(angle);
            } else if (type.equals(MouseEvent.MOUSE_CLICKED)) {
                int clicks = evt.getClickCount();
                if (clicks == 2) {
                    textField.setManaged(true);
                    textField.setVisible(true);
                }
            }
        };

        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void initGraphics() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 || Double.compare(getWidth(), 0.0) <= 0 ||
            Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        getStyleClass().add("angle-picker");

        rotate.setAngle(0);

        innerShadow = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(255, 255, 255, 0.3), 1, 0.0, 0, 0.5);

        background = new Circle();
        background.setFill(_backgroundPaint);
        background.setMouseTransparent(true);

        foreground = new Circle();
        foreground.setFill(_foregroundPaint);
        foreground.setEffect(innerShadow);

        indicator = new Rectangle();
        indicator.getTransforms().add(rotate);
        indicator.setMouseTransparent(true);

        text = new Text(String.format(Locale.US, "%.0f\u00b0", _angle));
        text.setTextOrigin(VPos.CENTER);
        text.setMouseTransparent(true);

        textField = new TextField(String.format(Locale.US, "%.0f\u00b0", _angle));
        //textField.setRegex("\\d{0,3}([\\.]\\d{0,1})?");
        textField.textFormatterProperty().setValue(new TextFormatter<>(converter, getAngle()));
        textField.setPadding(new Insets(2));
        textField.setAlignment(Pos.CENTER_RIGHT);
        textField.setVisible(false);
        textField.setManaged(false);

        pane = new Pane(background, foreground, indicator, text, textField);

        getChildren().setAll(pane);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
        foreground.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseFilter);
        foreground.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseFilter);
        textField.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) { updateTextField(); }
        });
        textField.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv) { updateTextField(); }
        });
        focusedProperty().addListener((o, ov, nv) -> {
            if (!nv) { updateTextField(); }
        });
    }


    // ******************** Methods *******************************************
    @Override public void layoutChildren() {
        super.layoutChildren();
    }

    @Override protected double computeMinWidth(final double HEIGHT) { return MINIMUM_WIDTH; }
    @Override protected double computeMinHeight(final double WIDTH) { return MINIMUM_HEIGHT; }
    @Override protected double computePrefWidth(final double HEIGHT) { return super.computePrefWidth(HEIGHT); }
    @Override protected double computePrefHeight(final double WIDTH) { return super.computePrefHeight(WIDTH); }
    @Override protected double computeMaxWidth(final double HEIGHT) { return MAXIMUM_WIDTH; }
    @Override protected double computeMaxHeight(final double WIDTH) { return MAXIMUM_HEIGHT; }

    @Override public ObservableList<Node> getChildren() { return super.getChildren(); }

    public double getAngle() { return null == angle ? _angle : angle.get(); }
    public void setAngle(final double angle) {
        if (null == this.angle) {
            _angle = angle;
            rotate.setAngle(_angle);
            text.setText(String.format(Locale.US, "%.0f\u00b0", _angle));
            text.relocate((size - text.getLayoutBounds().getWidth()) * 0.5, (size - text.getLayoutBounds().getHeight()) * 0.5);
            textField.setText(String.format(Locale.US, "%.0f\u00b0", _angle));
        } else {
            this.angle.set(angle);
        }
    }
    public DoubleProperty angleProperty() {
        if (null == angle) {
            angle = new DoublePropertyBase(_angle) {
                @Override protected void invalidated() {
                    rotate.setAngle(get());
                    text.setText(String.format(Locale.US, "%.0f\u00b0", get()));
                    text.relocate((size - text.getLayoutBounds().getWidth()) * 0.5, (size - text.getLayoutBounds().getHeight()) * 0.5);
                    textField.setText(String.format(Locale.US, "%.0f\u00b0", get()));
                }
                @Override public Object getBean() { return AnglePicker.this; }
                @Override public String getName() { return "angle"; }
            };
        }
        return angle;
    }

    public Paint getBackgroundPaint() { return null == backgroundPaint ? _backgroundPaint : backgroundPaint.get(); }
    public void setBackgroundPaint(final Paint backgroundPaint) {
        if (null == this.backgroundPaint) {
            _backgroundPaint = backgroundPaint;
            redraw();
        } else {
            this.backgroundPaint.set(backgroundPaint);
        }
    }
    public ObjectProperty<Paint> backgroundPaintProperty() {
        if (null == backgroundPaint) {
            backgroundPaint = new ObjectPropertyBase<Paint>(_backgroundPaint) {
                @Override protected void invalidated() { redraw(); }
                @Override public Object getBean() { return AnglePicker.this; }
                @Override public String getName() { return "backgroundPaint"; }
            };
            _backgroundPaint = null;
        }
        return backgroundPaint;
    }

    public Paint getForegroundPaint() { return null == foregroundPaint ? _foregroundPaint : foregroundPaint.get(); }
    public void setForegroundPaint(final Paint foregroundPaint) {
        if (null == this.foregroundPaint) {
            _foregroundPaint = foregroundPaint;
            redraw();
        } else {
            this.foregroundPaint.set(foregroundPaint);
        }
    }
    public ObjectProperty<Paint> foregroundPaintProperty() {
        if (null == foregroundPaint) {
            foregroundPaint = new ObjectPropertyBase<Paint>(_foregroundPaint) {
                @Override protected void invalidated() { redraw(); }
                @Override public Object getBean() { return AnglePicker.this; }
                @Override public String getName() { return "foregroundPaint"; }
            };
            _foregroundPaint = null;
        }
        return foregroundPaint;
    }

    public Paint getIndicatorPaint() { return null == indicatorPaint ? _indicatorPaint : indicatorPaint.get(); }
    public void setIndicatorPaint(final Paint indicatorPaint) {
        if (null == this.indicatorPaint) {
            _indicatorPaint = indicatorPaint;
            redraw();
        } else {
            this.indicatorPaint.set(indicatorPaint);
        }
    }
    public ObjectProperty<Paint> indicatorPaintProperty() {
        if (null == indicatorPaint) {
            indicatorPaint = new ObjectPropertyBase<Paint>(_indicatorPaint) {
                @Override protected void invalidated() { redraw(); }
                @Override public Object getBean() { return AnglePicker.this; }
                @Override public String getName() { return "indicatorPaint"; }
            };
            _indicatorPaint = null;
        }
        return indicatorPaint;
    }

    public Paint getTextPaint() { return null == textPaint ? _textPaint : textPaint.get(); }
    public void setTextPaint(final Paint textPaint) {
        if (null == this.textPaint) {
            _textPaint = textPaint;
            redraw();
        } else {
            this.textPaint.set(textPaint);
        }
    }
    public ObjectProperty<Paint> textPaintProperty() {
        if (null == textPaint) {
            textPaint = new ObjectPropertyBase<Paint>(_textPaint) {
                @Override protected void invalidated() { redraw(); }
                @Override public Object getBean() { return AnglePicker.this; }
                @Override public String getName() { return "textPaint"; }
            };
            _textPaint = null;
        }
        return textPaint;
    }

    private void updateTextField() {
        String text = textField.getText().replace("\\n", "").replace("\u00b0", "");
        if (!text.matches("\\d{0,3}([\\.]\\d{0,1})?")) { return; }
        if (null != text && !text.isEmpty()) { setAngle(Double.parseDouble(textField.getText())); }
        textField.setVisible(false);
        textField.setManaged(false);
    }

    private double getAngleFromXY(final double x, final double y, final double centerX, final double centerY, final double angleOffset) {
        // For ANGLE_OFFSET =  0 -> Angle of 0 is at 3 o'clock
        // For ANGLE_OFFSET = 90  ->Angle of 0 is at 12 o'clock
        double deltaX = x - centerX;
        double deltaY = y - centerY;
        double radius = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        double nx     = deltaX / radius;
        double ny     = deltaY / radius;
        double theta  = Math.atan2(ny, nx);
        theta         = Double.compare(theta, 0.0) >= 0 ? Math.toDegrees(theta) : Math.toDegrees((theta)) + 360.0;
        double angle  = (theta + angleOffset) % 360;
        return angle;
    }


    // ******************** Resizing ******************************************
    private void resize() {
        width  = getWidth() - getInsets().getLeft() - getInsets().getRight();
        height = getHeight() - getInsets().getTop() - getInsets().getBottom();
        size   = width < height ? width : height;

        if (width > 0 && height > 0) {
            pane.setMaxSize(size, size);
            pane.setPrefSize(size, size);
            pane.relocate((getWidth() - size) * 0.5, (getHeight() - size) * 0.5);

            rotate.setPivotX(indicator.getX() - size * 0.27777778);
            rotate.setPivotY(indicator.getHeight() * 0.5);

            innerShadow.setRadius(size * 0.0212766);
            innerShadow.setOffsetY(size * 0.0106383);

            background.setRadius(size * 0.5);
            background.relocate(0, 0);

            foreground.setRadius(size * 0.4787234);
            foreground.relocate(size * 0.0212766, size * 0.0212766);

            indicator.setWidth(size * 0.20);
            indicator.setHeight(size * 0.01587302);
            indicator.relocate(size * 0.77777778, (size - indicator.getHeight()) * 0.5);

            text.setFont(Font.font(size * 0.19148936));
            text.relocate((size - text.getLayoutBounds().getWidth()) * 0.5, (size - text.getLayoutBounds().getHeight()) * 0.5);

            textField.setPrefWidth(size * 0.6);
            textField.setPrefHeight(size * 0.22);
            textField.setFont(Font.font(size * 0.19148936));
            textField.relocate((size - textField.getPrefWidth()) * 0.5, (size - textField.getPrefHeight()) * 0.5);

            redraw();
        }
    }

    private void redraw() {
        background.setFill(getBackgroundPaint());
        foreground.setFill(getForegroundPaint());
        indicator.setFill(getIndicatorPaint());
        text.setFill(getTextPaint());
    }
}
