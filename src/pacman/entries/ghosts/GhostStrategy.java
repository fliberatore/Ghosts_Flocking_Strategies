package pacman.entries.ghosts;

import java.util.EnumMap;
import java.util.Random;

import pacman.game.Constants;

/**
 * This class describes a whole strategy for a ghost.
 */
public class GhostStrategy implements java.io.Serializable{
	
	
	/**
	 * Class describing a cell belonging to a strategy.
	 * A cell represents the behavior of a ghost having a
	 * certain status and with respect to a certain actor.
	 */
	public class StrategyCell{
		public double[] magnitude;
		public int num_areas;
		public int[] limit;
		
		/**
		 * Generates an empty Strategy Cell.
		 * @param num Number of areas to be created.
		 */
		public StrategyCell(int num){
			num_areas = num;
			magnitude = new double[num];
			limit = new int[num];
			for(int i = 0; i < num; ++i){
				magnitude[i] = 0.0;
				limit[i] = 0;
			}
		}
		
		/**
		 * Generates
		 * @param num Number of areas to be created.
		 * @param max_dist Maximum distance.
		 * @param r Random number generator.
		 * @param normMagn Use a Normal Distribution for the magnitudes when true.
		 */
		public StrategyCell(int num, int max_dist, Random r, boolean normMagn){
			this(num);
			
			int min_dist = 0;
			for(int i = 0; i < num; ++i){
				if(normMagn){
					magnitude[i] = r.nextGaussian() / 3.0;
					if(magnitude[i] < -1.0) magnitude[i] = -1.0;
					if(magnitude[i] > 1.0) magnitude[i] = 1.0;
				}else{
					int sign = r.nextInt(3) - 1;
					magnitude[i] = r.nextDouble() * sign;
				}
				if(min_dist == max_dist)
					limit[i] = min_dist;
				else{
					limit[i] = min_dist + r.nextInt(max_dist - min_dist) + 1;
					min_dist = limit[i];
				}
			}
			limit[num_areas - 1] = Integer.MAX_VALUE;
		}
	}
	
	/**
	 * Pseudo-random numbers generator
	 */
	public static final Random r = new Random(System.currentTimeMillis());
			
	/**
	 * Number of discrete areas considered
	 */
	public static int NUM_AREAS = 3;
	
	/**
	 * Maximum possible distance in the level map.
	 */
	public static final int MAX_DIST =(int) Math.ceil(Math.sqrt(Math.pow(Constants.GV_WIDTH,2)+Math.pow(Constants.GV_HEIGHT,2)));
	
	/**
	 * Status of a ghost: Normal, Edible, or Blinking.
	*/
	public enum GSTATUS{
		NORMAL,
		EDIBLE,
		BLINK;
		@Override
		public String toString() {
			switch(this) {
				case NORMAL: return "NORMAL";
				case EDIBLE: return "EDIBLE";
				case BLINK: return "BLINK";
				default: throw new IllegalArgumentException();
			}
		}
	}
	
	/**
	 * Actors in the game: Pacman, Normal ghost, Edible ghost, Blinking Ghost, Powerpill, and Pill.
	 * @author Federico
	 *
	 */
	public enum ACTOR{
		PACMAN,
		NORMALG,
		EDIBLEG,
		BLINKG,
		POWERPILL,
		PILL;
		@Override
		public String toString() {
			switch(this) {
				case PACMAN: return "PACMAN";
				case NORMALG: return "NORMALG";
				case EDIBLEG: return "EDIBLEG";
				case BLINKG: return "BLINKG";
				case POWERPILL: return "POWERPILL";
				case PILL: return "PILL";
				default: throw new IllegalArgumentException();
			}
		}
	}
	
	//Fields
	private static final long serialVersionUID = 1L;
	public EnumMap<GSTATUS, EnumMap<ACTOR, StrategyCell>> gsTable = new EnumMap<GSTATUS, EnumMap<ACTOR, StrategyCell>>(GSTATUS.class);
	
