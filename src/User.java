/*
 * Created by: Cody Thomas, Hao Gao
 * Created on: January 24, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab0*/
import java.net.InetAddress;


public class User {
	private String name;
	private InetAddress ip;
	private int port;
	private int seqNum;
	public User(String name, InetAddress ip, int port){
		this.name = name;
		this.ip = ip;
		this.port = port;
		seqNum = 0;
	}
	//If the supplied IP belongs to this user, return the user's name
	public String getUser(InetAddress ip){
		if(this.ip.equals(ip))
			return name;
		return "";
	}
	public InetAddress getIP(){
		return ip;
	}
	public int getPort(){
		return port;
	}
	public Boolean isMyName(String name){
		return name.equals(this.name);
	}
	public String toString(){
		return "{name=" + name + " ip=" + ip.toString() + " port=" + port + "}";
	}
	public void incrementSeqNum(){
		seqNum++;
	}
	public int getSeqNum(){
		return seqNum;
	}
	public String getName(){
		return name;
	}
}
