package hku.cs.reilly.model;

import java.util.ArrayList;

public class AppState {
	private int stateID;
	private String mBelongingPackageName;
	private ArrayList<StateTransition> stateTransitions;
	public int getStateID() {
		return stateID;
	}
	public void setStateID(int stateID) {
		this.stateID = stateID;
	}
	public ArrayList<StateTransition> getStateTransitions() {
		return stateTransitions;
	}
	public void setStateTransitions(ArrayList<StateTransition> stateTransitions) {
		this.stateTransitions = stateTransitions;
	}
	public String getmBelongingPackageName() {
		return mBelongingPackageName;
	}
	public void setmBelongingPackageName(String mBelongingPackageName) {
		this.mBelongingPackageName = mBelongingPackageName;
	}
	
}
