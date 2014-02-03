/*
 * Created by: Cody Thomas, Rachita Jain
 * Created on: February 3, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab1*/
public class Rule {
	private String action, src, dest, kind;
	private int seqNum;
	private Boolean dupe;
	
	public Rule(String action){
		this.action = action;
		this.src = "";
		this.dest = "";
		this.kind = "";
		this.seqNum = 0;
		this.dupe = false;
	}
	public String getAction(){
		return action;
	}
	public String getSrc(){
		return src;
	}
	public String getDest(){
		return dest;
	}
	public String getKind(){
		return kind;
	}
	public int getSeqNum(){
		return seqNum;
	}
	public Boolean getDupe(){
		return dupe;
	}
	public void setSrc(String src){
		this.src = src;
	}
	public void setDest(String dest){
		this.dest = dest;
	}
	public void setKind(String kind){
		this.kind = kind;
	}
	public void setSeqNum(int seqNum){
		this.seqNum = seqNum;
	}
	public void setDupe(Boolean dupe){
		this.dupe = dupe;
	}
	/*
	 * Return false if no match
	 * return true if match
	 * */
	public Boolean match(Message msg){
		//In order for a match to be successful either:
		//1. The rule consists of JUST action (which matches all messages)
		//2. msg's fields much match Rules' fields, but a null/0 rule field matches all msgs
		//so check each field and if it's not a wildcard, check against msg's corresponding value
		//    if they differ, return 0. If we get all the way through the fields, return 2
		if(!src.equals("") && !src.equals(msg.get_source()))
			return false;
		if(!dest.equals("") && !dest.equals(msg.get_dest()))
			return false;
		if(!kind.equals("") && !kind.equals(msg.get_kind()))
			return false;
		if(seqNum != 0 && seqNum != msg.get_seqNum())
			return false;
		if(dupe == true && msg.get_dupe() == false)
			return false;
		return true;
	}
	public String toString(){
		return "{action=" + action + " src=" + src + " dest=" + dest + " kind=" + kind + " seqNum=" + seqNum + " dupe=" + dupe + "}";
	}
}
