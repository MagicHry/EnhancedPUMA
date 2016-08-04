# EnhancedPUMA
Built on top of PUMA, available for testing several applications at one shot, provide event handlers for the user to define the exploration logic.
## Usage
To luanch Enhanced PUMA, you also need to follow the instructions defined in PUMA, which are:

* First of all, environment variables like ```JAVA_HOME``` and ```ANDROID_HOME``` should be set up properly.
* replace ```app.info``` and to fill in your target applications, this point is different from the origin PUMA, cause I made it available for analyzing several applications at one shot
* ```./setup-phone.sh``` // this one will set up the connection between the cellphone/monitor
* ```./run.sh``` // start monkey execution

## app.info
You need to put in all the target applications package name as well as their app name, and with the format **packagename@application name**. If you want to analyze serveral applications, then just type in the above infomation in a **line by line** manner.

## Implement the user handler
Based on the reason that PUMA script handler will not work, I personally implement 8 user event handlers for the user to define their own exploration logic, the handler is defined in ```hku.cs.reilly.launcher```, and for a user, you need to implement this interface and implement the corresponding handler to launch the Enhanced PUMA correctly. I will give a brief intro about these handlers, and for further introduction, you guys can take a look at my pre-defined example defined in ```hku.cs.reilly.handler```.

### getAppExecutionSequence()
The execution sequence for all the ready-to-test apps, if you want to use the default sequence defined in the ```app.info```, then just `return null`.

### onNewAppLaunched()
What to do when the application is launched first time, currently, the user can have 3 options, `HOME`,`BACK` and `CONTINUE`.

### onReachingDiffApp()
What to do when the tool encounter a third party application, the user can have 3 options, `HOME`,`BACK` and `CONTINUE`.

### getClickingPolicy()
What kind of widget the tool should consider to click (e.g., Button, ImageView), `return null` to let the Enhanced PUMA adapt the default way, which is click every clickable items.

### onStateEquivalent()
Allows the user to define whether 2 states are equal or not.

### getNextClickItem()
Allows the user to define which widget shall be clicked

### explorationDone()
When is the time to stop the exploration, noted that, the Enhanced PUMA has a internal tester to see whether the exploration shall be done or not, I only provide this external tester for the user based on the security issues.

### onResultAnalysis()
When the exploration is done, allows the user to analyze the result, and output the result in any kind of format based on user definition. Take a look at my json format example in ```hku.cs.reilly.launcher.ResultAnalysisUtils```.
