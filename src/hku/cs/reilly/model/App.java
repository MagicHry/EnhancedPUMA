package hku.cs.reilly.model;

import java.util.ArrayList;

public class App {
	private String packageName;
	private ArrayList<AppState> appStates;
	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	public ArrayList<AppState> getAppStates() {
		return appStates;
	}
	public void setAppStates(ArrayList<AppState> appStates) {
		this.appStates = appStates;
	}
	
}
