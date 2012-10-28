import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.*;
import org.neo4j.kernel.*;



public class Bo {
	private static final String DB_PATH = "/Programs/neo4j-community-1.8.M04/";
	
	String myString;
	GraphDatabaseService graphDb;
	Node myFirstNode;
	Node mySecondNode;
	Relationship myRelationship;
	
	ArrayList<Node> times = new ArrayList<Node>();
	
	Node REFERENCE_TIME;
	Node UNIVERSAL_TYPE;
	
    HashMap<Integer, ArrayList<String>> events = new HashMap<Integer, ArrayList<String>>();
    
    ArrayList<Node> allEvents = new ArrayList<Node>();
	
	public static void main(String[] args) {
		System.out.println("Bo!");
		Bo bo = new Bo();
		bo.createDb();
		bo.setupTimes();
		
		
		bo.initializeEvents();
		

		bo.deLinkEvents();
		bo.deLinkTimes();
		bo.shutDown();
	}
	
	public void initializeEvents(){
		addEvent(0, "Hi");
		addEvent(1, "Hello");

		for(Integer time : events.keySet()){
			ArrayList<String> occurances = events.get(time);
			for(String occ : occurances){
				Node tempEvent = createEventNode(getTime(time), occ);
			}
		}
	}
	
	public void probXgivenE(ArrayList<Node> given){
		// Bayes theorem
		// Pr(H | E) =  (Pr(E | H) * Pr(H)) / Pr(E)
		
		
	}
	
	public Node createEventNode(Node timeNode, String eventDesc){
		Node eventNode;
		Transaction tx = graphDb.beginTx();
		try{
			eventNode = graphDb.createNode();
			eventNode.createRelationshipTo(timeNode, RelTypes.TIME);
			tx.success();
		}finally{
			tx.finish();
		}
		if(eventNode != null)
			allEvents.add(eventNode);
		return eventNode;
		
	}
	
	
	
	public void addEvent(int time, String event){
		if(!events.containsKey(time)){
			events.put(time, new ArrayList<String>());
		}
		events.get(time).add(event);
	}
	
	public Node getTime(int t){
		if(t >= times.size()){
			Transaction tx = graphDb.beginTx();
			try{
				for(int i = times.size(); i <= t; i++){
					Node previous = times.get(i-1);
					
					Node newTime = graphDb.createNode();
					newTime.setProperty("time", i);
					newTime.createRelationshipTo(REFERENCE_TIME, RelTypes.IS);
					
					previous.createRelationshipTo(newTime, RelTypes.NEXT);
					newTime.createRelationshipTo(previous, RelTypes.PREVIOUS);
					times.add(newTime);
				}
				tx.success();
			}finally{
				tx.finish();
			}
		}
		
		return times.get(t);
	}
	
	
	void createDb(){
//		graphDb = new ImpermanentGraphDatabase(DB_PATH);
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(DB_PATH).newGraphDatabase();
		
		//graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
//		Transaction tx = graphDb.beginTx();
//		try{
//			myFirstNode = graphDb.createNode();
//			myFirstNode.setProperty("name", "Mr. Awesomeness");
//			
//			mySecondNode = graphDb.createNode();
//			mySecondNode.setProperty("name", "Mrs. Awesomeness");
//			
//			myRelationship = myFirstNode.createRelationshipTo(mySecondNode, RelTypes.POKES);
//			myRelationship.setProperty("relationship-type", "pokes");
//			myString = (myFirstNode.getProperty("name").toString()) 
//					+ " " + (myRelationship.getProperty("relationship-type").toString()) 
//					+ " " + (mySecondNode.getProperty("name").toString());
//			
//			System.out.println(myString);
//			
//			tx.success();
//			
//		}finally{
//			tx.finish();
//		}
	}
	
	void setupTimes(){
		Transaction tx = graphDb.beginTx();
		try{
			Node ref;
			try{
				ref = graphDb.getReferenceNode();
			}catch(Exception e){
				System.out.println("Recreating node ");
			    ref = graphDb.createNode();
			}
			if(!ref.hasRelationship(RelTypes.TIME)){
				System.out.print("Creating REFERENCE Time node ");
				REFERENCE_TIME = graphDb.createNode();
				REFERENCE_TIME.createRelationshipTo(ref, RelTypes.TIME);
			}else{
				System.out.print("Loading REFERENCE Time node");
				for(Relationship r : ref.getRelationships(RelTypes.TIME)){
					REFERENCE_TIME = r.getEndNode();
				}
			}
			System.out.println(" : "+REFERENCE_TIME);
			
			System.out.println("Loading all time instances");
			TreeMap<Integer, Node> tempTimes = new TreeMap<Integer, Node>();
			Iterable<Relationship> allTimes = REFERENCE_TIME.getRelationships(Direction.INCOMING);
			for(Relationship r : allTimes){
				Node t = r.getStartNode();
				if(t.hasProperty("time")){
					Integer index = (Integer) t.getProperty("time");
					tempTimes.put(index, t);
				}
			}
			
			Integer i = 0;
			while(tempTimes.containsKey(i)){
				times.add(tempTimes.get(i));
				i++;
			}
			
			// If there was no keys added, create a default time zero
			if(times.size() == 0){
				Node genesis = graphDb.createNode();
				genesis.setProperty("time", 0);
				times.add(genesis);
			}
			
			System.out.println("Times is now "+times);
			
			tx.success();
		}finally{
			tx.finish();
		}
		
	}
	
	void clear(){
		System.out.println("Clearing graph");
		Transaction tx = graphDb.beginTx();
		try{
			Iterable<Node> allNodes = graphDb.getAllNodes(); 
			for(Node n : allNodes){
//				if(n != graphDb.getReferenceNode()){
					Iterable<Relationship> allRels = n.getRelationships();
					for(Relationship r : allRels)
						r.delete();
					n.delete();
	//			}
			}
			tx.success();
		}finally{
			tx.finish();
		}
	}
	
	void removeData(){
//		Transaction tx = graphDb.beginTx();
//		try
//		{
//			myFirstNode.getSingleRelationship(RelTypes.POKES, Direction.OUTGOING).delete();
//			System.out.println("Removing nodes...:) ");
//			myFirstNode.delete();
//			mySecondNode.delete();
//			tx.success();
//		}
//		finally{
//			tx.finish();
//		}
	}
	
	void deLinkTimes(){
		deLink(times);
	}
	
	public void deLinkEvents(){
		deLink(allEvents);
	}
	
	public void deLink(ArrayList<Node> occ){
		Transaction tx = graphDb.beginTx();
		try{
			for(Node n : occ){
				Iterable<Relationship> allRels = n.getRelationships();
				for(Relationship r : allRels)
					r.delete();
				n.delete();
			}
			tx.success();
		}
		finally{
			tx.finish();
		}
	}
	
	void shutDown(){
		graphDb.shutdown();
		System.out.println("Graph db shut down properly.");
	}

	
	public static enum RelTypes implements RelationshipType{
		POKES, NEXT, PREVIOUS, TIME, IS
	}
	
	
}
