
public class MessagePasserTester {
	//can create messages (Message) and call MessagePasser.send
	//     will set dst, src, kind, data
	//can also call receive method to get anything on MessagePasser's receive buffer
	public static void main(String args[]){
		MessagePasser MP = new MessagePasser("/Users/Cody/Google Drive/Spring 2014/DS/Lab0/config_file.yaml", "alice");
	}
}
