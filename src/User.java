/*
 * Created by: Cody Thomas, Rachita Jain
 * Created on: February 3, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab1*/
import java.net.InetAddress;


public class User {
	private String name;
	private InetAddress ip;
	private int port;
	private int seqNum;
	private int fromPort;
	public User(String name, InetAddress ip, int port){
		this.name = name;
		this.ip = ip;
		this.port = port;
		seqNum = 0;
		fromPort = 0;
	}
	//If the supplied IP belongs to this user, return the user's name
	public String getUser(InetAddress ip){
		if(this.ip.equals(ip))
			return name;
		return "";
	}
	public String getUser(InetAddress ip, int fPort){
		if(this.ip.equals(ip) && (fromPort == fPort || port == fPort))
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
		return "{name=" + name + " ip=" + ip.toString() + " port=" + port + " fromPort=" + fromPort + "}";
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
	public void setFromPort(int fPort){
		//When a user connects to another user from a random port, that's what is stored
		fromPort = fPort;
	}
}
