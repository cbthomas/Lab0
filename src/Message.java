/*
 * Created by: Cody Thomas, Vivek Munagala
 * Created on: February 10, 2014
 * Created for: Carnegie Mellon University, Distributed Systems, Lab2*/

import java.io.Serializable;


public class Message implements Serializable{

	private static final long serialVersionUID = 1L;
	//each message in entire system will have a unique combination of source/seq Num
	private String src, dest, kind;
	private int seqNum;
	private Boolean duplicate, delayed;
	private Object data;
	public Message(String dest, String kind, Object data){
		this.dest = dest;
		this.kind = kind;
		this.data = data;
		delayed = false;
	}
	//These setters are used by MessagePasser.send
	public void set_source(String source){
		src = source;
	}
	public void set_seqNum(int sequenceNumber){
		seqNum = sequenceNumber;
	}
	public void set_duplicate(Boolean dupe){
		duplicate = dupe;
	}
	public void set_dest(String dest){
		this.dest = dest;
	}
	//This might be set by receiver thread after checking the Message against receiveRules
	public void set_delayed(Boolean delay){
		delayed = delay;
	}
	public Boolean get_delayed(){
		return delayed;
	}
	//These getters are used by Rule.match
	public String get_source(){
		return src;
	}
	public String get_dest(){
		return dest;
	}
	public String get_kind(){
		return kind;
	}
	public void set_kind(String kind){
		this.kind = kind;
	}
	public int get_seqNum(){
		return seqNum;
	}
	public Boolean get_dupe(){
		return duplicate;
	}
	public Object get_data(){
		return data;
	}
	public String toString(){
		return "from=" + src + " kind=" + kind + " seqNum=" + seqNum + " \n\tmsg: " + data.toString();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + seqNum;
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
		Message other = (Message) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (seqNum != other.seqNum)
			return false;
		if (!src.equals(other.src))
			return false;
		return true;
	}
}
