/*
 * Created by: Cody Thomas, Hao Gao
 * Created on: January 24, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab0*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class MessagePasserTester {
	//can create messages (Message) and call MessagePasser.send
	//     will set dst, src, kind, data
	//can also call receive method to get anything on MessagePasser's receive buffer
	private static String local_name, config_file, data, dest, kind, clockType;
	private static int selection;
	private static Message receivedMsg;
	private static ClockService ourClock;
	public static void main(String args[]){
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("MessagePasser Testing Interface\nPlease specify configuration file location:");		
			config_file = br.readLine();
			System.out.println("Please specify your name:");
			local_name = br.readLine();
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
					System.out.println("Please input a number for your selection (1-3):\n"
							+ "1. Send Message\n2. Check Messages\n3. Exit");
					selection = Integer.parseInt(br.readLine());
					if(selection == 1){
						System.out.print("Please name the destination: ");
						dest = br.readLine();
						System.out.print("Specify the kind of message: ");
						kind = br.readLine();
						System.out.print("What is the data: ");
						data = br.readLine();
						System.out.println("Processing message......");
						MP.send(new TimeStampedMessage(dest, kind, data));
					}else if(selection == 2){
						System.out.println("Checking....");
						receivedMsg = MP.receive();
						if(receivedMsg != null)
							System.out.println("You have received the following message:\n" + receivedMsg.toString());
						else
							System.out.println("You have no new messages ready at this time.");
						
					}else if(selection == 3){
						System.out.println("Thank you for sending messages with MessagePasser!");
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
}
