package nguy0001;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import nguy0001.CustomMove;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
//import spacesettlers.actions.MoveAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.RectangleGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

/**
 * This collector only retrieves beacons using the A * algorithm.
 * 
 * The secondary algorithm used to compare is the Hill Climbing algorithm
 * 
 * @author amy modified by Anthony and Spencer
 */
public class AnthonyModelTeamClient extends TeamClient {
	HashMap<UUID, Ship> asteroidToShipMap;
	HashMap<UUID, Boolean> aimingForBase;
	// Priority Queue of GridSquares
	PriorityQueue<GridSquare> queue;
	// Grid represented by a matrix of GridSquares
	// Queue and Grid both initialized in the initialize() method
	ArrayList<ArrayList<GridSquare>> grid;
	ArrayList<GridSquare> path = new ArrayList<GridSquare>();
	// GridSquare grid;
	UUID asteroidCollectorID;
	double weaponsProbability = 1;
	boolean shouldShoot = false;
	AbstractObject goal;

	/**
	 * Assigns ships to asteroids and beacons, as described above
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		// loop through each ship
		for (AbstractObject actionable : actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;

				// the first time we initialize, decide which ship is the asteroid collector
				if (asteroidCollectorID == null) {
					asteroidCollectorID = ship.getId();
				}

				AbstractAction action = getAggressiveAsteroidCollectorAction(space, ship);
				actions.put(ship.getId(), action);

			} else {
				// it is a base. Heuristically decide when to use the shield (TODO)
				actions.put(actionable.getId(), new DoNothingAction());
			}
		}
		return actions;
	}

	/**
	 * Gets the action for the asteroid collecting ship (while being aggressive
	 * towards the other ships)
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getAggressiveAsteroidCollectorAction(Toroidal2DPhysics space, Ship ship) {
		GridSquare square = null;
		updateObstacles(space, ship, goal);
		if (space.getCurrentTimestep() % 25 == 0 || space.getCurrentTimestep() == 0) {			
			Beacon beacon = pickNearestBeacon(space, ship);
			shouldShoot = false;
			aimingForBase.put(ship.getId(), false);
			this.goal = beacon;
			System.out.println("Yay");
			this.path = HillClimb(space, goal, ship);
			square = this.path.remove(0);
			if (square.containsGoal)
				return new CustomMove(space, ship.getPosition(), beacon.getPosition());
			else
				return new CustomMove(space, ship.getPosition(), square.center);
		}		
		// Gets the first of the path arraylist (i.e., the next step)
		// 'Popping' the queue so next time the next step will be the second, then the
		// third, and so on

		else
			return ship.getCurrentAction();

	}

	public void avoidAsteroid(Toroidal2DPhysics space, Ship object1, Asteroid object2) {
		Vector2D distanceVec = space.findShortestDistanceVector(object1.getPosition(), object2.getPosition());
		Vector2D unitNormal = distanceVec.getUnitVector();
		Vector2D unitTangent = new Vector2D(-unitNormal.getYValue(), unitNormal.getXValue());

		double m1 = object1.getMass();
		double m2 = object2.getMass();

		// get the velocity vectors
		Vector2D velocity1 = object1.getPosition().getTranslationalVelocity();
		Vector2D velocity2 = object2.getPosition().getTranslationalVelocity();

		// get the scalars in each direction
		double u1 = velocity1.dot(unitNormal);
		double u2 = velocity2.dot(unitNormal);
		double t1 = velocity1.dot(unitTangent);

		double v1 = ((u1 * (m1 - m2)) + (2 * m2 * u2)) / (m1 + m2);
		// now get it back to the original space
		Vector2D vel1Normal = unitNormal.multiply(v1);
		Vector2D vel1Tangent = unitTangent.multiply(t1);

		// add the normal and tangential parts
		Vector2D newVelocity1 = vel1Normal.add(vel1Tangent);

		object1.getPosition().setTranslationalVelocity(newVelocity1);
	}
	/*
	 * 
	 * Goes back to base, if there are any asteroids in the path, take a new path
	 * 
	 */

	public AbstractAction goBackToNearestBase(Toroidal2DPhysics space, Ship ship, Base base) {
		AbstractAction newAction = new MoveToObjectAction(space, ship.getPosition(), base,
				ship.getPosition().getTranslationalVelocity());
		return newAction;
	}

