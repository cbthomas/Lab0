/*
 * Created by: Cody Thomas, Rachita Jain
 * Created on: February 3, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab1*/
import java.io.Serializable;
import java.util.HashMap;


public class TimeStampedMessage extends Message implements Serializable, Comparable<TimeStampedMessage>{

	private static final long serialVersionUID = 1L;
	
	HashMap<String,TimeStamp> msg_timestamp;
	Boolean log;
	String group;

	public TimeStampedMessage(String dest, String kind, Object data, Boolean to_log) {
		super(dest, kind, data);
		log = to_log;
		msg_timestamp = new HashMap<String,TimeStamp>();
		group = "";
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
	public void setGroup(String group){
		this.group = group;
	}
	public String getGroup(){
		return group;
	}
	public Boolean isACK(TimeStampedMessage ack){
		if(ack.get_kind().equals("ACK")){
			if(ack.get_data().equals(super.get_data())){
				if(ack.getGroup().equals(group)){
					HashMap<String, TimeStamp> ackTS = ack.getTimeStamp();
					for(String name : msg_timestamp.keySet()){
						if(!msg_timestamp.get(name).isEqual(ackTS.get(name))){
							return false; //if timestamps don't match, then can't be a matching ack
						}
					}
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public int compareTo(TimeStampedMessage secondMsg) {
		//this message is < secondMsg if every value in this Set is < every value in secondMsg Set
		HashMap<String, TimeStamp> secondMsgTS = secondMsg.getTimeStamp();
		Boolean testing = true;
		for(String name : msg_timestamp.keySet()){
			if(!msg_timestamp.get(name).isEqual(secondMsgTS.get(name))){
				testing=false;
			}
		}
		if(testing)
			return 0;
		//this means they aren't equal, so must be <, >, or = by way of ||
		for(String name : msg_timestamp.keySet()){
			if(!msg_timestamp.get(name).isLesser(secondMsgTS.get(name))){
				testing=false;
			}
		}
		if(testing)
			return -1; //this means that this message is < secondMsg
		testing = true;
		for(String name : msg_timestamp.keySet()){
			if(!msg_timestamp.get(name).isGreater(secondMsgTS.get(name))){
				testing=false;
			}
		}
		if(testing)
			return 1; //this means that this message is > secondMsg
		return 0; //this means that this message is == secondMsg by way of || being assigned ==
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result
				+ ((msg_timestamp == null) ? 0 : msg_timestamp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimeStampedMessage other = (TimeStampedMessage) obj;
		if (group == null) {
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (msg_timestamp == null) {
			if (other.msg_timestamp != null)
				return false;
		} else {
			for(String name : msg_timestamp.keySet()){
				if(!msg_timestamp.get(name).equals(other.msg_timestamp.get(name)))
					return false;
			}
		}
		return true;
	}
}
