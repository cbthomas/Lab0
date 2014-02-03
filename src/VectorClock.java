/*
 * Created by: Cody Thomas, Rachita Jain
 * Created on: February 3, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab1*/
import java.util.HashMap;


public class VectorClock extends ClockService {

	HashMap<String,TimeStamp> vector_clock;

	VectorClock(){
		 vector_clock = new HashMap<String,TimeStamp>();
	}

	@Override
	public TimeStamp getMyTime() {
		// TODO Auto-generated method stub
		return super.MyTime;
	}

	@Override
	public void setMyTime(TimeStamp myTime) {
		// TODO Auto-generated method stub
		super.MyTime = myTime;
		vector_clock.put(super.MyTime.getProcName(),myTime);
	}
	
	public void setMyTime(String procName, int time){
		super.MyTime.setTime(time);
		vector_clock.put(procName,new TimeStamp(procName,time));
	}


	public TimeStamp getTimeStamp(String procName){
		return vector_clock.get(procName);
	}

	public void setTimeStamp(String procName, int time){
		vector_clock.put(procName,new TimeStamp(procName,time));
	}
	
	public HashMap<String,TimeStamp> getVectorClock(){
		return vector_clock;
	}
	
	@Override
	public void incrementTime(){
		super.MyTime.incrementTime();
		vector_clock.put(super.MyTime.getProcName(),super.MyTime);
	}

}