	/*
	 * 
	 * Retrieves the chosen Beacon
	 * 
	 */
	public AbstractAction retrieveBeacon(Toroidal2DPhysics space, Ship ship, Beacon beacon) {
		AbstractAction newAction = null;
		newAction = new CustomMove(space, ship.getPosition(), beacon.getPosition(), beacon.getPosition().getTranslationalVelocity());
		return newAction;
	}

	/*
	 * 
	 * Checks if the path to the mineable asteroid is clear of other non-mineable
	 * asteroids Takes a rotated path if it isn't
	 * 
	 */
	public AbstractAction mineAsteroid(Toroidal2DPhysics space, Ship ship, Asteroid asteroid) {
		AbstractAction newAction = null;
		newAction = new MoveToObjectAction(space, ship.getPosition(), asteroid,
				ship.getPosition().getTranslationalVelocity());
		return newAction;
	}

	/**
	 * Find the nearest ship on another team and aim for it
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private Ship pickNearestEnemyShip(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		for (Ship otherShip : space.getShips()) {
			// don't aim for our own team (or ourself)
			if (otherShip.getTeamName().equals(ship.getTeamName())) {
				continue;
			}

			double distance = space.findShortestDistance(ship.getPosition(), otherShip.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestShip = otherShip;
			}
		}

		return nearestShip;
	}

	/**
	 * Find the base for this team nearest to this ship
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private Base findNearestBase(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.MAX_VALUE;
		Base nearestBase = null;

		for (Base base : space.getBases()) {
			if (base.getTeamName().equalsIgnoreCase(ship.getTeamName())) {
				double dist = space.findShortestDistance(ship.getPosition(), base.getPosition());
				if (dist < minDistance) {
					minDistance = dist;
					nearestBase = base;
				}
			}
		}
		return nearestBase;
	}

	/**
	 * Returns the closest asteroid of any value that isn't already being chased by
	 * this team
	 * 
	 * @return
	 */
	private Asteroid pickNearestFreeAsteroid(Toroidal2DPhysics space, Ship ship) {
		Set<Asteroid> asteroids = space.getAsteroids();
		Asteroid bestAsteroid = null;
		double minDistance = Double.MAX_VALUE;

		for (Asteroid asteroid : asteroids) {
			if (!asteroidToShipMap.containsKey(asteroid.getId())) {
				if (asteroid.isMineable()) {
					double dist = space.findShortestDistance(asteroid.getPosition(), ship.getPosition());
					if (dist < minDistance) {
						// System.out.println("Considering asteroid " + asteroid.getId() + " as a best
						// one");
						bestAsteroid = asteroid;
						minDistance = dist;
					}
				}
			}
		}
		// System.out.println("Best asteroid has " + bestMoney);
		return bestAsteroid;
	}

