package hku.cs.reilly.launcher;

import hku.cs.reilly.handler.BackwardCheckingHandler;
import hku.cs.reilly.handler.ForwardCheckingHandler;
import hku.cs.reilly.launcher.RYEventHandler.option;
import hku.cs.reilly.launcher.RYTransition.direction;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import nsl.stg.core.AccessibilityEventProcessor;
import nsl.stg.core.EventDoneListener;
import nsl.stg.core.ExplorationState;
import nsl.stg.core.ExplorationStateManager;
import nsl.stg.core.FeatureValuePair;
import nsl.stg.core.HashIdDictionary;
import nsl.stg.core.MergeNodeInfo;
import nsl.stg.core.MyAccessibilityNodeInfo;
import nsl.stg.core.MyAccessibilityNodeInfoSuite;
import nsl.stg.core.UIAction;
import nsl.stg.core.UIState;
import nsl.stg.core.UIStateManager;
import nsl.stg.tests.Util;
import nsl.stg.uiautomator.core.MyAccessibilityNodeInfoHelper;
import nsl.stg.uiautomator.core.MyInteractionController;
import nsl.stg.uiautomator.core.MyUiDevice;
import nsl.stg.uiautomator.testrunner.MyUiAutomatorTestCase;
import android.app.UiAutomation.OnAccessibilityEventListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.TrafficStats;
import android.net.http.EventHandler;
import android.os.Parcel;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.android.uiautomator.core.Configurator;
import com.android.uiautomator.core.UiAutomatorBridge;
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;

public class EnhancedPUMALauncher extends MyUiAutomatorTestCase {
	private MyUiDevice dev;
	private UiAutomatorBridge bridge;
	private MyInteractionController controller;
	private AccessibilityNodeInfo root;
	private Configurator mConfig = Configurator.getInstance();

	private String appName, packName, launcherPackName; //current appName, current packageName and launcher package name
	private LinkedHashMap<String, String> mAppInfo = new LinkedHashMap<String, String>(); //Adding Appinfo map to let the PUMA scan across different apps
	private HashMap<String, Integer> mAppEnterCounts = new HashMap<String, Integer>(); //Counts how many times each app is scanned
	private Queue<String> mAppExcSequence; //App execution sequence
	private HashSet<String> mClickingPolicy; //User defined clicking policy
	private RYEventHandler mEventHandler = new BackwardCheckingHandler();
	private int uid; // used by network usage monitor
	private final long WINDOW_CONTENT_UPDATE_PERIOD = 2000; // 2s
	private final long WINDOW_CONTENT_UPDATE_TIMEOUT = 60000; // 60000; // 60s

	private RYStateManager sManager = RYStateManager.getInstance();
	private ExplorationStateManager eManager = ExplorationStateManager.getInstance();

	// ============================================================
	// Proof-of-concept for passive monkey
	public void _testPassiveMonkey() {
		dev = getMyUiDevice();
		bridge = dev.getUiAutomatorBridge();

		final String appPackageName = "com.chenio.android.sixpark";
		final String fn = "/sdcard/haos/events.log";
		Util.openFile(fn, true);

		// intercept all events 
		dev.getUiAutomation().setOnAccessibilityEventListener(new OnAccessibilityEventListener() {
			public void onAccessibilityEvent(AccessibilityEvent event) {
				CharSequence pack_name = event.getPackageName();
				// only process related events
				if (pack_name != null && pack_name.equals(appPackageName)) {
					// log events
					Util.log2File(event);
				} else {
					// ignore the rest: e.g. notification etc
					// Util.err("UNKNOWN EVENT " + pack_name);
				}
			}
		});

		Thread t = new Thread() {
			public void run() {
				while (true) {
					dev.waitForIdle();

					AccessibilityNodeInfo rootNode = bridge.getRootInActiveWindow();
					if (rootNode.getPackageName().equals(appPackageName)) {
						String timestamp = new SimpleDateFormat("MM-dd'T'HH-mm-ss-SSS").format(new Date());

						boolean status = dev.takeScreenshot(new File("/sdcard/haos/" + timestamp + ".png"), 0.1f, 90);
						Util.log("Dumped screenshot: " + status);

						dev.dumpWindowHierarchy(timestamp + ".xml"); // default location "/data/local/tmp/local/tmp/*.xml"
						Util.log("Dumped view tree");
					}

					SystemClock.sleep(2000);
				}
			}
		};
		t.start();

		SystemClock.sleep(30000); // 30s

		Util.closeFile(fn);
	}

	// ============================================================
	// testStart for dex2jar phase 3 check
	public void _testStart() throws UiObjectNotFoundException {
		// 0. get pointer to the important objects
		dev = getMyUiDevice();
		bridge = dev.getUiAutomatorBridge();
		controller = new MyInteractionController(bridge);
		loadAppInfo();

		Configurator.getInstance().setWaitForSelectorTimeout(30000); // default is 10s, but emulator is too slow

		start_target_app("TEST");

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// IMPORTANT: NOT WORKING ON EMULATOR
		// dev.takeScreenshot(new File("/sdcard/haos/" + packName + ".png"), 0.5f, 90);
		takeScreenshot("/sdcard/screen-app.png");

		String currPackName = dev.getCurrentPackageName();
		if (currPackName == null) {
			Util.log("FAIL," + packName + ",NULL");
		} else {
			if (currPackName.equalsIgnoreCase(packName)) {
				Util.log("PASS," + packName);
			} else {
				Util.log("FAIL," + packName + "," + currPackName);
			}
		}

		boolean isHome = dev.pressHome();

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (isHome) {
			Util.log("OK: At HOME");
		} else {
			Util.log("ERR: At HOME");
		}
	}

