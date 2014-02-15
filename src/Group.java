/*
 * Created by: Cody Thomas, Vivek Munagala
 * Created on: February 10, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab2*/
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Group {
	private String gName;
	private ArrayList<String> members;
	private VectorClock groupTS;
	private ArrayList<TimeStampedMessage> ackQueue;
	private ArrayList<TimeStampedMessage> holdbackQueue;
	private ArrayList<TimeStampedMessage> mySentList;
	
	private Lock ackLock;
	private Lock holdbackLock;
	private Lock sentLock;
	
	Group(String gName){
		this.gName = gName;
		members = new ArrayList<String>();
		groupTS = new VectorClock();
		ackQueue = new ArrayList<TimeStampedMessage>();
		holdbackQueue = new ArrayList<TimeStampedMessage>();
		mySentList = new ArrayList<TimeStampedMessage>();
		
		ackLock = new ReentrantLock();
		holdbackLock = new ReentrantLock();
		sentLock = new ReentrantLock();
	}
	public TimeStampedMessage getFromSentList(int index){
		return mySentList.get(index-1);
	}
	public void addToMySentList(TimeStampedMessage msg){
		msg.set_delayed(false);
		sentLock.lock();
		mySentList.add(msg);
		Collections.sort(mySentList);
		sentLock.unlock();
	}
	public boolean alreadyACKed(TimeStampedMessage msg){
		ackLock.lock();
		for(TimeStampedMessage inQueueMsg : ackQueue){
			if(inQueueMsg.equals(msg))
			{
				ackLock.unlock();
				return true;
			}
		}
		ackLock.unlock();
		
		return false;
	}
	public boolean delivered(TimeStampedMessage msg){
		//if msg's timestamp is < the group timestamp, then it's already been delivered
		//msg could also be in holdbackqueue or in global HBQ possibly
		holdbackLock.lock();
		for(TimeStampedMessage inQueueMsg : holdbackQueue){
			if(inQueueMsg.equals(msg))
			{
				holdbackLock.unlock();
				return true;
			}
		}
		holdbackLock.unlock();
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
		ackLock.lock();
		if(!ackQueue.contains(msg)){
			ackQueue.add(msg);
			Collections.sort(ackQueue);
		}
		//System.out.println("ackQueue for group " + gName + ": " + ackQueue);
		ackLock.unlock();
	}
	public ArrayList<String> missingAck(TimeStampedMessage msg){
		ArrayList<String> missingAcks = new ArrayList<String>();
		//add everybody initially and remove the names of the ones we have acks from
		for(String name : members)
			missingAcks.add(name);
		ackLock.lock();
		for(int i = 0; i < ackQueue.size(); i++){
			if(msg.isACK(ackQueue.get(i))){
				missingAcks.remove(ackQueue.get(i).get_source());
			}
		}
		ackLock.unlock();
		return missingAcks;
	}
	public void removeFromAckQueue(TimeStampedMessage msg){
		//remove all ACKs for message msg from ackQueue and mark msg in holdbackQueue as no longer delayed
		
		ackLock.lock();
		for(int i = ackQueue.size()-1; i >=0; i--){
			if(msg.isACK(ackQueue.get(i))){
				ackQueue.remove(i);
			}
		}
		holdbackLock.lock();
		TimeStampedMessage update = holdbackQueue.remove(0);
		update.set_delayed(false);
		holdbackQueue.add(0, update);
		Collections.sort(holdbackQueue); //shouldn't be necessary, but just in case
		holdbackLock.unlock();
		ackLock.unlock();
		
	}
	public TimeStampedMessage peekAtHBQ(){
		holdbackLock.lock();
		if(holdbackQueue.size() > 0)
		{
			holdbackLock.unlock();
			return holdbackQueue.get(0);
		}
		else
		{holdbackLock.unlock();
			return null;
		}
	}
	public void addToHoldbackQueue(TimeStampedMessage msg){
		msg.set_delayed(true); //tells us that we don't have all the necessary ACKs for this message yet
		holdbackLock.lock();
		holdbackQueue.add(msg);
		Collections.sort(holdbackQueue);
		//System.out.println("HBQ of " + gName +": " + holdbackQueue);
		holdbackLock.unlock();
	}
	public TimeStampedMessage getFromHoldbackQueue(TimeStampedMessage ackForMsg){
		TimeStampedMessage removedMsg = null;
		holdbackLock.lock();
		for(TimeStampedMessage inQueue : holdbackQueue){
			if(inQueue.getTimeStamp().hashCode() == ackForMsg.getTimeStamp().hashCode()){
				if(inQueue.getGroup().equals(ackForMsg.getGroup())){
					if(inQueue.get_seqNum() == ackForMsg.get_seqNum()){
						if(inQueue.get_data().equals(ackForMsg.get_data())){
							removedMsg = new TimeStampedMessage(inQueue.get_dest(), inQueue.get_kind(), inQueue.get_data(), false);
							removedMsg.set_seqNum(inQueue.get_seqNum());
							removedMsg.setGroup(inQueue.getGroup());
							removedMsg.setTimeStamp(inQueue.copyMsgTimeStamp());
							removedMsg.set_source(inQueue.get_source());

							int index = holdbackQueue.indexOf(inQueue);
							holdbackQueue.remove(index);
							break;
						}
					}
				}
			}
		}
		//System.out.println("just removed " + removedMsg + " from group HBQ");
		//System.out.println("still in removed, now it is: " + holdbackQueue);
		holdbackLock.unlock();
		return removedMsg;
	}
	public boolean allAcks(TimeStampedMessage msg){
		//check if we have members.size()-1 ACKs for message msg in ackQueue
		int count = 0;
		ackLock.lock();
		for(int i = 0; i < ackQueue.size(); i++){
			//msg will be something like: src=A, dst=me TS={A=1,B=0,C=0}, data=something, kind=whatever, group=group1
			//ack will be something like: src=B, dst=me TS={A=1,B=0,C=0}, data=something, kind=ACK, group=group1
			if(msg.isACK(ackQueue.get(i))){
				count++;
			}
		}
		ackLock.unlock();
		if(count == (members.size())){
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