	/**
	 * Constructor that generates a random Ghost Strategy.
	 */
	public GhostStrategy(boolean normMagn)
	{		
		//Loop on ghost status
		for(GSTATUS gs : GSTATUS.values()){
			EnumMap<ACTOR, StrategyCell> em = new EnumMap<ACTOR, StrategyCell>(ACTOR.class);
			
			//Loop on game actors
			for(ACTOR a : ACTOR.values()){
				StrategyCell sc = new StrategyCell(NUM_AREAS, MAX_DIST, r, normMagn);
				if(a == ACTOR.PILL){
					for(int i = 0; i < sc.num_areas; ++i)
						sc.magnitude[i] = 0.0;
				}
				
				em.put(a, sc);
			}
			gsTable.put(gs, em);
		}
		
	}
	
	/*
	@Override
	public String toString() {
		String s = new String();
		
		//Loop on ghost status
		for(GSTATUS gs : GSTATUS.values()){
			EnumMap<ACTOR, StrategyCell> em = gsTable.get(gs);
			//Loop on game actors
			for(ACTOR a : ACTOR.values()){
				StrategyCell sc = em.get(a);
				s += gs + "\t" + a;
				//Loop on vector elements
				for(int i = 0; i < NUM_AREAS; ++i){
					s += "\t(" + sc.magnitude[i] + ";" + ((sc.limit[i]==Integer.MAX_VALUE)?"MAX":sc.limit[i]) + ")";
				}
				s += "\n";
			}
		}
		return s;	
	}*/
	
	@Override
	public String toString() {
		String s = new String();
		s += "public GhostStrategy(){\n";
		s += "//Loop on ghost status\n";
		s += "for(GSTATUS gs : GSTATUS.values()){\n";
		s += "EnumMap<ACTOR, StrategyCell> em = new EnumMap<ACTOR, StrategyCell>(ACTOR.class);\n";
		s += "//Loop on game actors\n";
		s += "for(ACTOR a : ACTOR.values()){\n";
		s += "StrategyCell sc = new StrategyCell(" + NUM_AREAS + ");\n";

		//Loop on ghost status
		for(GSTATUS gs : GSTATUS.values()){	
			EnumMap<ACTOR, StrategyCell> em = gsTable.get(gs);
			//Loop on game actors
			for(ACTOR a : ACTOR.values()){
				StrategyCell sc = em.get(a);
				s += "if(gs == GSTATUS." + gs + " && a == ACTOR." + a + "){\n";
				for(int i = 0; i < sc.num_areas; ++i){
					s += "sc.limit["+i+"] = " + sc.limit[i] + ";\n";
					s += "sc.magnitude["+i+"] = " + sc.magnitude[i] + ";\n";
				}
				s += "}\n";
			}
		}
		s += "em.put(a, sc);\n";
		s += "}\n";
		s += "gsTable.put(gs, em);\n";
		s += "}\n";
		s += "}\n";	
		return s;
	}
	