	// workaround for emulator
	private void takeScreenshot(final String filePath) {
		try {
			// IMPORTANT: WRONG WAY TO USE PROCESS, WASTED TWO DAYS! 
			// Process p = new ProcessBuilder("screencap -p " + filePath).start();
			Process p = Runtime.getRuntime().exec("screencap -p " + filePath);
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void _testAfa() {
		Util.log(UIState.DUMMY_3RD_PARTY_STATE.dumpShort());
		Util.log(ExplorationState.DUMMY_3RD_PARTY_STATE.dumpShort());
	}

	// ============================================================
	// Entry method fromIndex TestRunner
	public void testMain() throws UiObjectNotFoundException {
		// 0. get pointer to the important objects
		dev = getMyUiDevice();
		bridge = dev.getUiAutomatorBridge();
		controller = new MyInteractionController(bridge);
		mClickingPolicy = mEventHandler.getClickingPolicy();
		loadAppInfo();

		// 0.5 intercept all events
		dev.getUiAutomation().setOnAccessibilityEventListener(new OnAccessibilityEventListener() {
			public void onAccessibilityEvent(AccessibilityEvent event) {
				CharSequence pack_name = event.getPackageName();
				// only process related events
				// intercept all related apps
				if (pack_name != null && mAppInfo.containsKey(pack_name)) {
					AccessibilityEventProcessor.process(event);
				} else {
					// ignore the rest: e.g. notification etc
				}
			}
		});

		RYState currState, lastState; //clusterHead;
		currState = lastState = null; // = clusterHead = null;
		option userNewAppOption = null;
		option userDiffAppOption = null;
		direction lastDirection = null;
		int lastFeatureHash, currentFeatureHash;
		lastFeatureHash = currentFeatureHash = 0;
		int clickId = -1, i = 0;

		mAppExcSequence = this.mEventHandler.getAppExecutionSequence(mAppInfo);
		
		long startTimeMillis = SystemClock.uptimeMillis();
		long currTimeMillis;
		final long TIMEOUT = 1200000; // 20 mins
		while ((mAppExcSequence.size() > 0 || !(packName == null)) && !(mEventHandler.explorationDone(sManager))) {
			currTimeMillis = SystemClock.uptimeMillis();
			if (currTimeMillis - startTimeMillis > TIMEOUT) {
				Util.log("TIMEOUT");
				break;
			}
			SystemClock.sleep(350);
			Util.log("--------------- iter " + (i++));
			if (i % 5 == 0) {
				lastFeatureHash = currentFeatureHash;
				currentFeatureHash = sManager.getTotalStateCount();
			}
			if ((lastFeatureHash == currentFeatureHash) && i > 5 && lastDirection == direction.BACKWARD) { // we made no progress in last 5 iterations
				//means we somehow stuck in the app, and back button does not work,
				//press home button instead
				Util.log("RY:Stuck in the app, home");
				dev.pressHome();
				//clear the current packageName & appName & direction & clickID & lastState & currentState
				this.packName = null;
				this.appName = null;
				currState = null;
				lastState = null;
				clickId = -1;
				lastDirection = null;
				//clear the option
				userDiffAppOption = null;
				userNewAppOption = null;
				continue;
			}
			//0.Check whether the back pressed leads us back to the launcher
			if (appName != null && packName != null && 
					dev.getCurrentPackageName().equals(launcherPackName)) {
				//Means the last app is fully explored
				//Now we need to link the last state with the launcher state
				currState = sManager.getState(RYState.generateLuancherState(launcherPackName), packName);
				lastState = sManager.getState(lastState, packName);
				RYTransition transition = new RYTransition(clickId, currState, lastDirection);
				lastState.addTransition(transition);
				//After the linking, we need to reset every var to explore next app
				this.packName = null;
				this.appName = null;
				currState = null;
				lastState = null;
				clickId = -1;
				lastDirection = null;
				//clear the option
				userDiffAppOption = null;
				userNewAppOption = null;
				//however, if the AppExcSequence is empty too, then it means we are done
				if (mAppExcSequence.size() == 0) {
					//12. Do the result analysis
					this.mEventHandler.onResultAnalysis(sManager);
					break;
				}
			}
			
			//1.Check whether we need to launche a new app
			if(appName == null && packName == null){
				//start a new app
				packName = mAppExcSequence.poll();
				appName = mAppInfo.get(packName);
				//2.Start the targe app
				start_target_app(appName);
				int appCounts = mAppEnterCounts.get(packName);
				if (appCounts == 0) {
					//firstly enter into this application
					userNewAppOption = mEventHandler.onNewAppLaunched(packName, packName);
				}
				appCounts++;
				mAppEnterCounts.put(packName, appCounts);
				Util.log("RY: SIZE OF COUNTER = "+mAppEnterCounts.size());
				//TODO: Move it back to the downer side
			}
			
			String currPackName = dev.getCurrentPackageName();
			//3.Get the current UIState
			//Noted that, in here, currPackName may not equal to the supposed packName
			currState = getCurrentUIState(currPackName);
			if (currPackName == null) { // the app is not UIAutomator friendly
				Util.err("FATAL: Package name NULL. Quit...");
				break;
			}
			else if (!currPackName.equals(this.packName)) {
				//4.Encounter a different application
				userDiffAppOption =  this.mEventHandler.onReachingDiffApp(this.packName, currPackName);
			}
			
			//5.Add this state into the state manager, noted that, this state may not belong to the supposed app
			//But we need it to be inside of the supposed app`s graph 
			if(!mEventHandler.onStateEquivalent(lastState, currState)){
				//make sure these 2 states are diff in terms of the user definition
				sManager.addState(currState,this.packName);
				currState = sManager.getState(currState, this.packName);
			}
	
			//6.Add transition to link the current state and the last state
			RYTransition transition;
			if (lastState == null) {
				//means we start from the launcher
				lastState = RYState.generateLuancherState(launcherPackName);
				//because it is from the launcher, we set it to be forward
				lastDirection = direction.FOWARD;
				transition = new RYTransition(-1, currState, lastDirection);
				sManager.addState(lastState, packName);
				//try to get it from the manager, in case it already lies in the manager
				lastState = sManager.getState(lastState, packName);
				lastState.addTransition(transition);
			}
			else{
				//make sure we get the last state from the manager
				lastState = sManager.getState(lastState, packName);
				if (lastState != null && !mEventHandler.onStateEquivalent(lastState, currState)) {
					transition = new RYTransition(clickId, currState, lastDirection);
					lastState.addTransition(transition);
				}
			}
			
			//7.Time to signal UI_LOAD_DONE event
			Util.log("UI_LOAD_DONE");
			dev.dumpWindowHierarchy(packName + "_" + i + ".xml"); // default location "/data/local/tmp/local/tmp/*.xml"
			
			//8.Perform the corresponding action based on the current UIState
			RYState stateReadyForAction = sManager.getState(currState,packName);
			
			//9.First we need to check the user`s option in NewAppLaunch & EnterDiffApp
			//Noted that the EnterDiffApp have the higher priority (This 2 can happen at the same time)
			if (userDiffAppOption != null) {
				if (userDiffAppOption == option.CONTINUE) {
					Util.log("RY:Enter into diff app, continue");
					//clear the option
					userDiffAppOption = null;
					if (userNewAppOption != null) {
						//in case this 2 options are both not null, we need to update this newAppOption either
						userNewAppOption = null;
					}
				}
				else if(userDiffAppOption == option.PRESS_BACK){
					Util.log("RY:Enter into diff app, back");
					dev.pressBack();
					lastDirection = direction.BACKWARD;
					lastState = stateReadyForAction;
					clickId = -1;
					//clear the option
					userDiffAppOption = null;
					if (userNewAppOption != null) {
						//in case this 2 options are both not null, we need to update this newAppOption either
						userNewAppOption = null;
					}
					continue;
				}
				else if(userDiffAppOption == option.PRESS_HOME){
					Util.log("RY:Enter into diff app, home");
					dev.pressHome();
					//clear the current packageName & appName & direction & clickID & lastState & currentState
					this.packName = null;
					this.appName = null;
					currState = null;
					lastState = null;
					clickId = -1;
					lastDirection = null;
					//clear the option
					userDiffAppOption = null;
					if (userNewAppOption != null) {
						//in case this 2 options are both not null, we need to update this newAppOption either
						userNewAppOption = null;
					}
					continue;
				}
			}
			else if (userNewAppOption != null) {
				if (userNewAppOption == option.CONTINUE) {
						Util.log("RY:Enter into new app, continue");
						//do nothing
						userNewAppOption = null;
					}
					else if(userNewAppOption == option.PRESS_BACK){
						Util.log("RY:Enter into new app, back");
						dev.pressBack();
						lastDirection = direction.BACKWARD;
						lastState = stateReadyForAction;
						clickId = -1;
						//clear the option
						userNewAppOption = null;
						continue;
					}
					else if(userNewAppOption == option.PRESS_HOME){
						Util.log("RY:Enter into new app, home");
						dev.pressHome();
						//clear the current packageName & appName & direction & clickID & lastState & currentState
						this.packName = null;
						this.appName = null;
						currState = null;
						lastState = null;
						clickId = -1;
						lastDirection = null;
						//clear the option
						userNewAppOption = null;
						continue;
					}
			}
			
			//11.If the current state is fully observed, then back track
			if (stateReadyForAction.isFullyExplored()) {
				dev.pressBack();
				lastDirection = direction.BACKWARD;
				lastState = stateReadyForAction;
				clickId = -1;
			}
			else {
				ArrayList<AccessibilityNodeInfo> availableNodes = stateReadyForAction.getClickables();
				AccessibilityNodeInfo userChoice = mEventHandler.getNextClickItem(availableNodes);
				Rect bounds = new Rect();
				userChoice.getBoundsInScreen(bounds);
				click(controller, userChoice);
				lastDirection = direction.FOWARD;
				lastState = stateReadyForAction;
				clickId = userChoice.getWindowId();
				stateReadyForAction.setClicked(userChoice);
			}
			dev.waitForIdle();
			//12.Check whether we shall stop in here based on user`s definition
			if (this.mEventHandler.explorationDone(sManager)) {
				Util.log("RY:Over user`s definition, EXIT");
				break;
			}
		}

//		Util.log("========================================");
//		Util.log("Total UIState clusters: " + sManager.dumpShort());
//		Util.log(sManager);
//
//		Util.log("Total ExplorationStates: " + eManager.dumpShort());
//		Util.log("========================================");
		this.mEventHandler.onResultAnalysis(sManager);
	}
	
	private void force_stop() {
		Util.log("force_stop");
		execShell("am force-stop " + packName);
		execShell("pm clear " + packName);
	}

	private void execShellSu(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			os.writeBytes(cmd + "\n");
			os.writeBytes("exit\n");
			os.flush();
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// execute given command in shell
	private void execShell(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Perform replays according to the specified list of actions fromIndex the specified initial state
	// DO NOT NEED IT ANYMORE
//	private boolean replay_actions(ExplorationState initState, List<UIAction> actions) {
//		boolean ret = true;
//
//		for (int i = 0; i < actions.size(); i++) {
//			UIAction action = actions.get(i);
//			ExplorationState expEState = action.getState();
//			int clickId = action.getClickId();
//			Util.log("Repalying action: " + action);
//
//			UIState currState = getCurrentUIState(packName);
//			UIState clusterHead = sManager.getState(currState);
//			ExplorationState currEState = new ExplorationState(clusterHead, currState.computeFeatureHash());
//
//			if (currEState.equals(expEState)) {
//				List<AccessibilityNodeInfo> clickables = getClickables(root);
//
//				if (clickId < clickables.size()) {
//					AccessibilityNodeInfo candidate = clickables.get(clickId);
//					Util.log("Clicking " + clickId);
//
//					click(controller, candidate);
//					continue;
//				}
//			}
//
//			Util.err("State unexpected or click impossible");
//
//			ret = false;
//			break;
//		}
//
//		return ret;
//	}

	// Save timestamped screenshot 
	private void dump_screen(String fileName, Rect bounds) {
		dev.dumpWindowHierarchy(fileName + ".xml");
		String screenshotFile = "/data/local/tmp/local/tmp/" + fileName + ".png";
		boolean status = dev.takeScreenshot(new File(screenshotFile), 1.0f, 90);
		Util.log("screenshot: " + (status ? "OK" : "FAIL"));

		Bitmap original = BitmapFactory.decodeFile(screenshotFile);
		Bitmap modified = original.copy(Bitmap.Config.ARGB_8888, true);

		Canvas canvas = new Canvas(modified);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(5f);
		canvas.drawRect(bounds, paint);

		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(screenshotFile));
			if (bos != null) {
				modified.compress(Bitmap.CompressFormat.PNG, 90, bos);
				bos.flush();
			}
		} catch (IOException ioe) {
			Util.err("Failed to save screen shot to file");
			ioe.printStackTrace();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException ioe) {
					/* ignore */
				}
			}
			original.recycle();
			modified.recycle();
		}
	}

