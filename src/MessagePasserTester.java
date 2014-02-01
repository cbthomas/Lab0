/*
 * Created by: Cody Thomas, Hao Gao
 * Created on: January 24, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab0*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


public class MessagePasserTester {
	//can create messages (Message) and call MessagePasser.send
	//     will set dst, src, kind, data
	//can also call receive method to get anything on MessagePasser's receive buffer
	private static String local_name, config_file, data, dest, kind, clockType, to_log;
	private static int selection;
	private static TimeStampedMessage receivedMsg;
	private static ClockService ourClock;
	public static void main(String args[]){
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("MessagePasser Testing Interface\nPlease specify configuration file location:");		
			config_file = br.readLine();
			System.out.println("Please specify your name:");
			local_name = br.readLine();
			if(local_name.equals("logger"))
				LoggingService();
			System.out.println("Please select a clock type: logical / vector:");
			clockType = br.readLine();
			if(clockType.equals("logical")){
				ourClock = new LogicalClock(new TimeStamp(local_name, 0));
			}
			else if(clockType.equals("vector")){
				ourClock = new VectorClock();
				ourClock.setMyTime(new TimeStamp(local_name, 0));
			}
			else{
				System.out.println("You have entered in an invalid clock type. Please start over.");
				System.exit(1);
			}
			//Now that we have the config filename and local name, we can instantiate our MessagePasser
			MessagePasser MP = new MessagePasser(config_file, local_name);
			//Now to prompt the user for what to do
			while(true){
				try{
					System.out.println("Please input a number for your selection (1-4):\n"
							+ "1. Send Message\n2. Check Messages\n3. Get Event Timestamp\n4. Exit");
					selection = Integer.parseInt(br.readLine());
					if(selection == 1){
						System.out.print("Please name the destination: ");
						dest = br.readLine();
						System.out.print("Specify the kind of message: ");
						kind = br.readLine();
						System.out.print("What is the data: ");
						data = br.readLine();
						System.out.print("Do you want to log the message? (y/n)");
						to_log = br.readLine();
						System.out.println("Processing message......");
						if(to_log.equals("y")){
							MP.send(new TimeStampedMessage(dest, kind, data, true));
						}
						else
							MP.send(new TimeStampedMessage(dest, kind, data, false));
					}else if(selection == 2){
						System.out.println("Checking....");
						receivedMsg = MP.receive();
						if(receivedMsg != null){
							System.out.println("You have received the following message:\n" + receivedMsg.toString());
							System.out.println("Would you like the log the message you just received? (y/n)");
							to_log = br.readLine();
							if(to_log.equals("y")){
								if(clockType.equals("vector")){
									receivedMsg.setTimeStamp( ((VectorClock)ourClock).getVectorClock());
								}
								else{
									receivedMsg.addTimeStamp(local_name, ourClock.getMyTime());
								}
								//Modify received Msg's timestamp to decouple it from our global clock
								receivedMsg.setTimeStamp(receivedMsg.copyMsgTimeStamp());
								MP.send(new TimeStampedMessage("logger", "log", receivedMsg, false));
							}
						}
						else
							System.out.println("You have no new messages ready at this time.");
						
					}else if(selection == 3){
						//this is for getting a timestamp from the system for a non-message event
						//if you want to log this, just create a normal TSM with dst=logger and don't mark logged as true
						System.out.println("Do you want to also log this event? (y/n)");
						to_log = br.readLine();
						if(to_log.equals("y")){
							System.out.println("What is the data for the event?");
							data = br.readLine();
							dest = "logger";
							kind = "log";
							//possible race condition here? how to solve?
							ourClock.incrementTime();
							System.out.println("The timestamp for this event is: " + ourClock.MyTime.getTime());
							//Sending the data to be logged with MP.send will actually increment the time
							
							TimeStampedMessage tsm = new TimeStampedMessage(local_name, kind, data, false);
							if(clockType.equals("vector")){
								tsm.setTimeStamp( ((VectorClock)ourClock).getVectorClock());
							}
							else{
								tsm.addTimeStamp(local_name, ourClock.getMyTime());
							}
							tsm.setTimeStamp(tsm.copyMsgTimeStamp());
							MP.send(new TimeStampedMessage(dest, kind, tsm, false));
							
						}
						else{
							ourClock.incrementTime();
							System.out.println("The timestamp for this event is: " + ourClock.MyTime.getTime());
						}
					}
					else if(selection == 4){
						
						System.out.println("Thank you for sending messages with MessagePasser! Goodbye.");
						System.exit(1);
					}
					else{
						System.out.println("Sorry, that was not a valid command. Please try again\n");
					}
				}
				catch(NumberFormatException e){
					System.out.println("Sorry, you input an invalid command. Please start over.");
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("I don't understand your input. Please try again.");
		}
		
		//MessagePasser MP = new MessagePasser("/Users/Cody/Documents/DistributedSystems/Lab0/config_file.yaml", "alice");
	}
	public static ClockService getClock(){
		return ourClock;
	}
	public static String getClockType(){
		return clockType;
	}
	public static void LoggingService(){
		ArrayList<TimeStampedMessage> logs = new ArrayList<TimeStampedMessage>();
		String selection;
		TimeStampedMessage msg;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Welcome to the Logging Interface");
		clockType = "vector";
		ourClock = new VectorClock();
		ourClock.setMyTime(new TimeStamp(local_name, 0));

		//Now that we have the config filename and local name, we can instantiate our MessagePasser
		MessagePasser MP = new MessagePasser(config_file, local_name);
		//Now to prompt the user for what to do
		while(true){
			System.out.println("Please input a number for your selection (1-2):\n"
					+ "1. View Log\n2. Exit");
			try {
				selection = br.readLine();
				if(selection.equals("1")){
					//Pull everything out of the incoming buffer so we can analyze it
					while( (msg = MP.receive()) != null ){
						logs.add(0, msg);
					}
					displaySortedLogs(logs);
				}
				else if(selection.equals("2")){
					System.out.println("Thank you for logging with us. Goodbye.");
					System.exit(1);
				}
				else{
					System.out.println("Sorry, I don't understand that command. Please try again.");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public static void displaySortedLogs(ArrayList<TimeStampedMessage> logs){
		TimeStampedMessage currMsg, nextMsg;
		HashMap<String, TimeStamp> currTS, nextTS;
		if(logs.size() == 1){
			System.out.println(logs.get(0).get_source() + " logged the following: " + ((TimeStampedMessage)logs.get(0).get_data()).printForLogging());
			return;
		}
		for(int i = 0; i < logs.size()-1; i++){
			currMsg = (TimeStampedMessage)logs.get(i).get_data();
			currTS = currMsg.getTimeStamp();
			for(int j = i+1; j < logs.size(); j++){
				nextMsg = (TimeStampedMessage)logs.get(j).get_data();
				nextTS = nextMsg.getTimeStamp();
				if(isGreater(currTS, nextTS)){
					//nextMsg -> currMsg
					System.out.println(logs.get(j).get_source() + " logged the following: " + nextMsg.printForLogging());
					System.out.println("->");
					System.out.println(logs.get(i).get_source() + " logged the following: " + currMsg.printForLogging());
					System.out.println("\n");
				}
				else if(isLesser(currTS, nextTS)){
					//currMsg -> nextMsg
					System.out.println(logs.get(i).get_source() + " logged the following: " + currMsg.printForLogging());
					System.out.println("->");
					System.out.println(logs.get(j).get_source() + " logged the following: " + nextMsg.printForLogging());
					System.out.println("");
				}
				else if(isEqual(currTS, nextTS)){
					//currMsg == nextMsg
					System.out.println(logs.get(i).get_source() + " logged the following: " + currMsg.printForLogging());
					System.out.println("==");
					System.out.println(logs.get(j).get_source() + " logged the following: " + nextMsg.printForLogging());
					System.out.println("");
				}
				else{
					//currMsg || nextMsg
					System.out.println(logs.get(i).get_source() + " logged the following: " + currMsg.printForLogging());
					System.out.println("||");
					System.out.println(logs.get(j).get_source() + " logged the following: " + nextMsg.printForLogging());
					System.out.println("");
				}
			}
		}
	}
	public static Boolean isGreater(HashMap<String, TimeStamp> curr, HashMap<String, TimeStamp> next){
		for(String name : curr.keySet()){
			if(curr.get(name).isLesser(next.get(name)))
				return false;
		}
		return true;
	}
	public static Boolean isLesser(HashMap<String, TimeStamp> curr, HashMap<String, TimeStamp> next){
		for(String name : curr.keySet()){
			if(curr.get(name).isGreater(next.get(name)))
				return false;
		}
		return true;
	}
	public static Boolean isEqual(HashMap<String, TimeStamp> curr, HashMap<String, TimeStamp> next){
		for(String name : curr.keySet()){
			if(!curr.get(name).isEqual(next.get(name)))
				return false;
		}
		return true;
	}
}
