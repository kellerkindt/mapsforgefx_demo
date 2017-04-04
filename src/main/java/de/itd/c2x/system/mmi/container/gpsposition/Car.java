/*
 * Copyright (c) 2013 Michael Watzko and IT-Designers GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.itd.c2x.system.mmi.container.gpsposition;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import de.itd.maps.mapsforge.MapItem;

public class Car extends MapItem {

    private static final double METERS_TO_PIXEL = 1 / 7.5;
    private static final double _RADIUS = 300;

    boolean timelinePlaying = false;
    private Timeline radarTimeline;

    private final SimpleDoubleProperty radiusProperty = new SimpleDoubleProperty(_RADIUS);
    private final SimpleDoubleProperty radiusInPixelProperty = new SimpleDoubleProperty();
    private final SimpleBooleanProperty radiusVisibleProperty = new SimpleBooleanProperty(true);
    private final Polygon directionIndicator;

    private String id = "";

    /**
     * Create a plain Car
     *
     * @param lat
     * @param lon
     */
    public Car(double lat, double lon, String id) {
    	this(lat, lon, Color.RED, Color.PINK, id);
    }

    /**
     * Create a car with an ID String
     *
     * @param lat
     * @param lon
     * @param fill
     * @param stroke
     * @param id
     */
    public Car(double lat, double lon, Color fill, Color stroke, String id) {
    	this(lat, lon, fill, stroke, id, 2);
    }

    /**
     * Create a car with an ID String
     *
     * @param lat
     * @param lon
     * @param fill
     * @param stroke
     * @param id
     * @param fontsize
     */
    public Car(double lat, double lon, Color stroke, Color fill, String id, double fontsize) {
	    super(lat, lon);
	    
	    this.id = id;
	
	    directionIndicator = new Polygon();
	    directionIndicator.getPoints().addAll(new Double[] {
	        0.0, -4.0,
	        -1.0, -2.0,
	        1.0, -2.0 });
	    directionIndicator.setStroke(stroke);
	    directionIndicator.setFill(stroke);
	
	    this.getChildren().add(directionIndicator);
	    
	    Circle circle1	= new Circle(0, 0,  3, fill);
	    Circle circle2	= new Circle(0, 0, 10, new Color(0, 0, 0, 0));
	    
	    circle1.setStroke(stroke);
	    circle1.setStrokeWidth(0.5);
	    circle1.setFill(fill);
	    
	    circle2.setStroke(new Color(0, 0, 0, 0));
	    circle2.setStrokeWidth(0.0);
	    
	    getChildren().add(circle1);
	    getChildren().add(circle2);
	    
	    
	    
	    Text text = new Text(0, 0, id.toString());
	    
	    text.setFont(Font.font("Amble Cn", FontWeight.EXTRA_LIGHT, fontsize));
	    
	    text.setTextAlignment(TextAlignment.CENTER);
	    text.setTextOrigin(VPos.CENTER);
	    text.setFill(stroke);
	    
	    text.rotateProperty().bind(this.rotateProperty().multiply(-1));
	
	    // center text in circle
	    text.setLayoutX(-1 * text.getLayoutBounds().getWidth() / 2);
	
	    this.getChildren().add(text);
	
	    addCarFeatures();
    }

    private void addCarFeatures() {
	    radiusInPixelProperty.bind(radiusProperty().multiply(METERS_TO_PIXEL));
	    addOuterCircle();
	    addRadarSignal();
    }

    private void addOuterCircle() {
    	Circle circleOuter = new Circle(0, 0, 10, new Color(1, 1, 1, 0.15));
    	
    	circleOuter.setStroke(new Color(0.3, 0.3, 0.3, 0.4));
    	circleOuter.setStrokeWidth(.3);
    	
    	circleOuter.radiusProperty()	.bind(radiusInPixelProperty);
    	circleOuter.radiusProperty()	.bind(radiusInPixelProperty);
    	circleOuter.visibleProperty()	.bind(radiusVisibleProperty);
        
        getChildren().add(circleOuter);
    }

    private void addRadarSignal() {
    	Circle radar = new Circle (0, 0, 0, new Color(1, 1, 1, 0.0));
    	
    	radar.setStroke(new Color(0.3, 0.3, 0.3, 0.4));
    	radar.setStrokeWidth(.3);
    	radar.setOpacity(0);
    	
    	getChildren().add(radar);
    	
    	
    	radarTimeline = new Timeline();
    	radarTimeline.getKeyFrames().add(
    			new KeyFrame(
    				Duration.seconds(0),
	    			new KeyValue(radar.radiusProperty(), 0, Interpolator.LINEAR),
	    			new KeyValue(radar.opacityProperty(), 1, Interpolator.LINEAR)
    				)
    			);
    	
    	radarTimeline.getKeyFrames().add(
    			new KeyFrame(
    					Duration.seconds(0.2),
    					new KeyValue(radar.radiusProperty(), _RADIUS * METERS_TO_PIXEL, Interpolator.EASE_OUT),
    					new KeyValue(radar.opacityProperty(), 0, Interpolator.LINEAR)
    					)
    			);
    }

    /**
     * Shows one "Ping" on the Radar
     */
    public void playRadar() {
	    if (radarTimeline != null && radarTimeline.getCurrentRate() == 0)
	        radarTimeline.play();
    }

    /**
     * @return the radiusProperty
     */
    public SimpleDoubleProperty radiusProperty() {
    	return radiusProperty;
    }

    /**
     * @return the radiusVisibleProperty
     */
    public SimpleBooleanProperty radiusVisibleProperty() {
    	return radiusVisibleProperty;
    }

    public DoubleProperty directionProperty() {
    	return this.rotateProperty();
    }

    public String getID() {
    	return this.id;
    }
    
    @Override
    public String toString() {
    	return getClass().getSimpleName()+": "+id;
    }
}