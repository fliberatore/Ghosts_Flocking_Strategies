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

public class JSvenssonPacMan extends Controller<MOVE>
{
	public class Node
	{
		public int mIndex;
		public float mPositivePoints;
		public float mNegativePoints;
		public Node()
		{
			mIndex = 0;
			mPositivePoints = -1000;
			mNegativePoints = 1000;
		}
		public Node(int index)
		{
			mIndex = index;
			mPositivePoints = 0;
			mNegativePoints = 0;
		}
	}
	
	private float POWERPILL = 40;
	private float PILL = 4;
	private float GHOSTVALUE = 300;
	private float EDIBLEVALUE = 90;
	private float JUNCTIONVALUE = 120;
	private float ANTTRAIL = 15;
	private float GHOSTDISTANCE = 10;
	
	private int mCurrentLevel = -1;
	private int lastNode = 0;
	private Game mGame;
	
	private Vector<Node> mNodes;
	
	public MOVE getMove(Game game, long timeDue) 
	{
		mGame = game;
		if(mCurrentLevel != mGame.getCurrentLevel())
		{
			mCurrentLevel = mGame.getCurrentLevel();
			loadNodes();
		}
	
		
		updateGhost();
		updatePills();
		updatePacMan();
		//printField();
		
		
		int currentNodeIndex = mGame.getPacmanCurrentNodeIndex();
		
		int[] neighbourNodes = mGame.getNeighbouringNodes(currentNodeIndex);
		
		Node highNode = new Node();
		
		
		for (int i = 0; i < neighbourNodes.length; i++)
		{
			if(highNode.mPositivePoints - highNode.mNegativePoints < mNodes.get(neighbourNodes[i]).mPositivePoints - mNodes.get(neighbourNodes[i]).mNegativePoints)
			{
				highNode = mNodes.get(neighbourNodes[i]);
			}
		}
		
		for (int i = 0; i < mNodes.size(); i++) 
		{
			mNodes.get(i).mPositivePoints = 0;
			mNodes.get(i).mNegativePoints = 0;
		}
		
		return mGame.getNextMoveTowardsTarget(mGame.getPacmanCurrentNodeIndex(),highNode.mIndex,DM.PATH);
	}
	
	public void setSettings(float powerpill, float pill, float ghost, float edible, float junction)
	{
		
		POWERPILL = powerpill;
		PILL = pill;
		GHOSTVALUE = ghost;
		EDIBLEVALUE = edible;
		JUNCTIONVALUE = junction;
	}
	
	private void loadNodes()
	{
		mNodes = new Vector<Node>(mGame.getNumberOfNodes());
		
		for (int i = 0; i < mGame.getNumberOfNodes(); i++) 
		{
			mNodes.add(new Node(i));
		}
	}
	
