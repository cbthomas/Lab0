import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.yaml.snakeyaml.Yaml;


public class MessagePasser {
	private static Queue<Message> incoming_buffer; //buffer of what is coming into this instance of MP
	private Queue<Message> outgoing_buffer; //buffer of what this instance of MP is sending out
	private Yaml yaml; //This will parse the configuration_filename
	private long last_modified; //last modified time for the configuration_filename.yaml
	private static String config_filename;
	private Map<String, Object> config_parsing;
	//maybe have Map<String, Socket> connections to be global map of all active connections
	private static Map<String, Socket> nodes;
	//a list of all possible user names, ip, ports that aren't necessarily connected 
	private static ArrayList<User> users;
    private static ServerSocket local_socket;
    private static int local_port; //set this so the information can be used by the ServerSocket thread
    private static ArrayList<Rule> sendRules;
    private static ArrayList<Rule> receiveRules;
    
	public MessagePasser(String configuration_filename, String local_name){
		//parse configuration_filename and setup sockets for communicating with all processes
		//      listed in the configuration section of the file
		//initialize buffers to hold incoming and outgoing messages to the rest of the nodes in the system
		//     (may need additional state? threads?)
		incoming_buffer = new LinkedList<Message>();
		outgoing_buffer = new LinkedList<Message>();
		
		sendRules = new ArrayList<Rule>();
		receiveRules = new ArrayList<Rule>();

		yaml = new Yaml();
		try { 
			//try opening the configuration_filename
			File config_file = new File(configuration_filename);
			InputStream input = new FileInputStream(config_file);
			//the keySet is [configuration, sendRules, receiveRules] for the configuration_filename map
			config_parsing = (Map<String, Object>) yaml.load(input);
			last_modified = 0; //we don't want this to be the real time so we can update Rules initially
			config_filename = configuration_filename;
			users = new ArrayList<User>();
			nodes = new HashMap<String, Socket>();
			
			//for each key, get a list corresponding to each '-' under the header's name + :
			//	  Just need to deal with the configuration list here though
			for(Object config_item : (ArrayList<Object>) config_parsing.get("configuration")){
				//each config_item is a mapping of name, ip, and port
				Map<String, Object> connection_info = (Map<String, Object>) config_item;
				if(((String)connection_info.get("name")).equals(local_name)){
					//this connection info is the local host, so it should be listening, not making a connection
					//      spin off a thread that will act as a server listening for incoming connections
					local_port = (Integer)connection_info.get("port");
					Thread local = new Thread(new LocalServer());
					local.start();
				}
				else{
					//Add this connection (name, ip, port) to a socket, Node in nodes
					//    I want to store this info without making the connection yet!
					//Socket sock = new Socket(InetAddress.getByName((String)connection_info.get("ip")), (Integer)connection_info.get("port"));
					//nodes.put((String)connection_info.get("name"), sock);
					User currentUser = new User((String)connection_info.get("name"),InetAddress.getByName((String)connection_info.get("ip")), (Integer)connection_info.get("port"));
					users.add(currentUser);
				}
				//System.out.println(connection_info);
			}
			System.out.println(users);
			updateRules(configuration_filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Could not open file: " + configuration_filename);
			System.exit(0);
		}  catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void updateRules(String config_file){
		//try opening the configuration_filename
		File configuration_file = new File(config_file);
		//If the config file hasn't been changed, don't bother trying to update all the rules
		if(configuration_file.lastModified() == last_modified)
			return;
		last_modified = configuration_file.lastModified();
		//get rid of all rules currently stored because you have no guarantee that any are still the same
		sendRules.clear();
		receiveRules.clear();
		InputStream input;
		try {
			input = new FileInputStream(configuration_file);		
			//the keySet is [configuration, sendRules, receiveRules] for the configuration_filename map
			config_parsing = (Map<String, Object>) yaml.load(input);
			//Now go through sendRules and receiveRules to populate the two Rules arrays
			for(Object config_item : (ArrayList<Object>) config_parsing.get("sendRules")){
				//each config_item is a mapping of action, and some combination of src, dest, kind, seqNum, dupe
				Map<String, Object> rule_info = (Map<String, Object>) config_item;
				Rule newRule = new Rule((String)rule_info.get("action"));
				if(rule_info.containsKey("src"))
					newRule.setSrc((String)rule_info.get("src"));
				if(rule_info.containsKey("dest"))
					newRule.setDest((String)rule_info.get("dest"));
				if(rule_info.containsKey("kind"))
					newRule.setKind((String)rule_info.get("kind"));
				if(rule_info.containsKey("seqNum"))
					newRule.setSeqNum((Integer)rule_info.get("seqNum"));
				if(rule_info.containsKey("dupe")){
					if(((String)rule_info.get("dupe")).equals("true"))
						newRule.setDupe(true);
				}
				//add the new sendRule to the sendRule's array
				sendRules.add(newRule);	
			}
			System.out.println(sendRules);
			for(Object config_item : (ArrayList<Object>) config_parsing.get("receiveRules")){
				//each config_item is a mapping of action, and some combination of src, dest, kind, seqNum, dupe
				Map<String, Object> rule_info = (Map<String, Object>) config_item;
				Rule newRule = new Rule((String)rule_info.get("action"));
				if(rule_info.containsKey("src"))
					newRule.setSrc((String)rule_info.get("src"));
				if(rule_info.containsKey("dest"))
					newRule.setDest((String)rule_info.get("dest"));
				if(rule_info.containsKey("kind"))
					newRule.setKind((String)rule_info.get("kind"));
				if(rule_info.containsKey("seqNum"))
					newRule.setSeqNum((Integer)rule_info.get("seqNum"));
				if(rule_info.containsKey("dupe")){
					if(((String)rule_info.get("dupe")).equals("true"))
						newRule.setDupe(true);
				}
				//add the new sendRule to the sendRule's array
				receiveRules.add(newRule);
			}
			System.out.println(receiveRules);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void send(Message message){
		//first call a method to check for updates on rules
		updateRules(config_filename);
		//set the sequence number of the message before sending it
		//seqNum should be non-reused, monotonically increasing integer values
		//First check the message against any SendRules before delivering the message to the socket
		//Check to see if the connection exists
		//     if connection exists, just send the data across that socket
		//     if no connection exists, open connection, spin off listening thread, then send data
	}
	Message receive(){
		//first call a method to check for updates on rules
		updateRules(config_filename);
		//deliver a single message from the front of this input queue (if not marked as delayed)
		Message msg = new Message("dst", "kind", "object");
		return msg;
	}
	//This is a thread that will act as a local server to accept incoming connections
	private static class LocalServer implements Runnable{
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		public void run(){
			try {
				local_socket = new ServerSocket(local_port);
				System.out.println("Made a ServerSocket and about to listen\n");
				while(true){
					//wait for a connection and put it into aNode
					Socket aNode = local_socket.accept();
					/*
					 * get remote IP/port
					 * find corresponding name
					 * store connection into "nodes" Map object
					 * */
					InetAddress connectedIP = aNode.getInetAddress();
					int connectedPort = aNode.getPort();
					for(User currentUser : users){
						if(!currentUser.getUser(connectedIP, connectedPort).equals("")){
							nodes.put(currentUser.getUser(connectedIP, connectedPort), aNode);
							break;
						}
					}
					threadPool.submit(new ReceiveIncomingConnections(aNode));
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
				try{
					local_socket.close();
				} catch (Exception e){
					System.err.println("Could not close connection properly: " + e);
				}
			}	
		}
	}
	//This is a thread that will receive the data from an incoming connection and fill up incoming_buffer
	private static class ReceiveIncomingConnections implements Runnable{
		Socket node;
		public ReceiveIncomingConnections(Socket node){
			this.node = node;
		}
		public void run(){
			/*
			 * get input stream from socket (listen essentially)
			 * put received data into Message object
			 * check Message object against recieveRules
			 * put message in "incoming_buffer" array (as needed)
			 * thread to loop and keep reading from socket
			 * */
			try {
				while(true){
					ObjectInputStream ois = new ObjectInputStream(node.getInputStream());
					Message msg = (Message) ois.readObject();
					System.out.println(msg.toString());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Disconnected from user: "); //find the user
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			System.out.println("Something connected!");
		}
	}
}
