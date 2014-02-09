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

		if(this.time >= ts.time)
			return true;
		else
			return false;
	}
	public Boolean isStrictlyGreater(TimeStamp ts){
		return this.time > ts.time;
	}
	public Boolean isLesser(TimeStamp ts){

		if(this.time <= ts.time)
			return true;
		else
			return false;
	}
	public String toString(){
		return time+ " ";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((procName == null) ? 0 : procName.hashCode());
		result = prime * result + time;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimeStamp other = (TimeStamp) obj;
		if (procName == null) {
			if (other.procName != null)
				return false;
		} else if (!procName.equals(other.procName))
			return false;
		if (time != other.time)
			return false;
		return true;
	}

}