	public GhostStrategy(){
		//Loop on ghost status
		for(GSTATUS gs : GSTATUS.values()){
		EnumMap<ACTOR, StrategyCell> em = new EnumMap<ACTOR, StrategyCell>(ACTOR.class);
		//Loop on game actors
		for(ACTOR a : ACTOR.values()){
		StrategyCell sc = new StrategyCell(5);
		if(gs == GSTATUS.NORMAL && a == ACTOR.PACMAN){
		sc.limit[0] = 49;
		sc.magnitude[0] = 0.6694928769424008;
		sc.limit[1] = 111;
		sc.magnitude[1] = 0.3307736041055591;
		sc.limit[2] = 137;
		sc.magnitude[2] = -0.275038446494487;
		sc.limit[3] = 171;
		sc.magnitude[3] = -0.4600540967559781;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = -0.01982691706189702;
		}
		if(gs == GSTATUS.NORMAL && a == ACTOR.NORMALG){
		sc.limit[0] = 27;
		sc.magnitude[0] = -0.2694404988261837;
		sc.limit[1] = 162;
		sc.magnitude[1] = 0.2475506953751561;
		sc.limit[2] = 167;
		sc.magnitude[2] = 0.04905857067418693;
		sc.limit[3] = 173;
		sc.magnitude[3] = -0.6563623983903628;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = -0.13824762037559338;
		}
		if(gs == GSTATUS.NORMAL && a == ACTOR.EDIBLEG){
		sc.limit[0] = 39;
		sc.magnitude[0] = 0.11709934911501153;
		sc.limit[1] = 122;
		sc.magnitude[1] = -0.011213428880982979;
		sc.limit[2] = 147;
		sc.magnitude[2] = -0.4041169845295851;
		sc.limit[3] = 173;
		sc.magnitude[3] = -0.4041169845295851;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.012818839758736857;
		}
		if(gs == GSTATUS.NORMAL && a == ACTOR.BLINKG){
		sc.limit[0] = 23;
		sc.magnitude[0] = -0.32689623861526246;
		sc.limit[1] = 133;
		sc.magnitude[1] = 0.47833817730911066;
		sc.limit[2] = 134;
		sc.magnitude[2] = 0.7750742697068609;
		sc.limit[3] = 168;
		sc.magnitude[3] = 0.1525152977970012;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = -0.1526898329653739;
		}
		if(gs == GSTATUS.NORMAL && a == ACTOR.POWERPILL){
		sc.limit[0] = 124;
		sc.magnitude[0] = 0.14111462674221845;
		sc.limit[1] = 142;
		sc.magnitude[1] = -0.07762485170341746;
		sc.limit[2] = 159;
		sc.magnitude[2] = 0.1910837777223822;
		sc.limit[3] = 173;
		sc.magnitude[3] = -0.27060692501095246;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.2964507427036419;
		}
		if(gs == GSTATUS.NORMAL && a == ACTOR.PILL){
		sc.limit[0] = 56;
		sc.magnitude[0] = 0.0;
		sc.limit[1] = 155;
		sc.magnitude[1] = 0.0;
		sc.limit[2] = 158;
		sc.magnitude[2] = 0.020929229925147938;
		sc.limit[3] = 172;
		sc.magnitude[3] = 0.0;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.0;
		}
		if(gs == GSTATUS.EDIBLE && a == ACTOR.PACMAN){
		sc.limit[0] = 41;
		sc.magnitude[0] = -0.385452879803771;
		sc.limit[1] = 99;
		sc.magnitude[1] = -0.385452879803771;
		sc.limit[2] = 152;
		sc.magnitude[2] = 0.7572763658325483;
		sc.limit[3] = 173;
		sc.magnitude[3] = 0.06192677261891607;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.3391412075684707;
		}
		if(gs == GSTATUS.EDIBLE && a == ACTOR.NORMALG){
		sc.limit[0] = 123;
		sc.magnitude[0] = 0.23746289074514557;
		sc.limit[1] = 162;
		sc.magnitude[1] = -0.015667378805727473;
		sc.limit[2] = 163;
		sc.magnitude[2] = 0.24254338347256554;
		sc.limit[3] = 173;
		sc.magnitude[3] = 0.031684791986953155;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = -0.019413662577595064;
		}
		if(gs == GSTATUS.EDIBLE && a == ACTOR.EDIBLEG){
		sc.limit[0] = 40;
		sc.magnitude[0] = -0.07018382919406016;
		sc.limit[1] = 107;
		sc.magnitude[1] = 0.07701223599833587;
		sc.limit[2] = 118;
		sc.magnitude[2] = 0.3165720435500685;
		sc.limit[3] = 161;
		sc.magnitude[3] = 0.19853425246922288;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.5390055592393019;
		}
		if(gs == GSTATUS.EDIBLE && a == ACTOR.BLINKG){
		sc.limit[0] = 101;
		sc.magnitude[0] = 0.6116271220630656;
		sc.limit[1] = 154;
		sc.magnitude[1] = 0.25232781628725925;
		sc.limit[2] = 171;
		sc.magnitude[2] = 0.11217061593691131;
		sc.limit[3] = 172;
		sc.magnitude[3] = -0.3291660256385792;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = -0.5290161507154906;
		}
		if(gs == GSTATUS.EDIBLE && a == ACTOR.POWERPILL){
		sc.limit[0] = 85;
		sc.magnitude[0] = 0.27556344438644875;
		sc.limit[1] = 130;
		sc.magnitude[1] = 0.018905881300391687;
		sc.limit[2] = 155;
		sc.magnitude[2] = -0.9320079771789658;
		sc.limit[3] = 173;
		sc.magnitude[3] = -0.9320079771789658;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = -0.41740518407323335;
		}
		if(gs == GSTATUS.EDIBLE && a == ACTOR.PILL){
		sc.limit[0] = 123;
		sc.magnitude[0] = 0.0;
		sc.limit[1] = 149;
		sc.magnitude[1] = 0.0;
		sc.limit[2] = 161;
		sc.magnitude[2] = 0.0;
		sc.limit[3] = 171;
		sc.magnitude[3] = 0.0;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.2352993188139064;
		}
		if(gs == GSTATUS.BLINK && a == ACTOR.PACMAN){
		sc.limit[0] = 113;
		sc.magnitude[0] = -0.05962963685534706;
		sc.limit[1] = 149;
		sc.magnitude[1] = 0.06888222532154678;
		sc.limit[2] = 154;
		sc.magnitude[2] = 0.22081773874041433;
		sc.limit[3] = 173;
		sc.magnitude[3] = 0.011935617261857186;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.016177283554214605;
		}
		if(gs == GSTATUS.BLINK && a == ACTOR.NORMALG){
		sc.limit[0] = 2;
		sc.magnitude[0] = 0.2289399004564202;
		sc.limit[1] = 114;
		sc.magnitude[1] = 0.07965432450282744;
		sc.limit[2] = 159;
		sc.magnitude[2] = 0.6856169026711981;
		sc.limit[3] = 171;
		sc.magnitude[3] = 0.44138695263822747;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.18832452212023218;
		}
		if(gs == GSTATUS.BLINK && a == ACTOR.EDIBLEG){
		sc.limit[0] = 70;
		sc.magnitude[0] = 0.2265207096378065;
		sc.limit[1] = 133;
		sc.magnitude[1] = 0.013742510109736226;
		sc.limit[2] = 147;
		sc.magnitude[2] = -0.39468722359323505;
		sc.limit[3] = 172;
		sc.magnitude[3] = 0.15235012901766062;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = -0.3979243899336196;
		}
		if(gs == GSTATUS.BLINK && a == ACTOR.BLINKG){
		sc.limit[0] = 142;
		sc.magnitude[0] = -0.1757777578435711;
		sc.limit[1] = 142;
		sc.magnitude[1] = -0.4071727075674752;
		sc.limit[2] = 161;
		sc.magnitude[2] = 0.44196903244548014;
		sc.limit[3] = 166;
		sc.magnitude[3] = -0.13433297357308108;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.2940293462461597;
		}
		if(gs == GSTATUS.BLINK && a == ACTOR.POWERPILL){
		sc.limit[0] = 27;
		sc.magnitude[0] = -0.01698162746478585;
		sc.limit[1] = 116;
		sc.magnitude[1] = -0.16187164357703826;
		sc.limit[2] = 169;
		sc.magnitude[2] = -0.49612049620165055;
		sc.limit[3] = 169;
		sc.magnitude[3] = -0.09213091130404842;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.6069705543944796;
		}
		if(gs == GSTATUS.BLINK && a == ACTOR.PILL){
		sc.limit[0] = 106;
		sc.magnitude[0] = 0.0;
		sc.limit[1] = 151;
		sc.magnitude[1] = 0.0;
		sc.limit[2] = 166;
		sc.magnitude[2] = 0.0;
		sc.limit[3] = 171;
		sc.magnitude[3] = 0.0;
		sc.limit[4] = 2147483647;
		sc.magnitude[4] = 0.0;
		}
		em.put(a, sc);
		}
		gsTable.put(gs, em);
		}
	}


}