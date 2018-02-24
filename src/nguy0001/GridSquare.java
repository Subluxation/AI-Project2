package nguy0001;

import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

import java.awt.Color;

import spacesettlers.graphics.RectangleGraphics;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;

public class GridSquare {
	double startX;
	double endX;
	double startY;
	double endY;
	Position center;
	
	double pathCost;
	boolean isEmpty = false;
	boolean containsGoal = false;
	boolean containsShip = false;
	
	public GridSquare(double startX, double endX, double startY, double endY)
	{
		this.startX = startX;
		this.endX = endX;
		
		this.startY = startY;
		this.endY = endY;
		
		this.center = new Position((endX + startX) / 2, (endY + startY) / 2);
	}
	
	public void calculatePathCost(Toroidal2DPhysics space, AbstractObject goal, Ship ship)
	{
		if (containsShip)
			this.pathCost = space.findShortestDistance(center, goal.getPosition());
		else if (containsGoal)
			this.pathCost = 0.0;
		else
			this.pathCost = space.findShortestDistance(ship.getPosition(), center) + space.findShortestDistance(center, goal.getPosition());
	}
	
	public boolean containsGoal(Toroidal2DPhysics space, AbstractObject goal)
	{
		if (space.findShortestDistance(center, goal.getPosition()) <= space.getHeight()/25)
			containsGoal = true;
		else
			containsGoal = false;
		
		return containsGoal;
	}
	
	public boolean containsShip(Toroidal2DPhysics space, Ship ship)
	{
		if (space.findShortestDistance(center, ship.getPosition()) <= space.getHeight()/25)
			containsShip = true;
		else
			containsShip = false;
		
		return containsShip;
	}
	
	public boolean isEmpty(Toroidal2DPhysics space)
	{
		if (space.isLocationFree(center, space.getWidth()/50))
			this.isEmpty = true;
		else
			this.isEmpty = false;
		return isEmpty;
	}
	
	public RectangleGraphics getGraphics()
	{
		if (containsShip)
			return new RectangleGraphics((int) (endX - startX), (int) (endY - startY), Color.BLUE, new Position(startX, startY));
		else if (containsGoal)
			return new RectangleGraphics((int) (endX - startX), (int) (endY - startY), Color.YELLOW, new Position(startX, startY));
		else
			return new RectangleGraphics((int) (endX - startX), (int) (endY - startY), Color.RED, new Position(startX, startY));
	}
	
	
	public double getWidth()
	{
		return endX - startX;
	}
	
	public double getHeight()
	{
		return endY - startY;
	}
	
	public double getPathCost()
	{
		return pathCost;
	}
}
