/**
 * 
 */
package alphabetsoup.simulators.simpleexample;

import java.util.*;
import java.sql.*;

import alphabetsoup.base.*;
import alphabetsoup.framework.*;
import alphabetsoup.userinterface.BucketRender;
import alphabetsoup.userinterface.BucketbotRender;
import alphabetsoup.userinterface.LetterStationRender;
import alphabetsoup.userinterface.MapRender;
import alphabetsoup.userinterface.RenderWindow;
import alphabetsoup.userinterface.WordListRender;
import alphabetsoup.userinterface.WordStationRender;

/**Example AlphabetSoup simulation file, which puts buckets in a grid, lays out bots randomly,
 * parameratizes everything based on "alphabetsoup.config", and starts everything running.
 * @author Chris Hazard
 */
public class SimulationWorldSimpleExample extends SimulationWorld 
{
	private double simulationDuration = 0.0;
	public LetterManagerExample letterManager = null;
	public Updateable wordManager = null;
	public BucketbotManagerExample bucketbotManager = null;
	
	private static SimulationWorldSimpleExample simulationWorldExample;
	public static SimulationWorldSimpleExample getSimulationWorld() 
	{
		return simulationWorldExample;
	}
	
	public SimulationWorldSimpleExample() 
	{
		super("kiva.config");
		
		simulationWorldExample = this;

		usingGUI = (Integer.parseInt(params.getProperty("useGUI")) == 1);
		String window_size[] = params.getProperty("window_size").split("x");
		
		float bucketbot_size = Float.parseFloat(params.getProperty("bucketbot_size"));
		float bucket_size = Float.parseFloat(params.getProperty("bucket_size"));
		float station_size = Float.parseFloat(params.getProperty("station_size"));
		int bundle_size = Integer.parseInt(params.getProperty("bundle_size"));
		
		int bucket_capacity = Integer.parseInt(params.getProperty("bucket_capacity"));
		int letter_station_capacity = Integer.parseInt(params.getProperty("letter_station_capacity"));
		int word_station_capacity = Integer.parseInt(params.getProperty("word_station_capacity"));
		
		float bucket_pickup_setdown_time = Float.parseFloat( params.getProperty("bucket_pickup_setdown_time"));
		float letter_to_bucket_time = Float.parseFloat( params.getProperty("letter_to_bucket_time"));
		float bucket_to_letter_time = Float.parseFloat( params.getProperty("bucket_to_letter_time"));
		float word_completion_time = Float.parseFloat( params.getProperty("word_completion_time"));
		float collision_penalty_time = Float.parseFloat( params.getProperty("collision_penalty_time"));
		
		simulationDuration = Double.parseDouble(params.getProperty("simulation_duration"));
		
		//Set up base map to add things to
		if(usingGUI)
		{
			RenderWindow.initializeUserInterface(Integer.parseInt(window_size[0]), Integer.parseInt(window_size[1]), this);
		}

		//Create classes, and add them to the map accordingly
		for(int i = 0; i < bucketbots.length; i++)
		{
			bucketbots[i] = (Bucketbot) new BucketbotExample(bucketbot_size, bucket_pickup_setdown_time, map.getMaxAcceleration(), map.getMaxVelocity(), collision_penalty_time);
		}
		
		for(int i = 0; i < letterStations.length; i++)
		{
			letterStations[i] = (LetterStation) new LetterStationBase(station_size, letter_to_bucket_time, bundle_size, letter_station_capacity);
		}
		
		for(int i = 0; i < wordStations.length; i++)
		{
			wordStations[i] = (WordStation) new WordStationBase(station_size, bucket_to_letter_time, word_completion_time, word_station_capacity);
		}
		
		for(int i = 0; i < buckets.length; i++)
		{
			buckets[i] = (Bucket) new BucketBase(bucket_size, bucket_capacity);
		}
		
		bucketbotManager	= new BucketbotManagerExample(buckets);		// // ADD THOSE BUCKETS TO THE MANAGER
		letterManager	= new LetterManagerExample();
		wordManager		= new WordOrderManagerExample();
		
		//populate update list
		// updateable WILL BE UPDATED OR REFRESHED FOR EVERY SOME SECOND COMPUTED IN Update() in SimulationWorld.java
		updateables = new ArrayList<Updateable>();
		// ADD THINGS TO BE RENDERED 
		for(Bucketbot r : bucketbots) updateables.add((Updateable)r);
		updateables.add((Updateable)map);
		updateables.add((Updateable)bucketbotManager); 	
		updateables.add((Updateable)wordManager);		
		updateables.add((Updateable)letterManager);	
		for(WordStation s : wordStations) updateables.add((Updateable)s);
		for(LetterStation s : letterStations) updateables.add((Updateable)s);
		//finish adding things to be rendered
		
		initializeRandomLayout();
		
		//generate words
		///////////////////////////////////////////////////////////////////////////
		// THIS IS WHERE ORDER ARE GENERATED
		///////////////////////////////////////////////////////////////////////////
		wordList.generateWordsFromFile(params.getProperty("dictionary"), letterColors, Integer.parseInt(params.getProperty("number_of_words")) );
		
		//////////////////////////////////////////////////////////////////////////
		// THIS IS WHERE INITIAL INVENTORY IS CREATED (populate buckets
		//////////////////////////////////////////////////////////////////////////
		initializeBucketContentsRandom(Float.parseFloat(params.getProperty("initial_inventory")), bundle_size);
		
		if(usingGUI) 
		{
			RenderWindow.addAdditionalDetailRender(new WordListRender((WordListBase)wordList));
			RenderWindow.addLineRender(new MapRender(map));
			
			for(LetterStation s : letterStations)
			{
				RenderWindow.addSolidRender(new LetterStationRender((LetterStationBase)s));
			}
			for(WordStation s : wordStations)
			{
				RenderWindow.addSolidRender(new WordStationRender((WordStationBase)s));
			}
			for(Bucket b : buckets)
			{
				RenderWindow.addLineRender(new BucketRender((BucketBase)b));
			}
			for(Bucketbot r : bucketbots)
			{
				RenderWindow.addLineRender(new BucketbotRender((BucketbotBase)r));
			}
		}
	}
	
