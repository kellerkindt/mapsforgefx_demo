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

package de.itd.maps.mapsforge.examples.fx;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import org.apache.log4j.Logger;
import org.mapsforge.core.model.GeoPoint;

import de.itd.c2x.system.mmi.container.gpsposition.Car;
import de.itd.maps.mapsforge.MapItem;
import de.itd.maps.mapsforge.MapsforgeMap;
import de.itd.maps.mapsforge.MapsforgeMapContextMenu.ContextActionEvent;
import de.itd.maps.mapsforge.MapsforgeMapContextMenu.ContextEntry;
import de.itd.maps.mapsforge.tiles.LiveRenderRule;
import de.itd.maps.mapsforge.tiles.LiveRenderRule.Drawable;

public class Main extends Application {
	
	public static final int PRIO_FOLLOW_HARD = 1000;
	public static final int PRIO_FOLLOW_SOFT = 100;

    private MapsforgeMap 	map;
    private Logger 			logger = Logger.getLogger(getClass());

    /**
     * Start, just launch the {@link Application}
     * @param args Arguments given while starting this program
     * @throws IOException
     */
    public static void main(String args[]) throws IOException {
		// start the JavaFX scene
		launch(args);
    }

    @Override
    public void start(final Stage stage) throws Exception {
    
		logger.debug("Application is starting");
		logger.debug("Going to load and show scene");

		
		// load the main scene and show it
		stage.setScene(new Scene(loadFXML("/fxml/scene_main.fxml"))); // TODO
		stage.show();
		stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				logger.debug("---------------------");
				logger.debug("Received close rquest");
				logger.debug("Going to destroy "+map.getClass().getSimpleName());
				map.destroy();
				
				logger.debug("Going to exit platform");
				Platform.exit();
			}
		});
		
	
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
	
				logger.debug("Going to load map");
				map.loadMap( new File(
						getClass().getProtectionDomain().getCodeSource().getLocation().getFile(),
						"maps/stuttgart_with_lanes.map"
					)
				);
				logger.debug("Map loaded");
				
				
				
				
				/*
				 * 
				 * Create a small ContextEntry, that creates a Car
				 * The Car has a ContextMenu, that allows to delete it
				 * 
				 */
				map.getMapContextMenu().add(new ContextEntry() {

					private StringProperty text = new SimpleStringProperty("Add Car");
					
					@Override
					public ReadOnlyStringProperty textProperty() {
						return text;
					}
					
					@Override
					public void onAction(ContextActionEvent event) {
						// get the position in latitude and longitude
						GeoPoint point = map.getGeoPoint(
								event.getPositionMap().getX(),
								event.getPositionMap().getY()
								);
						
						// create the car
						addSampleCar(map, point.latitude, point.longitude);
					}
				});
				
				
				
				// create a Car and follow it softly
				Car car = addSampleCar(map, 48.71, 9.36);

				addCarMovement(stage, car, 0.0, 0.001);
				
				map.getMapView().follow(
						car,
						PRIO_FOLLOW_SOFT
						);
				
				
				
				// be able to change the render behavior
				addLiveRenderRuleEntries();
				
				
				
				// update the map, if the view has changed
				InvalidationListener moveListener = new InvalidationListener() {
					@Override
					public void invalidated(Observable observable) {
						map.updateMap(false);
					}
				};
				
				map.getMapView().longitudeProperty().addListener(moveListener);
				map.getMapView().latitudeProperty() .addListener(moveListener);
				
				
				
				/*
				 * Set the priority of an MouseDrag
				 * 
				 * If there is a follow request, with a lower
				 * priority, the engine wont follow it anymore,
				 * if the priority is higher, the MouseDrag
				 * will be ignored
				 */
				map.setPriorityMouseDrag(PRIO_FOLLOW_SOFT);
				
				
				
				// initialization done, draw the map
				logger.debug("Going to draw the map");
				map.updateMap(false);
				
				
				
				/*
				 * Stress test results
				 *  - a lot of Platform.runLater calls slow it down
				 *  - more calls of Platform.runLater than able to process
				 *    will get the thing down
				 *    --> Heavy operations, should not be called again,
				 *        if the operation before has not finished yet
				 */
