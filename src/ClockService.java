/*
 * Created by: Cody Thomas, Rachita Jain
 * Created on: February 3, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab1*/
public abstract class ClockService {

	TimeStamp MyTime ;

	public abstract TimeStamp getMyTime();

	public abstract void setMyTime(TimeStamp myTime);
	
	public abstract void incrementTime();
	
}
