package pacman;

import java.util.Random;

import pacman.controllers.Controller;
import pacman.controllers.examples.*;
import pacman.entries.pacman.*;
import pacman.entries.ghosts.GhostStrategy;
import pacman.entries.ghosts.GhostSwarm;
import pacman.entries.ghosts.GhostStrategy.*;
import pacman.game.Game;
import pacman.game.Constants.MOVE;

public class GeneticAlgorithm {
	
	static class scoreKeeper{
		static final int NUM_PACMAN_AI = 8;
		int[] scores = new int[NUM_PACMAN_AI];
		double rankFitness = 0;
		int l1 = -1;
		int l2 = -1;

		public int level1(){
			if(l1 < 0){
				l1 = scores[0];
				for(int i = 1; i < NUM_PACMAN_AI; ++i){
					if(scores[i] > l1)
						l1 = scores[i];
				}
			}
			return l1;
		}
		
		public int level2(){
			if(l2 < 0){
				l2 = 0;
				for(int i = 0; i < NUM_PACMAN_AI; ++i){
					l2 += scores[i]^2;
				}
			}
			return l2;
		}
		
		public boolean dominates(scoreKeeper other){
			if(this.level1() < other.level1() ||
				(this.level1() == other.level1() && this.level2() < other.level2()))
					return true;
			
			return false;
		}
		
		@Override
		public String toString() {
			String s = Integer.toString(scores[0]);
	        for(int i = 1; i < NUM_PACMAN_AI; ++i){
	        	s += "\t" + Integer.toString(scores[i]);
	        }
	        return s;
	    }

	};	
	
	/**
	 * Genetic Algorithm Parameters
	 */
	static int POP_SIZE = 50;
	static int ELITISM = 25;
	static int TRIALS = 1;//30;
	static int NUM_GENERATIONS = 50;
	static double MUTATION_PROB = 0.00125;
	static double CROSSOVER_PROB = 0.5;
	
	/* Population data structures */
	static GhostStrategy[] oldPopulation;
	static GhostStrategy[] newPopulation;
	static GhostStrategy bestIndividual;
	static int bestGenFound = 0;
	static GhostStrategy[] bestGenIndividual= new GhostStrategy[NUM_GENERATIONS];
	static scoreKeeper bestFitness;
	static scoreKeeper[] bestGenFitness = new scoreKeeper[NUM_GENERATIONS];
	static scoreKeeper[] oldFitness;
	static scoreKeeper[] newFitness;

	/* Other variables*/
	static final Random rnd = new Random(System.currentTimeMillis());
	static long startTime;
	
	public static void main(String[] args)
	{
		paramConfig(args);
		startTime = System.currentTimeMillis();
		
		//Create Initial Population
		createInitPop();
		printStat(0);
		
		//EVOLUTION LOOP
		for(int gen = 1; gen < NUM_GENERATIONS; ++gen){
			//The new becomes the old...
			oldPopulation = newPopulation;
			newPopulation = new GhostStrategy[POP_SIZE];
			oldFitness = newFitness;
			newFitness = new scoreKeeper[POP_SIZE];
			
			//Assigning rank-based fitness
			rankBasedFitness(gen);
			//Elitist selection
			elitistSelection(gen);
			
			//Calculating total fitness for roulette-wheel selection
			double totFitness = 0.0d;
			for(int i = 0; i < POP_SIZE; ++i)
				totFitness += oldFitness[i].rankFitness;
			for(int i = ELITISM; i < POP_SIZE; ++i){
				//	- Selection
				//	- Crossing
				//	- Mutation
				newPopulation[i] = mutateIndividual(
						crossIndividuals(
								oldPopulation[selectIndividual(totFitness)], 
								oldPopulation[selectIndividual(totFitness)]));
	
				//	- Fitness evaluation
				newFitness[i] = evalScores(newPopulation[i]);
				updateBestFitness(i, gen);			
			}//END New Individual Loop
			
			//Uncomment when adaptive mutation factor required
			//adaptiveMutationFactor();
		
			printStat(gen);
		} //END Evolution Loop	
		System.out.println(bestIndividual);
	}
	
	/**
	 * Print current generation statistics.
	 * @param gen Current generation.
	 */
	static void printStat(int gen){
		if(gen==0)
			System.out.println("Gen\tTime\t1\t2\t3\t4\t5\t6\t7\t8");
		
		System.out.println(gen + "\t" + (System.currentTimeMillis() - startTime)/1000 + "\t" + bestFitness);
	}
	
