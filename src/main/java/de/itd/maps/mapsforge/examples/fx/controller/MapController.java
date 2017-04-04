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

package de.itd.maps.mapsforge.examples.fx.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import org.mapsforge.core.model.GeoPoint;

import de.itd.maps.mapsforge.MapsforgeMap;
import de.itd.maps.mapsforge.examples.fx.Main;


public class MapController implements Initializable {
	

	@FXML private AnchorPane	paneMap					= null;
	@FXML private Label			labelMouseX				= null;
	@FXML private Label			labelMouseY				= null;
	@FXML private Label			labelMouseLatitude		= null;
	@FXML private Label			labelMouseLongitude		= null;
	@FXML private Slider		sliderZoom				= null;
	@FXML private Slider		sliderFileLimit			= null;
	@FXML private Canvas		canvas					= null;
	
	@FXML private Label			labelMemoryTotal		= null;
	@FXML private Label			labelMemoryMax			= null;
	@FXML private Label			labelMemoryUsed			= null;
	@FXML private Label			labelMemoryFree			= null;
	@FXML private Label			labelFileLimit			= null;
	
	@FXML private ToggleButton	checkBoxRecordRoute			= null;
	@FXML private CheckBox		checkBoxUseFileCache		= null;
	@FXML private CheckBox		checkBoxFileLimitUnlimit	= null;
	
	
	@FXML private ProgressBar	progressBarMemoryTileCacheUsage		= null;
	@FXML private ProgressBar	progressBarFileTileCacheUsage		= null;
	@FXML private TextField		textFieldMemoryTileCacheCapacity	= null;
	
	
	private GeoPoint		lastMousePosition;
	private Main			main;
	
	private MapsforgeMap		mapEngine;
	
	public MapController (Main main) {
		// initialize
		this.main		= main;
	}
	
	

	@Override
	@SuppressWarnings("unchecked")
	public void initialize(URL location, ResourceBundle resources) {


		mapEngine	= new MapsforgeMap();
		paneMap.getChildren().add(mapEngine);
		
		// register the mapEngine
		main.setMap(mapEngine);
		
		
		// to see the changes
		checkBoxUseFileCache.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				mapEngine.updateMap(false);
			}
		});
		
		
//		// fit the canvas to parent width
//		paneMap.widthProperty().addListener(new ChangeListener<Number>() {
//			@Override
//			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
//				canvas.setWidth((Double)newValue);
//			}
//		});
//		
//		// fit the canvas to parent height
//		paneMap.heightProperty().addListener(new ChangeListener<Number>() {
//			@Override
//			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
//				canvas.setHeight((Double)newValue);
//			}
//		});


		// register the Event listeners
		mapEngine.setOnMouseMoved(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent paramT) {
				onMouseMoved(paramT);
			}
		});
		
		
		// does not want to work with bindings :(
		InvalidationListener progressBarMemoryListener = new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) {
				if (mapEngine.memoryTileCacheCapacity().get() > 0) {
					progressBarMemoryTileCacheUsage.setProgress(
							mapEngine.memoryTileCacheTileCount().doubleValue() / mapEngine.memoryTileCacheCapacity().doubleValue()
							);
				}
			}
		};
		
		
		mapEngine.memoryTileCacheCapacity()	.addListener(progressBarMemoryListener);
		mapEngine.memoryTileCacheTileCount().addListener(progressBarMemoryListener);
		
		InvalidationListener progressBarFileListener = new InvalidationListener() {
			
			@Override
			public void invalidated(Observable observable) {
				if (mapEngine.fileTileCacheCapacity().get() > 0) {
					progressBarFileTileCacheUsage.setProgress(
							mapEngine.fileTileCacheTileCount().doubleValue() / mapEngine.fileTileCacheCapacity().doubleValue()
							);
				}
			}
		};
		
		mapEngine.fileTileCacheCapacity()	.addListener(progressBarFileListener);
		mapEngine.fileTileCacheTileCount()	.addListener(progressBarFileListener);
		
	
		
		// binds
		labelFileLimit.visibleProperty().bind(checkBoxFileLimitUnlimit.selectedProperty().not());
		
		checkBoxFileLimitUnlimit.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				mapEngine.fileTileCacheCapacity().set( newValue ? Integer.MAX_VALUE : sliderFileLimit.valueProperty().intValue() );
				
			}
		});
		
		sliderFileLimit.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				checkBoxFileLimitUnlimit.setSelected(false);
				mapEngine.fileTileCacheCapacity().set(newValue.intValue());
			}
		});
		
		mapEngine.fileTileCacheCapacity().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				labelFileLimit.setText("" + newValue.intValue());
			}
		});
	
		
		// mapEngine-binds
		mapEngine.getMapView().zoomProperty()	.bindBidirectional(sliderZoom.valueProperty());
		mapEngine.useFileTileCacheProperty()	.bindBidirectional(checkBoxUseFileCache.selectedProperty());
		
		StringConverter<? extends Number> converter = new IntegerStringConverter();
		Bindings.bindBidirectional(textFieldMemoryTileCacheCapacity.textProperty(), mapEngine.memoryTileCacheCapacity(), (StringConverter<Number>)converter);
		
		
		
		sliderZoom.setValue(15);
		
		
		// update the zoom level (MapView)
		sliderZoom.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				mapEngine.getMapView().setZoomLevel((byte)(double)((Double)newValue));
				mapEngine.updateMap(false);
			}
		});
		
		
