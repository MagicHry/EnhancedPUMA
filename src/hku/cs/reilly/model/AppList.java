package hku.cs.reilly.model;

import java.util.ArrayList;

public class AppList {
	private int totalAppCount;
	private ArrayList<App> appList;
	public int getTotalAppCount() {
		return totalAppCount;
	}
	public void setTotalAppCount(int totalAppCount) {
		this.totalAppCount = totalAppCount;
	}
	public ArrayList<App> getAppList() {
		return appList;
	}
	public void setAppList(ArrayList<App> appList) {
		this.appList = appList;
	}
	
	
}

