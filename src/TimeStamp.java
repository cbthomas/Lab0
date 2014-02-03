/*
 * Created by: Cody Thomas, Rachita Jain
 * Created on: February 3, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab1*/
import java.io.Serializable;


public class TimeStamp implements Serializable{

	private static final long serialVersionUID = 1L;
	private String procName;
	private int time;

	public TimeStamp(String procName , int time){
		this.procName = procName;
		this.time = time;
	}

	public String getProcName() {
		return procName;
	}
	public void setProcName(String procName) {
		this.procName = procName;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public void incrementTime(){
		time++;
	}
	public Boolean isEqual(TimeStamp ts){

		if(this.time == ts.time)
			return true;
		else
			return false;

	}

	public Boolean isGreater(TimeStamp ts){

		if(this.time > ts.time)
			return true;
		else
			return false;
	}

	public Boolean isLesser(TimeStamp ts){

		if(this.time < ts.time)
			return true;
		else
			return false;
	}
	public String toString(){
		return time+ " ";
	}

}
