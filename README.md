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
Based on the reason that PUMA script handler will not work, I personally implement 8 user event handlers for the user to define their own exploration logic, the handler is defined in ```hku.cs.reilly.launcher```, and for a user, you need to implement this interface and implement the corresponding handler to launch the Enhanced PUMA correctly.
