package hku.cs.reilly.model;

public class StateTransition {
	private int clickingID;
	private String direction;
	private int toStateID;
	public int getClickingID() {
		return clickingID;
	}
	public void setClickingID(int clickingID) {
		this.clickingID = clickingID;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public int getToStateID() {
		return toStateID;
	}
	public void setToStateID(int toStateID) {
		this.toStateID = toStateID;
	}
	
}
