# PoCo: Policy Composition

PoCo is an open source library for evaluating and refining our proposed Policy Specification Language (PSL) - [PoCo](http://www.cse.usf.edu/~ligatti/projects/poco/). Implemented in Java and packaged as a Java library, this project provides the basic components for composing and enforcing atomic-security policies.


## Installing

This project uses [Maven](http://maven.apache.org/). To use the PoCo API, one can download the [poco.jar](https://github.com/caoyan66/PoCo-PolicyComposition/blob/master/PoCo.jar) file and then include it in the project's library sources. One simple way to include *poco.jar* is to add PoCo dependency to the project's *POM* file (as shown in below).

````xml
  <dependency>
      <groupId>edu.cseusf</groupId>
      <artifactId>poco</artifactId>
      <version>1.0</version>
      <scope>system</scope>
      <systemPath>${project.basedir}/lib/poco.jar</systemPath>
  </dependency>
````
PoCo project uses the following APIs: Apache commons IO (version 2.5), ASM 5 (version 5.0.1), Apache Commons BCEL 6.0, and Compiler Tree API(version 1.8.0). Thus, one will need to add dependencies for these APIs in the project. For simplicity, one can directly copy the dependencies below into the project's pom.xml file (one will need to update the java lib directory path for tools.jar dependency).
````xml
  <dependency>
  	<groupId>commons-io</groupId>
	<artifactId>commons-io</artifactId>
	<version>2.5</version>
  </dependency>
  
  <dependency>
  	<groupId>org.apache.bcel</groupId>
	<artifactId>bcel</artifactId>
	<version>6.0</version>
  </dependency>
  
  <dependency>
  	<groupId>org.kohsuke</groupId>
	<artifactId>asm5</artifactId>
	<version>5.0.1</version>
  </dependency>  
  
  <dependency>
  	<groupId>com.sun</groupId>
	<artifactId>tools</artifactId>
	<version>1.8.0</version>
	<scope>system</scope>
	<systemPath>${your java lib path}/tools.jar</systemPath>
  </dependency>
````

Once done configuring the project dependency, one can start to specify PoCo policies.

## Specification of PoCo policies.
In order to specify desired seucirty policies, one may needs to specify a set of policy files, a vote combinator, an obligation scheduler, and a set of abstract actions. 

### PoCo-Policy specification
PoCo allows policy writers to specify runtime policies on untrusted Java bytecode programs via a Java-like language and the file extension of a PoCo policy is __*'.pol'*__.
In PoCo, each policy contains three components: **_onTrigger_**, **_onObligation_**, and **_vote_**. Among them, **_onTrigger_** defines obligations in response to trigger events; **_onObligation_** defines obligations in response to other obligations; and, **_vote_** enables policies to indicate approval/disapproval of obligations.  

To give an example, let's consider a policy *DisSyscCalls* which prevents the target application from executing system-level calls.
1. To prevent system-level calls from been executed, the policy's **_onTrigger_** method should output an empty result every time it detects such attempt. To do so, one needs to make sure the trigger event will be an action type (by calling isAction method) and its signature is `java.lang.Runtime.exec(*)`(by calling the matches method).
	
	````java
	public void onTrigger(Event e){
		if( e.isAction() && e.matches(new Action("java.lang.Runtime.exec(*)")) ) 
			setOutput( new Result(e, null) );
	} 
	````
	
2. To avoid this policy from being violated by the obligations of other policies, the policy's **_vote_** function should yield deny to any obligation that may attempt to execute system-level calls, either by directly including system-level calls or by setting a system-level call as the obligation's output event.

	````java
	public boolean vote(CFG cfg) {  return !cfg.contains(sysCall) && !cfg.outputSets(sysCall); }
	````

3. This policy does not need to react to other policies' obligations, therefore, **_onObligation_** method can be omitted. Hence, P_dis policy is specified in P_dis.pol file as follows.

	````java
	//DisSysCalls.pol
	public class DisSysCalls extends Policy {
		public void onTrigger(Event e){
			if( e.isAction() && e.matches(new Action("java.lang.Runtime.exec(*)")) ) 
				setOutput( new Result(e, null) );
		}
		public boolean vote(CFG cfg) {  
			return !cfg.contains(sysCall) && !cfg.outputSets(sysCall); 
		}
	}
	````

4. From the above example, it is easy to see that an empty policy that allows all events to execute without restriction can be defined as follows.

	````java
	//P_trivial.pol
	public class P_trivial extends Policy{
    	public void    onTrigger(Event e)   { }
   	 	public boolean vote     (CFG cfg)   { return true; }
    	public void    onOblig  (Rtrace rt) { }
	}
	````

### Vote Combinator specification
**_Vote Combinator (VC)_** unites the votes of all policies into a single decision to permit or deny an obligation. By default, PoCo uses a **conjunction** combinator that requires all policies to permit an obligation for it to be executed. Of course, one can customize a vote combinator if different logic is desired. To do so, one can create a file with the *'.vc'* extension. In this file, one extends the abstract class **VC** by implementing its *'evaluate'* method; this method takes a single argument of an-ArrayList-of-boolean type and returns a single boolean value. For instance, a Disjunction VC that allows an obligation to execute as long as one policy allows it can be specified as below:

````java
	//Disjunction.vc
	import java.util.ArrayList;
	public class Disjunction extends VC{
		public boolean evaluate(ArrayList<Boolean> votes) {
			for(Boolean vote: votes) 
				if(vote) return true; 
			return false;
		}
	}
````

### Obligation Scheduler specification 
**_Obligation scheduler (OS)_** orders policies for collecting votes and executing obligations. By default, PoCo uses *_OrderAsListed_* scheduler (specified as below) that preserves the original order of input policies. In addition, one can customize a vote combinator if different logic is desired. To specify a customized *OS*, one needs to create a file (with the `.os` extension) that extends the abstract class **OS** by implementing its `prioritize` method. This method takes a single argument of an ArrayList-of-CFG type and returns a value with the same type. 

````java
	//OrderAsListed.os
	import java.util.ArrayList;
	public class OrderAsListed extends OS{
		public ArrayList<CFG> prioritize(ArrayList<CFG> obs) {
			return obs;
		}
	}
````

### Abstract action specification
In practice, there can be more than one method that relates to a security concern, and it can be cumbersome to enumerate all these methods in more than one policy. For instance, Java provides several methods to write to file (e.g., *java.io.File.createNewFile()*, *java.io.FileOutputStream.<init>(java.io.File)*, etc.). If one wants to specify a few policies concerning file-write actions, listing all those methods in each policy file would be burdensome. To alleviate this burden, PoCo enables users to group a set of related actions together into a single abstract action.
	
To specify an abstract-action class, one can create a file (with the *'action'* extension) that extends the abstract class - **AbsAction** by implementing its *'mapConc2Abs'* method; this method takes a single argument of *Action* type and returns a single boolean value. For instance, the abstract action *__FileWrite__* below groups six Java file-write methods. 

````java
public class FileWrite extends AbsAction {
	@Override
	public boolean mapConc2Abs(Action conc) { 
		if ( conc.matches(new Action("java.io.File.createNewFile()")) ||
		     conc.matches(new Action("java.io.File.createTempFile(java.lang.String,java.lang.String,..)")) ||
		     conc.matches(new Action("java.io.File.renameTo(java.io.File)")) ||
		     conc.matches(new Action("java.io.FileOutputStream.<init>(java.io.File)")) ||
		     conc.matches(new Action("java.io.FileOutputStream.<init>(java.lang.String,..)")) ||
		     conc.matches(new Action("java.io.RandomAccessFile.<init>(java.lang.String,java.lang.String)"))) {
			return true;
		}       
		else
			return false;
	}
}
````
Then, by matching trigger actions with *__FileWrite__*, one is able to specify policies that concerning the six file-write methods list above (as shown in below).
````java
	public void onTrigger(Event e){
		if( e.isAction() && e.matches(new FileWrite()) ) {
		... ...
		
````

## Enforce PoCo policies

Once done specifying the PoCo files discussed above, one needs to import them into the PoCo System to generate corresponding files for policy enforcement.

### Import policies
In order to correctly import all the files, one first needs to place all the policies files (*`.pol`* files), a vote-combinator-specification file (optional), and an obligation-scheduler-specification file (optional) into a single folder. To include abstract-action files (optional), one needs to create a sub-folder named *`absactions`* to include these files. For instance, the picture below shows that one has included two policy files (*P_trivial.pol* and *DisSysCalls.pol*), one vote-combinator-specification file (*Disjunction.vc*), an obligation-scheduler-specification file (*OrderAsListed.os*), and two abstract-action-specification files (*FileOpen.action* and *FileWrite.action*) to import.

<p align="center">
  <img src="https://github.com/caoyan66/PoCo-PolicyComposition/blob/master/pic/folder.png" width="600"/>
</p>

To import the correctly-placed files, one calls the static method **_importPolicies_** of *ImportPolicy* class. As shown in the code below, the method converts files by taking an array of strings as an argument. Specifically, the first element of the array depicts the absolute path of all the specified PoCo files; the second element depicts the desired output path for converted PoCo files; the third element is used to set desired package info to converted code; and last, the fourth element is used to depict the output path for converted PoCo files.
  
 ````java
	/**
	 * The ImportPolicy program converts the candidate policy files into
	 * policy-enforcement code in Java.
	 * 
	 * @param args { args[0]:the absolute path of all the specified PoCo files;
	 *               args[1]:the desired output path for converted PoCo files;
	 *               args[2]:the desired package name; 
	 *               args[3]:the desired output path for converted poco files}
	 */
	String[] policiesArgs = new String[4];
	policiesArgs[0] = Paths.get("").toAbsolutePath().toString() + "/pols/"; 
	policiesArgs[1] = Paths.get("").toAbsolutePath().toString() + "/policyFiles";
	policiesArgs[2] = "com.poco.test.policies";
	ImportPolicy.importPolicies(policiesArgs); 
	
 ````
    
### Generated policy files 
By taking a set of PoCo files as input, PoCo compiler 1) reconstructs the __*.pol*__ files into Java (.java) files; 2) generates an AspectJ (.aj) file including security-relevant methods monitored by the policies as the pointcut set; and, 3) generates a Java file that can be executed to enforce candidate policies onto the target application; 4) generates a *'abs2ConcreteAct.java'* which contains information to map from *abstract actions* to *concrete actions*; 5) may also generate a Java file that specifies a Vote Combinator if one included a __*.vc*__ file ; 6) may also generate a Java file that specifies an Obligation Scheduler if one included a __*.os*__ file.

