package hku.cs.reilly.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import nsl.stg.tests.Util;

import org.json.JSONException;
import org.json.JSONObject;

import android.view.accessibility.AccessibilityNodeInfo;
import hku.cs.reilly.launcher.RYEventHandler;
import hku.cs.reilly.launcher.RYState;
import hku.cs.reilly.launcher.RYStateManager;
import hku.cs.reilly.launcher.ResultAnalysisUtils;
import hku.cs.reilly.model.AppList;

public class BackwardCheckingHandler implements RYEventHandler {

	@Override
	public option onNewAppLaunched(String supposedAppPackageName,
			String currentAppPackageName) {
		// TODO Auto-generated method stub
		return option.CONTINUE;
	}

	@Override
	public Queue<String> getAppExecutionSequence(
			LinkedHashMap<String, String> appInfo) {
		// TODO Auto-generated method stub
		Queue<String> executionSeq = new LinkedList<String>();
		Iterator it = appInfo.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry entity = (Entry) it.next();
			executionSeq.offer((String) entity.getKey());
		}
		return executionSeq;
	}

	@Override
	public boolean explorationDone(RYStateManager sManager) {
		// TODO Auto-generated method stub
		if (sManager.getTotalStateCount() > 300) {
			return true;
		}
		return false;
	}

	@Override
	public option onReachingDiffApp(String supposedAppPackageName,
			String currentAppPackageName) {
		// TODO Auto-generated method stub
		return option.PRESS_BACK;
	}

	@Override
	public HashSet<String> getClickingPolicy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onStateEquivalent(RYState lastState, RYState currentState) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AccessibilityNodeInfo getNextClickItem(
			ArrayList<AccessibilityNodeInfo> availableNodes) {
		for (AccessibilityNodeInfo node : availableNodes){
			if (node.getContentDescription() != null) {
				if (node.getContentDescription().toString().contains("Share")) {
					return node;
				}
				else if (node.getContentDescription().toString().contains("Open")) {
					return node;
				}
				else if (node.getClassName().equals("android.widget.Button")) {
					return node;
				}
			}
		}
	    double d = Math.random();
	    int i = (int)(d*1000);
	    int randomIndex = i%(availableNodes.size());
		return availableNodes.get(randomIndex);
	}

	@Override
	public void onResultAnalysis(RYStateManager manager) {
		// TODO Auto-generated method stub
		AppList infoAppList = ResultAnalysisUtils.constructAppList(manager);
		String analysisResult = "";
		try {
			analysisResult = ResultAnalysisUtils.toJson(infoAppList);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Util.log("RY:Result:+"+analysisResult);
	}

}