	/**
	 * Assign rank to individuals according to their rank.
	 * @param gen Next generation.
	 */
	static void rankBasedFitness(int gen){
		//Sort the individuals according to fitness
		for(int i = 0; i < POP_SIZE-1; ++i){
			for(int j = i+1; j < POP_SIZE; ++j){
				if(oldFitness[j].dominates(oldFitness[i])){
					scoreKeeper sk_tmp = oldFitness[i];
					oldFitness[i] = oldFitness[j];
					oldFitness[j] = sk_tmp;
					GhostStrategy gs_tmp = oldPopulation[i];
					oldPopulation[i] = oldPopulation[j];
					oldPopulation[j] = gs_tmp;
				}
			}
		}
		
		//assign fitness based on rank 
		for(int i = 0; i < POP_SIZE; ++i){
			oldFitness[i].rankFitness = ((double)POP_SIZE - i) / POP_SIZE; 
		}

	}
	
	/**
	 * Implements elitist selection:
	 * Best individuals propagated to the next generation.
	 * @param gen Next generation.
	 */
	static void elitistSelection(int gen){

		for(int i = 0; i < ELITISM; ++i){
			newPopulation[i] = oldPopulation[i];
			newFitness[i] = oldFitness[i];
		}
		updateBestFitness(0, gen);
	}
	
	/**
	 * Read the input arguments and sets the options accordingly
	 * @param args Input arguments.
	 */
	static void paramConfig(String[] args){

		int i = 0;
		while(i < args.length){
			switch(args[i].toLowerCase()){
				case "-h":
					System.out.println("Arguments:");
					System.out.println("-H or -h\tThis help information.");
					System.out.println("-POP or -pop\tGenetic Algorithm: Size of the population. Default value: "+POP_SIZE);
					System.out.println("-GEN or -gen\tGenetic Algorithm: Number of generations. Default value: "+NUM_GENERATIONS);
					System.out.println("-ELI or -eli\tGenetic Algorithm: Elitism selection. Default value: "+ELITISM);
					System.out.println("-MUT or -mut\tGenetic Algorithm: Mutation probability. Default value: "+MUTATION_PROB);
					System.out.println("-CRO or -CRO\tGenetic Algorithm: Crossover probability. Default value: "+CROSSOVER_PROB);
					System.out.println("-NEI or -nei\tFlocking Algorithm: Number of neighborhoods. Default value: "+GhostStrategy.NUM_AREAS);
					System.exit(0);
					break;
				case "-pop":
					POP_SIZE = Integer.parseInt(args[i+1]);
					if(POP_SIZE <= 0){
						System.err.println("Error: -POP does not take values equal or smaller than zero.");
						System.exit(0);
					}
					i += 2;
					break;
				case "-gen":
					NUM_GENERATIONS = Integer.parseInt(args[i+1]);
					if(NUM_GENERATIONS <= 0){
						System.err.println("Error: -GEN does not take values equal or smaller than zero.");
						System.exit(0);
					}
					i += 2;
					break;
				case "-eli":
					ELITISM = Integer.parseInt(args[i+1]);
					if(ELITISM < 0 || ELITISM > POP_SIZE){
						System.err.println("Error: -ELI does not take values smaller than zero or greater than the population size.");
						System.exit(0);
					}
					i += 2;
					break;
				case "-mut":
					MUTATION_PROB = Double.parseDouble(args[i+1]);
					if(MUTATION_PROB < 0 || MUTATION_PROB > 1){
						System.err.println("Error: -MUT does not take values smaller than zero or greater than one.");
						System.exit(0);
					}
					i += 2;
					break;
				case "-cro":
					CROSSOVER_PROB = Double.parseDouble(args[i+1]);
					if(CROSSOVER_PROB < 0 || CROSSOVER_PROB > 1){
						System.err.println("Error: -CRO does not take values smaller than zero or greater than one.");
						System.exit(0);
					}
					i += 2;
					break;
				case "-nei":
					GhostStrategy.NUM_AREAS =  Integer.parseInt(args[i+1]);
					if(GhostStrategy.NUM_AREAS <= 0){
						System.err.println("Error: -NEI does not take values equal or smaller than zero.");
						System.exit(0);
					}
					i += 2;
					break;
				default:
					//Error: Option not recognized.
					System.err.println("Error: Input parameter not recognized");
					System.exit(0);
					break;
			}
		}
	}
	
