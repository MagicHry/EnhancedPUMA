package hku.cs.reilly.launcher;

/**
 * A node that aggregates the original AccessibilitNodeInfo
 * Key point is to record whether this node is clicked or not
 */

import android.view.accessibility.AccessibilityNodeInfo;

public class RYNodeInfo{
	
	private AccessibilityNodeInfo mNodeInfo;
	private boolean mClicked = false;
	RYNodeInfo(AccessibilityNodeInfo nodeInfo, boolean isClicked) {
		// TODO Auto-generated constructor stub
		this.mNodeInfo = nodeInfo;
		this.mClicked = isClicked;
	}
	
	public AccessibilityNodeInfo getAccessibilityNodeInfo(){
		return this.mNodeInfo;
	}
	
	public boolean isClicked(){
		return this.mClicked;
	}
	
	public void setClicked(){
		this.mClicked = true;
	}

}
