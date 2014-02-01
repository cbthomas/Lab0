import java.io.Serializable;
import java.util.HashMap;


public class TimeStampedMessage extends Message implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	HashMap<String,TimeStamp> msg_timestamp;
	Boolean log;

	public TimeStampedMessage(String dest, String kind, Object data, Boolean to_log) {
		super(dest, kind, data);
		log = to_log;
		msg_timestamp = new HashMap<String,TimeStamp>();
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
		return super.toString() + " TimeStamp: " + msg_timestamp.toString();
	}
	
	public Boolean getLogStatus(){
		return log;
	}
	public void setLogStatus(Boolean log){
		this.log = log;
	}
	public HashMap<String,TimeStamp> copyMsgTimeStamp(){
		HashMap<String, TimeStamp> copy = new HashMap<String,TimeStamp>();
		for(String name : msg_timestamp.keySet()){
			copy.put(name, new TimeStamp(name, msg_timestamp.get(name).getTime()));
		}
		return copy;
	}
	public String printForLogging(){
		return ("Src=" + super.get_source() + " Dst=" + super.get_dest() + " TS=" + msg_timestamp.toString() + "msg= " + super.get_data());
	}

}