	/**
	 * Find the nearest beacon to this ship
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private Beacon pickNearestBeacon(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Beacon beacon : beacons) {
			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestBeacon = beacon;
			}
		}

		return closestBeacon;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		ArrayList<Asteroid> finishedAsteroids = new ArrayList<Asteroid>();

		for (UUID asteroidId : asteroidToShipMap.keySet()) {
			Asteroid asteroid = (Asteroid) space.getObjectById(asteroidId);
			if (asteroid == null || !asteroid.isAlive() || asteroid.isMoveable()) {
				finishedAsteroids.add(asteroid);
				// System.out.println("Removing asteroid from map");
			}
		}

		for (Asteroid asteroid : finishedAsteroids) {
			asteroidToShipMap.remove(asteroid.getId());
		}

	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		asteroidToShipMap = new HashMap<UUID, Ship>();
		asteroidCollectorID = null;
		aimingForBase = new HashMap<UUID, Boolean>();
		grid = new ArrayList<ArrayList<GridSquare>>();
		queue = new PriorityQueue<GridSquare>(new GridComparator());
		// Width and HeightGrids hold the respective sizes of each grid
		// Where each grid square has a width of 1/25 of the map's width
		// and likewise for its height
		double widthGrid = space.getWidth() / 25;
		double heightGrid = space.getHeight() / 25;
		// temp used to add in the Grid matrix
		ArrayList<GridSquare> temp = new ArrayList<GridSquare>();
		// First grid square at 0,0
		temp.add(new GridSquare(0, widthGrid, 0, heightGrid));
		// First row of gridSquares
		for (int i = 1; i < 25; i++) {
			temp.add(new GridSquare(temp.get(i - 1).endX, temp.get(i - 1).endX + widthGrid, temp.get(i - 1).startY,
					heightGrid));
		}
		grid.add(temp);
		// Rest of the rows
		// Represents the rows
		for (int i = 1; i < 25; i++) {
			temp = new ArrayList<GridSquare>();
			// For the first square of the row, it starts on the very left, and then uses
			// the previous row's endY to determine its startY
			temp.add(new GridSquare(0, widthGrid, grid.get(i - 1).get(0).endY,
					grid.get(i - 1).get(0).endY + heightGrid));
			// Represents the columns
			for (int j = 1; j < 25; j++) {
				// For the next squares in the row, it uses the previous squares in its own row
				// to determine its startX
				temp.add(new GridSquare(temp.get(j - 1).endX, temp.get(j - 1).endX + widthGrid,
						grid.get(i - 1).get(0).endY, grid.get(i - 1).get(0).endY + heightGrid));
			}
			grid.add(temp);
		}
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}

	public void updateObstacles(Toroidal2DPhysics space, Ship ship, AbstractObject goal) {
		for (ArrayList<GridSquare> grids : grid) {
			for (GridSquare square : grids) {
				square.isEmpty(space);
				square.containsShip(space, ship);
				if (goal != null)
					square.containsGoal(space, goal);
//				square.calculatePathCost(space, goal, ship);
			}
		}
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		// TODO Auto-generated method stub
		Set<SpacewarGraphics> graphics = new LinkedHashSet<SpacewarGraphics>();
		for (ArrayList<GridSquare> grids : grid) {
			for (GridSquare square : grids) {
				if (square.containsGoal || square.containsShip) {
					RectangleGraphics rect = square.getGraphics();
					// rect.setFill(true);
					graphics.add(rect);
					if (square.containsShip)
					{
						ArrayList<GridSquare> adjacentGrids = new ArrayList<GridSquare>();
						adjacentGrids = square.getAdjacent(grid);
						for (GridSquare adjGrid: adjacentGrids)
						{
//						GridSquare adjGrid = adjacentGrids.get(0);
							graphics.add(new RectangleGraphics((int) (adjGrid.endX - adjGrid.startX), (int) (adjGrid.endY - adjGrid.startY), Color.WHITE, new Position(adjGrid.startX, adjGrid.startY)));
						}
					}					
					// System.out.println(square.getPathCost());
				}
//				else if (!square.isEmpty) {
//					RectangleGraphics rect = square.getGraphics();
//					rect.setFill(true);
//					graphics.add(rect);
//				} else
//					graphics.add(square.getGraphics());
				// else if (!square.isEmpty && (square.containsShip || square.containsGoal))
				// {
				// graphics.add(square.getGraphics());
				// }
				// else
				// graphics.add(square.getGraphics());
			}
		}
//		
//		if (path != null)
//		{
//			System.out.println(path.size());
//			for (GridSquare adjGrid: path)
//			{
//				graphics.add(new RectangleGraphics((int) (adjGrid.endX - adjGrid.startX), (int) (adjGrid.endY - adjGrid.startY), Color.WHITE, new Position(adjGrid.startX, adjGrid.startY)));
//			}
//			
//		}
		
		return graphics;
	}

	@Override
	/**
	 * If there is enough resourcesAvailable, buy a base. Place it by finding a ship
	 * that is sufficiently far away from the existing bases
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, ResourcePile resourcesAvailable,
			PurchaseCosts purchaseCosts) {

		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		double BASE_BUYING_DISTANCE = 200;
		boolean bought_base = false;

		if (purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					Set<Base> bases = space.getBases();

					// how far away is this ship to a base of my team?
					double maxDistance = Double.MIN_VALUE;
					for (Base base : bases) {
						if (base.getTeamName().equalsIgnoreCase(getTeamName())) {
							double distance = space.findShortestDistance(ship.getPosition(), base.getPosition());
							if (distance > maxDistance) {
								maxDistance = distance;
							}
						}
					}

					if (maxDistance > BASE_BUYING_DISTANCE) {
						purchases.put(ship.getId(), PurchaseTypes.BASE);
						bought_base = true;
						// System.out.println("Buying a base!!");
						break;
					}
				}
			}
		}

		// see if you can buy EMPs
		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_EMP_LAUNCHER, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;

					if (!ship.getId().equals(asteroidCollectorID)
							&& !ship.isValidPowerup(PurchaseTypes.POWERUP_EMP_LAUNCHER.getPowerupMap())) {
						purchases.put(ship.getId(), PurchaseTypes.POWERUP_EMP_LAUNCHER);
					}
				}
			}
		}

		// can I buy a ship?
		// Commented out, don't buy a ship
		// if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable) &&
		// bought_base == false) {
		// for (AbstractActionableObject actionableObject : actionableObjects) {
		// if (actionableObject instanceof Base) {
		// Base base = (Base) actionableObject;
		//
		// purchases.put(base.getId(), PurchaseTypes.SHIP);
		// break;
		// }
		//
		// }
		//
		// }

		return purchases;
	}

	/**
	 * The aggressive asteroid collector shoots if there is an enemy nearby!
	 * 
	 * @param space
	 * @param actionableObjects
	 * @return
	 */
	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, SpaceSettlersPowerupEnum> powerUps = new HashMap<UUID, SpaceSettlersPowerupEnum>();

