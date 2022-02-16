package eu.iv4xr.ux.pxtestingPipeline;

import static eu.iv4xr.framework.extensions.ltl.LTL.*;
import static eu.iv4xr.framework.extensions.ltl.LTL2Buchi.getBuchi;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.fbk.iv4xr.mbt.MBTProperties;
import eu.fbk.iv4xr.mbt.MBTProperties.LR_random_mode;
import eu.fbk.iv4xr.mbt.efsm.EFSM;
import eu.fbk.iv4xr.mbt.efsm.EFSMFactory;
import eu.fbk.iv4xr.mbt.efsm.EFSMState;
import eu.fbk.iv4xr.mbt.efsm.EFSMTransition;
import eu.fbk.iv4xr.mbt.efsm.labRecruits.LabRecruitsRandomEFSM;
import eu.fbk.iv4xr.mbt.efsm.labRecruits.LabRecruitsRandomEFSM.StateType;
import eu.fbk.iv4xr.mbt.efsm.modelcheckingInterface.InterfaceToIv4xrModelCheker;
import eu.fbk.iv4xr.mbt.efsm.modelcheckingInterface.InterfaceToIv4xrModelCheker.EFSMTransitionWrapper;
import eu.fbk.iv4xr.mbt.strategy.GenerationStrategy;
import eu.fbk.iv4xr.mbt.strategy.SearchBasedStrategy;
import eu.fbk.iv4xr.mbt.testcase.AbstractTestSequence;
import eu.fbk.iv4xr.mbt.testcase.MBTChromosome;
import eu.fbk.iv4xr.mbt.testsuite.SuiteChromosome;
import eu.fbk.iv4xr.mbt.utils.TestSerializationUtils;
import eu.iv4xr.framework.extensions.ltl.BasicModelChecker;
import eu.iv4xr.framework.extensions.ltl.Buchi;
import eu.iv4xr.framework.extensions.ltl.BuchiModelChecker;
import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.ITransition;
import eu.iv4xr.framework.extensions.ltl.LTL;
import eu.iv4xr.framework.extensions.ltl.BasicModelChecker.Path;
import eu.iv4xr.ux.pxtestingPipeline.Distance;
import eu.iv4xr.framework.extensions.ltl.*;
import nl.uu.cs.aplib.utils.Pair;
import eu.fbk.iv4xr.mbt.Main;
import static eu.iv4xr.framework.extensions.ltl.LTL.*;
import static org.junit.jupiter.api.Assertions.* ;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static eu.iv4xr.framework.extensions.ltl.LTL.*;
import static org.junit.jupiter.api.Assertions.* ;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import info.debatty.java.stringsimilarity.*;
import java.util.HashMap;

/**
 * @author sansari
 */
public class MCtest_Generation {

	// use a logger to save output execution information
	protected static final Logger logger = LoggerFactory.getLogger(Main.class);	
	protected List <EFSMState> goalstates = new ArrayList<EFSMState>();
	public void setPropertiesMBT() {
		
		MBTProperties.LR_generation_mode = LR_random_mode.N_BUTTONS_DEPENDENT;
		MBTProperties.SEARCH_BUDGET = 60;
		MBTProperties.SUT_EFSM = "labrecruits.random_extreme";
		// there are some predefined configuration to pass to MBTProperties.SUT_EFSM
		// "labrecruits.random_simple", "labrecruits.random_medium",
		// "labrecruits.random_large"

		
		MBTProperties.LR_mean_buttons = 0.5;
		MBTProperties.LR_n_buttons = 40;
		MBTProperties.LR_n_doors = 28;
		MBTProperties.LR_seed = 3257439;
		MBTProperties.LR_n_rooms=8;
		MBTProperties.LR_n_goalFlags = 2 ;
		
	}

	/**
	 * Check if LabRecruits level exists
	 */
	public boolean existsLabRecruitLevel() {
		String efsmString = EFSMFactory.getInstance().getEFSM().getEFSMString();
		boolean out = !efsmString.equalsIgnoreCase("");
		return out;
	}
	
