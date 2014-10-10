package pacman.controllers.examples;

import pacman.controllers.Controller;
import pacman.controllers.examples.mixmax.*;
import pacman.game.Game;
import pacman.game.Constants.MOVE;


public class MixMaxPacMan extends Controller<MOVE> {
	
	ABCMixMax2 miniMax;

	/** 
	 * Default constructor
	 */
	public MixMaxPacMan() {
		miniMax = new ABCMixMax2( 30 );
	}
	
	public MixMaxPacMan( int mixmaxdepth ) {
		miniMax = new ABCMixMax2( 30, mixmaxdepth );
	}
	
	public MOVE getMove( Game game, long timeDue ) {
		MOVE nextMove = miniMax.Execute(game);
		
		return nextMove;
	}
}
