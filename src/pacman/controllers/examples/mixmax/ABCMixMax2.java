package pacman.controllers.examples.mixmax;

import java.util.BitSet;
import java.util.EnumMap;
import pacman.game.Constants;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class ABCMixMax2 {

	// minimum score
	private float MINSCORE = -5000000.0f;
	
	// the time limit of the algorithm
	private long timeLimit = pacman.game.Constants.DELAY;
	
	// the fixed depth to check
	private int fixedDepth = 15;
	
	public boolean complete = true;
	
	private GHOST[] ghosts = GHOST.values();
	
	public void DecrementDepth() {
		fixedDepth--;
	}
	
	/**
	 * Default constructor
	 */
	public ABCMixMax2( long runningTimeLimit ) {
		// set the time limit for the algorithm
		this.timeLimit = runningTimeLimit;
	}
	
	public ABCMixMax2( long runningTimeLimit, int mixmaxdepth ) {
		// set the time limit for the algorithm
		this.timeLimit = runningTimeLimit;
		this.fixedDepth = mixmaxdepth;
	}
	
	/**
	 * Performs mini max to get the next ideal move
	 * @param game
	 * @return
	 */
	public MOVE Execute( Game game ) {		
		complete = false;
		
		// max utility
		float maxUtility = MINSCORE;
		// best move
		MOVE bestMove = MOVE.NEUTRAL;
		// starting time of the algorithm
		long startTime = System.currentTimeMillis();
		// depth limit for iterative deepening search
		int depthLimit = fixedDepth;
		//int depthLimit = 5;
		// get possible directions where pacman can move
		MOVE[] possibleMoves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
		//moveUtility = new float[possibleMoves.length];
		
		// start iteration loop
		while ( depthLimit <= fixedDepth && System.currentTimeMillis() - startTime < timeLimit ) {
			// increment current depth
			depthLimit++;
			// reset best move
			maxUtility = MINSCORE;
			// check each possible direction
			for ( int i = 0; i < possibleMoves.length; i++ ) {
				// clear visited junctions
				//visitedJunctions.clear();
				BitSet visitedJunctions = new BitSet();
				// get the utility of this move
				float tempUtility = MoveInDirection( game.copy(), possibleMoves[i], 1, depthLimit, 0, visitedJunctions );
				//moveUtility[i] += tempUtility * ( 1 - ( depthLimit / 100.0f ));
				
				// check if move is better
				//if ( ( moveUtility[i] / (float)depthLimit ) > maxUtility ) {
				if ( tempUtility > maxUtility ) {
					bestMove = possibleMoves[i];
					maxUtility = tempUtility;
				}
			}
			
		}

		complete = true;
		
		return bestMove;
	}

	private float MoveInDirection( Game game, MOVE direction, int currentDepth, int depthLimit, float pathUtility, BitSet visitedJunctions  ) {
		float utility = 0;
		int currentLevel = game.getCurrentLevel();
		boolean terminated = false;
		MOVE nextMove = direction;
		float distance = 0;
		
		do {
			// marks the distance moved in this iteration
			distance += 0.05f;
			// advance game
			game.advanceGameWithPowerPillReverseOnly(nextMove, GetGhostMoves( game ) );
			// check if pacman died
			if ( game.wasPacManEaten() ) {
				utility = MINSCORE + (distance*10.0f);// / distance;
				terminated = true;
			}
			// check if pacman won
			else if ( game.gameOver() || game.getCurrentLevel() > currentLevel ) {
				//utility = MAXSCORE;
				utility += 400;
				terminated = true;
			}
			// otherwise check for eaten pills/powerpills/ghosts
			else {
				// if pill or power pill eaten
				if ( game.wasPillEaten() )
					utility += (Math.max( 10 - distance, 1 ) );
				else if ( game.wasPowerPillEaten() ) {
					if ( game.getPacmanNumberOfLivesRemaining() == 1 )
						utility -= 70;
					else {
						float timeFactorPassed = game.getCurrentLevelTime() / (float)Constants.LEVEL_LIMIT;
						if ( timeFactorPassed <= 0.25 )
							utility -= 800;
						else if ( timeFactorPassed <= 0.5 )
							utility -= 500;
						else if ( timeFactorPassed <= 0.75 )
							utility -= 400;
						else
							utility -= 70;
					}
				}
					// if ghost was eaten
				for ( int i = 0; i < Constants.NUM_GHOSTS; i++ )
					utility += game.wasGhostEaten(ghosts[i]) ? game.getGhostCurrentEdibleScore() : 0;
				
				// prepare next move
				if ( !game.isJunction(game.getPacmanCurrentNodeIndex()) )
					nextMove = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), nextMove)[0];
			}
			
		} while ( !terminated && !game.isJunction(game.getPacmanCurrentNodeIndex()) /*&& game.getPowerPillIndex(game.getPacmanCurrentNodeIndex()) > -1 */);
		
		// if pacman has won/died or we are at the depth limit
		if ( ( currentDepth == depthLimit ) || terminated )
			return utility /*- distance*/;
		// otherwise we are in a junction
		else {
			// if we already visited the junction, stop here
			if ( visitedJunctions.get( game.getPacmanCurrentNodeIndex() ) ) {
				//if ( currentDepth <= 5 ) return utility - 5000.0f;
				return utility - 10.0f /*- distance*/;
			}
			// else, mark the junction as visited
			else /*if ( game.isJunction(game.getPacmanCurrentNodeIndex() ) )*/
				visitedJunctions.set( game.getPacmanCurrentNodeIndex() );
			
			// get the possible moves at the junction
			MOVE[] possibleMoves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), nextMove);
			float maxUtility = MINSCORE;
			BitSet visitedBranchJunctions = visitedJunctions;
			for ( int i = 0; i < possibleMoves.length; i++ ) {
				float tempUtility = MoveInDirection( game.copy(), possibleMoves[i], currentDepth + 1, depthLimit, utility, visitedBranchJunctions );
				maxUtility = Math.max(tempUtility, maxUtility);
			}

			return ( utility /*- distance*/ ) + ( /*0.5f * */maxUtility / (float)currentDepth );
		}
	}
	
	private EnumMap<GHOST,MOVE> GetGhostMoves( Game game ) {
		EnumMap<GHOST,MOVE> ghostMoves = new EnumMap<GHOST,MOVE>(Constants.GHOST.class);
		
		for ( int i = 0; i < Constants.NUM_GHOSTS; i++ ) {
			// if ghost in lair
			if ( game.getGhostLairTime(ghosts[i] ) > 0 )
				ghostMoves.put( ghosts[i], MOVE.NEUTRAL );
			// if ghost edible
			else if ( game.isGhostEdible(ghosts[i]) ) {
				int ghostNode = game.getGhostCurrentNodeIndex(ghosts[i]);
				if ( !game.isJunction(ghostNode ) )
					ghostMoves.put(ghosts[i], MOVE.NEUTRAL );
				else
					ghostMoves.put(ghosts[i], game.getApproximateNextMoveAwayFromTarget(
						ghostNode, 
						game.getPacmanCurrentNodeIndex(), 
						game.getGhostLastMoveMade(ghosts[i]), DM.PATH));
			}
			// if ghost threat
			else {
				int ghostNode = game.getGhostCurrentNodeIndex(ghosts[i]);
				// if ghost at corridor, get only possible move
				if ( !game.isJunction(ghostNode) )
					ghostMoves.put(ghosts[i], MOVE.NEUTRAL );
				// if ghost at junction
				else {
						ghostMoves.put(ghosts[i], game.getApproximateNextMoveTowardsTarget(
							ghostNode, 
							game.getPacmanCurrentNodeIndex(), 
							game.getGhostLastMoveMade(ghosts[i]), DM.PATH));
				}
			}
		}
		return ghostMoves;
	}
}
