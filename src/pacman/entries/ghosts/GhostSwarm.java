package pacman.entries.ghosts;

import java.util.EnumMap;
import java.util.Vector;
import pacman.controllers.Controller;
import pacman.game.Constants;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.entries.ghosts.GhostStrategy;
import pacman.entries.ghosts.GhostStrategy.ACTOR;
import pacman.entries.ghosts.GhostStrategy.GSTATUS;
import pacman.entries.ghosts.GhostStrategy.StrategyCell;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getActions() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.ghosts.mypackage).
 */
public class GhostSwarm extends Controller<EnumMap<GHOST,MOVE>>
{
	
	class vector2d{
		double x = 0.0d;
		double y = 0.0d;
		double lenght = 0.0d;
		
		public vector2d(double x, double y){
			this.x = x;
			this.y = y;
			lenght = 0.0d;
		}
		
		public vector2d(){
			x = 0.0d;
			y = 0.0d;
			lenght = 0.0d;
			
		}
	}
	
	/**
	 * true if magnitudes are normally distributed. false otherwise.
	 */
	public static final boolean NORMAL_MAGNITUDES = true;
	
	// PRIVATE Fields
	private EnumMap<GHOST, MOVE> myMoves=new EnumMap<GHOST, MOVE>(GHOST.class);
	private GhostStrategy gStrategy;	
	
	// CONSTRUCTOR
	public GhostSwarm(GhostStrategy s){
		gStrategy = s;
	}
	
	public GhostSwarm(){
		//gStrategy = new GhostStrategy(NORMAL_MAGNITUDES);
		gStrategy = new GhostStrategy();
	}
	
	// PRIVATE Methods
	private vector2d calcVector(vector2d a, vector2d b){
		vector2d v = new vector2d();
		v.x = b.x - a.x;
		v.y = b.y - a.y;
		if(Math.abs(v.x) > (Constants.GV_WIDTH/2)){
			if(v.x > 0) v.x = v.x - Constants.GV_WIDTH;
			else v.x = Constants.GV_WIDTH + v.x;
		}

		//Vector normalization
		v.lenght = Math.sqrt(Math.pow(v.x, 2) + Math.pow(v.y, 2));
		if(v.lenght > 0.0){
			v.x /= v.lenght;
			v.y /= v.lenght;
		}

		return v;		
	}
	
