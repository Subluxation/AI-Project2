package nguy0001;

import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

import java.util.ArrayList;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;

public class GridSquare {
	double startX;
	double endX;
	double startY;
	double endY;
	Position center;
	
	double pathCost;
	
	public GridSquare(double startX, double endX, double startY, double endY)
	{
		this.startX = startX;
		this.endX = endX;
		
		this.startY = startY;
		this.endY = endY;
		
		this.center = new Position((endX + startX) / 2, (endY - startY) / 2);
	}
	
	public void calculatePathCost(Toroidal2DPhysics space, AbstractObject goal, Ship ship)
	{
		this.pathCost = space.findShortestDistance(ship.getPosition(), center) + space.findShortestDistance(center, goal.getPosition());
	}
	
	public boolean containsGoal(Toroidal2DPhysics space, AbstractObject goal)
	{
		if (space.findShortestDistance(center, goal.getPosition()) <= 10)
			return true;
		else
			return false;
	}
	
	public boolean containsShip(Toroidal2DPhysics space, Ship ship)
	{
		if (space.findShortestDistance(center, ship.getPosition()) <= 10)
			return true;
		else
			return false;
	}
	
	public boolean isEmpty(Toroidal2DPhysics space)
	{
		return space.isLocationFree(center, 10);
	}
	
}
