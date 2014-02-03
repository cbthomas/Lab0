/*
 * Created by: Cody Thomas, Rachita Jain
 * Created on: February 3, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab1*/
public class LogicalClock extends ClockService {

	public LogicalClock(TimeStamp myTime){
		super.MyTime = myTime;
	}
	
	@Override
	public TimeStamp getMyTime() {
		return super.MyTime;
	}

	@Override
	public void setMyTime(TimeStamp myTime) {
		// TODO Auto-generated method stub
		super.MyTime = myTime;
	}

	@Override
	public void incrementTime(){
		super.MyTime.incrementTime();
	}
	
}