	// Loads UID, package name and label
	private void loadAppInfo() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/data/local/tmp/app.uid"));
			uid = Integer.parseInt(br.readLine().trim());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		try {
			br = new BufferedReader(new FileReader("/data/local/tmp/app.info"));
			while (true){
				//change reading single app info to multiple app info
				String appInfo = br.readLine();
				if(appInfo == null || appInfo.equals("")){
					break;
				}
				String[] infos = appInfo.split("@");
				String appPackageName = infos[0];
				String appName = infos[1];
				mAppInfo.put(appPackageName, appName);
				mAppEnterCounts.put(appPackageName, 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Wrapper method
	private void waitForNetworkUpdate() {
		waitForNetworkUpdate(WINDOW_CONTENT_UPDATE_PERIOD, WINDOW_CONTENT_UPDATE_TIMEOUT);
	}

	// check network usage every x ms and timeout if there is no change for T ms.
	private void waitForNetworkUpdate(long idleTimeoutMillis, long globalTimeoutMillis) {
		final long startTimeMillis = SystemClock.uptimeMillis();
		long prevTraffic = TrafficStats.getUidTxBytes(uid) + TrafficStats.getUidRxBytes(uid);
		boolean idleDetected = false;
		long totTraffic = 0;

		while (!idleDetected) {
			final long elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis;
			final long remainingTimeMillis = globalTimeoutMillis - elapsedTimeMillis;
			if (remainingTimeMillis <= 0) {
				Util.err("NO_IDLE_TIMEOUT: " + globalTimeoutMillis);
				break;
			}

			try {
				Thread.sleep(idleTimeoutMillis);
				long currTraffic = TrafficStats.getUidTxBytes(uid) + TrafficStats.getUidRxBytes(uid);
				long delta = currTraffic - prevTraffic;
				if (delta > 0) {
					totTraffic += delta;
					prevTraffic = currTraffic;
				} else { // idle detected
					idleDetected = true;
				}
			} catch (InterruptedException ie) {
				/* ignore */
			}
		}

		if (idleDetected) {
			Util.log("Traffic: " + totTraffic);
		}
	}

	// More robust way to get source AccessibilityNodeInfo node
	// NOTE: possible to be null
	private AccessibilityNodeInfo getRootNode() {
		final int maxRetry = 4;
		final long waitInterval = 250; // 0.25s
		AccessibilityNodeInfo rootNode = null;

		for (int x = 0; x < maxRetry; x++) {
			rootNode = bridge.getRootInActiveWindow();
			if (rootNode != null) {
				return rootNode;
			}
			if (x < maxRetry - 1) {
				Util.err("Got null source node fromIndex accessibility - Retrying...");
				SystemClock.sleep(waitInterval);
			}
		}

		return rootNode;
	}

	// Find the scrollable node fromIndex sub-tree rooted at treeRoot
	private AccessibilityNodeInfo getScrollableNode(AccessibilityNodeInfo treeRoot) {
		List<AccessibilityNodeInfo> ret = new ArrayList<AccessibilityNodeInfo>();
		Queue<AccessibilityNodeInfo> Q = new LinkedList<AccessibilityNodeInfo>();
		Q.add(treeRoot);

		while (!Q.isEmpty()) {
			AccessibilityNodeInfo node = Q.remove();

			if (node == null) {
				// Util.log("Processing NULL");
				continue;
			}
			// Util.log("Processing " + node.getClassName());

			// check current node
			if (node.isVisibleToUser() && node.isEnabled() && node.isScrollable()) {
				ret.add(node);
			}

			// add its children to queue
			int childCnt = node.getChildCount();
			if (childCnt > 0) {
				for (int i = 0; i < childCnt; i++) {
					AccessibilityNodeInfo child = node.getChild(i);
					Q.add(child); // no need to check NULL, checked above
				}
			}
		}

		if (ret.isEmpty()) {
			Util.log("No scrollable node found");
			return null;
		} else {
			if (ret.size() > 1) {
				Util.log("NOTE: Found  " + ret.size() + " scrollable nodes.");
			}

			Util.log("Selected " + ret.get(0).getClassName());

			return ret.get(0);
		}
	}

	// Perform scrolling action vertically or horizontally, upwards or downwards
	// until last SCROLLED event received.
	private AccessibilityEvent scrollAndWaitForLastEvent(AccessibilityNodeInfo scrollNode, boolean isVertical, boolean isDown) {
		Rect rect = new Rect();
		scrollNode.getBoundsInScreen(rect);

		final double MARGIN_RATIO = 0.2;
		final int SCROLL_STEPS = 55; // TODO: determine this by calculation? SCROLL_STEPS (55) is default value in UiScrollable.

		int margin;
		int downX = 0;
		int downY = 0;
		int upX = 0;
		int upY = 0;

		if (isVertical) {
			margin = (int) (rect.height() * MARGIN_RATIO);
			downX = rect.centerX();
			downY = rect.bottom - margin;
			upX = rect.centerX();
			upY = rect.top + margin;
		} else {
			margin = (int) (rect.width() * MARGIN_RATIO);
			downX = rect.right - margin;
			downY = rect.centerY();
			upX = rect.left + margin;
			upY = rect.centerY();
		}

		MyEventDoneListener callback = new MyEventDoneListener();
		AccessibilityEventProcessor.waitForLastEventNonBlocking(AccessibilityEvent.TYPE_VIEW_SCROLLED, 1000, callback);

		boolean status;
		if (isDown) {
			status = controller.swipe(downX, downY, upX, upY, SCROLL_STEPS);
		} else {
			status = controller.swipe(upX, upY, downX, downY, SCROLL_STEPS);
		}

		while (!callback.isDone) {
			SystemClock.sleep(500);
		}

		AccessibilityEvent event = callback.getEvent();

		// debug only
		Util.log("Scrolled(" + (isVertical ? "UD" : "LR") + "," + (isDown ? "FWD" : "BWD") + ") " + status);

		return event;
	}

	// Asynchronous call
	class MyEventDoneListener implements EventDoneListener {
		private AccessibilityEvent event = null;
		private boolean isDone = false;

		public void onEventDone(AccessibilityEvent aEvent) {
			event = aEvent;
			isDone = true;
		}

		public AccessibilityEvent getEvent() {
			return event;
		}

		public boolean isDone() {
			return isDone;
		}
	}

	// Convert tree to a list
	private List<AccessibilityNodeInfo> flattenTree(AccessibilityNodeInfo rootNode) {
		List<AccessibilityNodeInfo> allNodes = new ArrayList<AccessibilityNodeInfo>();
		Queue<AccessibilityNodeInfo> Q = new LinkedList<AccessibilityNodeInfo>();

		Q.add(rootNode);

		// BFS, level-order traversal
		while (!Q.isEmpty()) {
			AccessibilityNodeInfo node = Q.poll();
			allNodes.add(node);

			for (int i = 0; i < node.getChildCount(); i++) {
				AccessibilityNodeInfo child = node.getChild(i);
				Q.add(child);
			}
		}

		return allNodes;
	}

	// Reconstruct tree fromIndex list
	private MyAccessibilityNodeInfo rebuildTree(List<AccessibilityNodeInfo> allNodes) {
		Queue<MyAccessibilityNodeInfo> Q = new LinkedList<MyAccessibilityNodeInfo>();
		MyAccessibilityNodeInfo mRootNode = new MyAccessibilityNodeInfo(allNodes.get(0));
		Q.add(mRootNode);

		int idx = 1;

		while (!Q.isEmpty()) {
			MyAccessibilityNodeInfo mNode = Q.poll();
			AccessibilityNodeInfo node = mNode.getOriginal();

			for (int i = 0; i < node.getChildCount(); i++) {
				AccessibilityNodeInfo child = allNodes.get(idx);
				MyAccessibilityNodeInfo mChild = new MyAccessibilityNodeInfo(child);
				mNode.addChild(mChild);

				Q.add(mChild);
				idx++;
			}
		}

		return mRootNode;
	}

	// Save whole tree
	private int saveTreeToFile(AccessibilityNodeInfo node, String fn) {
		List<AccessibilityNodeInfo> nodes = flattenTree(node);
		final Parcel p = Parcel.obtain();

		p.writeTypedList(nodes);

		byte[] rawData;
		int bytes = 0;
		try {
			FileOutputStream fos = new FileOutputStream(new File(fn));
			rawData = p.marshall();
			bytes = rawData.length;
			fos.write(rawData);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Util.log("Saved " + nodes.size() + " nodes");
		return bytes;

	}

	// Load whole tree
	private MyAccessibilityNodeInfo loadTreeFromFile(String fn, int expSize) {
		final Parcel p = Parcel.obtain();
		List<AccessibilityNodeInfo> allNodes = p.createTypedArrayList(AccessibilityNodeInfo.CREATOR);

		try {
			FileInputStream fis = new FileInputStream(new File(fn));
			byte[] rawData = new byte[expSize];
			int actualSize = fis.read(rawData);

			if (actualSize == expSize) {
				p.unmarshall(rawData, 0, expSize);
				p.setDataPosition(0);
				p.readTypedList(allNodes, AccessibilityNodeInfo.CREATOR);
			} else {
				Util.err("Read failed: read " + actualSize + " bytes, expected " + expSize + " bytes");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Util.log("Loaded " + allNodes.size() + " nodes");

		return rebuildTree(allNodes);
	}

	// Save root node
	private int saveViewToFile(AccessibilityNodeInfo node, String fn) {
		final Parcel p = Parcel.obtain();
		node.writeToParcel(p, 0);

		byte[] rawData;
		int bytes = 0;

		try {
			FileOutputStream fos = new FileOutputStream(new File(fn));
			rawData = p.marshall();
			bytes = rawData.length;
			fos.write(rawData);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return bytes;
	}

	// Load root node
	private AccessibilityNodeInfo loadViewFromFile(String fn, int size) {
		final Parcel p = Parcel.obtain();
		AccessibilityNodeInfo node = null;

		try {
			FileInputStream fis = new FileInputStream(new File(fn));
			byte[] rawData = new byte[size];
			int actualSize = fis.read(rawData);

			if (actualSize == size) {
				p.unmarshall(rawData, 0, size);
				p.setDataPosition(0);
				node = AccessibilityNodeInfo.CREATOR.createFromParcel(p);
			} else {
				Util.err("Read failed: read " + actualSize + " bytes, expected " + size + " bytes");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return node;
	}

	class ScrollDirection {
		public boolean horizontal = false;
		public boolean vertical = false;

		public String toString() {
			return "[L-->R: " + horizontal + ", U-->D: " + vertical + "]";
		}
	}

	// Check whether generated event is from a scrolling action
	private boolean checkScrollEvent(AccessibilityEvent event) {
		return ((event != null) && (event.getFromIndex() > -1 || event.getScrollX() > -1 || event.getScrollY() > -1));
	}

	// Make test move along both directions and record the response
	private ScrollDirection findScrollDirection() {
		AccessibilityNodeInfo localRoot = getRootNode();
		AccessibilityNodeInfo toScroll = getScrollableNode(localRoot);
		ScrollDirection sDir = new ScrollDirection();
		AccessibilityEvent event;
		boolean atBeginning;

		// Util.log(toScroll);

		// Test Up --> Down
		event = scrollAndWaitForLastEvent(toScroll, true, true);
		sDir.vertical = checkScrollEvent(event);

		if (!toScroll.getClassName().equals(ListView.class.getCanonicalName())) {
			// Test Left --> Right
			event = scrollAndWaitForLastEvent(toScroll, false, true);
			sDir.horizontal = checkScrollEvent(event);

			// Restore view position
			do {
				event = scrollAndWaitForLastEvent(toScroll, false, false);
				atBeginning = (event == null) || (event.getFromIndex() == 0) || (event.getScrollX() == 0);
			} while (!atBeginning);
		}

		// Restore view position
		do {
			event = scrollAndWaitForLastEvent(toScroll, true, false);
			atBeginning = (event == null) || (event.getFromIndex() == 0) || (event.getScrollY() == 0);
		} while (!atBeginning);

		return sDir;
	}

	// Save relevant attributes from AccessibilityEvent
	private void saveScrollHint(AccessibilityEvent event, Map<String, Integer> map, List<MergeNodeInfo> toMergeList) {
		AccessibilityNodeInfo localRoot = getRootNode();

		String fn = "/sdcard/haos-" + map.size() + ".dat";
		int size = saveTreeToFile(localRoot, fn);
		map.put(fn, size);

		MergeNodeInfo hint = new MergeNodeInfo(event);
		toMergeList.add(hint);
		Util.log("Saved " + hint);
	}

	// Compare two events from the same source
	// Return true if they are same and false otherwise
	private boolean compareScrollEvent(AccessibilityEvent event1, AccessibilityEvent event2) {
		boolean same = false;

		if (event1 == null && event2 == null) {
			same = true;
		} else if (event1 != null && event2 != null) {
			String s1 = event1.getFromIndex() + "," + event1.getToIndex() + "," + event1.getScrollX() + "," + event1.getScrollY();
			String s2 = event2.getFromIndex() + "," + event2.getToIndex() + "," + event2.getScrollX() + "," + event2.getScrollY();
			same = s1.equals(s2);
		}

		return same;
	}

	// Scroll to complete the merging of different views with given direction hint.
	private MyAccessibilityNodeInfo scrollView(ScrollDirection sDir) {
		if (!sDir.horizontal && !sDir.vertical) { // no need to scroll at all
			return null;
		}

		List<MergeNodeInfo> toMergeList = new ArrayList<MergeNodeInfo>();
		Map<String, Integer> map = new Hashtable<String, Integer>();
		int k = 0;

		// 1. Store initial view
		AccessibilityNodeInfo localRoot = getRootNode();
		String fn = "/sdcard/haos-" + k + ".dat";
		int size = saveTreeToFile(localRoot, fn);
		map.put(fn, size);

		MyAccessibilityNodeInfo mLocalRoot = loadTreeFromFile(fn, map.get(fn));
		localRoot = mLocalRoot.getOriginal();
		AccessibilityNodeInfo toScroll = getScrollableNode(localRoot);

		AccessibilityEvent lastEvent, currEvent;
		boolean proceed = false;

		// 2. Scroll and store the rest view
		if (sDir.horizontal && sDir.vertical) { // Zig-Zag scrolling
			boolean LR = true, UD = true;
			AccessibilityEvent lastUDEvent = null, currUDEvent;

			do {
				lastEvent = null;
				do {
					Util.log("Scrolling " + (LR ? "L --> R" : "R --> L"));
					currEvent = scrollAndWaitForLastEvent(toScroll, false, LR);

					proceed = (currEvent != null) && !compareScrollEvent(lastEvent, currEvent);
					if (proceed) {
						saveScrollHint(currEvent, map, toMergeList);
					}

					lastEvent = currEvent;
				} while (proceed);

				Util.log("Scrolling U --> D");
				currUDEvent = scrollAndWaitForLastEvent(toScroll, true, true);

				if (currUDEvent == null) {
					UD = false;
				} else {
					if (lastUDEvent != null) {
						UD = (lastUDEvent.getScrollY() < currUDEvent.getScrollY());
					} else {
						UD = true;
					}
				}

				if (UD) {
					saveScrollHint(currUDEvent, map, toMergeList);
				}

				lastUDEvent = currUDEvent;
				LR = !LR;
			} while (UD);
		} else if (sDir.horizontal) { // Horizontal scroll only
			lastEvent = null;
			do {
				Util.log("Scrolling L --> R");
				currEvent = scrollAndWaitForLastEvent(toScroll, false, true);

				proceed = (currEvent != null) && !compareScrollEvent(lastEvent, currEvent);
				if (proceed) {
					saveScrollHint(currEvent, map, toMergeList);
				}

				lastEvent = currEvent;
			} while (proceed);
		} else if (sDir.vertical) { // Vertical scroll only
			lastEvent = null;
			do {
				Util.log("Scrolling U --> D");
				currEvent = scrollAndWaitForLastEvent(toScroll, true, true);

				proceed = (currEvent != null) && !compareScrollEvent(lastEvent, currEvent);
				if (proceed) {
					saveScrollHint(currEvent, map, toMergeList);
				}

				lastEvent = currEvent;
			} while (proceed);
		}

		// Now we have explored the whole space
		Util.log(map.size() + " views to merge.");

		// 3. Time to merge based on hints
		fn = "/sdcard/haos-0.dat";
		MyAccessibilityNodeInfo originalTree = loadTreeFromFile(fn, map.get(fn));

		MergeNodeInfo hint = toMergeList.get(0);
		if (hint.involveIndex()) {
			mergeTreeOnIndex(originalTree, toMergeList, map);
		} else if (hint.involveCoordinate()) {
			mergeTreeOnCoordinate(originalTree, toMergeList, map);
		}

		// Now the tree should have all merged information
		MyAccessibilityNodeInfoSuite.dumpTree(originalTree, 0);

		return originalTree;
	}

	private void mergeTreeOnCoordinate(MyAccessibilityNodeInfo originalTree, List<MergeNodeInfo> toMergeList, Map<String, Integer> map) {
		List<MyAccessibilityNodeInfo> allMergeRootNodes = new ArrayList<MyAccessibilityNodeInfo>();
		MyAccessibilityNodeInfo originalMergeRootNode = MyAccessibilityNodeInfoSuite.findNode(originalTree, toMergeList.get(0).source);
		MyAccessibilityNodeInfoSuite.markLeafNodeForMerging(originalMergeRootNode);

		Util.log("hahaha");

		// 1. Mark all leaf nodes for merging.
		for (int i = 0; i < toMergeList.size(); i++) {
			String fn = "/sdcard/haos-" + (i + 1) + ".dat";
			MyAccessibilityNodeInfo toMergeRoot = loadTreeFromFile(fn, map.get(fn));
			MergeNodeInfo hint = toMergeList.get(i);
			MyAccessibilityNodeInfo mergeRootNode = MyAccessibilityNodeInfoSuite.findNode(toMergeRoot, hint.source);
			MyAccessibilityNodeInfoSuite.markLeafNodeForMerging(mergeRootNode);
			allMergeRootNodes.add(mergeRootNode);
		}

		// 2. Start from leaf nodes and merge level by level.
		int gNumClusters = 0;
		Map<Integer, Rect> boundMap = new Hashtable<Integer, Rect>();

		while (true) {
			List<MyAccessibilityNodeInfo> candidateWidgets = new ArrayList<MyAccessibilityNodeInfo>();

			// 2.1 Find leaf nodes in each scrolled view.

			// First view.
			List<MyAccessibilityNodeInfo> tmpList = MyAccessibilityNodeInfoSuite.findMergeNodes(originalMergeRootNode);
			for (Iterator<MyAccessibilityNodeInfo> iter = tmpList.iterator(); iter.hasNext();) {
				MyAccessibilityNodeInfo leafNode = iter.next();

				Rect b0 = new Rect();
				leafNode.getOriginal().getBoundsInScreen(b0);
				leafNode.setScrollBound(b0);
				candidateWidgets.add(leafNode);
			}

			// Other scrolled view.
			for (int i = 0; i < allMergeRootNodes.size(); i++) {
				MyAccessibilityNodeInfo mergeRootNode = allMergeRootNodes.get(i);
				MergeNodeInfo hint = toMergeList.get(i);

				tmpList = MyAccessibilityNodeInfoSuite.findMergeNodes(mergeRootNode);

				Rect b0 = new Rect();
				mergeRootNode.getOriginal().getBoundsInScreen(b0);

				for (Iterator<MyAccessibilityNodeInfo> iter = tmpList.iterator(); iter.hasNext();) {
					MyAccessibilityNodeInfo leafNode = iter.next();

					Rect b1 = new Rect();
					leafNode.getOriginal().getBoundsInScreen(b1);

					int delX = b1.left - b0.left;
					int delY = b1.top - b0.top;

					int left = hint.scrollX + delX;
					int right = left + b1.width();
					int top = hint.scrollY + delY;
					int bottom = top + b1.height();

					Rect b3 = new Rect();
					b3.set(left, top, right, bottom);

					leafNode.setScrollBound(b3);
					candidateWidgets.add(leafNode);
				}
			}

			Util.log("Found " + candidateWidgets.size() + " partial widgets");
			for (int i = 0; i < candidateWidgets.size(); i++) {
				Util.log(i + " " + candidateWidgets.get(i).dumpShort());
			}

			if (candidateWidgets.isEmpty()) { // We are done.
				break;
			}

			// 2.2 Merge leaf nodes
			int N = candidateWidgets.size();
			BitSet[] bSets = new BitSet[N];
			for (int i = 0; i < N; i++) {
				bSets[i] = new BitSet(N);
			}

			// 2.2.1 Collect intersection info
			for (int i = 0; i < N; i++) {
				MyAccessibilityNodeInfo nodeA = candidateWidgets.get(i);
				Rect ba = nodeA.getScrollBound();

				for (int j = i + 1; j < N; j++) {
					MyAccessibilityNodeInfo nodeB = candidateWidgets.get(j);
					Rect bb = nodeB.getScrollBound();

					if (Rect.intersects(ba, bb) && !ba.contains(bb) && !bb.contains(ba)) {
						bSets[i].set(j);
						bSets[j].set(i);
					}
				}
			}

			// 2.2.2 Run BFS for reach-ability analysis (connected components)
			List<BitSet> clusters = new ArrayList<BitSet>();
			BitSet visited = new BitSet(N);
			int start = 0;

			do {
				BitSet cluster = new BitSet(N);
				Queue<Integer> Q = new LinkedList<Integer>();
				Q.add(start);

				while (!Q.isEmpty()) {
					int node = Q.remove();
					visited.set(node);
					cluster.set(node);

					for (int i = 0; i < N; i++) {
						if (bSets[node].get(i) && !visited.get(i)) {
							Q.add(i);
						}
					}
				}

				clusters.add(cluster);

				start = -1;
				for (int i = 0; i < N; i++) {
					if (!visited.get(i)) {
						start = i;
						break;
					}
				}
			} while (start >= 0);

			Util.log("Found " + clusters.size() + " clustered widgets");

			// 2.2.3 Merge partial widgets in each cluster
			for (int i = 0; i < clusters.size(); i++) {
				BitSet cluster = clusters.get(i);

				Rect fullBound = new Rect();
				int minLeft, minTop, maxRight, maxBottom;
				minLeft = minTop = Integer.MAX_VALUE;
				maxRight = maxBottom = Integer.MIN_VALUE;

				for (int j = 0; j < N; j++) {
					if (cluster.get(j)) {
						MyAccessibilityNodeInfo partialWidget = candidateWidgets.get(j);
						partialWidget.setClusterId(gNumClusters);
						partialWidget.setToMerge(false);

						MyAccessibilityNodeInfo parent = partialWidget.getParent();
						if (parent != null) {
							parent.setToMerge(true);
						}

						Rect b = partialWidget.getScrollBound();

						minLeft = Math.min(minLeft, b.left);
						minTop = Math.min(minTop, b.top);
						maxRight = Math.max(maxRight, b.right);
						maxBottom = Math.max(maxBottom, b.bottom);
					}
				}

				// This is the actual bound for the widget that spans several scrolls.
				fullBound.set(minLeft, minTop, maxRight, maxBottom);
				boundMap.put(gNumClusters, fullBound);

				Util.log("Cluster " + gNumClusters + " done.");
				gNumClusters++;
			}
		} // end while

		// 3. Reconstruct merged tree based on nodes with cluster id
		Map<Integer, MyAccessibilityNodeInfo> nodeList = new Hashtable<Integer, MyAccessibilityNodeInfo>();
		allMergeRootNodes.add(0, originalMergeRootNode);

		// 3.1 Handle all nodes except the root of merging place
		if (gNumClusters == 1) {
			Rect bound = boundMap.get(0);
			MyAccessibilityNodeInfo oldNode = MyAccessibilityNodeInfoSuite.findNodeWithClusterId(allMergeRootNodes, 0);
			MyAccessibilityNodeInfo newNode = new MyAccessibilityNodeInfo(oldNode.getOriginal());
			newNode.setScrollBound(bound);
			nodeList.put(0, newNode);
		} else {

			for (int i = 0; i < gNumClusters - 1; i++) {
				Rect bound = boundMap.get(i);
				MyAccessibilityNodeInfo oldNode = MyAccessibilityNodeInfoSuite.findNodeWithClusterId(allMergeRootNodes, i);
				MyAccessibilityNodeInfo oldParentNode = oldNode.getParent();

				MyAccessibilityNodeInfo newNode;
				if (!nodeList.containsKey(i)) {
					newNode = new MyAccessibilityNodeInfo(oldNode.getOriginal());
					newNode.setScrollBound(bound);
					nodeList.put(i, newNode);
				} else {
					newNode = nodeList.get(i);
				}

				int pClusterId = oldParentNode.getClusterId();

				if (!nodeList.containsKey(pClusterId)) {
					MyAccessibilityNodeInfo newParentNode = new MyAccessibilityNodeInfo(oldParentNode.getOriginal());
					bound = boundMap.get(pClusterId);

					newParentNode.setScrollBound(bound);
					newParentNode.addChild(newNode);

					nodeList.put(pClusterId, newParentNode);
				} else {
					nodeList.get(pClusterId).addChild(newNode);
				}
			}
		}

		// 3.2 Decide which cluster needs scrolling to reach
		// 3.2.1 By default, set everyone to true
		for (int i = 0; i < nodeList.size(); i++) {
			nodeList.get(i).setNeedScroll(true);
		}

		// 3.2.2 Now find out which does not need
		Queue<MyAccessibilityNodeInfo> Q = new LinkedList<MyAccessibilityNodeInfo>();
		Q.add(originalMergeRootNode);

		while (!Q.isEmpty()) {
			MyAccessibilityNodeInfo node = Q.poll();

			int clusterId = node.getClusterId();
			if (clusterId >= 0) {
				nodeList.get(clusterId).setNeedScroll(false);
			}

			for (int i = 0; i < node.getChildCount(); i++) {
				MyAccessibilityNodeInfo child = node.getChild(i);
				if (child != null && child.getOriginal().isVisibleToUser()) {
					Q.add(child);
				}
			}
		}

		// 3.3 Now it's time to replace the original root
		MyAccessibilityNodeInfo newSubTree = nodeList.get(gNumClusters - 1);
		MyAccessibilityNodeInfo rootParent = originalMergeRootNode.getParent();

		MyAccessibilityNodeInfoSuite.dumpTree(newSubTree, 0);

		if (rootParent != null) {
			boolean res = rootParent.removeChild(originalMergeRootNode);
			Util.log("Removed child: " + res);
			rootParent.addChild(newSubTree);
		} else {
			originalTree = newSubTree;
		}
	}

	private void mergeTreeOnIndex(MyAccessibilityNodeInfo originalTree, List<MergeNodeInfo> toMergeList, Map<String, Integer> map) {
		for (int i = 0; i < toMergeList.size(); i++) {
			String fn = "/sdcard/haos-" + (i + 1) + ".dat";
			MyAccessibilityNodeInfo toMergeRoot = loadTreeFromFile(fn, map.get(fn));
			MergeNodeInfo hint = toMergeList.get(i);
			MyAccessibilityNodeInfo found1 = MyAccessibilityNodeInfoSuite.findNode(originalTree, hint.source);
			MyAccessibilityNodeInfo found2 = MyAccessibilityNodeInfoSuite.findNode(toMergeRoot, hint.source);

			int fromIdx = Math.max(found1.getChildCount(), hint.fromIndex);
			for (int j = fromIdx; j <= hint.toIndex; j++) {
				MyAccessibilityNodeInfo child = found2.getChild(j - hint.fromIndex);

				child.setNeedScrollRecursive(true);
				found1.addChild(child);
			}
		}
	}

	// Get current UIState that represents the screen
	// TODO: add support for ListView and ScrollView
	private RYState getCurrentUIState(String packageName) {
		root = getRootNode();

		Hashtable<String, Integer> map = new Hashtable<String, Integer>();
		// computeFeatureVector(map, root, 0);

		Hashtable<String, Integer> dict = new Hashtable<String, Integer>();
		computeFeatureVectorExactMatch(dict, map, root, 0);

		List<AccessibilityNodeInfo> clickables = getClickables(root);
		RYState state = new RYState(map, clickables, packageName, mClickingPolicy);
		return state;
	}

	// This is an estimate of original ImageView size
	private int get_imageview_size(String fileName, Rect bounds, String dbgFileName) {
		Bitmap original = BitmapFactory.decodeFile(fileName);
		Bitmap cropped = Bitmap.createBitmap(original, bounds.left, bounds.top, bounds.width(), bounds.height());
		int size = cropped.getByteCount();

		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(dbgFileName));
			if (bos != null) {
				cropped.compress(Bitmap.CompressFormat.PNG, 90, bos);
				bos.flush();
			}
		} catch (IOException ioe) {
			Util.log("Failed to save screen shot to file");
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException ioe) {
					/* ignore */
				}
			}
			cropped.recycle();
		}

		return size;
	}

	// Compute feature vectors with count only
	private void computeFeatureVector(Hashtable<String, Integer> map, AccessibilityNodeInfo node, int level) {
		if (node == null || node.getClassName() == null) {
			Util.err("node or class name is NULL");
			return;
		}

		String type = node.getClassName().toString();
		String key = type + "@" + level;

		int count = map.containsKey(key) ? map.get(key) : 0;
		count++;

		map.put(key, count);

		int child_cnt = node.getChildCount();
		if (child_cnt > 0) {
			// Util.log("Branch: " + key + ", " + count);
			for (int i = 0; i < child_cnt; i++) {
				AccessibilityNodeInfo child = node.getChild(i);
				computeFeatureVector(map, child, level + 1);
			}
		} else {
			// Util.log("Leaf: " + key + ", " + count);
		}
	}

	private String getTextDigest(CharSequence text) {
		MessageDigest m = null;

		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			/* ignore*/
		}

		if (m == null) {
			return "";
		}

		String plaintext = "";

		if (text != null) {
			plaintext = text.toString();
		}

		m.reset();
		m.update(plaintext.getBytes());
		byte[] digest = m.digest();
		BigInteger bigInt = new BigInteger(1, digest);
		String hashtext = bigInt.toString(16);

		// Now we need to zero pad it if you actually want the full 32 chars.
		while (hashtext.length() < 32) {
			hashtext = "0" + hashtext;
		}

		return HashIdDictionary.add(hashtext);
	}

	// Compute feature vectors with exact match on text
	private void computeFeatureVectorExactMatch(Hashtable<String, Integer> dict, Hashtable<String, Integer> map, AccessibilityNodeInfo node, int level) {
		if (node == null || node.getClassName() == null) {
			Util.err("node or class name is NULL");
			return;
		}

		String type = node.getClassName().toString();
		String key = type + "@" + level;

		int count = dict.containsKey(key) ? dict.get(key) : 0;
		count++;

		dict.put(key, count);

		String keyExactMatch = type + "@" + level + "@" + count + "@" + getTextDigest(node.getText());
		map.put(keyExactMatch, 1);

		int child_cnt = node.getChildCount();
		if (child_cnt > 0) {
			// Util.log("Branch: " + key + ", " + count);
			for (int i = 0; i < child_cnt; i++) {
				AccessibilityNodeInfo child = node.getChild(i);
				computeFeatureVectorExactMatch(dict, map, child, level + 1);
			}
		} else {
			// Util.log("Leaf: " + key + ", " + count);
		}
	}

	// OLD: Compute feature vectors with both count and size info
	private void compute_feature_vector(Hashtable<String, FeatureValuePair> map, AccessibilityNodeInfo node, int level) {
		int child_cnt = node.getChildCount();

		if (child_cnt > 0) {
			for (int i = 0; i < child_cnt; i++) {
				AccessibilityNodeInfo child = node.getChild(i);
				compute_feature_vector(map, child, level + 1);
			}
		} else {
			String key = node.getClassName().toString() + "@" + level;
			FeatureValuePair value;
			int size = 0;

			CharSequence text = node.getText();
			if (text != null) {
				size = text.toString().length();
			}

			if (node.getClassName().toString().equals(ImageView.class.getCanonicalName())) {
				Rect bounds = new Rect();
				node.getBoundsInScreen(bounds);
				String timestamp = new SimpleDateFormat("MM-dd'T'HH-mm-ss-SSS").format(new Date());
				size = get_imageview_size("/data/local/tmp/local/tmp/haos.png", bounds, "/data/local/tmp/local/tmp/" + key + "_" + timestamp + ".png");
			} else {
				// Util.log(node.getClassName() + " NOT " + ImageView.class.getCanonicalName());
			}

			value = map.containsKey(key) ? map.get(key) : new FeatureValuePair(0, 0);
			value.count++;
			value.size += size;
			map.put(key, value);

			// Util.log("Leaf: " + key + ", " + size);
		}
	}

	// Start the target app
	private void start_target_app(String appName) throws UiObjectNotFoundException {
		// 0. Start fromIndex HOME
		dev.pressHome();

		// 1. Find and click "Apps" button
		//NO Need to use that
//		UiObject allAppsButton = new UiObject(new UiSelector().description("Apps"));
//
//		allAppsButton.clickAndWaitForNewWindow();

		// 2. Find and click “Apps” tab
		UiObject appsTab = new UiObject(new UiSelector().description("Apps"));
		if (launcherPackName == null) {
			launcherPackName = appsTab.getPackageName(); // remember the launcher package
		}
		appsTab.click();

		// 3. Select scrollable "container view" 
		UiScrollable appViews = new UiScrollable(new UiSelector().scrollable(true));
		// Set the swiping mode to horizontal (the default is vertical)
		appViews.setAsHorizontalList();

		// 4. This API does not work properly in 4.2.2 
		appViews.scrollTextIntoView(appName);

		// 5. Click target app
		UiObject targetApp = appViews.getChildByText(new UiSelector().className(android.widget.TextView.class.getName()), appName, true);

		boolean done = targetApp.clickAndWaitForNewWindow();

		waitForNetworkUpdate();
		// AccessibilityEventProcessor.waitForLastEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, WINDOW_CONTENT_UPDATE_TIMEOUT);
	}

	// Find the first webview node in subtree rooted at "node"
	private AccessibilityNodeInfo get_webview(AccessibilityNodeInfo node) {
		Util.log(node.getClassName());

		if (WebView.class.getCanonicalName().equals(node.getClassName())) {
			return node;
		} else {
			int child_cnt = node.getChildCount();

			if (child_cnt > 0) {
				for (int i = 0; i < child_cnt; i++) {
					AccessibilityNodeInfo tmp = get_webview(node.getChild(i));
					if (tmp != null) {
						return tmp;
					}
				}
			}
			return null;
		}
	}

	private boolean clickAndWaitForNewWindow(MyInteractionController iController, AccessibilityNodeInfo node) {
		Rect rect = getVisibleBounds(node);

		long old_val = mConfig.getActionAcknowledgmentTimeout();
		mConfig.setActionAcknowledgmentTimeout(10000);
		Util.log("before clickAndWaitForNewWindow");
		boolean done = iController.clickAndWaitForNewWindow(rect.centerX(), rect.centerY(), mConfig.getActionAcknowledgmentTimeout()); // 10s
		mConfig.setActionAcknowledgmentTimeout(old_val);

		// Util.log("after clickAndWaitForNewWindow: " + done);
		return done;
	}

	private boolean click(MyInteractionController iController, AccessibilityNodeInfo node) {
		Rect rect = getVisibleBounds(node);
		boolean result = iController.clickAndSync(rect.centerX(), rect.centerY(), mConfig.getActionAcknowledgmentTimeout());
		// Util.log("clickAndSync: " + result);
		waitForNetworkUpdate();
		// AccessibilityEventProcessor.waitForLastEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, WINDOW_CONTENT_UPDATE_TIMEOUT);
		return result;
	}

	/**
	* Finds the visible bounds of a partially visible UI element
	*
	* @param node
	* @return null if node is null, else a Rect containing visible bounds
	*/
	private Rect getVisibleBounds(AccessibilityNodeInfo node) {
		if (node == null) {
			return null;
		}

		// targeted node's bounds
		int w = MyUiDevice.getInstance().getDisplayWidth();
		int h = MyUiDevice.getInstance().getDisplayHeight();
		Rect nodeRect = MyAccessibilityNodeInfoHelper.getVisibleBoundsInScreen(node, w, h);

		// is the targeted node within a scrollable container?
		AccessibilityNodeInfo scrollableParentNode = getScrollableParent(node);
		if (scrollableParentNode == null) {
			// nothing to adjust for so return the node's Rect as is
			return nodeRect;
		}

		// Scrollable parent's visible bounds
		Rect parentRect = MyAccessibilityNodeInfoHelper.getVisibleBoundsInScreen(scrollableParentNode, w, h);
		// adjust for partial clipping of targeted by parent node if required
		nodeRect.intersect(parentRect);
		return nodeRect;
	}

	/**
	* Walk the hierarchy up to find a scrollable parent. A scrollable parent
	* indicates that this node may be in a content where it is partially
	* visible due to scrolling. its clickable center maybe invisible and
	* adjustments should be made to the click coordinates.
	*
	* @param node
	* @return The accessibility node info.
	*/
	private AccessibilityNodeInfo getScrollableParent(AccessibilityNodeInfo node) {
		AccessibilityNodeInfo parent = node;
		while (parent != null) {
			parent = parent.getParent();
			if (parent != null && parent.isScrollable()) {
				return parent;
			}
		}
		return null;
	}

	// Print all nodes rooted at "node"
	private void traverse_node_recursive(AccessibilityNodeInfo node, int level) {
		if (node == null) {
			return;
		}

		int child_cnt = node.getChildCount();
		StringBuilder sb = new StringBuilder();

		if (child_cnt > 0) {
			for (int i = 0; i < child_cnt; i++) {
				traverse_node_recursive(node.getChild(i), level + 1);
			}
		} else {
			Rect bounds = new Rect();
			node.getBoundsInScreen(bounds);

			for (int i = 0; i < level; i++) {
				sb.append(" ");
			}

			sb.append(node.getText());
			sb.append(bounds.toShortString());

			Util.log(sb);
		}
	}

	// Find all leaf nodes (guarantee not NULL) 
	private List<AccessibilityNodeInfo> get_leaf_nodes(AccessibilityNodeInfo root) {
		List<AccessibilityNodeInfo> ret = new ArrayList<AccessibilityNodeInfo>();
		Queue<AccessibilityNodeInfo> Q = new LinkedList<AccessibilityNodeInfo>();
		Q.add(root);

		while (!Q.isEmpty()) {
			AccessibilityNodeInfo node = Q.poll();

			if (node == null) {
				Util.log("Processing NULL");
				continue;
			}

			int childCnt = node.getChildCount();
			if (childCnt > 0) {
				for (int i = 0; i < childCnt; i++) {
					AccessibilityNodeInfo child = node.getChild(i);
					Q.add(child); // no need to check NULL, checked above
				}
			} else {
				ret.add(node);
			}
		}
		return ret;
	}

	// Check whether given node is clickable or not (according to our assumption)
	private boolean matchNode(AccessibilityNodeInfo node) {
		String clsName = node.getClassName().toString();
		Class EDITTEXT, B, WEBVIEW;
		boolean matchedEditText = false;
		boolean matchedWebView = false;

//		try {
//			B = Class.forName(clsName, false, this.getClass().getClassLoader());
//
//			EDITTEXT = Class.forName(EditText.class.getCanonicalName(), false, this.getClass().getClassLoader());
//			matchedEditText = EDITTEXT.isAssignableFrom(B);
//
//			WEBVIEW = Class.forName(WebView.class.getCanonicalName(), false, this.getClass().getClassLoader());
//			matchedWebView = WEBVIEW.isAssignableFrom(B);
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}

		return node.isClickable() && node.isEnabled() && node.isVisibleToUser() && !node.isCheckable();
	}

	// Find list of clickable nodes in the subtree rooted at "source"
	private List<AccessibilityNodeInfo> getClickables(AccessibilityNodeInfo root) {
		if (root == null) {
			Util.err("FATAL: getClickables() source is NULL");
		}

		List<AccessibilityNodeInfo> ret = new ArrayList<AccessibilityNodeInfo>();
		List<AccessibilityNodeInfo> leaves = get_leaf_nodes(root);

		for (int i = 0; i < leaves.size(); i++) {
			AccessibilityNodeInfo leaf = leaves.get(i);
			AccessibilityNodeInfo node = leaf;
			boolean matched = matchNode(node);

			while (!matched) {
				node = node.getParent();

				if (node != null) {
					// Util.log("UP " + node.getClassName() + ", " + node.getText());
					matched = matchNode(node);
				} else {
					break;
				}
			}

			if (matched && !ret.contains(node)) {
				// Util.log("FOUND " + node.getClassName() + ", " + node.getText());
				ret.add(node);
			}
		}

		return ret;
	}
}