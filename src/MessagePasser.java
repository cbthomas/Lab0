/*
 * Created by: Cody Thomas, Rachita Jain
 * Created on: February 3, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab1*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.yaml.snakeyaml.Yaml;


public class MessagePasser {
	private static LinkedList<TimeStampedMessage> incoming_buffer; //buffer of what is coming into this instance of MP
	private static ArrayList<TimeStampedMessage> holdbackQueue; //buffer of what comes off of the group's HBQueues when they receive enough ACKs
	private static LinkedList<TimeStampedMessage> outgoing_buffer; //buffer of what this instance of MP is sending out
	//private static Set<TimeStampedMessage> receivedMsgSet; //holds all messages we've seen, to account for reliable delivery
	//private static ArrayList<TimeStampedMessage> mySentMsgs; //holds all messages I created and sent
	private static Yaml yaml; //This will parse the configuration_filename
	private static long last_modified; //last modified time for the configuration_filename.yaml
	private static String config_filename;
	private static Map<String, Object> config_parsing;
	//maybe have Map<String, Socket> connections to be global map of all active connections
	private static Map<String, Socket> nodes;
	private static Map<String, Group> groups;
	//a list of all possible user names, ip, ports that aren't necessarily connected 
	private static ArrayList<User> users;
	private static User local_user;
    private static ServerSocket local_socket;
    private static int local_port; //set this so the information can be used by the ServerSocket thread
    private static ArrayList<Rule> sendRules;
    private static ArrayList<Rule> receiveRules;
    //Clock variables
    private static VectorClock clock;
    private static String clockType;
    
	public MessagePasser(String configuration_filename, String local_name){
		//parse configuration_filename and setup sockets for communicating with all processes
		//      listed in the configuration section of the file
		//initialize buffers to hold incoming and outgoing messages to the rest of the nodes in the system
		//     (may need additional state? threads?)
		incoming_buffer = new LinkedList<TimeStampedMessage>();
		outgoing_buffer = new LinkedList<TimeStampedMessage>();
		//receivedMsgSet = new HashSet<TimeStampedMessage>();
		holdbackQueue = new ArrayList<TimeStampedMessage>();
		//mySentMsgs = new ArrayList<TimeStampedMessage>();
		
		sendRules = new ArrayList<Rule>();
		receiveRules = new ArrayList<Rule>();

		yaml = new Yaml();
		
		//get a hold on the clock type
		clock = (VectorClock) MessagePasserTester.getClock();
		clockType = MessagePasserTester.getClockType();
		
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
			groups = new HashMap<String, Group>();
			
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
					//Add this connection (name, ip, port) to a global list of all possible connections
					User currentUser = new User((String)connection_info.get("name"),InetAddress.getByName((String)connection_info.get("ip")), (Integer)connection_info.get("port"));
					users.add(currentUser);
					if(clockType.equals("vector")){
						if(!currentUser.getName().equals("logger")) //Don't add "logger" to our vector clock!
							((VectorClock)clock).setTimeStamp(currentUser.getName(), 0);
					}
				}
				//System.out.println(connection_info);
			}
			//update the groups
			for(Object group_item : (ArrayList<Object>) config_parsing.get("groups")){
				//each group_item is a mapping of group and a list of names
				Map<String, Object> group_info = (Map<String, Object>) group_item;
				Group currGroup = new Group((String)group_info.get("name"));
				ArrayList<String> group_names = (ArrayList<String>)group_info.get("members");
				for(String currName : group_names){
					currGroup.addToGroup(currName);
				}
				currGroup.createGroupTS();
				groups.put((String)group_info.get("name"), currGroup);
			}
			//Now that the groups have been created, we can spin off our timeout/liveness thread
			Thread liveness = new Thread(new livenessMultiCast());
			liveness.start();
			//System.out.println(users);
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
	static void updateRules(String config_file){
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
					if(((Boolean)rule_info.get("dupe")) == true)
						newRule.setDupe(true);
				}
				//add the new sendRule to the sendRule's array
				sendRules.add(newRule);	
			}
			//System.out.println(sendRules);
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
					if(((Boolean)rule_info.get("dupe")) == true)
						newRule.setDupe(true);
				}
				//add the new sendRule to the sendRule's array
				receiveRules.add(newRule);
			}
			//System.out.println(receiveRules);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Error updating rules. All rules have been deleted. Please check your config file.");
		}
	}
	void send(TimeStampedMessage message){
		String action = "";
		//first call a method to check for updates on rules
		updateRules(config_filename);
		//set the sequence number of the message before sending it
		//seqNum should be non-reused, monotonically increasing integer values
		local_user.incrementSeqNum();
		message.set_seqNum(local_user.getSeqNum());
		message.set_source(local_user.getName());
		message.set_duplicate(false);
		//update our own clock and timestamp the message for the group we are sending the message to
		Group currGroup = groups.get(message.get_dest());
		if(currGroup == null){
			//then this message is to a single user and not a group
			clock.incrementTime();
			message.setGroup("");
			message.setTimeStamp(((VectorClock) clock).getVectorClock());			
		}
		else{
			currGroup.incGroupTS(local_user.getName()); //increment the TS for me because i'm sending a message in this specific group
			message.setTimeStamp(currGroup.getTS());
		}
		message.setTimeStamp(message.copyMsgTimeStamp());

		//First check the message against any SendRules before delivering the message to the socket
		applySendRules(message);
		//By getting to this point, we want to send all messages that are in the outgoing_buffer
		modify_outgoing();
		//Now, all that's left on the outgoing_buffer are messages that we couldn't send due to connection issues
		//We need to mark them as no longer delayed so that we can try sending them the next time we call send
		for(Message msg : outgoing_buffer){
			msg.set_delayed(false);
			//System.out.println("Still on outgoing buffer: " + msg);
		}
		
	}
	private static void applySendRules(TimeStampedMessage message){
		updateRules(config_filename);
		String action = "";
		if(sendRules.size() > 0){
			Rule currentRule = sendRules.get(0);
			for(int i = 0; i< sendRules.size(); i++){
				currentRule = sendRules.get(i);
				//System.out.println("checking sendRule: " + currentRule.toString());
				if(currentRule.match(message)){
					//System.out.println("Matched a sendRule: " + currentRule.toString());
					action = currentRule.getAction();
					break;
				}
				//currentRule = sendRules.get(i);
			}
		}
		if(!action.equals("")){
			//This means that the message matched a rule, so now handle one of the three possibilities appropriately
			if(action.toLowerCase().equals("drop")){
				//Don't send the message and mark all delayed messages as no longer delayed
				/*for(Message msg : outgoing_buffer){
					msg.set_delayed(false);
				}*/
				
			} else if(action.toLowerCase().equals("delay")){
				//message.set_delayed(true);
				message.setTimeStamp(message.copyMsgTimeStamp());
				outgoing_buffer.add(message);				
				return;
			} else if(action.toLowerCase().equals("duplicate")){
				//Mark all delayed messages as no longer delayed
				/*for(Message msg : outgoing_buffer){
					msg.set_delayed(false);
				}*/
				//add one copy of the message to our outgoing_buffer
				message.setTimeStamp(message.copyMsgTimeStamp());
				outgoing_buffer.addFirst(message);				
				//now add a copy of the message with duplicate set to true
				TimeStampedMessage duped = new TimeStampedMessage(message.get_dest(), message.get_kind(), message.get_data(), message.getLogStatus());
				duped.set_seqNum(local_user.getSeqNum());
				duped.set_source(local_user.getName());
				duped.set_duplicate(true);
				outgoing_buffer.addFirst(duped);
			}
		}
		else
			outgoing_buffer.addFirst(message); //message did not match any sendRules, so just send it normally
	}
	TimeStampedMessage receive(){
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
					threadPool.submit(new ReceiveIncomingConnections(aNode, false));
					
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Could not receive message properly. Connection not made.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Could not receive message properly. Connection not made.");
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
		Boolean identified_source = false;
		String listening_for = "";
		public ReceiveIncomingConnections(Socket node, Boolean identified){
			this.node = node;
			//this pertains to if we know the right port for the connecting user or not
			//identified being true initially means we initiated the connection to the listening port
			//identified being false initially means we got a connection and need to find the right user
			identified_source = identified;
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
					//This is a blocking call that should only move on once we read in a full Message object
					//System.out.println("waiting to read in a message from a listening thread");
					TimeStampedMessage msg = (TimeStampedMessage) ois.readObject();	
					System.out.println("just got a message: " + msg);
					if(!identified_source){
						modify_nodes(msg.get_dest(), node, 1);
						for(User usr : users){
							if(usr.isMyName(msg.get_dest()))
								usr.setFromPort(node.getPort());
						}
						identified_source = true;
					}
					//Check against receiveRules
					Rule currentRule = receiveRules.get(0);
					String action = "";
					for(int i = 0; i < receiveRules.size(); i++){
						currentRule = receiveRules.get(i);
						if(currentRule.match(msg)){
							action = currentRule.getAction();
							break;
						}
					}
					//At this point action is either the action to be performed, or "" if we didn't match any rules
					if(!action.equals("")){
						if(action.toLowerCase().equals("drop")){
							//don't add the message, but must change all current msgs in incoming_buffer to not be delayed anymore
							//modify_incoming(Message, add_to_queue, mark_everything_as_not_delayed, receive_a_message_from_queue)
							modify_incoming(null, false, true, false);
						}
						else if(action.toLowerCase().equals("delay")){
							msg.set_delayed(true);
							msg.setTimeStamp(msg.copyMsgTimeStamp());
							modify_incoming(msg, true, false, false);
						}
						else if(action.toLowerCase().equals("duplicate")){
							//must change all current msgs in incoming_buffer to not be delayed anymore
							//add the message to the incoming_buffer twice since we matched a duplicate rule
							msg.setTimeStamp(msg.copyMsgTimeStamp());
							modify_incoming(msg, true, false, false);
							msg.setTimeStamp(msg.copyMsgTimeStamp());
							modify_incoming(msg, true, true, false);
						}
					}
					else{
						//If we don't match any rules, just put the message on the incoming_buffer
						//since we received something that wasn't delay, we make sure everything is changed from delay on the buffer
						msg.setTimeStamp(msg.copyMsgTimeStamp());
						modify_incoming(msg, true, true, false);
						
					}
					//System.out.println(msg.toString());
				}
			} catch (IOException e) {
				//THIS IS WHAT HAPPENS WHEN A USER DISCONNECTS
				InetAddress connectedIP = node.getInetAddress();
				int fromPort = node.getPort();
				String connectedUser;
				for(User currentUser : users){
					if(!currentUser.getUser(connectedIP, fromPort).equals("")){
						connectedUser = currentUser.getUser(connectedIP, fromPort);
						//System.out.println("matched user: " + currentUser);
						System.out.println("Disconnected from user: " + connectedUser); //find the user
						//make sure that this user's connection Socket is removed from the global Map<String, Socket> nodes
						modify_nodes(connectedUser, null, 2);
						break;
					}
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("There was a problem receiving a message.");
			} 
			//System.out.println("Something connected!");
		}
	}
	private static synchronized void modify_outgoing(){
		/*
		 * For each message in outgoing_buffer
		 * 	   check if there is a connection already open for that message
		 * 	   if there is, send the data
		 *     if there is not, find the right dest user, try to open socket, add to global nodes, spin off listening thread, send data
		 *          if fail to open socket, what to do with message? inform user regardless that you cannot connect
		 */
		Socket sendSocket;
		TimeStampedMessage msg;
		Group messageGroup;
		while( outgoing_buffer.peek() != null && outgoing_buffer.peek().get_delayed() == false){
			msg = outgoing_buffer.poll();
			//check and see if the dst of msg is a group name, if it is, add n messages to front of outgoing_buffer
			messageGroup = groups.get(msg.get_dest());
			if(messageGroup != null){
				//Create an exact copy of this message that we're goina send out to all members of the group
				for(String currName : groups.get(msg.get_dest()).traverseGroup()){
					if(currName.equals(local_user.getName())){
						//we don't want to be sending our ACK to ourself, otherwise if it's a message we are creating, we make note
						if(!msg.get_kind().equals("ACK")){
							TimeStampedMessage nextMsg = new TimeStampedMessage(currName, msg.get_kind(), msg.get_data(), false);
							nextMsg.set_seqNum(msg.get_seqNum());
							nextMsg.set_source(local_user.getName());
							nextMsg.setGroup(msg.get_dest());
							nextMsg.setTimeStamp(msg.copyMsgTimeStamp());
							messageGroup.addToHoldbackQueue(nextMsg); //src and dest are me, that's how i know i created it
							messageGroup.addToMySentList(nextMsg);; //add it in order to the list of messages I sent, may not be used...
						}
					}
					else{
						TimeStampedMessage nextMsg = new TimeStampedMessage(currName, msg.get_kind(), msg.get_data(), false);
						nextMsg.set_seqNum(msg.get_seqNum());
						nextMsg.set_source(local_user.getName());
						nextMsg.setGroup(msg.get_dest());
						nextMsg.setTimeStamp(msg.copyMsgTimeStamp());
						//need to apply rules to each message first before just sticking them on the outgoing_buffer
						applySendRules(nextMsg); //this method will put the messages on the outgoing_buffer
					}
					
				}
				//update msg with the one of the non-group named messages we just put on the buffer
				msg = outgoing_buffer.poll();
			}
			sendSocket = modify_nodes(msg.get_dest(), null, 3);
			
			if(sendSocket == null){
				User foundUser = null;
				//find the user that we're trying to send to and get their IP/port
				for(User destUser : users){
					if(destUser.isMyName(msg.get_dest())){
						foundUser = destUser;
						break;
					}
				}
				if(foundUser != null){
					try {
						//System.out.println("Trying to connect to " + msg.get_dest() + " for first time to send message");
						//System.out.println("   Because current nodes are: " + nodes.toString());
						//try to open a connection with that destination user
						sendSocket = new Socket(foundUser.getIP(), foundUser.getPort());
						//if connection successful, add this connection to the global nodes list
						modify_nodes(msg.get_dest(), sendSocket, 1);
						//now spin off a thread to listen on this socket
						Thread newListener = new Thread(new ReceiveIncomingConnections(sendSocket, true));
						newListener.start();
						//now actually send the data
						sendData(msg, sendSocket);
						
					} catch (IOException e) {
						// Inform user that you cannot connect or send that message due to connection issues
						System.out.println("Cannot connect to user " + msg.get_dest() + " at this time.  Please try again later.");
						msg.set_delayed(true);
						outgoing_buffer.add(msg);
						}
					}else{
						//this means the user isn't in our list
						System.out.println("Sorry, but user " + msg.get_dest() + " is not in our system.");
					}				
			}else{
				//System.out.println("Sending a message and already have a connection to " + msg.get_dest());
				//We already have an open connection to the message's destination, so just send the data
				sendData(msg, sendSocket);
			}
		}
	}
	private static void sendData(TimeStampedMessage msg, Socket sendSocket){
		try {
			ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
			oos.writeObject(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed to send message to " + msg.get_dest() + " of kind " + msg.get_kind() + " of seqNum " + msg.get_seqNum() 
					+ "\nand data: " + msg.get_data().toString());
			System.out.println("The message has been added back to the queue for a later attempt.");
			outgoing_buffer.add(msg);
			//e.printStackTrace();
		}
	}
	private static TimeStampedMessage modify_incoming(TimeStampedMessage msg, Boolean add, Boolean changeDelay, Boolean receive){
		if(changeDelay){
			for(Message currMessage : incoming_buffer){
				currMessage.set_delayed(false);
			}
		}
		if( msg != null && multiCastReceiveCheck(msg) )
			return null; //this means we handled the message already, no need to put it on the incoming_buffer
						//    i.e. the message was a NACK, ACK, or RACK that was just related to multicast, not to other messages
		if(add)
			incoming_buffer.add(msg);	
		if(receive){
			return orderedMulticastReceive();
		}
		return null;
	}
	private static synchronized boolean multiCastReceiveCheck(TimeStampedMessage msg){
		//This is where we will handle NACK, ACK, RACK-creator messages without putting them on the incoming_buffer
		//we return true if the message is one of the ones we will handle. If it is a normal message, return false
		if(msg.getGroup() != null && !msg.getGroup().equals("")){
			Group currGroup = groups.get(msg.getGroup());
			if(msg.get_kind().equals("ACK")){
				System.out.println(msg.get_dest() + " just got an ACK from " + msg.get_source());
				currGroup.addToAckQueue(msg); //add to the queue that we got an ACK
				if(currGroup.allAcks(msg)){
					//This means the message we just got was the last ACK we needed
					currGroup.removeFromAckQueue(msg);
					TimeStampedMessage addToHBQ = currGroup.getFromHoldbackQueue();
					if(!addToHBQ.get_dest().equals(addToHBQ.get_source()))
						holdbackQueue.add(addToHBQ); //don't add messages we created to the overall HBQ
				}
			}
			else if(msg.get_kind().equals("NACK")){
				//This means that we need to send an original message back to the requester from our sendList
				//   this format will have group=group1, data=# of message they want from us, kind=NACK, src=who we will send it back to
				System.out.println("Just got a NACK for " + msg.get_data());
				Integer nackNum = (Integer) msg.get_data();
				TimeStampedMessage tempMsg = currGroup.getFromSentList(nackNum);
				TimeStampedMessage nackMsg = new TimeStampedMessage(msg.get_source(), tempMsg.get_kind(), tempMsg.get_data(), false);
				nackMsg.set_seqNum(tempMsg.get_seqNum());
				nackMsg.setGroup(tempMsg.getGroup());
				nackMsg.setTimeStamp(tempMsg.copyMsgTimeStamp());
				//nackMsg.set_dest(msg.get_source());
				nackMsg.set_source(local_user.getName());
				applySendRules(nackMsg);
				//outgoing_buffer.addFirst(nackMsg);
				modify_outgoing(); //send the message back to the user that requested it
			}
			else if(msg.get_kind().contains("RACK-")){
				System.out.println(local_user.getName() + " just got a RACK: " + msg);
				String requestor = msg.get_kind().substring(5); //get what's after RACK-
				//if this is a message I've delivered, just send an ACK back
				//otherwise, send a NACK to the creator
				//	  also, count this as an ACK if you don't have it already
				TimeStampedMessage checkMsg = new TimeStampedMessage(msg.get_dest(), msg.get_kind(), msg.get_data(), false);
				checkMsg.set_source(requestor);
				checkMsg.setTimeStamp(msg.copyMsgTimeStamp());
				checkMsg.set_seqNum(msg.get_seqNum());
				
				Boolean seenCheck = currGroup.delivered(checkMsg);
				System.out.println("Group said " + seenCheck + " on seen previous message");
				if(!seenCheck){
					//we also need to check the global HBQ, just in case we removed it from the group, but haven't delivered it yet
					for(TimeStampedMessage inQueueMsg : holdbackQueue){
						if(inQueueMsg.equals(checkMsg))
							seenCheck = true;
					}
				}
				if( seenCheck ){
					//this message is either in my group holdbackqueue or i've already delivered it, so just send an ACK back
					System.out.println("RACK response: i've seen it, sending an ACK");
					checkMsg.set_dest(msg.get_source());
					checkMsg.set_kind("ACK");
					checkMsg.set_source(local_user.getName());
					applySendRules(checkMsg);
					//outgoing_buffer.addFirst(checkMsg);
					modify_outgoing();//send the ACK back
				}
				else{
					//I've never seen this message before, send a nack to the person that created it and keep this RACK as an ACK for future reference
					System.out.println("RACK response: what? sending a NACK to " + requestor);
					TimeStampedMessage nackMsg = new TimeStampedMessage(requestor, "NACK", msg.getTimeStamp().get(requestor).getTime(), false);
					nackMsg.set_source(local_user.getName());
					nackMsg.setGroup(msg.getGroup());
					nackMsg.setTimeStamp(msg.copyMsgTimeStamp());
					applySendRules(nackMsg);
					//outgoing_buffer.addFirst(nackMsg);
					modify_outgoing();
					msg.set_kind("ACK");
					
					currGroup.addToAckQueue(msg);
				}
			}
			else{
				//this was a normal message,just broadcasted. So we have to broadcast ACKs to everybody in the group
				//first make sure we haven't seen it before
				Boolean seenCheck = currGroup.delivered(msg);
				if(!seenCheck){
					//we also need to check the global HBQ, just in case we removed it from the group, but haven't delivered it yet
					for(TimeStampedMessage inQueueMsg : holdbackQueue){
						if(inQueueMsg.equals(msg))
							seenCheck = true;
					}
				}
				TimeStampedMessage rDeliverMsg = new TimeStampedMessage(msg.getGroup(), "ACK", msg.get_data(), false);
				rDeliverMsg.set_seqNum(msg.get_seqNum());
				rDeliverMsg.set_source(local_user.getName());
				rDeliverMsg.setGroup(msg.getGroup());
				rDeliverMsg.setTimeStamp(msg.copyMsgTimeStamp());
				if(!seenCheck){			
					applySendRules(rDeliverMsg);
					//outgoing_buffer.addFirst(rDeliverMsg);
					System.out.println("Just got a normal group message. Sending ACKs");
					modify_outgoing(); //actually send the messages. This will go through and split them up into msg/user instead of /group
					msg.set_delayed(true); //we haven't received an ACK from everybody for this, so delayed is true for now
					currGroup.addToHoldbackQueue(msg);
					//getting the message in the first place is an implicate ACK that the sender also go the message, so add an ACK to the group ACKqueue
					rDeliverMsg.set_source(msg.get_source());
					System.out.println(msg.get_dest() + " just got an implicit ACK from " + msg.get_source());
					currGroup.addToAckQueue(rDeliverMsg);
					if(currGroup.allAcks(msg)){
						//This means the message we just got was the last ACK we needed
						currGroup.removeFromAckQueue(msg);
						TimeStampedMessage addToHBQ = currGroup.getFromHoldbackQueue();
						if(!addToHBQ.get_dest().equals(addToHBQ.get_source()))
							holdbackQueue.add(addToHBQ); //don't add messages we created to the overall HBQ
					}
				}
				else{
					//if we have seen it, just send an ACK back
					rDeliverMsg.set_dest(msg.get_source());
					System.out.println("Got a message I've seen before, sending ACK back to " + rDeliverMsg.get_dest());
					System.out.println("with message: " + rDeliverMsg);					
					applySendRules(rDeliverMsg);
					modify_outgoing();
				}
			}
			return true;
		}
		return false;
		
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
		else if(action == 4){
			//just for debugging purposes
			System.out.println(nodes);
		}
		return null;
	}
	private static void lab1receive(TimeStampedMessage msg){
		if(clockType.equals("vector")){ //This is the vector clock
			HashMap<String, TimeStamp> src_timestamp = msg.getTimeStamp();
			for(String name : src_timestamp.keySet()){
				if( src_timestamp.get(name).isGreater( ((VectorClock)clock).getTimeStamp(name))){
					((VectorClock)clock).setTimeStamp(name, src_timestamp.get(name).getTime());
				}
			}
			if(msg.getTimeStamp().get(msg.get_source()).isGreater(clock.getMyTime())){
				//System.out.println("Msg timestamp: " + msg.getTimeStamp().get(msg.get_source()).getTime() + "is greater than receiver's timestamp: " + clock.MyTime.getTime());
				
				//this means that the source's timestamp is larger than ours
				//so set our timestamp to their's + 1
				int newTime = msg.getTimeStamp().get(msg.get_source()).getTime()+1;
				((VectorClock)clock).setMyTime(msg.get_dest(), newTime);
				//System.out.println(msg.get_dest() + "'s new timestamp: " + clock.MyTime.getTime());
			}
			else
				clock.incrementTime();
		}
		else{ //This is the logical clock
			TimeStamp src_timestamp = msg.getTimeStamp().get(msg.get_source());
			//if the sender's clock is larger, our time must be that + 1
			if(src_timestamp.isGreater(clock.getMyTime()) ){
				clock.MyTime.setTime(src_timestamp.getTime() + 1);
			}
			else
				clock.incrementTime();
		}
	}
	private static TimeStampedMessage orderedMulticastReceive(){
		TimeStampedMessage returnMsg = null;
		while(incoming_buffer.peek() != null && incoming_buffer.peek().get_delayed() == false){
			returnMsg = incoming_buffer.poll();
			//so what will be on the incoming_buffer? non_broadcasted messages
			holdbackQueue.add(returnMsg);
		}
		//now our holdbackQueue is populated with everything we can get out of our incoming_buffer
		//    and we have broadcasted everything we haven't seen before for reliability
		Collections.sort(holdbackQueue);
		System.out.println("In global HBQ when trying to receive: " + holdbackQueue);

		//check front of the holdbackQueue
		if(!holdbackQueue.isEmpty()){
			returnMsg = holdbackQueue.get(0);
			if(returnMsg.getGroup().equals("")){
				//if (  (Msg[sender] = MyV[sender] + 1) && (for all other i, Msg[i] <= MyV[i] s.t. i != sender)  )
				if( returnMsg.getTimeStamp().get(returnMsg.get_source()).getTime() == (clock.getTimeStamp(returnMsg.get_source()).getTime() + 1)){
					//This means that returnMsg is the next message we are expecting from returnMsg.get_source
					//Now check that we have delivered everything that returnMsg.get_source has delivered
					//for every process' TS, returnMsg[i], > MyV[i] we want to send a NACK
					HashMap<String, TimeStamp> nextMsg = returnMsg.getTimeStamp();
					for(String name : nextMsg.keySet()){
						if(!name.equals(returnMsg.get_source())){
							//remember, we only want to compare vector values that are NOT the sender's
							if(!nextMsg.get(name).isLesser(clock.getTimeStamp(name))){
								//this means we got a message that has delivered messages that we haven't. That's a problem, NACK it
								//send the NACK to the process that has the higher TS than us with data saying which TS we want
								TimeStampedMessage nack = new TimeStampedMessage(name, "NACK", nextMsg.get(name).getTime() , false);
								outgoing_buffer.addFirst(nack);
								modify_outgoing();
								return null;
							}
						}
					}
					//    if we never send a NACK, we can put msg in real buffer so that the user can get it and MyV[sender]++, remove msg from holdbackQueue
					clock.setTimeStamp(returnMsg.get_source(),clock.getTimeStamp(returnMsg.get_source()).getTime() + 1 );
					return holdbackQueue.remove(0);
				}         
				//else
				//         We are missing a message, send NACK to ask for it
				//         a NACK is a message with the data field blank and the kind = NACK with the timestamp of the message we are looking for
				else{
					TimeStampedMessage nack = new TimeStampedMessage(returnMsg.get_source(), "NACK", (clock.getTimeStamp(returnMsg.get_source()).getTime() + 1), false);
					nack.setGroup(returnMsg.getGroup());
					outgoing_buffer.addFirst(nack);
					modify_outgoing();
					return null;
				}
			}
			else{
				Group currGroup = groups.get(returnMsg.getGroup());
				
				//if (  (Msg[sender] = MyV[sender] + 1) && (for all other i, Msg[i] <= MyV[i] s.t. i != sender)  )
				if( returnMsg.getTimeStamp().get(returnMsg.get_source()).getTime() == (currGroup.getSingleTSval(returnMsg.get_source()) + 1)){
					//This means that returnMsg is the next message we are expecting from returnMsg.get_source
					//Now check that we have delivered everything that returnMsg.get_source has delivered
					//for every process' TS, returnMsg[i], > MyV[i] we want to send a NACK
					HashMap<String, TimeStamp> returnMsgTS = returnMsg.getTimeStamp();
					for(String name : returnMsgTS.keySet()){
						if(!name.equals(returnMsg.get_source())){
							//remember, we only want to compare vector values that are NOT the sender's
							if(!returnMsgTS.get(name).isLesser(currGroup.getSingleTS(name))){
								//this means we got a message that has delivered messages that we haven't. That's a problem, NACK it
								//send the NACK to the process that has the higher TS than us with data saying which TS we want
								TimeStampedMessage nack = new TimeStampedMessage(name, "NACK", returnMsgTS.get(name).getTime() , false);
								nack.setGroup(returnMsg.getGroup());
								outgoing_buffer.addFirst(nack);
								modify_outgoing();
								return null;
							}
						}
					}
					//    if we never send a NACK, we can put msg in real buffer so that the user can get it and MyV[sender]++, remove msg from holdbackQueue
					currGroup.incGroupTS(returnMsg.get_source());
					System.out.println("Delivered msg to user, updated group TS: " + currGroup.getTS());
					return holdbackQueue.remove(0);
				}         
				//else
				//         We are missing a message, send NACK to ask for it
				//         a NACK is a message with the kind = NACK with the timestamp of the message we are looking for in the data
				else{
					TimeStampedMessage nack = new TimeStampedMessage(returnMsg.get_source(), "NACK", (currGroup.getSingleTSval(returnMsg.get_source()) + 1) , false);
					nack.setGroup(returnMsg.getGroup());
					outgoing_buffer.addFirst(nack);
					modify_outgoing();
					return null;
				}
			}
		}
		else
			return null; //there is nothing to even attempt to deliver
	}
	private static class livenessMultiCast implements Runnable{

		@Override
		public void run() {
			//This thread runs forever in an infinite loop waiting 2 seconds each time
			try {
				while(true){
					Thread.sleep(10000); //10 seconds
					for(String groupName : groups.keySet()){
						Group currGroup = groups.get(groupName);
						if(currGroup.isMemberOfGroup(local_user.getName())){
							//for each group that I am part of, check that group's holdback queue to see if we are waiting on any ACKs
							TimeStampedMessage nextMsg = currGroup.peekAtHBQ();
							if(nextMsg != null && nextMsg.get_delayed() == true){
								ArrayList<String> RACKusers = currGroup.missingAck(nextMsg, local_user.getName());
								for(String dest : RACKusers){
									TimeStampedMessage resend;
									if(nextMsg.get_source().equals(local_user.getName())){
										//I'm still waiting for ACKs from a message I created, so just send the message again
										resend = new TimeStampedMessage(dest, nextMsg.get_kind(), nextMsg.get_data(), false);
									}
									else{
										//I'm still waiting for ACKs from other users, so RACK them
										resend = new TimeStampedMessage(dest, "RACK-" + nextMsg.get_source(), nextMsg.get_data(), false);	
									}
									resend.set_source(local_user.getName());
									resend.set_seqNum(nextMsg.get_seqNum());
									resend.setTimeStamp(nextMsg.copyMsgTimeStamp());
									resend.setGroup(nextMsg.getGroup());
									resend.set_delayed(false);
									System.out.println("Liveness: resending to " + dest + " " + resend);
									applySendRules(resend);
									//outgoing_buffer.addFirst(resend);
								}
								modify_outgoing();
							}
						}					
					}
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("timeout thread interrupted.");
			}
			
		}
		
	}
}
