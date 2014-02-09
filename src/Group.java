import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class Group {
	private String gName;
	private ArrayList<String> members;
	private VectorClock groupTS;
	private ArrayList<TimeStampedMessage> ackQueue;
	private ArrayList<TimeStampedMessage> holdbackQueue;
	private ArrayList<TimeStampedMessage> mySentList;
	
	Group(String gName){
		this.gName = gName;
		members = new ArrayList<String>();
		groupTS = new VectorClock();
		ackQueue = new ArrayList<TimeStampedMessage>();
		holdbackQueue = new ArrayList<TimeStampedMessage>();
		mySentList = new ArrayList<TimeStampedMessage>();
	}
	public TimeStampedMessage getFromSentList(int index){
		return mySentList.get(index-1);
	}
	public void addToMySentList(TimeStampedMessage msg){
		mySentList.add(msg);
	}
	public boolean delivered(TimeStampedMessage msg){
		//if msg's timestamp is < the group timestamp, then it's already been delivered
		//msg could also be in holdbackqueue or in global HBQ possibly
		for(TimeStampedMessage inQueueMsg : holdbackQueue){
			if(inQueueMsg.equals(msg))
				return true;
		}
		for(String name : msg.getTimeStamp().keySet()){
			if(msg.getTimeStamp().get(name).isStrictlyGreater(groupTS.getTimeStamp(name)))
				return false;
		}
		return true;
	}
	public void addToGroup(String name){
		members.add(name);
	}
	public Boolean isMemberOfGroup(String name){
		return members.contains(name);
	}
	public ArrayList<String> traverseGroup(){
		return members;
	}
	public void incGroupTS(String name){
		//increment the TS upon delivery for this instance of the group
		groupTS.setTimeStamp(name, groupTS.getTimeStamp(name).getTime() + 1);
	}
	public HashMap<String, TimeStamp> getTS(){
		return groupTS.getVectorClock();
	}
	public void createGroupTS(){
		for(String name : members){
			groupTS.setTimeStamp(name, 0);
		}
	}
	public void addToAckQueue(TimeStampedMessage msg){
		if(!ackQueue.contains(msg)){
			ackQueue.add(msg);
			Collections.sort(ackQueue);
		}
		System.out.println("ackQueue: " + ackQueue);
	}
	public ArrayList<String> missingAck(TimeStampedMessage msg, String myName){
		ArrayList<String> missingAcks = new ArrayList<String>();
		//add everybody initially and remove the names of the ones we have acks from
		for(String name : members)
			missingAcks.add(name);
		missingAcks.remove(myName); //you aren't missing an ack from yourself
		for(int i = 0; i < ackQueue.size(); i++){
			if(msg.isACK(ackQueue.get(i))){
				missingAcks.remove(ackQueue.get(i).get_source());
			}
		}
		return missingAcks;
	}
	public void removeFromAckQueue(TimeStampedMessage msg){
		//remove all ACKs for message msg from ackQueue and mark msg in holdbackQueue as no longer delayed
		
		for(int i = ackQueue.size()-1; i >=0; i--){
			if(msg.isACK(ackQueue.get(i))){
				ackQueue.remove(i);
			}
		}
		TimeStampedMessage update = holdbackQueue.remove(0);
		update.set_delayed(false);
		holdbackQueue.add(0, update);
		Collections.sort(holdbackQueue); //shouldn't be necessary, but just in case
		
	}
	public TimeStampedMessage peekAtHBQ(){
		if(holdbackQueue.size() > 0)
			return holdbackQueue.get(0);
		else
			return null;
	}
	public void addToHoldbackQueue(TimeStampedMessage msg){
		msg.set_delayed(true); //tells us that we don't have all the necessary ACKs for this message yet
		holdbackQueue.add(msg);
		Collections.sort(holdbackQueue);
		System.out.println("just added to group holdbackqueue: " + holdbackQueue);
	}
	public TimeStampedMessage getFromHoldbackQueue(){
		TimeStampedMessage removedMsg = null;
		if(!holdbackQueue.isEmpty()){
			if(holdbackQueue.get(0).get_delayed() == false)
				removedMsg = holdbackQueue.remove(0);
		}
		System.out.println("just removed " + removedMsg + " from group HBQ");
		System.out.println("still in removed, now it is: " + holdbackQueue);
		return removedMsg;
	}
	public boolean allAcks(TimeStampedMessage msg){
		//check if we have members.size()-1 ACKs for message msg in ackQueue
		int count = 0;
		for(int i = 0; i < ackQueue.size(); i++){
			//msg will be something like: src=A, dst=me TS={A=1,B=0,C=0}, data=something, kind=whatever, group=group1
			//ack will be something like: src=B, dst=me TS={A=1,B=0,C=0}, data=something, kind=ACK, group=group1
			if(msg.isACK(ackQueue.get(i))){
				count++;
			}
		}
		if(count == (members.size()-1)){
			//need an ack from everybody except yourself to have all acks
			return true;
		}
		return false;
	}
	public int getSingleTSval(String name){
		return groupTS.getTimeStamp(name).getTime();
	}
	public TimeStamp getSingleTS(String name){
		return groupTS.getTimeStamp(name);
	}
}