	//run a test for test generation using MC developed by Wishnu
	//Saba: right now it is written for one single goal but later on can be upgraded 
	//since I already included list of goalstates and absTestsuite --->just path_ needs to be replaced by list of path.
	@Test
	public void runMCGenerationTest() {
	
		// set the parameters for the generation
		setPropertiesMBT();
		
		if (!existsLabRecruitLevel()) {
			fail();
		}
		EFSM efsm=EFSMFactory.getInstance().getEFSM();

		//get goal state	
		for(var state : efsm.getStates()) {
			EFSMState state_ = (EFSMState) state ;
			if(LabRecruitsRandomEFSM.getStateType(state_) == StateType.GoalFlag) {
				goalstates.add(state_) ;
			}
		} ;
		
		assertTrue(goalstates != null) ;
		//String goalid = goalstates.get(0).getId() ;
		String goalid = "gf0" ;
		Predicate<IExplorableState> goal = state -> {
			InterfaceToIv4xrModelCheker.EFSMStateWrapper state_ = (InterfaceToIv4xrModelCheker.EFSMStateWrapper) state ;
			return state_.conf.getState().getId().equals(goalid) ;
		} ;
		
	    // buchi model checking for full-- state-- coverage along with the goal coverage.
		//List<AbstractTestSequence> absTestsuite=StateCoverage(efsm,goal);
		
		// buchi model checking for full-- transition-- coverage along with the goal coverage.
		List<AbstractTestSequence> absTestsuite=TransitionCoverage(efsm,goal);
		
		
		
		// Measure Similarity btw test cases in a suite.
		List<AbstractTestSequence> absTestsuite_Subset= AdaptiveRandomSampling(absTestsuite, 33);
		List<AbstractTestSequence> absTestsuite_Rand= RandomSampling(absTestsuite, 33);

		Distance dis=new Distance("jaro-winkler");
		double jarodistance= dis.distance(absTestsuite_Rand);
		System.out.println("Original-testsuite size is: "+ absTestsuite_Rand.size());
		System.out.println(dis.mtr  + "  Distance: "+jarodistance);

		 jarodistance= dis.distance(absTestsuite_Subset);
		System.out.println("Sub-testsuite size is: "+ absTestsuite_Subset.size());
		System.out.println(dis.mtr  + "  Distance: "+jarodistance);
		 jarodistance= dis.distance(absTestsuite);
			System.out.println("Sub-testsuite size is: "+ absTestsuite.size());
			System.out.println(dis.mtr  + "  Distance: "+jarodistance);
		// output folders
		String rootFolder = new File(System.getProperty("user.dir")).getParent();
		String testFolder = rootFolder + File.separator + "MCtest";
		String modelFolder = testFolder + File.separator + "Model";
		
		// save generated tests
		File testFolderFile = new File(testFolder);
		if (!testFolderFile.exists()) {
			testFolderFile.mkdirs();
		}
		File modelFolderFile = new File(modelFolder);
		if (!modelFolderFile.exists()) {
			modelFolderFile.mkdirs();
		}
		model_test_IOoperations io=new model_test_IOoperations();
		io.writeTests(absTestsuite, testFolder);
		io.writeModel(modelFolder);
	}
	
