/*
 * Created by: Cody Thomas, Vivek Munagala
 * Created on: February 10, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab2*/
public abstract class ClockService {

	TimeStamp MyTime ;

	public abstract TimeStamp getMyTime();

	public abstract void setMyTime(TimeStamp myTime);
	
	public abstract void incrementTime();
	
}
