import java.io.Serializable;
import java.util.HashMap;


public class TimeStampedMessage extends Message implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	HashMap<String,TimeStamp> msg_timestamp = new HashMap<String,TimeStamp>();

	public TimeStampedMessage(String dest, String kind, Object data) {
		super(dest, kind, data);
	}

	public HashMap<String,TimeStamp> getTimeStamp(){
		return msg_timestamp;
	}

	public void setTimeStamp(HashMap<String,TimeStamp> msg_timestamp){
		this.msg_timestamp= msg_timestamp;
	}

	public void addTimeStamp(String name, TimeStamp ts){
		msg_timestamp.put(name, ts);
	}
	
	public String toString(){
		return super.toString() + "TimeStamp: " + msg_timestamp.toString();
	}

}