//				performStressTest(stage);

		    }
		});
    }
    
    /**
     * Adds a sample to the {@link MapsforgeMap} at the 
     * given position
     * 
     * @param map	{@link MapsforgeMap} to add the sample to
     * @param lat	Latitude coordinate of the position
     * @param lon	Longitude coordinate of the position
     * @return The added sample
     */
    public Car addSampleCar (final MapsforgeMap map, double lat, double lon) {
    	// create a car at the given position
    	final Car car = new Car(lat, lon, "Sample");
    	
    	
    	// add it to the map
    	map.addMapItem(car);
    	
    	
    	
    	// add menu entry to un-follow
		car.getContextEntries().add(new ContextEntry() {
			
			private StringProperty text = new SimpleStringProperty("Unfollow");
			
			@Override
			public ReadOnlyStringProperty textProperty() {
				return text;
			}
			
			@Override
			public void onAction(ContextActionEvent event) {
				if (map.getMapView().isFollowing(car)) {
					map.getMapView().unfollow();
				}
			}
		});
		
		// add menu entry to follow soft
		car.getContextEntries().add(new ContextEntry() {
			
			private StringProperty text = new SimpleStringProperty("Follow (Soft)");
			
			@Override
			public ReadOnlyStringProperty textProperty() {
				return text;
			}
			
			@Override
			public void onAction(ContextActionEvent event) {
				map.getMapView().follow(car, PRIO_FOLLOW_SOFT);
			}
		});
		
		// add menu entry to follow hard
		car.getContextEntries().add(new ContextEntry() {
			
			private StringProperty text = new SimpleStringProperty("Follow (Hard)");
			
			@Override
			public ReadOnlyStringProperty textProperty() {
				return text;
			}
			
			@Override
			public void onAction(ContextActionEvent event) {
				map.getMapView().follow(car, PRIO_FOLLOW_HARD);
			}
		});
		
		// add menu entry to delete
		car.getContextEntries().add(new ContextEntry() {
			
			private StringProperty text = new SimpleStringProperty("Delete");
			
			@Override
			public ReadOnlyStringProperty textProperty() {
				return text;
			}
			
			@Override
			public void onAction(ContextActionEvent event) {
				map.removeMapItem(car);
			}
		});
		
		
    	return car;
    }
    
    public void performStressTest (final Stage stage) {
    	final long toSleep = 1000 / 5;
		new Thread(){
			public void run() {
				

				final List<MapItem> items	= new ArrayList<>();
				
				for (int i = 0; i < 9999; i++) {
					
					final MapItem car = new Car(48.71, 9.36007+(i*.001), "i="+i);
					car.getContextEntries().add(new ContextEntry() {
						
						private StringProperty text = new SimpleStringProperty("Delete");
						
						@Override
						public ReadOnlyStringProperty textProperty() {
							return text;
						}
						
						@Override
						public void onAction(ContextActionEvent event) {
							map.removeMapItem(car);
						}
					});
					items	.add(car);
				}
				
				logger.debug("All MapItems created");
				for (MapItem i : items) {
					map.addMapItem(i);
				}
				logger.debug("All MapItems added");
				
				

				final Object 			o 	= new Object ();
				final BooleanProperty	ran	= new SimpleBooleanProperty(true);
				
				while (stage.isShowing()) {
					try {
						while (!ran.get()) {
							Thread.sleep(1);
						}
						
						// reset
						ran.set(false);

						Platform.runLater(new Runnable() {
							
							@Override
							public void run() {
								try {
									double toAdd = 0.0005 * (toSleep / 1000d);
									
									// test
									for (final MapItem i : items) {
								
										// update the item
										i.latitudeProperty() .set( i.latitudeProperty() .get() + toAdd );
										i.longitudeProperty().set( i.longitudeProperty().get() + toAdd );

									}

									map.updateMap(false);
									
									synchronized (o) {
										o.notifyAll();
									}
								} finally {
									synchronized (o) {
										ran.set(true);
									}
								}
							}
						});
								
						
						long timeBefore = System.currentTimeMillis();
						synchronized (o) {
							if (!ran.get()) {
								o.wait(toSleep);
							}
						}
						
						long timeSleep = toSleep - (System.currentTimeMillis() - timeBefore);
						if (timeSleep > 0) {
							// some sleep
							Thread.sleep(timeSleep);
							
						} else {
							logger.warn("No time left to sleep!");
							while (!ran.get() && stage.isShowing()) {
								Thread.sleep(1);
							}
						}
						
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
			
		}.start();
    }
    
    /**
     * Adds a constantly movement to the given {@link Car}
     * 
     * @param stage	Current {@link Stage}, to watch when to end
     * @param car	{@link Car} to move
     * @param lat	latitude to move the {@link Car} by each second
     * @param lon	longitude to move the {@link Car} by each second
     */
    public void addCarMovement (final Stage stage, Car car, final double lat, final double lon) {
    	
    	final long 		sleep 	= 100;
    	final double	fact	= 1000d / sleep;
    	
    	final WeakReference<Car> ref = new WeakReference<Car>(car);
    	
		new Thread () {
			public void run() {
				// as long, as the stage is shown
				while (stage.isShowing() && ref.get() != null) {
					
					// some sleep time
					try { Thread.sleep(sleep); } catch (Throwable t) {}
					
					// needs to be executed in the FX-Thread
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// get the car
							Car car = ref.get();
							
							// check whether it still exists
							if (car != null) {
								// update the position
								car.latitudeProperty() .set( car.latitudeProperty() .get() + (lat/fact) );
								car.longitudeProperty().set( car.longitudeProperty().get() + (lon/fact) );
								
								// play the radar sometimes
								if (new Random().nextInt((int)fact) == 0) {
									car.playRadar();
								}
							}
						}
					});
				}
				
			}
		}.start();
    	
    }
    
    /**
     * Adds {@link ContextEntry}s to change the {@link LiveRenderRule}s
     */
    public void addLiveRenderRuleEntries () {
		for (final Drawable drawable : Drawable.values()) {
			
			map.getMapContextMenu().add(new ContextEntry() {

				private BooleanProperty			set	 = new SimpleBooleanProperty(); 
				private ReadOnlyStringProperty 	text = new SimpleStringProperty() {
					{
						set.addListener(new InvalidationListener() {
							@Override
							public void invalidated(Observable observable) {
								update();
							}
						});
						update();
					}
					
					private void update () {
						set((set.get() ? "[x] " : "[ ] ") + drawable);
					}
				};
				
				{
					// load
					set.set(map.getLiveRenderRule().isAllowed(drawable));
				}
				
				@Override
				public ReadOnlyStringProperty textProperty() {
					set.setValue(map.getLiveRenderRule().isAllowed(drawable));
					return text;
				}
				
				@Override
				public void onAction(ContextActionEvent event) {
					map.getLiveRenderRule().setAllowed(drawable, !map.getLiveRenderRule().isAllowed(drawable));
					
					// update
					set.set(map.getLiveRenderRule().isAllowed(drawable));
					
					// clear cache
					map.clearMemoryTileCache();
					map.updateMap(false);
				}
			}, "LiveRenderRule");
		}
    }
    

    /**
     * Sets the {@link MapsforgeMap} if it isn't set yet
     * @param engine {@link MapsforgeMap} to set
     */
    public void setMap(MapsforgeMap engine) {
		if (this.map == null) {
		    this.map = engine;
		}
    }

    /**
     * @return The {@link MapsforgeMap} that draws the map
     */
    public MapsforgeMap getMap() {
    	return map;
    }
    

    /**
     * Loads the {@link Parent} from the given FXML-file
     * Will initiate any controller with this as only parameter
     * in the {@link Constructor}
     * @param path	Path of the FXML-file to load
     * @return The {@link Parent} if loaded successfully
     * @throws IOException On any error
     */
    public Parent loadFXML(String path) throws IOException {
    	return loadFXML(getClass().getResource(path));
    }

    /**
     * Loads the {@link Parent} from the given FXML-file
     * Will initiate any controller with this as only parameter
     * in the {@link Constructor}
     * @param url	{@link URL} of the FXML-file to load
     * @return The {@link Parent} if loaded successfully
     * @throws IOException On any error
     */
    public Parent loadFXML(final URL url) throws IOException {
		return FXMLLoader.load(
			url,
			null, // ResourceBundle
			null, // BuilderFactory
			new Callback<Class<?>, Object>() { // Callback --> controller
							   // factory
			    @Override
			    public Object call(Class<?> clazz) {
					try {
					    return clazz.getConstructor(Main.this.getClass()).newInstance(Main.this);
					} catch (Throwable t) {
					    throw new RuntimeException("Failed to load fxml=" + url, t);
					}
			    }
			});
    }

}
