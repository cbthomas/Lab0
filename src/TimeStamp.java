import java.io.Serializable;


public class TimeStamp implements Serializable{

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