	/**
	 * Updates the globally best individual and the best individual in the current generation,
	 * if necessary. Also, updates the number of generations.
	 * @param currentElem Individual to be evaluated.
	 * @param currentGen Current generation.
	 */
	static void updateBestFitness(int currentElem, int currentGen){
		if(bestFitness == null || newFitness[currentElem].dominates(bestFitness)){
			bestFitness = newFitness[currentElem];
			bestIndividual = newPopulation[currentElem];
			bestGenFound = currentGen;

			//Uncomment if Adaptive Generations required
			//adaptiveGenerationsNumber(currentGen);
		}
		if(bestGenFitness[currentGen] == null || newFitness[currentElem].dominates(bestGenFitness[currentGen])){
			bestGenFitness[currentGen] = newFitness[currentElem];
			bestGenIndividual[currentGen] = newPopulation[currentElem];
		}
	}

	
	static int m_lastGenUpdate = 0;
	static int m_numIncrements = 0;
	static long m_sumIncrements = 0;
	static long m_sumIncrements2 = 0;
	/**
	 * Adaptive generations number. Extends the number of generations each time a better solution is found.
	 * @param currentGen Current generation number.
	 */
	static void adaptiveGenerationsNumber(int currentGen){
		if(currentGen == m_lastGenUpdate) return;
		
		int increment = currentGen - m_lastGenUpdate;
		++m_numIncrements;
		m_sumIncrements += increment;
		m_sumIncrements2 += Math.pow(increment, 2);
		m_lastGenUpdate = currentGen;
		
		int mean = (int)(m_sumIncrements/m_numIncrements);
		double stDev = Math.sqrt(((double)m_sumIncrements2)/m_numIncrements - Math.pow(mean, 2));
		
		int step = mean + (int)(3 * Math.ceil(stDev));
		
		if(NUM_GENERATIONS - currentGen < step){
			int newNumGen = currentGen + step;
			GhostStrategy[] tmpGSArray = new GhostStrategy[newNumGen];
			scoreKeeper[] tmpGFArray = new scoreKeeper[newNumGen];				
			System.arraycopy(bestGenIndividual, 0, tmpGSArray, 0, currentGen);
			bestGenIndividual = tmpGSArray;
			System.arraycopy(bestGenFitness, 0, tmpGFArray, 0, currentGen);
			bestGenFitness = tmpGFArray;
			NUM_GENERATIONS = newNumGen;
		}
	}
	
	/**
	 * Adaptive mutation factor. Increases the mutation probability when the
	 * population is too homogeneous. Resets the mutation probability when the
	 * population is too heterogeneous.
	 */
	/**
	static void adaptiveMutationFactor(){
		double sumX = 0.0, sumX2 = 0.0;
		for(int i = 0; i < POP_SIZE; ++i){
			sumX += newFitness[i].fitnessInv();
			sumX2 += Math.pow(newFitness[i].fitnessInv(), 2);
		}
		double mean = sumX/POP_SIZE;
		double stDev = Math.sqrt(sumX2/POP_SIZE - Math.pow(mean, 2));
		double cv = stDev / Math.abs(mean);
		
		if(cv > 0.6)
			MUTATION_PROB = 0.00125;
		else if(cv > 0.3)
			MUTATION_PROB = MUTATION_PROB;
		else if(cv > 0.2)
			MUTATION_PROB *= 2;
		else if(cv > 0.1)
			MUTATION_PROB *= 4;
		else MUTATION_PROB *= 8;

	}
	*/
	