	InterfaceToIv4xrModelCheker.EFSMStateWrapper cast(IExplorableState S) { return (InterfaceToIv4xrModelCheker.EFSMStateWrapper) S ; }

	
	private List<AbstractTestSequence> TransitionCoverage(EFSM efsm, Predicate<IExplorableState> goal) {
	    
		List<AbstractTestSequence> abstestsuite = new ArrayList<AbstractTestSequence>() ;
		
		for(var efsmtr : efsm.getTransitons())
		{
			
			Predicate<IExplorableState> tr_Src = state -> {
				InterfaceToIv4xrModelCheker.EFSMStateWrapper state_ = (InterfaceToIv4xrModelCheker.EFSMStateWrapper) state ;
				return state_.conf.getState().getId().equals(((EFSMTransition)efsmtr).getSrc().getId()) ;
			} ;
			
			Predicate<IExplorableState> tr_Tgt = state -> {
				InterfaceToIv4xrModelCheker.EFSMStateWrapper state_ = (InterfaceToIv4xrModelCheker.EFSMStateWrapper) state ;
				return state_.conf.getState().getId().equals(((EFSMTransition)efsmtr).getTgt().getId()) ;
			};
			
		   	//Buchi model checking	
			BuchiModelChecker bmc = new BuchiModelChecker(new InterfaceToIv4xrModelCheker(efsm)) ;
				
			// invoke the MC:
			var starttime = System.currentTimeMillis() ;
				
			float duration = ((float) (System.currentTimeMillis() - starttime)) / 1000f ; 
			
			
			LTL<IExplorableState> notgf0=ltlNot(now("gf0",S -> cast(S).conf.getState().getId().equals("gf0")));
			LTL<IExplorableState> f = notgf0.until(ltlAnd(now("n1",tr_Src),
					next(ltlAnd(now("n2",tr_Tgt),
				notgf0.until(now("gf0",S -> cast(S).conf.getState().getId().equals("gf0")))))));
			
		
			
			Buchi B = getBuchi(f) ;
			Path<Pair<IExplorableState,String>> path = bmc.find( B, 30) ;
			
			//Buchi
			//Path<Pair<IExplorableState,String>> path = bmc.find( eventuallyeventually(tr_Src,tr_Tgt,goal), 20) ;
			
			// print stats:
			System.out.println(">>> #nodes in efsm: " + efsm.getStates().size()) ;
			System.out.println(">>> #transitions in efsm: " + efsm.getTransitons().size()) ;
			System.out.println(">>> #concrete states and transitions:\n" + bmc.stats) ;
			System.out.println(">>> runtime(s): " + duration) ;
			if(path!=null) {
				System.out.println(">>> Solution length: " + path.path.size()) ;
			}
			System.out.println(">>> Solution: " + path) ;
				
			if(path!=null) {
				//transform transitions to AbstracttestSquence
				List<ITransition> theTransitions = path.path.stream()
						.map(step -> step.fst)
						.collect(Collectors.toList()) ;
				theTransitions.remove(0);
				List<EFSMTransition> efsmTransitions = new LinkedList<>() ;
				for(var tr : theTransitions) {
						EFSMTransitionWrapper tr_ = (EFSMTransitionWrapper) tr ;
						efsmTransitions.add(tr_.tr) ;	

				}	
					
				var path_ = new eu.fbk.iv4xr.mbt.testcase.Path(efsmTransitions) ;
				AbstractTestSequence absTestSeq = new AbstractTestSequence() ;
				absTestSeq.setPath(path_);
				if(!abstestsuite.contains(absTestSeq))
				{
					abstestsuite.add(absTestSeq);
				}
			}
		}
		return abstestsuite;
		
	}

	private List<AbstractTestSequence> StateCoverage(EFSM efsm, Predicate<IExplorableState> goal) {
		
		List<AbstractTestSequence> abstestsuite = new ArrayList<AbstractTestSequence>() ;
		for(var st : efsm.getStates())
		{
				

			Predicate<IExplorableState> state_cover = state -> {
				InterfaceToIv4xrModelCheker.EFSMStateWrapper state_ = (InterfaceToIv4xrModelCheker.EFSMStateWrapper) state ;
				return state_.conf.getState().getId().equals(((EFSMState)st).getId()) ;
			} ;
			

		    	//Buchi model checking	
				BuchiModelChecker bmc = new BuchiModelChecker(new InterfaceToIv4xrModelCheker(efsm)) ;
				
				// invoke the MC:
				var starttime = System.currentTimeMillis() ;
				
				float duration = ((float) (System.currentTimeMillis() - starttime)) / 1000f ; 
				
				
				//Buchi
				Path<Pair<IExplorableState,String>> path = bmc.find(eventuallyeventually(state_cover,goal), 19) ; //min depth to have all states covered in random.simple. 
				// print stats:
				System.out.println(">>> #nodes in efsm: " + efsm.getStates().size()) ;
				System.out.println(">>> #transitions in efsm: " + efsm.getTransitons().size()) ;
				System.out.println(">>> #concrete states and transitions:\n" + bmc.stats) ;
				System.out.println(">>> runtime(s): " + duration) ;
				if(path!=null) {
					System.out.println(">>> Solution length: " + path.path.size()) ;
				}
				System.out.println(">>> Solution: " + path) ;
				
				if(path!=null) {
					//transform transitions to AbstracttestSquence
					List<ITransition> theTransitions = path.path.stream()
							.map(step -> step.fst)
							.collect(Collectors.toList()) ;
					theTransitions.remove(0);
					List<EFSMTransition> efsmTransitions = new LinkedList<>() ;
					for(var tr : theTransitions) {
							EFSMTransitionWrapper tr_ = (EFSMTransitionWrapper) tr ;
							efsmTransitions.add(tr_.tr) ;	

					}	
					
					var path_ = new eu.fbk.iv4xr.mbt.testcase.Path(efsmTransitions) ;
					AbstractTestSequence absTestSeq = new AbstractTestSequence() ;
					absTestSeq.setPath(path_);

						if(!abstestsuite.contains(absTestSeq))
						{
							abstestsuite.add(absTestSeq);
						}

				}
		}

		return abstestsuite;
	}
	
