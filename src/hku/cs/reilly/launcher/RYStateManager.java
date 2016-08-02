package hku.cs.reilly.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nsl.stg.tests.Util;

public class RYStateManager {
	private final double SIMILARITY_THRESHOLD = 0.95;
	// private final double SIMILARITY_THRESHOLD = 0.9999;
	private HashMap<String, ArrayList<RYState>> mStateContainer; //A holder for all the states from every application
	// reference to self
	private static RYStateManager self;
	//total state count
	private int mTotalStateCount;
	//id counter
	private int mStateIDCounter;

	private RYStateManager() {
		/* hide constructor */
		mStateContainer = new HashMap<String, ArrayList<RYState>>();
		mTotalStateCount = 0;
		mStateIDCounter = 0;
	}

	public static RYStateManager getInstance() {
		if (self == null) {
			self = new RYStateManager();
		}
		return self;
	}
	
	public HashMap<String, ArrayList<RYState>> getGraph(){
		return this.mStateContainer;
	}
	
	public void addState(RYState s,String packageName) {
		if (getState(s,packageName) == null) {
			Util.log("RY: New State Found" + s.dumpShort());
			ArrayList<RYState> stateList = mStateContainer.get(packageName);
			if (stateList == null) {
				//create the corresponding list for the app
				mStateContainer.put(packageName, new ArrayList<RYState>());
			}
			mStateContainer.get(packageName).add(s);
			if (s.ismLauncherState()) {
				s.setmStateID(-1);
			}
			else{
				s.setmStateID(this.mStateIDCounter);
				this.mStateIDCounter++;
			}
			this.mTotalStateCount++;
		}
	}

	public RYState getState(RYState s,String packageName) {
		ArrayList<RYState> stateList = mStateContainer.get(packageName);
		if (stateList == null || stateList.size() == 0) {
			return null;
		}
		for (RYState state : stateList) {
			// return on the first found
			if (state.computeCosineSimilarity(s) >= SIMILARITY_THRESHOLD) {
				return state;
			}
			else if (state.ismLauncherState() && s.ismLauncherState()) {
				// it is a launcher state
				return state;
			}
		}

		return null;
	}

	public List<RYState> getAllTodoState(String packageName) {
		List<RYState> ret = new ArrayList<RYState>();
		ArrayList<RYState> stateList = this.mStateContainer.get(packageName);
		for (RYState s : stateList) {
			if (!s.isFullyExplored()) {
				ret.add(s);
			}
		}

		return ret;
	}
	
	public int getTotalStateCount() {
		return this.mTotalStateCount;
	}

// DO NOT NEED IT ANYMORE
//	public int getExecutionSnapshot() {
//		int total = 0, finished = 0;
//		for (int i = 0; i < slist.size(); i++) {
//			RYState s = slist.get(i);
//			total += s.getTotClickables();
//
//			int next = s.getNextClickId();
//			if (next >= 0) {
//				finished += next;
//			}
//		}
//
//		return total + finished;
//	}

//	public String dumpShort() {
//		return "{" + slist.size() + " RYStates }";
//	}
//
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("{");
//
//		for (int i = 0; i < slist.size(); i++) {
//			RYState s = slist.get(i);
//			sb.append("  " + i + " " + s.dumpShort());
//		}
//
//		sb.append("}");
//
//		return sb.toString();
//	}
}