		// PUBLIC Methods
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue)
	{
		myMoves.clear();
		
		//DEFINING ACTORS POSITIONS
		// - Pacman position
		vector2d pacmanPos = new vector2d(game.getCurrentMaze().graph[game.getPacmanCurrentNodeIndex()].x,
				game.getCurrentMaze().graph[game.getPacmanCurrentNodeIndex()].y);
		// - Ghosts positions
		EnumMap<GHOST,vector2d> ghostsPos = new EnumMap<GHOST,vector2d>(GHOST.class);
		for(GHOST g : GHOST.values())
			ghostsPos.put(g, new vector2d(game.getCurrentMaze().graph[game.getGhostCurrentNodeIndex(g)].x,
					game.getCurrentMaze().graph[game.getGhostCurrentNodeIndex(g)].y));
		// - Powerpills positions
		Vector<vector2d> powPillsPos = new Vector<vector2d>();
    	for(int i=0;i<game.getNumberOfPowerPills();i++)
    		if(game.isPowerPillStillAvailable(i))
    			powPillsPos.add(new vector2d(game.getCurrentMaze().graph[game.getPowerPillIndices()[i]].x,
    					game.getCurrentMaze().graph[game.getPowerPillIndices()[i]].y));
    	// - Pills positions
    	Vector<vector2d> pillsPos = new Vector<vector2d>();
    	for(int i=0; i<game.getNumberOfPills();i++)
    		if(game.isPillStillAvailable(i))
    			pillsPos.add(new vector2d(game.getCurrentMaze().graph[game.getPillIndices()[i]].x,
    					game.getCurrentMaze().graph[game.getPillIndices()[i]].y));
    	
    	//LOOP ON EACH GHOST
    	for(GHOST ghost : GHOST.values())	//for each ghost
		{			
			if(game.doesGhostRequireAction(ghost))	//if ghost requires an action
			{
				//GET THE VECTOR OF POSSIBLE MOVES
				MOVE[] possibleMoves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost),game.getGhostLastMoveMade(ghost));
				//CHECK IF ONLY ONE MOVE IS POSSIBLE
				if(possibleMoves.length == 1){
					myMoves.put(ghost, possibleMoves[0]);
					continue;									
				}
				//ELSE, DETERMINE MOVE
				//INITIALIASE MOVES RANK VECTOR
				EnumMap<MOVE,Double> movesRanks=new EnumMap<MOVE,Double>(MOVE.class);
				for(MOVE m : MOVE.values()){
					movesRanks.put(m, 0.0d);
				}	
				vector2d finalV = new vector2d();
				
				//CALCULATE DISTANCE VECTORS TO DIFFERENT ACTORS
				vector2d pacmanV = calcVector(ghostsPos.get(ghost), pacmanPos);
				EnumMap<GHOST,vector2d> ghostsV = new EnumMap<GHOST,vector2d>(GHOST.class);
				for(GHOST g : GHOST.values())
					if(g != ghost)
						ghostsV.put(g, calcVector(ghostsPos.get(ghost), ghostsPos.get(g)));
				Vector<vector2d> powPillsV = new Vector<vector2d>();
				for(int i = 0; i < powPillsPos.size(); i++)
					powPillsV.add(calcVector(ghostsPos.get(ghost), powPillsPos.get(i)));
				Vector<vector2d> pillsV = new Vector<vector2d>();
				for(int i = 0; i < pillsPos.size(); i++)
					pillsV.add(calcVector(ghostsPos.get(ghost), pillsPos.get(i)));
				
				//DETERMINE STATUS
				EnumMap<ACTOR, StrategyCell> tmpTable;
				StrategyCell tmpForce;
				int pos;
				
				if(game.getGhostEdibleTime(ghost)>0)
					//if status is EDIBLE
					if(game.getGhostEdibleTime(ghost)>Constants.EDIBLE_ALERT)
						tmpTable = gStrategy.gsTable.get(GSTATUS.EDIBLE);
					//else status is BLINK
					else tmpTable = gStrategy.gsTable.get(GSTATUS.BLINK);
				//else status is NORMAL
				else tmpTable = gStrategy.gsTable.get(GSTATUS.NORMAL);
				
				//PACMAN
				tmpForce = tmpTable.get(ACTOR.PACMAN);
				pos = 0;
				while(pacmanV.lenght > tmpForce.limit[pos]) ++pos;
				finalV.x += tmpForce.magnitude[pos] * pacmanV.x;
				finalV.y += tmpForce.magnitude[pos] * pacmanV.y;
								
				//NORMAL GHOSTS
				tmpForce = tmpTable.get(ACTOR.NORMALG);
				for(GHOST g : GHOST.values())
					if(g != ghost && 
					game.getGhostEdibleTime(g) <= 0 &&
					game.getGhostLairTime(g) <= 0){
						pos = 0;
						while(ghostsV.get(g).lenght > tmpForce.limit[pos]) ++pos;
						finalV.x += tmpForce.magnitude[pos] * ghostsV.get(g).x;
						finalV.y += tmpForce.magnitude[pos] * ghostsV.get(g).y;
					}
				
				//EDIBLE GHOSTS
				tmpForce = tmpTable.get(ACTOR.EDIBLEG);
				for(GHOST g : GHOST.values())
					if(g != ghost &&
					game.getGhostEdibleTime(g) > Constants.EDIBLE_ALERT){
						pos = 0;
						while(ghostsV.get(g).lenght > tmpForce.limit[pos]) ++pos;
						finalV.x += tmpForce.magnitude[pos] * ghostsV.get(g).x;
						finalV.y += tmpForce.magnitude[pos] * ghostsV.get(g).y;
					}
					
				
				//BLINKING GHOST
				tmpForce = tmpTable.get(ACTOR.BLINKG);
				for(GHOST g : GHOST.values())
					if(g != ghost &&
					game.getGhostEdibleTime(g) > 0 &&
					game.getGhostEdibleTime(g) <= Constants.EDIBLE_ALERT){
						pos = 0;
						while(ghostsV.get(g).lenght > tmpForce.limit[pos]) ++pos;
						finalV.x += tmpForce.magnitude[pos] * ghostsV.get(g).x;
						finalV.y += tmpForce.magnitude[pos] * ghostsV.get(g).y;
					}
				
				//POWER PILLS
				tmpForce = tmpTable.get(ACTOR.POWERPILL);
				for(int i = 0; i < powPillsV.size(); i++){
					pos = 0;
					while(powPillsV.get(i).lenght > tmpForce.limit[pos]) ++pos;
					finalV.x += tmpForce.magnitude[pos] * powPillsV.get(i).x;
					finalV.y += tmpForce.magnitude[pos] * powPillsV.get(i).y;
				}
					
				//PILLS
				tmpForce = tmpTable.get(ACTOR.PILL);
				for(int i = 0; i < pillsV.size(); i++){
					pos = 0;
					while(pillsV.get(i).lenght > tmpForce.limit[pos]) ++pos;
					finalV.x += tmpForce.magnitude[pos] * pillsV.get(i).x;
					finalV.y += tmpForce.magnitude[pos] * pillsV.get(i).y;
				}		
				
				//ASSIGNING RANKS TO MOVES
				movesRanks.put(MOVE.RIGHT, finalV.x);
				movesRanks.put(MOVE.LEFT, - finalV.x);
				movesRanks.put(MOVE.DOWN, finalV.y);
				movesRanks.put(MOVE.UP, - finalV.y);
			
				//Moves are now ranked
				//FIND THE BEST-RANKED MOVE
				double r = Double.NEGATIVE_INFINITY;
				for(MOVE m : possibleMoves){
					if(movesRanks.get(m) > r){
						r = movesRanks.get(m);
						myMoves.put(ghost, m);
					}
				}
			}	//end if ghost requires an action
		}	//end for each ghost
		
		return myMoves;
	}	//end getMove
	
}