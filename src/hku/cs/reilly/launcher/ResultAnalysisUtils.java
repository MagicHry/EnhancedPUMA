package hku.cs.reilly.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import hku.cs.reilly.model.App;
import hku.cs.reilly.model.AppList;
import hku.cs.reilly.model.AppState;
import hku.cs.reilly.model.StateTransition;

public class ResultAnalysisUtils {
	static public AppList constructAppList(RYStateManager manager){
		HashMap<String, ArrayList<RYState>> stateGraph = manager.getGraph();
		AppList result = new AppList();
		Iterator it = stateGraph.entrySet().iterator();
		ArrayList<App> apps = new ArrayList<App>();
		while(it.hasNext()){
			Map.Entry entity = (Entry) it.next();
			App app = new App();
			app.setPackageName((String) entity.getKey());
			ArrayList<AppState> states = new ArrayList<AppState>();
			ArrayList<RYState> realStates = (ArrayList<RYState>) entity.getValue();
			for (RYState realState : realStates){
				states.add(realState.construteAppState());
			}
			app.setAppStates(states);
			apps.add(app);
		}
		result.setAppList(apps);
		result.setTotalAppCount(apps.size());
		return result;
	}
	
	static public String toJson(AppList appInfo) throws JSONException{
		JSONObject result = new JSONObject();
		result.put("TotalCount", appInfo.getTotalAppCount());
		JSONArray appArr = new JSONArray();
		for (App app : appInfo.getAppList()){
			JSONObject appObject = new JSONObject();
			appObject.put("PackageName", app.getPackageName());
			JSONArray appStateArr = new JSONArray();
			for (AppState state : app.getAppStates()){
				JSONObject stateObject = new JSONObject();
				stateObject.put("StateID", state.getStateID());
				stateObject.put("BelongingPackageName", state.getmBelongingPackageName());
				JSONArray appStateTransitionArr = new JSONArray();
				if (state.getStateTransitions().size() > 0) {
					for (StateTransition transition : state.getStateTransitions()){
						JSONObject transitionObject = new JSONObject();
						transitionObject.put("ClickingID", transition.getClickingID());
						transitionObject.put("Direction", transition.getDirection());
						transitionObject.put("ToStateID", transition.getToStateID());
						appStateTransitionArr.put(transitionObject);
					}
				}				
				stateObject.put("Transitions", appStateTransitionArr);
				appStateArr.put(stateObject);
			}
			appObject.put("States", appStateArr);
			appArr.put(appObject);
			
		}
		result.put("Apps", appArr);
		return result.toString();
	}
}
