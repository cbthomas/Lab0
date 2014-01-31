import java.io.Serializable;
import java.util.HashMap;


public class TimeStampedMessage extends Message{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	HashMap<String,TimeStamp> msg_timestamp = new HashMap<String,TimeStamp>();

	public TimeStampedMessage(String dest, String kind, Serializable data) {
		super(dest, kind, data);
	}

	public HashMap<String,TimeStamp> getTimeStamp(){
		return msg_timestamp;
	}

	public void setTimeStamp(HashMap<String,TimeStamp> msg_timestamp){
		this.msg_timestamp= msg_timestamp;
	}



}
