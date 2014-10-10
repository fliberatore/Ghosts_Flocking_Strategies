package pacman.controllers.examples;

import java.awt.Color;
import java.util.ArrayList;

import pacman.controllers.Controller;
import pacman.game.Constants;
import pacman.game.Constants.GHOST;
import pacman.game.Game;
import pacman.game.GameView;
import pacman.game.Constants.MOVE;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class ICEP_feat_Spooks extends Controller<MOVE>
{
	Game 	game;
	int[]	pacman;	//Distance of pacman from each node - updated each game tick
	int[]	closest; //Which ghost is nearest to each node
	int[][]	ghosts; //Distance of each ghost from each node - updated each game tick
	float[][] odds; //The odds for each ghost to reach each node
	float[] block; //The combined odds for any ghost to block each node
	int[]	bestPath; //The shortest route to the highest scoring node

	int[] pacmanP;
	int[] Dummy;

	int distc=0;

	int edibleTime=0;
	boolean setGhost=false;

	private static final boolean PATH_DEBUG = true;
	private static final boolean ZONE_DEBUG = true;
	private static final float	CONTRAST = 0.75f;

	private static final int	CUTOFF = 350;
	private static final int	ESCAPE_PRIORITY = 50;

	//Place your game logic here to play the game as Ms Pac-Man
	public MOVE getMove(Game game,long timeDue)
	{
		this.game = game;
		float [] scores = scoreNodes();
		int best = bestNode(scores);
		int dir = bestDir(best, scores);

		/*
		if (PATH_DEBUG && d(dir) != -1)
			for (int i=0; i<bestPath.length; i++)
				GameView.addPoints(game, new Color(1-block[bestPath[i]]*CONTRAST, 0, 0), bestPath[i]);
			 */

		return d(dir);
	}

	private MOVE d(int d){
		if(d==0)return MOVE.UP;
		else if(d==1)return MOVE.RIGHT;
		else if(d==2)return MOVE.DOWN;
		else return MOVE.LEFT;
	}

	private int d(MOVE d){
		if(d==MOVE.UP)return 0;
		else if(d==MOVE.RIGHT)return 1;
		else if(d==MOVE.DOWN)return 2;
		else return 3;
	}

	private GHOST g(int g){
		if(g==0)return GHOST.BLINKY;
		else if(g==1)return GHOST.PINKY;
		else if(g==2)return GHOST.INKY;
		else return GHOST.SUE;
	}

	private MOVE getReverse(MOVE move){
		if(move==MOVE.UP)return MOVE.DOWN;
		else if(move==MOVE.DOWN)return MOVE.UP;
		else if(move==MOVE.LEFT)return MOVE.RIGHT;
		else return MOVE.LEFT;
	}

	/*
	 * Returns the score for each node
	 * -1 means unsafe, anything positive means safe, the higher the score the better
	 */
	private float[] scoreNodes() {
		pacman = new int[game.getNumberOfNodes()];
		walk(game.getPacmanCurrentNodeIndex(), 0, -1);
		pacman[game.getPacmanCurrentNodeIndex()] = 0;
		ghosts = new int[Constants.NUM_GHOSTS][game.getNumberOfNodes()];
		closest = new int[game.getNumberOfNodes()];
		odds = new float[Constants.NUM_GHOSTS][game.getNumberOfNodes()];
		block = new float[game.getNumberOfNodes()];

		Dummy = new int[game.getNumberOfNodes()];
		pacmanP = new int[game.getNumberOfNodes()];

		for (int g=0; g<Constants.NUM_GHOSTS; g++) {
			if (game.getGhostLairTime(g(g)) > 0)
				ghostWalk(g, game.getGhostInitialNodeIndex(), d(2), game.getGhostLairTime(g(g)), 1f);
			else
				ghostWalk(g, game.getGhostCurrentNodeIndex(g(g)), getReverse(game.getGhostLastMoveMade(g(g))), 0, 1f);
		}

		float[] scores = new float[game.getNumberOfNodes()];
		for (int node=0; node<scores.length; node++) {
			scores[node] = -1;
			if (game.getNeighbouringNodes(node).length > 0) {
				int nearest = -1;

				//Find nearest Hunting Ghost
				for (int g=0; g<Constants.NUM_GHOSTS; g++) {
					int time = ghosts[g][node];
					if (time > game.getGhostEdibleTime(g(g)) && (nearest == -1 || time < ghosts[nearest][node]))
						nearest = g;
				}
				closest[node] = nearest;
				if ((node == game.getPacmanCurrentNodeIndex() || pacman[node] > 0) && (nearest == -1 || ghosts[nearest][node] - pacman[node] > Constants.EAT_DISTANCE)) //Safe
					scores[node] = 1;
			}
		}

		for (int g=0; g<Constants.NUM_GHOSTS; g++) {
			if (game.getGhostLairTime(g(g)) == 0 && !game.isGhostEdible(g(g)) && scores[ game.getNeighbour(game.getGhostCurrentNodeIndex(g(g)) ,getReverse(game.getGhostLastMoveMade(g(g))) ) ] == 1)
				reGhostScore(g, game.getGhostCurrentNodeIndex(g(g)), getReverse(game.getGhostLastMoveMade(g(g))), 0, scores, 1f);
		}



		//Walk from the pacman position and remove any "safe" nodes that we can only reach via unsafe nodes
		boolean[] reachable = new boolean[game.getNumberOfNodes()];
		mark(game.getPacmanCurrentNodeIndex(), scores, reachable);
		//Remove unreachable nodes
		for (int node = 0; node < scores.length; node++){
			if (!reachable[node])
				scores[node] = -1;
			/*else if (ZONE_DEBUG)
				GameView.addPoints(game, new Color(0, 1-block[node]*CONTRAST, 0), node);*/

			float chance = 1f;
			for (int g=0; g<Constants.NUM_GHOSTS; g++)
				chance *= (1f-odds[g][node]);
			block[node] = 1f - chance;
		}



		//Score escape routes - these are the safe junction nodes closest to the event horizon
		int []eventHorizon = getEventHorizon(scores);

		int escapeRoutes = eventHorizon.length - Constants.NUM_GHOSTS;
		if (escapeRoutes < 0)
			escapeRoutes = 0;
		
		if(game.getGhostLairTime(g(2))>0 && game.getCurrentLevel()%4==2 && game.getGhostLairTime(g(3))>0){
			if(scores[946]!=-1 && game.getPacmanNumberOfLivesRemaining()%2==0)scores[946] += (100+ghosts[closest[946]][946])*(1-block[946]);
			else if(scores[895]!=-1 && game.getPacmanNumberOfLivesRemaining()%2==1)scores[895] += (100+ghosts[closest[895]][895])*(1-block[895]);
		}


		for (int n: eventHorizon) {
			int jn = nearestJunction(n, scores);
			if (jn != -1) {
				float priority = ESCAPE_PRIORITY;
				if (closest[jn] != -1)
					priority += ghosts[closest[jn]][jn];
				scores[jn] += priority*(1-block[jn])/(escapeRoutes + 1);
			}
		}

		//Score Pills
		for (int p: game.getActivePillsIndices())
			if (reachable[p]) scores[p] += Constants.PILL;


		//
		edibleTime = (int)(Constants.EDIBLE_TIME*(Math.pow(Constants.EDIBLE_TIME_REDUCTION,game.getCurrentLevel())));
		int Num_Ghost=1;
		boolean checkLair=false;
		boolean checkEdible = false;
		boolean checkInitial = false;
		int k=0;
		int lTime=0;
		int min;
		int gnum = -1;
		int[] saveNum = new int[4];
		int[] saveDist = new int[4];

		int[] saveGL = new int[4];
		int checkNumGhost=0;
		int dad=0;

		//int cutTime = (int)( (Constants.LEVEL_LIMIT)*(1f-2f/(7-game.getNumberOfActivePowerPills())) - edibleTime);
		lTime = (int)(Constants.COMMON_LAIR_TIME*(Math.pow(Constants.LAIR_REDUCTION,game.getCurrentLevel())));

		walkBlock(game.getGhostInitialNodeIndex(), 0, -1);
		int cut=0;
		int checkNeighbour=0;
		int checkDir=0;
		int goodDist = 0;
		int minDist = 0;
		int[] minNum= new int[4];
		int pg=game.getPacmanCurrentNodeIndex();
		int least=0;
		for(int g=0;g<4;g++){
			if(game.getGhostLairTime(g(g))>0){
				checkLair=true;
				break;
			}
			else if(game.isGhostEdible(g(g))){
				checkEdible=true;
				break;
			}
			else if(game.getNeighbour(game.getGhostCurrentNodeIndex(g(g)), getReverse(game.getGhostLastMoveMade(g(g))))==-1){
				checkInitial = true;
				break;
			}
		}
		if(!checkLair && !checkEdible && !checkInitial)
		for(int g=0; g<4; g++){

			for(int i=0; i<game.getNumberOfNodes(); i++){
				pacmanP[i]=0;
			}
			walkForPowerPill(pg, 0, -1);
			pacmanP[pg]=0;
			min = Integer.MAX_VALUE;
			for(int gg=0; gg<4; gg++){
				if(pacmanP[game.getGhostCurrentNodeIndex(g(gg))]<min && saveNum[gg]!=-1 && pacmanP[game.getGhostCurrentNodeIndex(g(gg))]!=0){
					min = pacmanP[game.getGhostCurrentNodeIndex(g(gg))];
					gnum = gg;
				}
			}

			checkNeighbour = game.getNeighbour(game.getGhostCurrentNodeIndex(g(gnum)), getReverse(game.getGhostLastMoveMade(g(gnum))));
			checkDir = pacmanP[game.getGhostCurrentNodeIndex(g(gnum))] - pacmanP[checkNeighbour];
			if(checkDir == -1){
				if(!setGhost)cut = pacmanP[game.getGhostCurrentNodeIndex(g(gnum))] + goodDist + least;
				else cut = pacmanP[game.getGhostCurrentNodeIndex(g(gnum))] + least*2;
			}
			else cut = (pacmanP[game.getGhostCurrentNodeIndex(g(gnum))]+goodDist+least)/3;
			distc=0;

			setGhost=false;
			saveGL[gnum] = nJunction(game.getGhostCurrentNodeIndex(g(gnum)),0,d( getReverse(game.getGhostLastMoveMade(g(gnum))) ),cut);
			least = cut - distc;
			if(checkDir != -1)least *= 3;
			goodDist = pacmanP[saveGL[gnum]]+least - Constants.EAT_DISTANCE;

			if(!setGhost || checkDir != -1)pg=saveGL[gnum];
			saveNum[gnum]=-1;
			saveDist[g]=goodDist;
			k++;
			if(g==0){
				minDist = saveDist[g];
			}
			if(lTime+Dummy[saveGL[gnum]]+minDist-least-Constants.EAT_DISTANCE <= goodDist){
				saveDist[g]=100000;
			}

			minNum[g] = gnum;
			if(k==4 && edibleTime>=dad+saveDist[g])
				checkNumGhost = 4;

			else if(k==4){
				if(750*(4-game.getNumberOfActivePowerPills()) <= game.getCurrentLevelTime() && edibleTime<dad+saveDist[g]){
					checkNumGhost = 3;
					saveDist[g]=0;
					break;
				}
			}

			else if(k==3){
				if(750*(5-game.getNumberOfActivePowerPills()) <= game.getCurrentLevelTime() && edibleTime<dad+saveDist[g]){
					checkNumGhost = 2;
					saveDist[g]=0;
					break;
				}
			}

			dad+=saveDist[g];
		}

		Num_Ghost=1;
		int allDist=0;
		for(int g=0; g<4; g++){
			allDist += saveDist[g];
			saveNum[g]=0;
			saveDist[g]=0;

			if(ghosts[g][game.getPacmanCurrentNodeIndex()]<=12){
		//		Num_Ghost+=Num_Ghost;
			}
		}
		
		
		//
		boolean allSafe = true;
		bestPath = null;
		if(!checkLair && !checkEdible && !checkInitial && closest[game.getPacmanCurrentNodeIndex()]!=-1 && 
				ghosts[closest[game.getPacmanCurrentNodeIndex()]][game.getPacmanCurrentNodeIndex()]>Constants.EAT_DISTANCE+2 &&  escapeRoutes>=4){
			for(int p: game.getActivePowerPillsIndices()) {
				if(reachable[p]){
					int[] travelled = new int[game.getNumberOfNodes()];
					float[] pathScore = new float[game.getNumberOfNodes()];
					for (int i=0; i<pathScore.length; i++) {
						pathScore[i] = -1;
						travelled[i] = -1;
					}
					bestPath = null;
					travel(travelled, scores, pathScore, p, game.getPacmanCurrentNodeIndex(), 0, scores[game.getPacmanCurrentNodeIndex()], new ArrayList<Integer>());
					if (bestPath != null && bestPath.length > 1){
						for(int i=0;i<bestPath.length;i++){
							if(closest[bestPath[i]]!=-1 && ghosts[closest[bestPath[i]]][bestPath[i]]-pacman[bestPath[i]]<Constants.EAT_DISTANCE+4){
								allSafe=false;
								break;
							}
						}
					}
				}
			}
			if(bestPath!=null && allSafe){
				for(int i=0;i<game.getNumberOfNodes();i++)
					scores[i]=1;
				scores[game.getGhostCurrentNodeIndex(g(minNum[0]))]=1000;
			}
		}


		//Score Power pill
		for (int p: game.getActivePowerPillsIndices()) {
			if (reachable[p]) {

				if (Constants.LEVEL_LIMIT-game.getCurrentLevelTime() <= game.getNumberOfActivePowerPills()*(edibleTime + 150))scores[p]+=1000000 - pacman[p]*1000;

				if(allDist <= edibleTime && !checkLair && !checkEdible && !checkInitial)scores[p] += 10000000 - pacman[p]*1000;

				if (pacman[p] > 4 || (closest[p] != -1  && ghosts[closest[p]][p] <= 5 + Constants.EAT_DISTANCE)){
					scores[p] += 50*Num_Ghost*((game.getCurrentLevelTime())/750+1) / (escapeRoutes + 1);

					if(escapeRoutes == 0 && game.getCurrentLevelTime() <= 750)
						scores[p] += 100 / (escapeRoutes + 1);
				}

				else if(Constants.LEVEL_LIMIT-game.getCurrentLevelTime() <= game.getNumberOfActivePowerPills()*(edibleTime + 150)-100 ||
						checkNumGhost>=4 ||
						(checkNumGhost>=3 && 750*(4-game.getNumberOfActivePowerPills()) <= game.getCurrentLevelTime())) /*||
						(checkNumGhost>=2 && 750*(5-game.getNumberOfActivePowerPills()) <= game.getCurrentLevelTime()))*/;
				else
					scores[p] = -1;
			}
		}


		//Score edible ghosts
		for (int g=0; g<Constants.NUM_GHOSTS; g++) {
			int target = game.getGhostCurrentNodeIndex(g(g));
			if (reachable[target] && game.isGhostEdible(g(g))) {
				int dist = 2*pacman[target]; //Chasing takes twice the current distance;
				int jn = nextJunction(g); //Find junction that ghost is heading towards
				if (jn != -1) {
					if (reachable[jn]) {
						if (pacman[jn] >= ghosts[g][jn]) //Head to jn and then chase
							dist = ghosts[g][jn] + 2*pacman[jn];
						else //Head to jn then intercept
							dist = (ghosts[g][jn] + 2*pacman[jn])/3;

						if (dist < 2*pacman[target]) //Is is quicker to chase it down or intercept
							target = jn;
						else
							dist = 2*pacman[target];
					}
				} else //PacMan must be blocking the junction - simply head towards it
					dist = 2*pacman[target]/3;

				int count=0;
				for(int d=0; d<4; d++){
					distc=0;
					if(game.getNeighbour(target, d(d)) == -1)continue;
					int jct = nJunction(target,0,d);
					if(closest[jct]!=g && closest[jct]!=-1 && pacman[target] + distc > ghosts[closest[jct]][jct]-Constants.EAT_DISTANCE-5)//5or7
						count++;
				}
				if(game.getNeighbouringNodes(target).length == count)dist=10000;

				if (dist - game.getGhostEdibleTime(g(g)) < Constants.EAT_DISTANCE) {

					if(game.getGhostEdibleTime(g(g)) - pacman[target] >= 0)
						scores[target] += (game.getGhostEdibleTime(g(g)) - pacman[target])*100*(1-block[target]);
					else
						scores[target] += 200*(1-block[target]);
				}
			}
		}

		return scores;
	}

	
	private int nJunction(int node, int dist, int dir) {
		while (!game.isJunction(node)&& !game.isPowerPillStillAvailable(node)) {
			int d = 0;
			while (game.getNeighbour(node, d(d)) == -1 || d(d) == getReverse(d(dir)))
				d++;
			dir = d;
			node = game.getNeighbour(node, d(d));
			distc++;
		}
		return node;
	}

	private int nJunction(int node, int dist, int dir, int cut) {
		while (!game.isJunction(node)&& distc < cut) {
			int d = 0;
			while (game.getNeighbour(node, d(d)) == -1 || d(d) == getReverse(d(dir)))
				d++;
			dir = d;
			node = game.getNeighbour(node, d(d));
			distc++;
			if(!game.isJunction(node)&&dist!=0){
				for(int g=0;g<4;g++){
					if(game.getGhostCurrentNodeIndex(g(g))==node)setGhost = true;
				}
			}
		}
		return node;
	}

	private void reGhostScore(int g, int node, MOVE banned, int dist, float[] scores, float chance) {

		if (dist < CUTOFF) {
			for(int ghost=0 ;ghost < 4 ;ghost++)
				if(ghost != g && ghosts[ghost][node] - (pacman[game.getGhostCurrentNodeIndex(g(g))] + dist) < Constants.EAT_DISTANCE)
					 return;

			chance /= (game.getNeighbouringNodes(node).length - 1);

			if(dist > ghosts[g][node] && dist < pacman[node]){
				scores[node] = -1;
			}
			else{
				for (int d=0; d<4; d++) {
					int next = game.getNeighbour(node, d(d));
					if(next != -1){
						if(scores[next] == 1 && odds[g][next] < chance){
							odds[g][next] = chance;
						}
						else{
							scores[node] = 1;
							odds[g][node] = 0;
							if(Math.abs(ghosts[g][next] - pacman[next]) > Constants.EAT_DISTANCE)
								if (d(d) != banned)
									reGhostScore(g, next, getReverse(d(d)), dist+1, scores, chance);
						}
					}
				}
			}
		}
	}


	/*
	 * Find the next junction a ghost will reach.
	 * If we come across the pacman then return -1
	 */
	private int nextJunction(int g) {
		int node = game.getGhostCurrentNodeIndex(g(g));
		int dir = d(game.getGhostLastMoveMade(g(g)));

		while (!game.isJunction(node)) {
			if (game.getPacmanCurrentNodeIndex() == node)
				return -1;
			int d = 0;
			while (game.getNeighbour(node, d(d)) == -1 || d(d) == getReverse(d(dir)))
				d++;
			dir = d;
			node = game.getNeighbour(node, d(d));
		}

		if (game.getPacmanCurrentNodeIndex() == node)
			return -1;
		return node;
	}

	/*
	 * Find the nodes that are safe and have an unsafe neighbour
	 */
	private int [] getEventHorizon(float []scores) {
		ArrayList<Integer> edge = new ArrayList<Integer>();

		for (int n=0; n<game.getNumberOfNodes(); n++) {
			if (scores[n] >= 0) {
				boolean unsafe = false;
				for (int d=0; d<4; d++) {
					int next = game.getNeighbour(n, d(d));
					if (next != -1 && scores[next] < 0)
						unsafe = true;
				}
				if (unsafe)
					edge.add(n);
			}
		}
		int [] result = new int[edge.size()];
		for (int i=0; i<edge.size(); i++)
			result[i] = edge.get(i);
		return result;
	}

	/*
	 * Find the nearest junction to this node in the safe zone
	 */
	private int nearestJunction(int node, float[] scores) {
		int prev = -1;
		while (!game.isJunction(node)) {
			int d = 0;
			while (game.getNeighbour(node, d(d)) == -1 || game.getNeighbour(node, d(d)) == prev || scores[game.getNeighbour(node, d(d))] == -1) {
				d++;
				if (d == 4)
					return -1;
			}
			prev = node;
			node = game.getNeighbour(node, d(d));
		}
		return node;
	}

	private void mark(int node, float[] scores, boolean[] reachable) {
		reachable[node] = true;
		for (int d=0; d<4; d++) {
			int next = game.getNeighbour(node, d(d));
			if (next != -1 && !reachable[next] && scores[next] >= 0)
				if(Math.abs(pacman[node] - pacman[next]) <= 1 || Math.max(pacman[node] ,pacman[next]) > closest[next])
					mark(next, scores, reachable);
		}
	}


	/*
	 * Walk the maze and record the distance to get to each node
	 * Hunting ghosts block the path
	 */
	private void walk(int node, int dist, int dir) {
		if (dist < CUTOFF && (pacman[node] == 0 || dist < pacman[node])) {
			pacman[node] = dist;
			if (!isBlocked(node, dist, dir))
				for (int d=0; d<4; d++) {
					int next = game.getNeighbour(node, d(d));
					if (next != -1)
						walk(next, dist+1, d);
				}
		}
	}

	private void walkForPowerPill(int node, int dist, int dir) {
		//if (dist < edibleTime*2/3 && (pacmanP[node] == 0 || dist < pacmanP[node])) {
		if (dist < CUTOFF && (pacmanP[node] == 0 || dist < pacmanP[node])) {
			pacmanP[node] = dist;
				for (int d=0; d<4; d++) {
					int next = game.getNeighbour(node, d(d));
					if (next != -1){
						walkForPowerPill(next, dist+1, d);
					}
				}
		}
	}

	private void walkBlock(int node, int dist, int dir) {
		if (dist < CUTOFF && (Dummy[node] == 0 || dist < Dummy[node])) {
			Dummy[node] = dist;
				for (int d=0; d<4; d++) {
					int next = game.getNeighbour(node, d(d));
					if (next != -1)
						walkBlock(next, dist+1, d);
				}
		}
	}

	/*
	 * Walk the maze as a ghost and record the distance to each node and the odds of blocking it
	 */
	private void ghostWalk(int g, int node, MOVE banned, int dist, float chance) {
		if (dist < CUTOFF && (ghosts[g][node] == 0 || dist < ghosts[g][node])) {
			ghosts[g][node] = dist;
			if (dist < game.getGhostEdibleTime(g(g)))
				dist++;
			else
				odds[g][node] = chance;
			chance /= (game.getNeighbouringNodes(node).length - 1);
			for (int d=0; d<4; d++) {
				int next = game.getNeighbour(node, d(d));
				if (next != -1 && d(d) != banned)
					ghostWalk(g, next, getReverse(d(d)), dist+1, chance);
			}
		}
	}

	/*
	 * Power pills and hunter ghosts block the maze walk
	 */
	private boolean isBlocked(int node, int dist, int dir) {
		int p = game.getPowerPillIndex(node);
		if (p != -1 && game.isPowerPillStillAvailable(p))
			return true;
		for (int g=0; g<Constants.NUM_GHOSTS; g++)
			if (game.getGhostEdibleTime(g(g)) < dist && game.getGhostCurrentNodeIndex(g(g)) == node && game.getGhostLastMoveMade(g(g)) != d(dir))
				return true;
		return false;
	}

	/*
	 * Returns the id of the highest scoring node
	 */
	private int bestNode(float[] scores) {
		int best = -1;
		for (int i=0; i<scores.length; i++)
			if (scores[i] >= 0 && (best == -1 || scores[i] > scores[best]) && i != game.getPacmanCurrentNodeIndex())
				best = i;

		return best;
	}

	/*
	 * Travel the maze looking for the next node that is nearer to the destination node.
	 * In the cases where we have more than one "shortest" route, the one with the most point is taken.
	 */
	private void travel(int[] travelled, float [] scores, float [] pathScores, int to, int node, int dist, float score, ArrayList<Integer> path) {
		if (scores[node] < 0 || dist > 500 || (travelled[node] != -1 && (dist > travelled[node] || (dist == travelled[node] && pathScores[node] > score))))
			return;

		travelled[node] = dist;
		pathScores[node] = score;
		int ix = path.size();
		path.add(ix, node);
		if (node != to) {
			//Continue on paths nearer to the destination
			for (int d=0; d<4; d++) {
				int next = game.getNeighbour(node, d(d));
				if (next != -1)
					travel(travelled, scores, pathScores, to, next, dist+1, score+scores[next], path);
			}
		} else {
			bestPath = new int[path.size()];
			for (int i=0; i<path.size(); i++)
				bestPath[i] = path.get(i);
		}
		path.remove(ix);
	}

	/*
	 * Returns the direction to head safely to the given node
	 */
	private int bestDir(int to, float[] scores) {
		if (to == -1)
			return -1;

		//Walk the safe nodes (score >= 0) and find the path with the shortest distance and highest score
		int[] travelled = new int[game.getNumberOfNodes()];
		float[] pathScore = new float[game.getNumberOfNodes()]; //Total score of all nodes on path
		for (int i=0; i<pathScore.length; i++) {
			pathScore[i] = -1;
			travelled[i] = -1;
		}
		bestPath = null;
		travel(travelled, scores, pathScore, to, game.getPacmanCurrentNodeIndex(), 0, scores[game.getPacmanCurrentNodeIndex()], new ArrayList<Integer>());
		if (bestPath != null && bestPath.length > 1)
			for (int d=0; d<4; d++)
				if (game.getNeighbour(bestPath[0], d(d)) == bestPath[1])
					return d;
		System.out.printf("Failed to find direction from %d (%d, %d) to %d (%d, %d)\n",
				game.getPacmanCurrentNodeIndex(),game.getNodeXCood(game.getPacmanCurrentNodeIndex()), game.getNodeYCood(game.getPacmanCurrentNodeIndex()), to, game.getNodeXCood(to), game.getNodeYCood(to));
		return -1;
	}
}