	/**Moves the LetterStations evenly across the left side, WordStations evenly across the right side,
	 * and randomly distributes buckets and bucketbots. 
	 */
	public void initializeRandomLayout() 
	{
		//create a list to place all circles in to test later on (to eliminate any overlap)
		List<Circle> circles = new ArrayList<Circle>();
		
		float bucketbot_radius = bucketbots[0].getRadius();
		
		//spread letter stations evenly across on the left side
		// NEED TO SPREAD REPLENISHMENT STATION HERE
		//System.out.println("LETTER STATION POSITIONS");
		for(int i = 0; i < letterStations.length; i++ ) 
		{
			Circle c = (Circle) letterStations[i];
			//System.out.println(Math.max(c.getRadius(), bucketbot_radius) + " : " + (i + 1) * map.getHeight() / (1 + letterStations.length));
			c.setInitialPosition(Math.max(c.getRadius(), bucketbot_radius), (i + 1) * map.getHeight() / (1 + letterStations.length) );
			circles.add(c);
			map.addLetterStation(letterStations[i]);
		}
		
		//spread word stations evenly across on the right side
		// NEED TO SPREAD PICKERS HERE
		//System.out.println("WORD STATION POSITIONS");
		for(int i = 0; i < wordStations.length; i++ ) 
		{
			Circle c = (Circle) wordStations[i];
			//System.out.println(map.getWidth() - Math.max(c.getRadius(), bucketbot_radius) + " : " + (i + 1) * map.getHeight() / (1 + wordStations.length));
			c.setInitialPosition(map.getWidth() - Math.max(c.getRadius(), bucketbot_radius), (i + 1) * map.getHeight() / (1 + wordStations.length) );
			circles.add(c);
			map.addWordStation(wordStations[i]);
		}
		
		//find area to put buckets within
		// NEED TO SPREAD SKU STANDS HERE
		float placeable_width = map.getWidth() - wordStations[0].getRadius() - letterStations[0].getRadius() - 16 * bucketbots[0].getRadius();
		float placeable_height = map.getHeight() - 8 * bucketbots[0].getRadius();
		//System.out.println("Placeable width and height " + placeable_width + " : " + placeable_height);
		
		//find area to store bucket that will allow all buckets to be placed
		// WHAT IS THE DIFFERENCE BETWEEN THAT AND THE ABOVE POSITION
		int width_count = (int)(placeable_width / Math.sqrt(placeable_width * placeable_height / buckets.length));
		int height_count = (int)Math.ceil((float)buckets.length/width_count);
		float bucket_storage_spot_width = placeable_width / width_count;
		float bucket_storage_spot_height = placeable_height / height_count;
		//System.out.println("Width count and height count " + width_count + " : " + height_count);
		//System.out.println("Bucket storage spot width and height " + bucket_storage_spot_width + " : " + bucket_storage_spot_height);
		
		//put a bucket in each location
		float x_start = (map.getWidth() - placeable_width + bucket_storage_spot_width) / 2;
		float y_start = (map.getHeight() - placeable_height + bucket_storage_spot_height) / 2;
		//float x_pos = x_start, y_pos = y_start;
		int x_pos = 0;
		int y_pos = 0;
		
		for(Bucket b : buckets) 
		{
			//place bucket
			//float x_pos1 = x_pos * bucket_storage_spot_width + x_start;
			//float y_pos1 = y_pos * bucket_storage_spot_height + y_start;
			//System.out.println(x_pos * bucket_storage_spot_width + x_start + " : " + y_pos * bucket_storage_spot_height + y_start);
			((Circle)b).setInitialPosition(x_pos * bucket_storage_spot_width + x_start, y_pos * bucket_storage_spot_height + y_start);
			circles.add((Circle)b);
			bucketbotManager.addNewUsedBucketStorageLocation(b); // IMPORTANT: ADD BUCKET TO IT MANAGER
			
			x_pos++;
			//wrap around the end, when run out of width room
			if(x_pos >= width_count) 
			{
				x_pos = 0;
				y_pos++;
			}
		}
	
		//add the remaining storage locations to the manager
		while(y_pos < height_count) 
		{
			bucketbotManager.addNewValidBucketStorageLocation(x_pos * bucket_storage_spot_width + x_start, y_pos * bucket_storage_spot_height + y_start);
			x_pos += bucket_storage_spot_width;
			//wrap around the end, when run out of width room
			if(x_pos >= width_count) 
			{
				x_pos = 0;
				y_pos++;
			}
		}
	
		//keep track of bucket bots to add
		List<Circle> bucketbots_to_add = new ArrayList<Circle>();
		for(Bucketbot r: bucketbots) bucketbots_to_add.add((Circle)r);
		
		//set up random locations for buckets and bucketbots, making sure they don't collide
		MersenneTwisterFast rand = SimulationWorld.rand;
		for(Circle c : bucketbots_to_add)
		{
			boolean collision;
			float new_x, new_y;
			do 
			{
				new_x = rand.nextFloat() * (map.getWidth() - 2*c.getRadius()) + c.getRadius();
				new_y = rand.nextFloat() * (map.getHeight() - 2*c.getRadius()) + c.getRadius();

				collision = false;
				for(Circle d : circles)
				{
					if(collision = d.IsCollision(new_x, new_y, c.getRadius())) break;
				}
			} while(collision);
			c.setInitialPosition(new_x, new_y);
			circles.add(c);
		}
		
		//initialize bucketbots and buckets
		//(once this is done, their positions may no longer be directly written to)
		for(Bucket b : buckets)			map.addBucket(b);
		for(Bucketbot r: bucketbots)	map.addRobot(r);
	}
	
