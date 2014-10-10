package pacman.controllers.examples;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Vector;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.GameView;


public class JSvenssonGhosts extends Controller<EnumMap<GHOST,MOVE>>
{
	
	public class Node
	{
		public int mIndex;
		public float mPositivePoints;
		public float mNegativePoints;
		public Node()
		{
			mIndex = -1;
			mPositivePoints = 0;
			mNegativePoints = 0;
		}
		public Node(int index)
		{
			mIndex = index;
			mPositivePoints = 0;
			mNegativePoints = 0;
		}
	}
	private int mCurrentLevel = -1;
	private Game mGame;
	
	private float GHOSTVALUE = 100;
	private float PPDISTANCE = 12;
	private float PACMANDISTANCEFACTOR = 0;
	
	private Vector<Node> mNodes2;
	
	private EnumMap<GHOST, MOVE> myMoves=new EnumMap<GHOST, MOVE>(GHOST.class);
	
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue)
	{
		myMoves.clear();
		
		mGame = game;
		if(mCurrentLevel != mGame.getCurrentLevel())
		{
			mCurrentLevel = mGame.getCurrentLevel();
			loadNodes();
		}
		
		for (int i = 0; i < mNodes2.size(); i++)
		{
			
			mNodes2.get(i).mPositivePoints = 0;
			mNodes2.get(i).mNegativePoints = 0;
		}
		
		updatePacMan();
		updateGhost();
		//printField();
		
		
		
		for(GHOST ghost : GHOST.values())
		{
			if(mGame.doesGhostRequireAction(ghost))
        	{
				boolean anyEdible = false;
				for(GHOST ghost2 : GHOST.values())
				{
					if(mGame.getGhostEdibleTime(ghost2) > 0)
						anyEdible = true;
				}
				
				Node highNode = new Node();

				int[] neighbourNodes = mGame.getNeighbouringNodes(mGame.getGhostCurrentNodeIndex(ghost), mGame.getGhostLastMoveMade(ghost));
				for (int i = 0; i < neighbourNodes.length; i++)
				{
					if(anyEdible == false || mGame.getGhostEdibleTime(ghost) == 0)
					{
						if(highNode.mPositivePoints - highNode.mNegativePoints < mNodes2.get(neighbourNodes[i]).mPositivePoints - mNodes2.get(neighbourNodes[i]).mNegativePoints)
						{
							highNode = mNodes2.get(neighbourNodes[i]);
						}
					}
					else
					{
						if(highNode.mPositivePoints - highNode.mNegativePoints > mNodes2.get(neighbourNodes[i]).mPositivePoints - mNodes2.get(neighbourNodes[i]).mNegativePoints)
						{
							highNode = mNodes2.get(neighbourNodes[i]);
						}
					}
				}
				myMoves.put(ghost, game.getNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),highNode.mIndex,DM.PATH));
        	}
		}
		return myMoves;
	}

	private void loadNodes()
	{

		mNodes2 = new Vector<Node>(mGame.getNumberOfNodes());
		

		for (int k = 0; k < mGame.getNumberOfNodes(); k++) 
		{
			mNodes2.add(new Node(k));
		}	
	}
	
	private void updateGhost()
	{
		Vector<Integer> nodeQuene = new Vector<Integer>();
		Vector<Integer> visitedNodes = new Vector<Integer>();
		
		for(GHOST ghost : GHOST.values())
		{
			int eatTime = mGame.getGhostEdibleTime(ghost);
	
			if(eatTime==0 && mGame.getGhostLairTime(ghost)==0)
			{
				int ghostNode = mGame.getGhostCurrentNodeIndex(ghost);
				mNodes2.get(ghostNode).mNegativePoints += GHOSTVALUE;
				
				
				visitedNodes.add(ghostNode);
				int[] neighbourNodes = mGame.getNeighbouringNodes(ghostNode);
				
				for (int j = 0; j < neighbourNodes.length; j++) 
				{
					nodeQuene.add(neighbourNodes[j]);
				}
				
				while(!nodeQuene.isEmpty())
				{
					int visitNode = nodeQuene.remove(nodeQuene.size()-1);
					visitedNodes.add(visitNode);
					int distance = mGame.getShortestPathDistance(visitNode, visitedNodes.firstElement());
					
					int distanceToPacman = mGame.getShortestPathDistance(mGame.getGhostCurrentNodeIndex(ghost),mGame.getPacmanCurrentNodeIndex());
					
					float factor = PACMANDISTANCEFACTOR / distanceToPacman;
					
					float points = (float) (GHOSTVALUE * Math.pow(0.90, distance));
					
					if(distance != 1)
						mNodes2.get(visitNode).mNegativePoints += points;
					
					neighbourNodes = mGame.getNeighbouringNodes(visitNode);
					
					boolean foundGhost = false;
					
					for(GHOST ghost2 : GHOST.values())
					{
						for (int i = 0; i < neighbourNodes.length; i++) {
							if(visitNode == mGame.getGhostCurrentNodeIndex(ghost2))
							{
								foundGhost = true;
							}
						}
					}
					
					if(GHOSTVALUE - distance*factor >= 0)
					{
						for (int j = 0; j < neighbourNodes.length; j++) 
						{
							if(!visitedNodes.contains(neighbourNodes[j]) && foundGhost == false)
							{
								nodeQuene.add(neighbourNodes[j]);
							}
						}
					}

				}
				nodeQuene.removeAllElements();
				visitedNodes.removeAllElements();
			}
//			else if(eatTime > 0)
//			{
//				visitedNodes.add(mGame.getGhostCurrentNodeIndex(ghost));
//				int[] neighbourNodes = mGame.getNeighbouringNodes(mGame.getGhostCurrentNodeIndex(ghost));
//				
//				for (int j = 0; j < neighbourNodes.length; j++) 
//				{
//					nodeQuene.add(neighbourNodes[j]);
//				}
//				
//				while(!nodeQuene.isEmpty())
//				{
//					int visitNode = nodeQuene.remove(nodeQuene.size()-1);
//					visitedNodes.add(visitNode);
//					int distance = (int) mGame.getDistance(visitNode, visitedNodes.firstElement(), DM.PATH);
//					
//					float points = (float) (GHOSTVALUE * Math.pow(0.95, distance));
//					
//					int distanceToPacman = mGame.getShortestPathDistance(mGame.getGhostCurrentNodeIndex(ghost),mGame.getPacmanCurrentNodeIndex());
//					
//					float factor = PACMANDISTANCEFACTOR / distanceToPacman;
//					
//					mNodes2.get(visitNode).mPositivePoints += points;
//						
//					neighbourNodes = mGame.getNeighbouringNodes(visitNode);
//					
//					if(GHOSTVALUE - distance*factor >= 0)
//					{
//						for (int j = 0; j < neighbourNodes.length; j++) 
//						{
//							if(!visitedNodes.contains(neighbourNodes[j]))
//							{
//								nodeQuene.add(neighbourNodes[j]);
//							}
//						}
//					}
//				}
//				nodeQuene.removeAllElements();
//				visitedNodes.removeAllElements();
//			}
		}
	}
	
	public void setSettings(float powerpill, float pill, float ghost, float a, float b)
	{
		GHOSTVALUE = ghost;
		PPDISTANCE = pill;
		PACMANDISTANCEFACTOR = powerpill;
	}
	
	private void updatePacMan()
	{
		for (int i = 0; i < mNodes2.size(); i++)
		{
			mNodes2.get(i).mPositivePoints = 0;
			mNodes2.get(i).mNegativePoints = 0;
		}
		Vector<Integer> nodeQuene = new Vector<Integer>();
		Vector<Integer> visitedNodes = new Vector<Integer>();
		
		
		visitedNodes.add(mGame.getPacmanCurrentNodeIndex());
		int[] neighbourNodes = mGame.getNeighbouringNodes(mGame.getPacmanCurrentNodeIndex());
		
		for (int j = 0; j < neighbourNodes.length; j++) 
		{
			nodeQuene.add(neighbourNodes[j]);
		}
		
		int[] activePowerPills = mGame.getActivePowerPillsIndices();
		
		int closestPP = 0;
		
		if(activePowerPills.length != 0)
			closestPP = mGame.getClosestNodeIndexFromNodeIndex(mGame.getPacmanCurrentNodeIndex(), activePowerPills, DM.PATH);
		
		int ppDist = mGame.getShortestPathDistance(mGame.getPacmanCurrentNodeIndex(), closestPP);
		float factor = (float) (1.0/PPDISTANCE * ppDist);
		
		if(activePowerPills.length == 0)
			factor = 0.98f;
		
		mNodes2.get(mGame.getPacmanCurrentNodeIndex()).mPositivePoints = 255;
		while(!nodeQuene.isEmpty())
		{
			int visitNode = nodeQuene.remove(nodeQuene.size()-1);
			visitedNodes.add(visitNode);
			int distance = (int) mGame.getDistance(visitNode, visitedNodes.firstElement(), DM.PATH);
			
			boolean eatableGhost = false;
			
			for(GHOST ghost : GHOST.values())
			{
				int eatTime = mGame.getGhostEdibleTime(ghost);
				if(eatTime > 0)
					eatableGhost = true;
			}
			
			if(factor > 0.85)
			{
				if(factor > 0.98)
					factor = 0.98f;
				float points = (float) (200 * Math.pow(factor, distance));
				if(mNodes2.get(visitNode).mPositivePoints == 0)
				{
					if(eatableGhost == false)
						mNodes2.get(visitNode).mPositivePoints += points;
					else
						mNodes2.get(visitNode).mNegativePoints += points;
				}
			}
			else
			{
				float fac = factor;
				fac = 1.5f - fac;
				if(fac > 0.98)
					fac = 0.98f;
				float points = (float) (200 * Math.pow(fac , distance));
				mNodes2.get(visitNode).mNegativePoints += points;
			}
			
			neighbourNodes = mGame.getNeighbouringNodes(visitNode);
			

			for (int j = 0; j < neighbourNodes.length; j++) 
			{
				if(!visitedNodes.contains(neighbourNodes[j]))
				{
					nodeQuene.add(neighbourNodes[j]);
				}
			}
		}
	}
	
	private void printField()
	{
		for (int i = 0; i < mNodes2.size(); i++)
		{
			float[] color = new float[3];
			int pos = (int)mNodes2.get(i).mPositivePoints >= 255 ? 255 : (int)mNodes2.get(i).mPositivePoints;
			int neg = (int)mNodes2.get(i).mNegativePoints >= 255 ? 255 : (int)mNodes2.get(i).mNegativePoints;
			if(pos != 0 || neg != 0)
			{
				Color.RGBtoHSB(neg, pos, 0, color);
				int[] node = new int[1];
				node[0] = mNodes2.get(i).mIndex;
				GameView.addPoints(mGame, Color.getHSBColor(color[0], color[1], color[2]), node);
			}
		}
	}
}