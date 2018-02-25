package nguy0001;

import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

import java.awt.Color;

import spacesettlers.graphics.RectangleGraphics;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;

public class GridSquare {
	double startX;
	public double endX;
	public double startY;
	public double endY;
	Position center;
	
	double pathCost;
	public boolean isEmpty = false;
	public boolean containsGoal = false;
	public boolean containsShip = false;
	boolean inside1 = false;
	boolean inside2 = false;
	
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
	
//	public boolean isEmpty(Toroidal2DPhysics space)
//	{
//		if (space.isLocationFree(center, (int) ((endX - startX) / 2)))//space.getWidth()/50
//			this.isEmpty = true;
//		else
//			this.isEmpty = false;
//		return isEmpty;
//	}
	
	/**
	 * Test for whether or not the object is within the grid
	 * @param space
	 * @param object
	 * @return true if within, false if not.
	 */
	public boolean isWithin(Toroidal2DPhysics space, AbstractObject object) {
		double tempX = object.getPosition().getX();
		double tempY = object.getPosition().getY();
		// ***variance of 5 to try to "include" the objects center and radius***
		if(tempX - 5 >= startX && tempX - 5 <= endX) {
			if(tempY - 5 >= startY && tempY - 5 <= endY) {
				this.inside1 = true;
			}
		}
		else {
			this.inside1 = false;
		}
		if(tempX + 5 >= startX && tempX + 5 <= endX) {
			if(tempY + 5 >= startY && tempY + 5 <= endY) {
				this.inside2 = true;
			}
		}
		else {
			this.inside2 = false;
		}
		
		return (inside1 || inside2);
	}
	/**
	 * Checks all 8 locations around the main grid to see if this grid is adjacent to it
	 * @param space
	 * @param mainGrid
	 * @return true if adjacent, false is not.
	 */
	public boolean isAdjacent(GridSquare mainGrid) {
		//to the EAST
		if(center.getX() + getWidth() == mainGrid.center.getX()) {
			return true;
		}
		//to the WEST
		if(center.getX() - getWidth() == mainGrid.center.getX()) {
			return true;
		}
		//to the NORTH
		if(center.getY() + getHeight() == mainGrid.center.getY()) {
			return true;
		}
		//to the SOUTH
		if(center.getY() - getHeight() == mainGrid.center.getY()) {
			return true;
		}
		//to the NORTH-EAST
		if(new Position(center.getX() + getWidth(),center.getY() + getHeight()) == mainGrid.center) {
			return true;
		}
		//to the SOUTH-EAST
		if(new Position(center.getX() - getWidth(),center.getY() - getHeight()) == mainGrid.center) {
			return true;
		}
		//to the SOUTH-WEST
		if(new Position(center.getX() - getWidth(),center.getY() - getHeight()) == mainGrid.center) {
			return true;
		}
		//to the NORTH-WEST
		if(new Position(center.getX() - getWidth(),center.getY() + getHeight()) == mainGrid.center) {
			return true;
		}
		//if not adjacent, return false
		return false;
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
	/**
	 * Tests whether or not there is an object in the grid
	 * @param space
	 * @return isEmpty variable
	 */
	public boolean isEmpty(Toroidal2DPhysics space)
	{
		for(AbstractObject object : space.getAllObjects()) {
			if(isWithin(space,object)) {
				this.isEmpty = false;
				return isEmpty;
			}
		}
		this.isEmpty = true;
		return isEmpty;
		
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
