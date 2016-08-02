package hku.cs.reilly.launcher;

import hku.cs.reilly.model.AppState;
import hku.cs.reilly.model.StateTransition;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import nsl.stg.core.HashIdDictionary;

import android.net.NetworkInfo.State;
import android.view.accessibility.AccessibilityNodeInfo;

/***
 * Store list of feature vectors and compute cosine similarity between two states.
 * Every node info in a state is able to record the clicking info 
 */
public class RYState {

	public static final RYState DUMMY_3RD_PARTY_STATE = new RYState(new Hashtable<String, Integer>(), new ArrayList<AccessibilityNodeInfo>(0),"",null);
	private Hashtable<String, Integer> features;
	private int nextClickId;
	private int totClickables;
	private List<RYNodeInfo> clickables = new ArrayList<RYNodeInfo>();
	private String mBeloningApp; //The owner app`s packageName
	private HashSet<String> mClickingPolicy; //The user defined clicking policy
	private ArrayList<RYTransition> mTransitions = new ArrayList<RYTransition>(); //The transition from this state
	private boolean mLauncherState = false; //Whether this state is a fake launcher state
	private int mStateID; //used for result analysis

	public RYState(Hashtable<String, Integer> map, List<AccessibilityNodeInfo> clickables, String packageName, HashSet<String> 
	clickingPolicy) {
		features = map;
		nextClickId = 0;
		this.mClickingPolicy = clickingPolicy;
		for (AccessibilityNodeInfo clickable : clickables){		
			RYNodeInfo info;
			if (mClickingPolicy == null) {
				info = new RYNodeInfo(clickable, false);
			}
			else if (mClickingPolicy.contains(clickable.getClassName())) {
				//if the node`s type is within the user`s policy, set it to false, we need to click it
				info = new RYNodeInfo(clickable, false);
			}
			else {
				//outside of user`s policy, we will ignore it
				info = new RYNodeInfo(clickable, true);
			}
			this.clickables.add(info);
		}
		totClickables = clickables.size();
		this.mBeloningApp = packageName;
	}

	public ArrayList<AccessibilityNodeInfo> getClickables() {
		//return the clickables that are not clicked yet
		ArrayList<AccessibilityNodeInfo> availableNodes = new ArrayList<AccessibilityNodeInfo>();
		for (RYNodeInfo clickable : this.clickables){
			if (!clickable.isClicked()){
				availableNodes.add(clickable.getAccessibilityNodeInfo());
			}
		}
		return availableNodes;
	}
	
	public void setClicked(AccessibilityNodeInfo node){
		for (RYNodeInfo rYNode : this.clickables){
			if (rYNode.getAccessibilityNodeInfo() == node) {
				rYNode.setClicked();
			}
		}
	}
	
	public boolean ismLauncherState() {
		return mLauncherState;
	}

	public void setmLauncherState(boolean mLauncherState) {
		this.mLauncherState = mLauncherState;
	}

	public Hashtable<String, Integer> getFeatures() {
		return features;
	}
	
	public int getmStateID() {
		return mStateID;
	}

	public void setmStateID(int mStateID) {
		this.mStateID = mStateID;
	}
	
	public String getmBeloningApp() {
		return mBeloningApp;
	}

	public void setmBeloningApp(String mBeloningApp) {
		this.mBeloningApp = mBeloningApp;
	}

	public String computeFeatureHash() {
		MessageDigest m = null;

		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			/* ignore*/
		}

		if (m == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Iterator<Entry<String, Integer>> iter = features.entrySet().iterator(); iter.hasNext();) {
			Entry<String, Integer> entry = iter.next();
			sb.append(entry.getKey() + "," + entry.getValue() + "\n");
		}

		String plaintext = sb.toString();

		m.reset();
		m.update(plaintext.getBytes());
		byte[] digest = m.digest();
		BigInteger bigInt = new BigInteger(1, digest);
		String hashtext = bigInt.toString(16);

		// Now we need to zero pad it if you actually want the full 32 chars.
		while (hashtext.length() < 32) {
			hashtext = "0" + hashtext;
		}

		// return hashtext;
		return HashIdDictionary.add(hashtext);
	}

	public int getNextClickId() {
		return nextClickId;
	}

	public void incrementNextClickId() {
		this.nextClickId++;
	}

	public int getTotClickables() {
		return totClickables;
	}
	
	/**
	 * Generate a dummy state to represent the initial launcher state
	 */
	public static RYState generateLuancherState(String launcherPackageName){
		RYState fakeLauncher = new RYState(new Hashtable<String, Integer>(), new ArrayList<AccessibilityNodeInfo>(0),launcherPackageName,null); 
		fakeLauncher.setmLauncherState(true);
		return fakeLauncher;
	}
	
	public ArrayList<RYTransition> getmTransitions() {
		return mTransitions;
	}

	public void addTransition(RYTransition transition) {
		this.mTransitions.add(transition);
	}

	/**
	 * Calculate the cosine similarity between two UI states.
	 * @param s2
	 * @return cosine similarity
	 */
	public double computeCosineSimilarity(RYState s2) {
		double sim = 0;

		if (s2 == null) {
			return sim;
		}

		Set<String> U = new HashSet<String>();
		Hashtable<String, Integer> features2 = s2.getFeatures();

		U.addAll(features.keySet());
		U.addAll(features2.keySet());

		// Util.log("|U|=" + U.size());

		double N1 = 0, N2 = 0, dot = 0;
		for (Iterator<String> iter = U.iterator(); iter.hasNext();) {
			String key = iter.next();
			int v1 = features.containsKey(key) ? features.get(key) : 0;
			int v2 = features2.containsKey(key) ? features2.get(key) : 0;
			dot += v1 * v2;
			N1 += v1 * v1;
			N2 += v2 * v2;
		}

		sim = dot / (Math.sqrt(N1) * Math.sqrt(N2));
		return sim;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((features == null) ? 0 : features.hashCode());
		result = prime * result + nextClickId;
		result = prime * result + totClickables;
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
		RYState other = (RYState) obj;
		if (features == null) {
			if (other.features != null)
				return false;
		} else if (!features.equals(other.features))
			return false;
		if (nextClickId != other.nextClickId)
			return false;
		if (totClickables != other.totClickables)
			return false;
		return true;
	}

	public boolean isFullyExplored() {
		//iff all the clickables is clicked 
		for (RYNodeInfo info : this.clickables){
			if (!info.isClicked()) {
				return false;
			}
		}
		return true;
	}
	
	public AppState construteAppState(){
		AppState state = new AppState();
		state.setStateID(this.mStateID);
		state.setmBelongingPackageName(this.mBeloningApp);
		ArrayList<StateTransition> transitions = new ArrayList<StateTransition>();
		for (RYTransition transition : this.mTransitions){
			StateTransition stateTranstion = new StateTransition();
			stateTranstion.setClickingID(transition.getmClickID());
			stateTranstion.setDirection(String.valueOf(transition.getmDirection()));
			stateTranstion.setToStateID(transition.getmToState().getmStateID());
			transitions.add(stateTranstion);
		}
		state.setStateTransitions(transitions);
		return state;
	}

	public String dumpShort() {
		return "[" + computeFeatureHash() + ", " + nextClickId + "/" + totClickables + "]";
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[\n");
		sb.append("  nextClickId=" + nextClickId + " out of " + totClickables + "\n");
		for (Enumeration<String> enu = features.keys(); enu.hasMoreElements();) {
			String key = enu.nextElement();
			int value = features.get(key);
			sb.append("  <" + key + ", " + value + ">\n");

		}
		sb.append("]\n");
		return sb.toString();
	}
}
