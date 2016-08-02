package hku.cs.reilly.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Queue;

import android.view.accessibility.AccessibilityNodeInfo;

public interface RYEventHandler {
	
	public enum option{
		//this option is used in onNewAppLaunched & onReachingDiffApp
		PRESS_HOME, //tell the state machine to press home, noted that, once the home button is pressed, the PUMA will 
		//switch another application, if you still want to go into the same app, make sure you double the app packagename in 
		//getAppExecutionSequence
		PRESS_BACK, //tell the state machine to press BACK
		CONTINUE //do nothing, continue the exploration
	}
	
	public option onNewAppLaunched(String supposedAppPackageName, String currentAppPackageName);
	
	public Queue<String> getAppExecutionSequence(LinkedHashMap<String, String> appInfo);
	
	public boolean explorationDone(RYStateManager sManager);
	
	public option onReachingDiffApp(String supposedAppPackageName, String currentAppPackageName);
	
	public HashSet<String> getClickingPolicy();
	
	public boolean onStateEquivalent(RYState lastState, RYState currentState);
	
	public AccessibilityNodeInfo getNextClickItem(ArrayList<AccessibilityNodeInfo> availableNodes);
	
	public void onResultAnalysis(RYStateManager manager);
	
	
}
