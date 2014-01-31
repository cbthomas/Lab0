
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

	
	
}