//		// record listener
//		checkBoxRecordRoute.selectedProperty().addListener(new ChangeListener<Boolean>() {
//			@Override
//			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
//				if (newValue) {
//					// create the list for recording
//					currentRoute = new Route<MapPosition>();
//					
//					// add the route
//				//	main.getMapforge().addRoute(currentRoute);
//					
//				} else {
//					// do not record
//					currentRoute = null;
//				}
//				System.out.println ("recording="+newValue);	// TODO
//			}
//		});
		
		
		// TODO dirty
		// thread that updates the memory usage
		new Thread(){
			@Override
			public void run() {
				while (!mapEngine.isDestroyed()) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							updateMemoryUsage();
						}
					});
					try { Thread.sleep(500); } catch (Throwable t) {}
				}
			}
		}.start();;
	}
	
	/**
	 * Updates the memory-usage, needs to be performed by
	 * the FX-Thread
	 */
	public void updateMemoryUsage () {
		Runtime runtime = Runtime.getRuntime();
		
		double total  		= runtime.totalMemory() / 1024d / 1024d;
		double free			= runtime.freeMemory()  / 1024d / 1024d;
		double max			= runtime.maxMemory()	/ 1024d / 1024d;
		double used			= total - free;
		
		labelMemoryTotal.setText(String.format("%9.2f MB", total));
		labelMemoryMax	.setText(String.format("%9.2f MB", 	max));
		labelMemoryFree	.setText(String.format("%9.2f MB", free));
		labelMemoryUsed	.setText(String.format("%9.2f MB", used));
	}
	
	public void onClear (ActionEvent event) {
		int before = mapEngine.memoryTileCacheCapacity().get();
		mapEngine.memoryTileCacheCapacity().set(0);
		mapEngine.memoryTileCacheCapacity().set(before);
	}
	
	/**
	 * @param event {@link MouseEvent} to calculate on
	 * @return The absolute mouse x value for the given {@link MouseEvent}
	 */
	public double getMouseX (MouseEvent event) {
		return mapEngine.getMapView().getX() + (event.getX() - canvas.getWidth()/2);
	}
	
	/**
	 * @param event {@link MouseEvent} to calculate on
	 * @return The absolute mouse y value for the given {@link MouseEvent}
	 */
	public double getMouseY (MouseEvent event) {
		return mapEngine.getMapView().getY() + (event.getY() - canvas.getHeight()/2);
	}
	
	/**
	 * Called by JavaFx if the mouse moved over the {@link Canvas}
	 * @param event
	 */
	public void onMouseMoved (MouseEvent event) {
		labelMouseX.setText(String.format("%.0f", event.getX()));
		labelMouseY.setText(String.format("%.0f", event.getY()));
		
		lastMousePosition	= mapEngine.getGeoPoint(event.getX(), event.getY());
		
		labelMouseLatitude	.setText(String.format("%2.6f", lastMousePosition.latitude));
		labelMouseLongitude	.setText(String.format("%2.6f", lastMousePosition.longitude));
	}
}