		for (AbstractActionableObject actionableObject : actionableObjects) {
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.values()[random
					.nextInt(SpaceSettlersPowerupEnum.values().length)];
			if (actionableObject.isValidPowerup(powerup) && random.nextDouble() < weaponsProbability && shouldShoot) {
				powerUps.put(actionableObject.getId(), powerup);
			}
		}

		return powerUps;
	}

	/*
	 * Hill Climb method performs the hill climbing algorithm
	 * 
	 * Looks at its adjacent grids and naively chooses to go to the highest-valued one
	 * 
	 */
	public ArrayList<GridSquare> HillClimb(Toroidal2DPhysics space, AbstractObject goal, Ship ship) {
		ArrayList<GridSquare> temp = new ArrayList<GridSquare>();
		GridSquare shipGrid = null;
		GridSquare goalGrid = null;
		GridSquare nextGrid = null;
		// Root node has the shipGrid
//		TreeGrid<GridSquare> root;
//
//				// Initialize the root node
//		root = new TreeGrid<GridSquare>(shipGrid);
//		for (GridSquare child: root.getData().getAdjacent(grid)) 
//		{
////			child.calculatePathCost(space, goal, ship, shipGrid);
//			if (child.isEmpty || child.containsShip)
//				new TreeGrid<GridSquare>(root, child);
//		}
//		
//		// Get the children's children
//		for (TreeGrid<GridSquare> node: root.getChildren())
//		{
//			for (GridSquare child: node.getData().getAdjacent(grid))
//			{
////				child.calculatePathCost(space, goal, ship, shipGrid);
//				if (!root.contains(child) && (child.isEmpty || child.containsShip))
//				{
//					new TreeGrid<GridSquare>(node, child);
//				}
//			}
//		}
		
		// Find the grid containing the ship and the goal
		for (int i = 0; i < grid.size(); i++)
		{
			for (int j = 0; j < grid.size(); j++)
			{
				if (grid.get(i).get(j).containsGoal(space, goal))
					goalGrid = grid.get(i).get(j);
				else if (grid.get(i).get(j).containsShip(space, ship))
					shipGrid = grid.get(i).get(j);
			}
		}
		
		for (ArrayList<GridSquare> gridCol: grid)
		{
			for (GridSquare sq: gridCol)
				sq.calculatePathCost(space, goal, ship, shipGrid);
		}
		
		// Gets the first adjacent grid to the ship (which should be the one with the lowest pathcost)
		for (int adj = 0; adj < 8; adj++)
		{
			if (shipGrid.getAdjacent(grid).get(adj).isEmpty(space))
			{
				nextGrid = shipGrid.getAdjacent(grid).get(adj);
				temp.add(nextGrid);
				break;
			}
		}
		return temp;
		
	}

}