	/*
	private void MSAccessHandler()
	{
		try
    	{
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            String filename = "officedepot.mdb";
            String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
            database+= filename.trim() + ";DriverID=22;READONLY=true}"; // add on to the end
            Connection con = DriverManager.getConnection( database ,"","");

            Statement s = con.createStatement();
			s.execute("select x_pos,y_pos from Bucket"); // select the data from the table
			ResultSet rs = s.getResultSet(); // get any ResultSet that came from our query
			if (rs != null) // if rs == null, then there is no ResultSet to view
			while ( rs.next()) // this will step through our data row-by-row
			{
				System.out.println("x_pos: " + rs.getString(1) + " and y_pos: " + rs.getString(2));
			}
			s.close(); // close the Statement to let the database know we're done with it
			con.close();
		}
        catch (Exception e) 
        {
            System.out.println("Error: " + e);
        }
	}
	*/
	
	/**Launches the Alphabet Soup simulation without user interface.
	 * @param args
	 */
	public static void main(String[] args) 
	{
		simulationWorld = new SimulationWorldSimpleExample();
		if(simulationWorld.isUsingGUI())
		{
			RenderWindow.mainLoop(simulationWorld, ((SimulationWorldSimpleExample)simulationWorld).simulationDuration);
			RenderWindow.destroyUserInterface();
		}
		else
		{
			// move simulation forward by elapsed_time = simulationDuration
			simulationWorld.update(((SimulationWorldSimpleExample)simulationWorld).simulationDuration);
		}
		SummaryReport.generateReport(simulationWorld);
		
	}
}
