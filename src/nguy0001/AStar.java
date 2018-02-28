package nguy0001;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import nguy0001.GridSquare;
import nguy0001.GridComparator;

public class AStar {
	//______________________________________________
	//Tree structure based on adjacentGridsToShip ArrayList…
	//index0:first adjacent grid, index1:second adjacent grid, etc…
	//
	//gridChildren = current children
	//
	/**
	 * Performs a tree building operation for the ship with its adjacent grids
	 * @param adjacentGridsToShip grids that are adjacent to the ship, received from the getAdjacent Method in GridSquare class
	 * @return The ArrayList representation of a tree structure of the ship with its children
	 */
	public static ArrayList<ArrayList<GridSquare>> getAdjacentTree(ArrayList<GridSquare> adjacentGridsToShip) {
		ArrayList<ArrayList<GridSquare>> currentChildren = new ArrayList<ArrayList<GridSquare>>();
		ArrayList<GridSquare> temp = new ArrayList<GridSquare>();

		//Add all children to index i 
		for(int i = 0; i < adjacentGridsToShip.size(); i++) {
			ArrayList<GridSquare> adjacentToGridI = GridSquare.getAdjacent(AnthonyModelTeamClient.grid,adjacentGridsToShip.get(i));
			if(adjacentToGridI.isEmpty()) {
				System.err.println("Error: adjacentToGridI is empty, it should not be.");
			}
			else {
				for(int k = 0; k < adjacentToGridI.size(); k++) {
					//System.out.println(adjacentToGridI.get(k).center.toString());
					temp.add(adjacentToGridI.get(k));
				}
				currentChildren.add(temp);
				//System.out.println("Added to currentChildren");
			}
		}
		return currentChildren;
	}


	//find smallest value, compare the smallest child to the previous children, if smallest, then add gridSquare to queue and continue
	/**
	 * Performs the A* Method 
	 * @param currentChildren should be using the getAdjacentTree method in AStar class
	 * @return A priority queue with the solution path
	 */
	public static PriorityQueue<GridSquare> newAStarMethod(ArrayList<ArrayList<GridSquare>> currentChildren){
		//Necessary variables for method
		Comparator<GridSquare> comparator = new GridComparator();
		PriorityQueue<GridSquare> queue = new PriorityQueue(comparator);
		boolean solutionFound = false;
		int level = 0;
		ArrayList<ArrayList<GridSquare>> previousChildren = new ArrayList<ArrayList<GridSquare>>();
		GridSquare lowestVal = null;
		int currentindex = 0;
		int parentindex = 0;
		int nextLowestparentindex = 0;


		while(!solutionFound){
			
			//If we pop the top of the queue and see a gridsquare with pathcost 0.0, meaning its the goal, then change flag to break loop
			if(!queue.isEmpty() && queue.peek().pathCost == 0.0) {
				solutionFound = true;
				//break;
			}
			else{
				//Don’t compare to anything, so lowest val is temporarily added to queue
				if(level == 0) {
					//System.out.println("(if)Level: " + level + "Children size: " + currentChildren.get(level).size());
					for(int j = 0; j < currentChildren.get(level).size(); j++) {
						if(j == 0) {
							lowestVal = currentChildren.get(level).get(j);
						}
						else {
							if(currentChildren.get(level).get(j).pathCost < lowestVal.pathCost) {
								lowestVal = currentChildren.get(level).get(j);
								//get index of lowest pathcost GridSquare
								currentindex = j;
							}
						}
					}
					queue.add(lowestVal);
					//assign the current children tree to have adjacent children to the lowestVal gridsquare
					previousChildren = currentChildren;
					currentChildren = getAdjacentTree(GridSquare.getAdjacent(AnthonyModelTeamClient.grid,currentChildren.get(level).get(currentindex)));
					level++;
				}
				else {
					//compare to previous children
					//System.out.println("(else)Level: " + level + "Children size: " + currentChildren.get(level).size());
					for(int j = 0; j < currentChildren.get(level).size(); j++) {
						if(j == 0) {
							lowestVal = currentChildren.get(level).get(j);
						}
						else {
							if(currentChildren.get(level).get(j).pathCost < lowestVal.pathCost) {
								lowestVal = currentChildren.get(level).get(j);
								//get value of lowest pathcost GridSquare 
								currentindex = j;
							}
						}
					}
					queue.add(lowestVal);
					
					//If the lowest value is greater then the second lowest previous child, then remove from list and go up a level…
					//TODO: If we make a priority queue of the lowest values, we could just remove the top member and get the next lowest.
					//temporary work around---------------------
					ArrayList<ArrayList<GridSquare>> nextLowestChild = previousChildren;
					nextLowestChild.get(level).remove(parentindex);//TODO: HERE
					
					//Refers to the new lowest from the previous children
					GridSquare newLow = null;
					for(int j = 0; j < nextLowestChild.get(level).size(); j++) { 
						if(j == 0) {
							newLow = nextLowestChild.get(level).get(j);
						}
						else {
							if(nextLowestChild.get(level).get(j).pathCost < newLow.pathCost) {
								newLow = nextLowestChild.get(level).get(j);
								//get value of next lowest pathcost GridSquare 
								nextLowestparentindex = j;
							}
						}
					}
					//-------------------------
					level++;
					if(lowestVal.pathCost > newLow.pathCost) {
						queue.remove(lowestVal);
						//Remove the old "lowest" gridsquare since its lowest child value is greater than a previous child
						previousChildren.remove(parentindex);
						parentindex = nextLowestparentindex;
						currentChildren = previousChildren;
						level = level - 1;
						queue.add(newLow);

					}
				}
			}

		}
		System.out.println("Level size:" + level);
		return queue;//need to return the reverse of this queue, since the top value is lowest value, so it'll try to go to that location first
	}
}
