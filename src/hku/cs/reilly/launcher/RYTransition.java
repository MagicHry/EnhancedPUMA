package hku.cs.reilly.launcher;

import android.R.integer;

/**
 * This class represents the transtion between 2 RYStates
 * note that it is directional which means it can be forward transition to an unknown state
 * and it can also be a backward transtion lead to a known state
 * @author reillyhe
 *
 */
public class RYTransition {
	
	public enum direction{
		FOWARD,
		BACKWARD
	}
	
	private int mClickID; //the forward transtion needs a clickable`s ID
	private RYState mToState; //the state where this transition leads to
	private direction mDirection; //the direction of this transition
	
	public RYTransition(int mClickID, RYState mToState, direction mDirection) {
		super();
		this.mClickID = mClickID;
		this.mToState = mToState;
		this.mDirection = mDirection;
	}

	public int getmClickID() {
		return mClickID;
	}

	public void setmClickID(int mClickID) {
		this.mClickID = mClickID;
	}

	public RYState getmToState() {
		return mToState;
	}

	public void setmToState(RYState mToState) {
		this.mToState = mToState;
	}

	public direction getmDirection() {
		return mDirection;
	}

	public void setmDirection(direction mDirection) {
		this.mDirection = mDirection;
	}
	
	
}