	private void updatePills()
	{
		
		int pacManCurrPos = mGame.getPacmanCurrentNodeIndex();
		
		int[] activePills = mGame.getActivePillsIndices();
		
		int[] activePowerPills = mGame.getActivePowerPillsIndices();
		
		int ghostDistance = 0;
		
		for(GHOST ghost : GHOST.values())
		{
			int dist = mGame.getShortestPathDistance(pacManCurrPos, mGame.getGhostCurrentNodeIndex(ghost));
			
			
			ghostDistance += dist;
			
			if(dist == -1)
				ghostDistance = 500;
		}
		
		ghostDistance /= 4;
		
		Vector<Integer> nodeQuene = new Vector<Integer>();
		Vector<Integer> visitedNodes = new Vector<Integer>();
		
		for (int i = 0; i < activePowerPills.length; i++)
		{
			if(ghostDistance < 60)
			{
				mNodes.get(activePowerPills[i]).mPositivePoints += POWERPILL;
				
			}
			else
			{
				mNodes.get(activePowerPills[i]).mNegativePoints += POWERPILL;
			}
			
			visitedNodes.add(activePowerPills[i]);
			int[] neighbourNodes = mGame.getNeighbouringNodes(activePowerPills[i]);
			
			for (int j = 0; j < neighbourNodes.length; j++) 
			{
				nodeQuene.add(neighbourNodes[j]);
			}
			
			while(!nodeQuene.isEmpty())
			{
				int visitNode = nodeQuene.remove(nodeQuene.size()-1);
				visitedNodes.add(visitNode);
				int distance = (int) mGame.getDistance(visitNode, visitedNodes.firstElement(), DM.PATH);
				
				float points = (float) (POWERPILL * Math.pow(0.95, distance));
				
				
				if(ghostDistance < 60)
				{
					mNodes.get(visitNode).mPositivePoints += points;
					
				}
				else
				{
					mNodes.get(visitNode).mNegativePoints += points;
				}
				
				
				neighbourNodes = mGame.getNeighbouringNodes(visitNode);
				
				if(POWERPILL - distance >= 0)
				{
					for (int j = 0; j < neighbourNodes.length; j++) 
					{
						if(!visitedNodes.contains(neighbourNodes[j]))
						{
							nodeQuene.add(neighbourNodes[j]);
						}
					}
				}

			}
			nodeQuene.removeAllElements();
			visitedNodes.removeAllElements();
			
		}
		
		for (int i = 0; i < activePills.length; i++)
		{
			float points = PILL* (mGame.getNumberOfPills() - mGame.getNumberOfActivePills())/100;
			int[] nodePath = mGame.getShortestPath(activePills[i], pacManCurrPos);
			
			for (int j = 0; j < nodePath.length; j++) 
			{
				mNodes.get(nodePath[j]).mPositivePoints += points;
				points *= 0.95;
			}
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
				mNodes.get(ghostNode).mNegativePoints += GHOSTVALUE;
				
				
				visitedNodes.add(ghostNode);
				int[] neighbourNodes = mGame.getNeighbouringNodes(ghostNode, mGame.getGhostLastMoveMade(ghost));
				
				for (int j = 0; j < neighbourNodes.length; j++) 
				{
					nodeQuene.add(neighbourNodes[j]);
				}
				
				//nodeQuene.add(ghostNode);
				
				while(!nodeQuene.isEmpty())
				{
					int visitNode = nodeQuene.remove(nodeQuene.size()-1);
					visitedNodes.add(visitNode);
					int distance = (int) mGame.getDistance(visitNode, visitedNodes.firstElement(), DM.PATH);
					
					float points = (float) (GHOSTVALUE * Math.pow(0.95, distance));
					
					if(mNodes.get(visitNode).mNegativePoints < points || mNodes.get(visitNode).mNegativePoints == 0)
						mNodes.get(visitNode).mNegativePoints = points;
					
					if(distance < 5)
						mNodes.get(visitNode).mNegativePoints += 2000;
					
					neighbourNodes = mGame.getNeighbouringNodes(visitNode);
					
					if(GHOSTVALUE - distance*GHOSTDISTANCE >= 0)
					{
						for (int j = 0; j < neighbourNodes.length; j++) 
						{
							if(!visitedNodes.contains(neighbourNodes[j]) && neighbourNodes[j] != mGame.getPacmanCurrentNodeIndex())
							{
								nodeQuene.add(neighbourNodes[j]);
							}
						}
					}

				}
				nodeQuene.removeAllElements();
				visitedNodes.removeAllElements();
			}
			else if(eatTime > 0)
			{
				int ghostNode = mGame.getGhostCurrentNodeIndex(ghost);
				if(eatTime > mGame.getShortestPathDistance(ghostNode, mGame.getPacmanCurrentNodeIndex()))
				{
					
					mNodes.get(ghostNode).mPositivePoints += EDIBLEVALUE;
					
					float points = EDIBLEVALUE;
					int[] nodePath = mGame.getShortestPath(ghostNode, mGame.getPacmanCurrentNodeIndex());
					
					for (int j = 0; j < nodePath.length; j++) 
					{
						mNodes.get(nodePath[j]).mPositivePoints += points;
						points *= 0.95;
					}
				}
			}
		}
	}
	
	private void updatePacMan()
	{
		//if(mNodes.contains(lastNode))
		
		if(lastNode >= 0 && lastNode < mNodes.size())
			mNodes.get(lastNode).mNegativePoints += ANTTRAIL;
		lastNode = mGame.getPacmanCurrentNodeIndex();
		
		int[] closeNodes = mGame.getNeighbouringNodes(mGame.getPacmanCurrentNodeIndex());
		
		if(closeNodes.length == 2)
		{
			float negpoint = mNodes.get(closeNodes[0]).mNegativePoints + mNodes.get(closeNodes[1]).mNegativePoints;
	
			if(negpoint > JUNCTIONVALUE && negpoint < 600)
			{
				Vector<Integer> nodeQuene = new Vector<Integer>();
				Vector<Integer> visitedNodes = new Vector<Integer>();
				Vector<Integer> ghostPos = new Vector<Integer>();
				
				visitedNodes.add(mGame.getPacmanCurrentNodeIndex());
				
				int[] neighbourNodes = mGame.getNeighbouringNodes(mGame.getPacmanCurrentNodeIndex());
			
				
				for(GHOST ghost : GHOST.values())
				{
					ghostPos.add(mGame.getGhostCurrentNodeIndex(ghost));
				}
				
				for (int j = 0; j < neighbourNodes.length; j++) 
				{
					nodeQuene.add(neighbourNodes[j]);
				}
				
				while(!nodeQuene.isEmpty())
				{
					int visitNode = nodeQuene.remove(nodeQuene.size()-1);
					visitedNodes.add(visitNode);
					
					
	
					if(mGame.isJunction(visitNode))
					{
						junctionPulse(visitNode);
						break;
					}
	
					int[] neighbourNodes2 = mGame.getNeighbouringNodes(visitNode);
					
					for (int j = 0; j < neighbourNodes2.length; j++) 
					{
						if(!visitedNodes.contains(neighbourNodes2[j]) && !ghostPos.contains(neighbourNodes2[j]))
						{
							nodeQuene.add(neighbourNodes2[j]);
						}
					}
				}
				nodeQuene.removeAllElements();
				visitedNodes.removeAllElements();
			}
		}
	}
	
	private void junctionPulse(int jNode)
	{
		float negPoint = 600;
		
		mNodes.get(jNode).mPositivePoints = negPoint;
			
		int[] nodePath = mGame.getShortestPath(jNode, mGame.getPacmanCurrentNodeIndex());
		
		for (int j = 0; j < nodePath.length; j++) 
		{
			mNodes.get(nodePath[j]).mPositivePoints += negPoint;
			negPoint *= 0.95;
		}

	}
	
	private void printField()
	{
		for (int i = 0; i < mNodes.size(); i++)
		{
			float[] color = new float[3];
			int pos = (int)mNodes.get(i).mPositivePoints >= 255 ? 255 : (int)mNodes.get(i).mPositivePoints;
			int neg = (int)mNodes.get(i).mNegativePoints >= 255 ? 255 : (int)mNodes.get(i).mNegativePoints;
			if(pos != 0 || neg != 0)
			{
				Color.RGBtoHSB(neg, neg, neg, color);
				int[] node = new int[1];
				node[0] = mNodes.get(i).mIndex;
				GameView.addPoints(mGame, Color.getHSBColor(color[0], color[1], color[2]), node);
			}
		}
	}
}