	/**
	 * Calculates the fitness of one individual.
	 * @param gs GhostStrategy (individual) to be evaluated.
	 */
	static scoreKeeper evalScores(GhostStrategy gs){
		scoreKeeper s = new scoreKeeper();
		Random rnd = new Random(System.currentTimeMillis());
		Game game;
		GhostSwarm gControl = new GhostSwarm(gs);
		Controller<MOVE> pControl;
		double sumX = 0.0, sumX2= 0.0, mean, stDev, ciMax;
		//double cv, ciMin;
		double Z = 1.959963985;

		//NearestPillPacMan
		sumX = 0.0d;
		sumX2 = 0.0d;
		for(int i = 0; i < TRIALS; ++i){
			game=new pacman.game.Game(rnd.nextLong());
			pControl = new NearestPillPacMan();
			while(!game.gameOver())
			{
		        game.advanceGame(pControl.getMove(game.copy(), pacman.game.Constants.DELAY),
		        		gControl.getMove(game.copy(),pacman.game.Constants.DELAY));
			}	
			sumX += game.getScore();
			sumX2 += Math.pow(game.getScore(), 2);
		}
		mean = sumX / TRIALS;
		stDev = Math.sqrt((sumX2 / TRIALS) - Math.pow(mean, 2));
		//cv = stDev/Math.abs(mean);
		//ciMin = mean - Z * stDev / Math.sqrt(TRIALS);
		ciMax = mean + Z * stDev / Math.sqrt(TRIALS);
		s.scores[0] =(int) ciMax;
		
		//StarterPacMan
		sumX = 0.0d;
		sumX2 = 0.0d;
		for(int i = 0; i < TRIALS; ++i){
			game=new pacman.game.Game(rnd.nextLong());
			pControl = new StarterPacMan();
			while(!game.gameOver())
			{
		        game.advanceGame(pControl.getMove(game.copy(),pacman.game.Constants.DELAY),
		        		gControl.getMove(game.copy(),pacman.game.Constants.DELAY));
			}	
			sumX += game.getScore();
			sumX2 += Math.pow(game.getScore(), 2);
		}
		mean = sumX / TRIALS;
		stDev = Math.sqrt((sumX2 / TRIALS) - Math.pow(mean, 2));
		//cv = stDev/Math.abs(mean);
		//ciMin = mean - Z * stDev / Math.sqrt(TRIALS);
		ciMax = mean + Z * stDev / Math.sqrt(TRIALS);	
		s.scores[1] = (int) ciMax;
		
		//JSvenssonPacMan
		sumX = 0.0d;
		sumX2 = 0.0d;
		for(int i = 0; i < TRIALS; ++i){
			game=new pacman.game.Game(rnd.nextLong());
			pControl = new JSvenssonPacMan();
			while(!game.gameOver())
			{
		        game.advanceGame(pControl.getMove(game.copy(), pacman.game.Constants.DELAY),
		        		gControl.getMove(game.copy(),pacman.game.Constants.DELAY));
			}	
			sumX += game.getScore();
			sumX2 += Math.pow(game.getScore(), 2);
		}
		mean = sumX / TRIALS;
		stDev = Math.sqrt((sumX2 / TRIALS) - Math.pow(mean, 2));
		//cv = stDev/Math.abs(mean);
		//ciMin = mean - Z * stDev / Math.sqrt(TRIALS);
		ciMax = mean + Z * stDev / Math.sqrt(TRIALS);
		s.scores[2] = (int) ciMax;
		
		//TomPepelsPacMan
		/*sumX = 0.0d;
		sumX2 = 0.0d;
		for(int i = 0; i < TRIALS; ++i){
			game=new pacman.game.Game(rnd.nextLong());
			pControl = new TomPepelsPacMan();
			while(!game.gameOver())
			{
		        game.advanceGame(pControl.getMove(game.copy(),pacman.game.Constants.DELAY),
		        		gControl.getMove(game.copy(),pacman.game.Constants.DELAY));
			}	
			sumX += game.getScore();
			sumX2 += Math.pow(game.getScore(), 2);
		}
		mean = sumX / TRIALS;
		stDev = Math.sqrt((sumX2 / TRIALS) - Math.pow(mean, 2));
		//cv = stDev/Math.abs(mean);
		//ciMin = mean - Z * stDev / Math.sqrt(TRIALS);
		ciMax = mean + Z * stDev / Math.sqrt(TRIALS);
		s.scores[3] = ciMax;
		*/
		
		//SimulationPacMan
		sumX = 0.0d;
		sumX2 = 0.0d;
		for(int i = 0; i < TRIALS; ++i){
			game=new pacman.game.Game(rnd.nextLong());
			pControl = new SimulationPacMan();
			while(!game.gameOver())
			{
		        game.advanceGame(pControl.getMove(game.copy(),pacman.game.Constants.DELAY),
		        		gControl.getMove(game.copy(),pacman.game.Constants.DELAY));
			}	
			sumX += game.getScore();
			sumX2 += Math.pow(game.getScore(), 2);
		}
		mean = sumX / TRIALS;
		stDev = Math.sqrt((sumX2 / TRIALS) - Math.pow(mean, 2));
		//cv = stDev/Math.abs(mean);
		//ciMin = mean - Z * stDev / Math.sqrt(TRIALS);
		ciMax = mean + Z * stDev / Math.sqrt(TRIALS);
		s.scores[3] = (int) ciMax;		
		
		//MixMaxPacMan
		sumX = 0.0d;
		sumX2 = 0.0d;
		for(int i = 0; i < TRIALS; ++i){
			game=new pacman.game.Game(rnd.nextLong());
			pControl = new MixMaxPacMan();
			while(!game.gameOver())
			{
		        game.advanceGame(pControl.getMove(game.copy(),pacman.game.Constants.DELAY),
		        		gControl.getMove(game.copy(),pacman.game.Constants.DELAY));
			}	
			sumX += game.getScore();
			sumX2 += Math.pow(game.getScore(), 2);
		}
		mean = sumX / TRIALS;
		stDev = Math.sqrt((sumX2 / TRIALS) - Math.pow(mean, 2));
		//cv = stDev/Math.abs(mean);
		//ciMin = mean - Z * stDev / Math.sqrt(TRIALS);
		ciMax = mean + Z * stDev / Math.sqrt(TRIALS);
		s.scores[4] = (int) ciMax;
		
		//StarterPacManEx
		sumX = 0.0d;
		sumX2 = 0.0d;
		for(int i = 0; i < TRIALS; ++i){
			game=new pacman.game.Game(rnd.nextLong());
			pControl = new StarterPacManEx(15);
			while(!game.gameOver())
			{
		        game.advanceGame(pControl.getMove(game.copy(),pacman.game.Constants.DELAY),
		        		gControl.getMove(game.copy(),pacman.game.Constants.DELAY));
			}	
			sumX += game.getScore();
			sumX2 += Math.pow(game.getScore(), 2);
		}
		mean = sumX / TRIALS;
		stDev = Math.sqrt((sumX2 / TRIALS) - Math.pow(mean, 2));
		//cv = stDev/Math.abs(mean);
		//ciMin = mean - Z * stDev / Math.sqrt(TRIALS);
		ciMax = mean + Z * stDev / Math.sqrt(TRIALS);
		s.scores[5] = (int) ciMax;
		
		//ICEP_feat_Spooks
		sumX = 0.0d;
		sumX2 = 0.0d;
		for(int i = 0; i < TRIALS; ++i){
			game=new pacman.game.Game(rnd.nextLong());
			pControl = new ICEP_feat_Spooks();
			while(!game.gameOver())
			{
		        game.advanceGame(pControl.getMove(game.copy(),pacman.game.Constants.DELAY),
		        		gControl.getMove(game.copy(),pacman.game.Constants.DELAY));
			}	
			sumX += game.getScore();
			sumX2 += Math.pow(game.getScore(), 2);
		}
		mean = sumX / TRIALS;
		stDev = Math.sqrt((sumX2 / TRIALS) - Math.pow(mean, 2));
		//cv = stDev/Math.abs(mean);
		//ciMin = mean - Z * stDev / Math.sqrt(TRIALS);
		ciMax = mean + Z * stDev / Math.sqrt(TRIALS);	
		s.scores[6] = (int) ciMax;
		
		//ICEP_IDDFS
		sumX = 0.0d;
		sumX2 = 0.0d;
		for(int i = 0; i < TRIALS; ++i){
			game=new pacman.game.Game(rnd.nextLong());
			pControl = new ICEP_IDDFS();
			while(!game.gameOver())
			{
		        game.advanceGame(pControl.getMove(game.copy(),pacman.game.Constants.DELAY),
		        		gControl.getMove(game.copy(),pacman.game.Constants.DELAY));
			}	
			sumX += game.getScore();
			sumX2 += Math.pow(game.getScore(), 2);
		}
		mean = sumX / TRIALS;
		stDev = Math.sqrt((sumX2 / TRIALS) - Math.pow(mean, 2));
		//cv = stDev/Math.abs(mean);
		//ciMin = mean - Z * stDev / Math.sqrt(TRIALS);
		ciMax = mean + Z * stDev / Math.sqrt(TRIALS);
		s.scores[7] = (int) ciMax;
		
		return s;		
	}
	
