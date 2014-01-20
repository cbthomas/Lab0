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
	private User local_user;
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
					local_user = new User(local_name,InetAddress.getByName((String)connection_info.get("ip")),local_port );
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
		String action = "";
		//first call a method to check for updates on rules
		updateRules(config_filename);
		//set the sequence number of the message before sending it
		//seqNum should be non-reused, monotonically increasing integer values
		local_user.incrementSeqNum();
		message.set_seqNum(local_user.getSeqNum());
		message.set_source(local_user.getName());
		message.set_duplicate(false);
		
		//First check the message against any SendRules before delivering the message to the socket
		if(sendRules.size() > 0){
			Rule currentRule = sendRules.get(0);
			for(int i = 0; i< sendRules.size(); i++){
				if(currentRule.match(message)){
					action = currentRule.getAction();
					break;
				}
				currentRule = sendRules.get(i);
			}
		}
		if(!action.equals("")){
			//This means that the message matched a rule, so now handle one of the three possibilities appropriately
			if(action.toLowerCase().equals("drop")){
				//Don't send the message and mark all delayed messages as no longer delayed
				for(Message msg : outgoing_buffer){
					msg.set_delayed(false);
				}
			} else if(action.toLowerCase().equals("delay")){
				message.set_delayed(true);
				outgoing_buffer.add(message);
				return;
			} else if(action.toLowerCase().equals("duplicate")){
				//Mark all delayed messages as no longer delayed
				for(Message msg : outgoing_buffer){
					msg.set_delayed(false);
				}
				//add one copy of the message to our outgoing_buffer
				outgoing_buffer.add(message);
				//now add a copy of the message with duplicate set to true
				Message duped = new Message(message.get_dest(), message.get_kind(), message.get_data());
				duped.set_seqNum(local_user.getSeqNum());
				duped.set_source(local_user.getName());
				duped.set_duplicate(true);
				outgoing_buffer.add(duped);
			}
		}
		else
			outgoing_buffer.add(message); //message did not match any sendRules, so just send it normally
		//By getting to this point, we want to send all messages that are in the outgoing_buffer
		//TODO 
		//Check to see if the connection exists
		//     if connection exists, just send the data across that socket
		//     if no connection exists, open connection, spin off listening thread, then send data
		
	}
	Message receive(){
		//first call a method to check for updates on rules
		updateRules(config_filename);
		//deliver a single message from the front of this input queue (if not marked as delayed)
		return modify_incoming(null, false, false, true);
	}
	//This is a thread that will act as a local server to accept incoming connections
	private static class LocalServer implements Runnable{
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		public void run(){
			try {
				local_socket = new ServerSocket(local_port);
				//System.out.println("Made a ServerSocket and about to listen\n");
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
							modify_nodes(currentUser.getUser(connectedIP, connectedPort), aNode, 1);
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
				ObjectInputStream ois = new ObjectInputStream(node.getInputStream());
				while(true){			
					//This is a blocking call that should only move on once we read in a full Message object
					Message msg = (Message) ois.readObject();
					//Check against receiveRules
					Rule currentRule = receiveRules.get(0);
					String action = "";
					for(int i = 0; i < receiveRules.size(); i++){
						if(currentRule.match(msg)){
							action = currentRule.getAction();
							break;
						}
						currentRule = receiveRules.get(i);
					}
					//At this point action is either the action to be performed, or "" if we didn't match any rules
					if(!action.equals("")){
						if(action.toLowerCase().equals("drop")){
							//don't add the message, but must change all current msgs in incoming_buffer to not be delayed anymore
							modify_incoming(null, false, true, false);
						}
						else if(action.toLowerCase().equals("delay")){
							msg.set_delayed(true);
							modify_incoming(msg, true, false, false);
						}
						else if(action.toLowerCase().equals("duplicate")){
							//must change all current msgs in incoming_buffer to not be delayed anymore
							//add the message to the incoming_buffer twice since we matched a duplicate rule
							modify_incoming(msg, true, false, false);
							modify_incoming(msg, true, true, false);
						}
					}
					else{
						//If we don't match any rules, just put the message on the incoming_buffer
						//since we received something that wasn't delay, we make sure everything is changed from delay on the buffer
						modify_incoming(msg, true, true, false);
						
					}
					System.out.println(msg.toString());
				}
			} catch (IOException e) {
				//THIS IS WHAT HAPPENS WHEN A USER DISCONNECTS
				InetAddress connectedIP = node.getInetAddress();
				int connectedPort = node.getPort();
				String connectedUser;
				for(User currentUser : users){
					if(!currentUser.getUser(connectedIP, connectedPort).equals("")){
						connectedUser = currentUser.getUser(connectedIP, connectedPort);
						System.out.println("Disconnected from user: " + connectedUser); //find the user
						//make sure that this user's connection Socket is removed from the global Map<String, Socket> nodes
						modify_nodes(connectedUser, null, 2);
						break;
					}
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			//System.out.println("Something connected!");
		}
	}
	private static synchronized Message modify_incoming(Message msg, Boolean add, Boolean changeDelay, Boolean receive){
		if(add)
			incoming_buffer.add(msg);
		if(changeDelay){
			for(Message currMessage : incoming_buffer){
				currMessage.set_delayed(false);
			}
		}
		if(receive){
			if(incoming_buffer.peek() != null){
				if(incoming_buffer.peek().get_delayed() == false)
					return incoming_buffer.poll();
			}
		}
		return null;
	}
	private static synchronized Socket modify_nodes(String name, Socket sock, int action){
		//Add or remove a user from the global Map of nodes (active connections)
		/*
		 * Action of 1 means to add name/socket pair to the global map
		 * Action of 2 means to remove node 'name' from global map
		 * Action of 3 means to check if there is an active connection for name, and return it if it exists*/
		if(action == 1){
			nodes.put(name, sock);
		}
		else if(action == 2){
			nodes.remove(name);
		}
		else if(action == 3){
			for(String key : nodes.keySet()){
				if(key.equals(name)){
					//this means there is an active connection, so return the socket
					return nodes.get(key);
				}
			}
		}
		return null;
	}
}
