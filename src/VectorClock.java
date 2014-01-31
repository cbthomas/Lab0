import java.util.HashMap;


public class VectorClock extends ClockService {

	HashMap<String,TimeStamp> vector_clock = new HashMap<String,TimeStamp>();

	VectorClock(){

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


	public TimeStamp getTimeStamp(String procName){
		return vector_clock.get(procName);
	}

	public void setTimeStamp(String procName, int time){
		vector_clock.put(procName,new TimeStamp(procName,time));
	}

}