	static List<AbstractTestSequence> RandomSampling(List<AbstractTestSequence> Testsuite, int size) {
		
		Random r = new Random();
		List<AbstractTestSequence> Testsuite_New= new ArrayList<AbstractTestSequence>();

		int[] unique = r.ints(0, Testsuite.size()-1).distinct().limit(size).toArray();
		for(var u: unique)
		{
			Testsuite_New.add(Testsuite.get(u));
		}	
		return Testsuite_New;
		
	}
	static List<AbstractTestSequence> AdaptiveRandomSampling(List<AbstractTestSequence> Testsuite, int size) {
		
		Random r = new Random();
		List<AbstractTestSequence> Testsuite_New= new ArrayList<AbstractTestSequence>();
		 
		int rand1=r.nextInt(Testsuite.size()-1);
		Testsuite_New.add(Testsuite.get(rand1));
		 while(Testsuite_New.size()!=size)
		{
		
			int additional=-1;
			int[] unique = r.ints(0, Testsuite.size()-1).distinct().limit(20).filter(c->c!= rand1).toArray();
			if(unique.length!=20)
			{
				additional=nextIntInRangeButExclude(0,Testsuite.size()-1, rand1);
			}
			HashMap<Integer,Double> dismap=new HashMap<Integer,Double>();

			for(var u : unique)
			{
				JaroWinkler jar=new JaroWinkler();
				double totaldistance=0;
				for (var l : Testsuite_New)
				{

					totaldistance+=jar.distance(Testsuite.get(u).toString(), l.toString());
					

				}
				
				dismap.put(u, (double) totaldistance);
			}
			if(additional!=-1)
			{
				JaroWinkler jar=new JaroWinkler();
				double totaldistance=0;
				for (var l : Testsuite_New)
				{
					totaldistance+=jar.distance(Testsuite.get(additional).toString(), l.toString());
				}
				
				dismap.put(additional, (double) totaldistance);
			}
			double maxdistance=dismap.values().stream().max(Double::compare).get();
			
			if(maxdistance!=0)
			{				
				List <Integer> maxindex=dismap.entrySet().stream().filter(c->c.getValue()==maxdistance ).map(Map.Entry::getKey).collect(Collectors.toList());
				Testsuite_New.add(Testsuite.get(maxindex.get(0)));
			}
		}
		
		
		return Testsuite_New;
	}
	
	public static int nextIntInRangeButExclude(int start, int end, int... excludes){
		
		Random r = new Random();
		int rangeLength = end - start - excludes.length;
	    int randomInt = r.nextInt(rangeLength) + start;

	    for(int i = 0; i < excludes.length; i++) {
	        if(excludes[i] > randomInt) {
	            return randomInt;
	        }

	        randomInt++;
	    }

	    return randomInt;
	}
	
	
	Buchi eventuallyeventually(Predicate<IExplorableState> p, Predicate<IExplorableState> q) {
		Buchi buchi = new Buchi() ;
		buchi.withStates("S0","S1","accept") 
		.withInitialState("S0")
		.withNonOmegaAcceptance("accept")
		.withTransition("S0", "S0", "~p", S ->  ! p.test(S))
		.withTransition("S0", "S1", "p", S -> p.test(S))
	    .withTransition("S1", "S1", "~q", S -> !q.test(S))
	    .withTransition("S1", "accept", "q", S -> q.test(S));
		return buchi ;
	}
	
	
	Buchi eventuallyeventually(Predicate<IExplorableState> p,Predicate<IExplorableState> q, Predicate<IExplorableState> r) {
		
		
		Buchi buchi = new Buchi() ;
		buchi.withStates("S0","S1","S2","accept") 
		.withInitialState("S0")
		.withNonOmegaAcceptance("accept")
		.withTransition("S0", "S0", "~p", S ->  ! p.test(S))
		.withTransition("S0", "S1", "p", S -> p.test(S))
	    .withTransition("S1", "S2", "q", S -> q.test(S))
	    .withTransition("S2", "S2", "~r", S -> !r.test(S))
	    .withTransition("S2", "accept", "r", S -> r.test(S));
		return buchi ;
	}
	
}