For example, by taking those files demonstated in the above example as input, PoCo compiler will output five files, which are DisSysCalls.java, P_trivial.java, PoCoPolicies.aj, PoCoDemo.java, and Abs2ConcreteAct.java(as shown in below).

<p align="center">
  <img src="https://github.com/caoyan66/PoCo-PolicyComposition/blob/master/pic/genFiles.png" width="200"/>
</p>

## Enforce policies
In order to properly enforce the policies, one may need to make some modification to these files once done copying the generated files to the desired folder. In specific,

1. One may need to modify package information for those Java files.

2. One may need to manually add policy arguments for PoCo policies that take arguments in **PoCoDemo.java** file,

3. One may manually modify the pointcuts of the AspectJ file to insert or remove pointcuts. 

After deploying all necessary code in a project, one then needs to set up the project configuration to specify a target application and weave the aspects into the target application at load-time. 
To specify a target application, one needs to specify the target appliction in the project's program arguments. In specific, the main method of the **PoCoDemo.java** file takes three arguments: the first one is the absolute path for the policy files; the second argument depicts the absolute path for the target application; and, the third one specifies the main class of the target application. Thus, to 
specify the target appliction, one needs to set the target application as the second argument of the main method, and the main class of the target application as the third arguement. 
For example, the configuration shown below depicts that *'Pooka.jar'* is the target application, and its main class of the target application is *'net.suberic.pooka.Pooka'*

