package hku.cs.reilly.handler;

import java.util.ArrayList;
import java.util.DuplicateFormatFlagsException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.json.JSONException;

import nsl.stg.tests.Util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import android.app.backup.BackupAgent;
import android.app.backup.BackupManager;
import android.view.accessibility.AccessibilityNodeInfo;
import hku.cs.reilly.launcher.RYEventHandler;
import hku.cs.reilly.launcher.RYState;
import hku.cs.reilly.launcher.RYStateManager;
import hku.cs.reilly.launcher.ResultAnalysisUtils;
import hku.cs.reilly.model.AppList;

public class ForwardCheckingHandler implements RYEventHandler {

	@Override
	public option onNewAppLaunched(String supposedAppPackageName,
			String currentAppPackageName) {
		// TODO Auto-generated method stub
		return option.PRESS_BACK;
	}

	@Override
	public Queue<String> getAppExecutionSequence(LinkedHashMap<String, String> appInfo) {
		// TODO Auto-generated method stub
		Queue<String> executionSeq = new LinkedList<String>();
		Iterator it = appInfo.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry entity = (Entry) it.next();
			executionSeq.offer((String) entity.getKey());
		}
		Queue<String> tmp = executionSeq;
		executionSeq.addAll(tmp);
		executionSeq.poll();
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
		HashSet<String> policy = new HashSet<String>();
		policy.add("android.widget.TextView");
		policy.add("android.widget.ImageView");
		policy.add("android.widget.Button");
		policy.add("android.widget.ImageButton");
		return policy;
	}

	@Override
	public boolean onStateEquivalent(RYState lastState, RYState currentState) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AccessibilityNodeInfo getNextClickItem(
			ArrayList<AccessibilityNodeInfo> availableNodes) {
		// TODO Auto-generated method stub
		for (AccessibilityNodeInfo node : availableNodes){
			if (node.getClassName().equals("android.widget.Button")) {
				return node;
			}
		}
		return availableNodes.get(0);
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