	/**
	 * Creates, initialises, and evaluates the fitness of the initial population. 
	 */
	static void createInitPop(){
		newPopulation = new GhostStrategy[POP_SIZE];
		newFitness = new scoreKeeper[POP_SIZE];
		for(int i = 0; i < POP_SIZE; ++i){
			newPopulation[i] = new GhostStrategy(GhostSwarm.NORMAL_MAGNITUDES);
			newFitness[i] = evalScores(newPopulation[i]);
			updateBestFitness(i, 0);
		}
	}
	
	static int selectIndividual(double totFitness){
		double rndFitness, sumFitness;
		int pIndex;
		
		//Parents selection
		rndFitness = Math.random() * totFitness;
		pIndex = 0;
		sumFitness = oldFitness[0].rankFitness;
		while(sumFitness < rndFitness){
			++pIndex;
			sumFitness += oldFitness[pIndex].rankFitness;
		}
		
		return pIndex;
	}
	
	static GhostStrategy mutateIndividual(GhostStrategy gs){
		for(GSTATUS gstatus : GSTATUS.values()){
			for(ACTOR a : ACTOR.values()){
				StrategyCell sc = gs.gsTable.get(gstatus).get(a);
				for(int i = 0; i < sc.num_areas;++i){
					if(rnd.nextDouble() < MUTATION_PROB){
						if(GhostSwarm.NORMAL_MAGNITUDES){
							sc.magnitude[i] = rnd.nextGaussian() / 3.0;
							if(sc.magnitude[i] < -1.0) sc.magnitude[i] = -1.0;
							if(sc.magnitude[i] > 1.0) sc.magnitude[i] = 1.0;
						}else{
							int sign = rnd.nextInt(3) - 1;
							sc.magnitude[i] = rnd.nextDouble() * sign;
						}
					}
				}
				for(int i = 0; i < sc.num_areas - 1;++i){
					if(rnd.nextDouble() < MUTATION_PROB){
						int prev;
						if(i == 0) prev = 0;
						else prev = sc.limit[i-1];
						if(prev == GhostStrategy.MAX_DIST)
							sc.limit[i] = GhostStrategy.MAX_DIST;
						else sc.limit[i] = prev + rnd.nextInt(GhostStrategy.MAX_DIST - prev);
					}
				}
				
				//Bubble-sort-it
				for(int i = 0; i < sc.num_areas-1;++i)
					for(int j = i+1; j < sc.num_areas; ++j)
						if(sc.limit[i] > sc.limit[j]){
							int limit = sc.limit[i];
							double magnitude = sc.magnitude[i];
							sc.limit[i] = sc.limit[j];
							sc.magnitude[i] = sc.magnitude[j];
							sc.limit[j] = limit;
							sc.magnitude[j] = magnitude;
						}
			}
		}
		return gs;		
	}
	