<p align="center">
  <img src="https://github.com/caoyan66/PoCo-PolicyComposition/blob/master/pic/arguments.png" width="800"/>
</p>

In addition, to set Load-time weaving for AspectJ, one can simply add the Pooka.jar file to the Inpath property. For instance, the configuration below shows that aspects will be woven into *Pooka.jar* at its loading time.

<p align="center">
  <img src="https://github.com/caoyan66/PoCo-PolicyComposition/blob/master/pic/inpath.png" width="800"/>
</p>

Once done setting up the project, one then can enforce specified email policies on the target application __Pooka__ by running the project.

## A PoCo example 
To demonstrate PoCo system, we have specified a set of ten policies that constrains the behavior of email-client applications (e.g., [Pooka](http://www.suberic.net/pooka/)). 
To enforce these email policies on **Pooka**, one needs to first download the files under the [*poco_demo* folder](https://github.com/caoyan66/PoCo-PolicyComposition/tree/master/src/main/java/edu/cseusf/poco/poco_demo) as well as [Pooka.Jar file](https://github.com/caoyan66/PoCo-PolicyComposition/tree/master/performanceTest).
After downloading these files, one may need to modify all Java-source files' package information to the proper values before adding them to their project. Once done adding them, one needs to set up the project's __*Run Configurations*__. Specifically, 
1. one needs to set the *Main class* as __*edu.cseusf.poco.poco_demo.PoCoDemo*__ for the project;
2. one needs to set program arguments for this project. the main method takes three arguments: the first one is the absolute path for the policy files; the second argument depicts the absolute path for *Pooka.jar* file; and, the third one specifies the main class of the Pooka application (i.e., net.suberic.pooka.Pooka).
3. one needs to set Load-time weaving for AspectJ. 

Once done setting up the project, one can then enforce specified email policies on the target application __Pooka__ by running the project. For example, enforcing the *IncomingEmail* policy,  a warning message will be issued when receiving attachments on new mail from unknown addresses (as shown in below).

<p align="center">
  <img src="https://github.com/caoyan66/PoCo-PolicyComposition/blob/master/pic/policyEnforcement.png" width="800"/>
</p>


## Authors

* **Yan Albright** - *Initial work* - [YanAlbright](https://github.com/caoyan66) 

## License

This project is licensed under the [GNU General Public License v3.0](https://choosealicense.com/licenses/gpl-3.0/).

## Copyright

Copyright (c) 2017-2018 University of South Florida 