	static GhostStrategy crossIndividuals(GhostStrategy gs1, GhostStrategy gs2){
		GhostStrategy gs = new GhostStrategy(GhostSwarm.NORMAL_MAGNITUDES);
		
		for(GSTATUS gstatus : GSTATUS.values()){
			for(ACTOR a : ACTOR.values()){
				StrategyCell sc = gs.gsTable.get(gstatus).get(a);
				StrategyCell sc1 = gs1.gsTable.get(gstatus).get(a);
				StrategyCell sc2 = gs2.gsTable.get(gstatus).get(a);
				for(int i = 0; i < sc.num_areas;++i){
					if(rnd.nextDouble() < CROSSOVER_PROB)
						sc.limit[i] = sc1.limit[i];
					else sc.limit[i] = sc2.limit[i];
					if(rnd.nextDouble() < CROSSOVER_PROB)
						sc.magnitude[i] = sc1.magnitude[i];
					else sc.magnitude[i] = sc2.magnitude[i];
				}
				
				//Bubble-sort-it
				for(int i = 0; i < sc.num_areas-1;++i)
					for(int j = i+1; j < sc.num_areas; ++j)
						if(sc.limit[i] > sc.limit[j]){
							int limit = sc.limit[i];
							double magnitude = sc.magnitude[i];
							sc.limit[i] = sc.limit[j];
							sc.magnitude[i] = sc.magnitude[j];
							sc.limit[j] = limit;
							sc.magnitude[j] = magnitude;
						}
			}
		}
		return gs;
	}

}
