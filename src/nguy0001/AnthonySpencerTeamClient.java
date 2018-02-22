package nguy0001;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
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
 * Modification of the aggressive heuristic asteroid collector to a team that only has one ship.  It 
 * tries to collect resources but it also tries to shoot other ships if they are nearby.
 * 
 * Made changes to retreat to a nearby ally if low on energy
 * 
 * @author amy
 * modified by Anthony and Spencer
 */
public class AnthonySpencerTeamClient extends TeamClient {
	HashMap <UUID, Ship> asteroidToShipMap;
	HashMap <UUID, Boolean> aimingForBase;
	UUID asteroidCollectorID;
	double weaponsProbability = 1;
	boolean shouldShoot = false;
	int threatZone = 200;
	Asteroid goalObject;
	Set<AbstractObject> badAsteroids = Collections.EMPTY_SET;

	// Representing internal state
	String goal = "";

	/**
	 * Assigns ships to asteroids and beacons, as described above
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		Vector2D fast = new Vector2D(100,100);

		System.out.println("The max X is: " + space.getHeight() + " The max Y is:" +space.getWidth());
		
		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;

				// the first time we initialize, decide which ship is the asteroid collector
				if (asteroidCollectorID == null) {
					asteroidCollectorID = ship.getId();
				}

				AbstractAction action = getAggressiveAsteroidCollectorAction(space, ship);
				actions.put(ship.getId(), action);

			} else {
				// it is a base.  Heuristically decide when to use the shield (TODO)
				actions.put(actionable.getId(), new DoNothingAction());
			}
		} 
		return actions;
	}

	/**
	 * Gets the action for the asteroid collecting ship (while being aggressive towards the other ships)
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getAggressiveAsteroidCollectorAction(Toroidal2DPhysics space,
			Ship ship) {
		AbstractAction current = ship.getCurrentAction();
		Position currentPosition = ship.getPosition();
		Vector2D speed = new Vector2D(100,100);

		
		System.out.println("The max X is: " + space.getHeight() + " The max Y is:" +space.getWidth());

		if(space.getCurrentTimestep() % 60 == 0) {
			goalObject = pickHighestValueNearestFreeAsteroid(space, ship);
			return new MoveToObjectAction(space,ship.getPosition(),goalObject);
		}
		if(space.getCurrentTimestep() % 100 == 0) {
			//			for (Asteroid object : space.getAsteroids()) {
			//				if(!object.isMineable()) {
			//					badAsteroids.add((AbstractObject)object);
			//				}
			//			}
			badAsteroids = space.getAllObjects();
			if(!space.isPathClearOfObstructions(ship.getPosition(), goalObject.getPosition(), badAsteroids, ship.getRadius()*2)) {
				System.out.println("Avoiding asteroid...");
				avoidAsteroid1(space,ship,pickNearestUselessAsteroid(space, ship));
			}
		}



		//		if(space.getCurrentTimestep() % 200 == 0) {
		//			System.out.println("Stopping forward velocity...");
		//			ship.getPosition().setTranslationalVelocity(ship.getPosition().getTranslationalVelocity().negate());
		//		}
		//		if(ship.getCurrentAction().isMovementFinished(space)) {
		//			System.out.println("Stopping forward velocity...");
		//			ship.getPosition().setTranslationalVelocity(ship.getPosition().getTranslationalVelocity().negate());
		//		}

		//space.isPathClearOfObstructions(ship.getPosition(), goalPosition, obstructions, ship.getRadius()*2)

		//		if(space.getCurrentTimestep() % 400 == 0) {
		//			//if(ship.getCurrentAction() == null || ship.getCurrentAction().isMovementFinished(space) == true ){
		//			System.out.println("Acquiring new High valued Asteroid...");
		//			goalObject = pickHighestValueNearestFreeAsteroid(space, ship);
		//			return new MoveAction(space,ship.getPosition(),goalObject.getPosition(),goalObject.getPosition().getTranslationalVelocity().add(ship.getPosition().getTranslationalVelocity()));
		//			//}
		//			
		//		}


		//		if(space.getCurrentTimestep() % 70 == 0 && (ship.getCurrentAction().isMovementFinished(space) || ship.isAlive() == false)) {
		//			Asteroid badAsteroid = pickNearestUselessAsteroid(space, ship);
		//			if(isBadAsteroidWithinRadius(space, ship, badAsteroid)) {
		//				System.out.println("Bad Asteroid nearby!!");
		//				Asteroid asteroid = badAsteroid;
		//				return avoidAsteroid(space, ship, asteroid);
		//			}
		//		}




		return ship.getCurrentAction();


		//		//-------------------------------------------------
		//		//Bad Asteroid detection
		//		//
		//		if(isBadAsteroidWithinRadius(space, ship, pickNearestUselessAsteroid(space, ship))) {
		//			//TODO: Avoid asteroid
		//			
		//			Asteroid asteroid = pickNearestUselessAsteroid(space, ship);
		//			avoidAsteroid(space, ship, asteroid);
		//		}
		//		
		//
		//		// aim for a beacon if there isn't enough energy
		//		if (ship.getEnergy() < 2000) {
		//			Beacon beacon = pickNearestBeacon(space, ship);
		//			AbstractAction newAction = null;
		//			// if there is no beacon, then just skip a turn
		//			if (beacon == null) {
		//				newAction = new DoNothingAction();
		//			} else {
		//				newAction = new MoveToObjectAction(space, currentPosition, beacon);
		//			}
		//			aimingForBase.put(ship.getId(), false);
		//			shouldShoot = false;
		//			return newAction;
		//		}
		//
		//
		//		// -------------------------------------------------
		//		// if the ship has resources but energy is low
		//		//
		//		// purpose to get score without risk of getting killed
		//		// -------------------------------------------------
		//		if (ship.getResources().getTotal() > 500 && ship.getEnergy() < 2000) {
		//			Base base = findNearestBase(space, ship);
		//			AbstractAction newAction = new MoveToObjectAction(space, currentPosition, base);
		//			aimingForBase.put(ship.getId(), true);
		//			shouldShoot = false;
		//			return newAction;
		//		}
		//
		//
		//		// if the ship has enough resourcesAvailable, take it back to base
		//		if (ship.getResources().getTotal() > 1000) {
		//			Base base = findNearestBase(space, ship);
		//			AbstractAction newAction = new MoveToObjectAction(space, currentPosition, base);
		//			aimingForBase.put(ship.getId(), true);
		//			shouldShoot = false;
		//			return newAction;
		//		}
		//
		//
		//		// -------------------------------------------------
		//		// if energy is really low and an enemy ship is nearby
		//		//
		//		// if low on energy and potentially getting chased by enemy
		//		// retreat to an ally ship
		//		// -------------------------------------------------
		//
		//		if (ship.getEnergy() < 1000 && pickNearestEnemyShip(space, ship) != null && pickNearestFriendlyShip(space, ship) != null)
		//		{
		//			Ship ally = pickNearestFriendlyShip(space, ship);
		//			AbstractAction newAction = new MoveToObjectAction(space, currentPosition, ally);
		//			aimingForBase.put(ship.getId(), false);
		//			shouldShoot = false;
		//			return newAction;
		//		}
		//
		//
		//		// did we bounce off the base?
		//		if (ship.getResources().getTotal() == 0 && ship.getEnergy() > 2000 && aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
		//			current = null;
		//			aimingForBase.put(ship.getId(), false);
		//			shouldShoot = false;
		//		}
		//
		//		// otherwise either for an asteroid or an enemy ship (depending on who is closer and what we need)
		//		if (current == null || current.isMovementFinished(space)) {
		//			aimingForBase.put(ship.getId(), false);
		//
		//			// see if there is an enemy ship nearby
		//			Ship enemy = pickNearestEnemyShip(space, ship);
		//
		//			// find the highest valued nearby asteroid
		//			Asteroid asteroid = pickHighestValueNearestFreeAsteroid(space, ship);
		//
		//			AbstractAction newAction = null;
		//
		//			// if there is no enemy nearby, go for an asteroid
		//			if (enemy == null) {
		//				if (asteroid != null) {
		//					newAction = new MoveToObjectAction(space, currentPosition, asteroid,
		//							asteroid.getPosition().getTranslationalVelocity().add(ship.getPosition().getTranslationalVelocity()));
		//					shouldShoot = false;
		//					return newAction;
		//				} else {
		//					// no enemy and no asteroid, just skip this turn (shouldn't happen often)
		//					shouldShoot = true;
		//					newAction = new MoveAction();
		//					return newAction;
		//				}
		//			}
		//			//TEST
		//			if (ship.getEnergy() > 0) {
		//				newAction = new MoveToObjectAction(space,currentPosition,pickHighestValueNearestFreeAsteroid(space, ship),pickHighestValueNearestFreeAsteroid(space, ship).getPosition().getTranslationalVelocity().multiply(3));
		//				return newAction;
		//			}
		//
		//			// now decide which one to aim for
		//			if (asteroid != null) {
		//				double enemyDistance = space.findShortestDistance(ship.getPosition(), enemy.getPosition());
		//				double asteroidDistance = space.findShortestDistance(ship.getPosition(), asteroid.getPosition());
		//
		//				// we are aggressive, so aim for enemies if they are nearby
		//				if (enemyDistance < asteroidDistance && enemy.getResources().getTotal() > 0) {
		//					shouldShoot = true;
		//					newAction = new MoveToObjectAction(space, currentPosition, enemy,
		//							enemy.getPosition().getTranslationalVelocity());
		//
		//				} else {
		//					shouldShoot = false;
		//					newAction = new MoveToObjectAction(space, currentPosition, asteroid,
		//							asteroid.getPosition().getTranslationalVelocity());
		//				}
		//				return newAction;
		//			} else {
		//				newAction = new MoveToObjectAction(space, currentPosition, enemy,
		//						enemy.getPosition().getTranslationalVelocity());
		//			}
		//			return newAction;
		//		}
		//
		//		// -------------------------------------------------
		//		// if an asteroid is in the threat zone
		//		//
		//		// avoid it
		//		// -------------------------------------------------
		//		if (isInCollision(space, ship) && !current.isMovementFinished(space)) {
		//			aimingForBase.put(ship.getId(), false);
		//			shouldShoot = false;
		//			Asteroid asteroid = pickNearestUselessAsteroid(space, ship);
		//			return avoidAsteroid(space, ship,asteroid);
		//		}
		//
		//		// return the current if new goals haven't formed
		//		return ship.getCurrentAction();
	}
	public ArrayList gridalize(Toroidal2DPhysics space, Ship ship) {
		double gridMaxX = space.getWidth();
		double gridMaxY = space.getHeight();
		ArrayList adjacentGrid = new ArrayList();
		//Sample of grid size, 40x40
		double shipx = ship.getPosition().getX() + 20;
		double shipy = ship.getPosition().getY() + 20;
		//creating the first grid which contains the ship
		GridSquare gridSHIP  =new GridSquare((shipx - shipy),(shipx + shipy),(shipy - shipx),(shipy + shipx));
		//for all adjacent grids, create a grid for the 8 remaining grids
		//GridSquare gridUPLEFT  =new GridSquare();
		//add all adjacent grids to the adjacentGrid ArrayList
		return adjacentGrid;
		
	}

	/**
	 * Is the bad asteroid within the specified radius of the ship?
	 * @param space
	 * @param ship
	 * @param badAsteroid
	 * @return true if the asteroid is within radius
	 * 			false if the asteroid is not within radius
	 */
	private Boolean isBadAsteroidWithinRadius(Toroidal2DPhysics space, Ship ship, Asteroid badAsteroid) {
		System.out.println("Checking asteroids....");
		int radius = 500;
		for (AbstractObject object : space.getAllObjects()) {
			if (space.findShortestDistanceVector(object.getPosition(), ship.getPosition()).getMagnitude() <= 
					(radius + (2 * object.getRadius()))) {
				if(object.getId() == badAsteroid.getId()) {
					System.out.println("Bad Asteroid within radius!");
					return true;
				}
			}
		}
		return false;
	}


	/**
	 * 	Attempts to avoid the asteroid specified
	 * @param space
	 * @param ship
	 * @param asteroid
	 * @return action corresponding to new position to move to 
	 */
	private AbstractAction avoidAsteroid(Toroidal2DPhysics space, Ship ship, Asteroid asteroid)
	{
		AbstractAction newAction = null;



		Position pos = new Position(asteroid.getPosition().getX(), asteroid.getPosition().getY());

		newAction = new MoveAction(space, ship.getPosition(),pos ,ship.getPosition().getTranslationalVelocity().add(asteroid.getPosition().getTranslationalVelocity()));

		return newAction;
	}

	private boolean isInCollision(Toroidal2DPhysics space, Ship ship)
	{
		Asteroid closestUselessAsteroid = pickNearestUselessAsteroid(space, ship);
		double asteroidDistance = space.findShortestDistance(ship.getPosition(), closestUselessAsteroid.getPosition());
		if (threatZone * ship.getPosition().getTotalTranslationalVelocity() - asteroidDistance > 0)
			return true;
		else
			return false;
	}

	private void avoidAsteroid1(Toroidal2DPhysics space, Ship ship, Asteroid asteroid) {
		// get the masses
		double m1 = ship.getMass();
		double m2 = asteroid.getMass();

		// now get the vector from the first to the second, get the unit normal and tangent
		Vector2D distanceVec = space.findShortestDistanceVector(ship.getPosition(), asteroid.getPosition());
		Vector2D unitNormal = distanceVec.getUnitVector();
		Vector2D unitTangent = new Vector2D(-unitNormal.getYValue(), unitNormal.getXValue());

		// get the velocity vectors
		Vector2D velocity1 = ship.getPosition().getTranslationalVelocity();
		Vector2D velocity2 = asteroid.getPosition().getTranslationalVelocity();

		// get the scalars in each direction
		double u1 = velocity1.dot(unitNormal);
		double u2 = velocity2.dot(unitNormal);
		double t1 = velocity1.dot(unitTangent);
		double t2 = velocity2.dot(unitTangent);

		// elastically collide in the one dimension
		double v1 = ((u1 * (m1 - m2)) + (2 * m2 * u2)) / (m1 + m2);
		double v2 = ((u2 * (m2 - m1)) + (2 * m1 * u1)) / (m1 + m2);



		// now get it back to the original space
		Vector2D vel1Normal = unitNormal.multiply(v1);
		Vector2D vel2Normal = unitNormal.multiply(v2);
		Vector2D vel1Tangent = unitTangent.multiply(t1);
		Vector2D vel2Tangent = unitTangent.multiply(t2);

		// add the normal and tangential parts
		Vector2D newVelocity1 = vel1Normal.add(vel1Tangent);
		Vector2D newVelocity2 = vel2Normal.add(vel2Tangent);

		ship.getPosition().setTranslationalVelocity(newVelocity1);
		asteroid.getPosition().setTranslationalVelocity(newVelocity2);
	}





	/**
	 * Returns the asteroid of no value
	 * 
	 * potentially avoid asteroids instead of running into them
	 * 
	 * @return
	 */
	private Asteroid pickNearestUselessAsteroid(Toroidal2DPhysics space, Ship ship)
	{
		Set<Asteroid> asteroids = space.getAsteroids();
		Asteroid bestAsteroid = null;
		double minDistance = Double.POSITIVE_INFINITY;

		for (Asteroid asteroid : asteroids) {
			if (!asteroid.isMineable()) {
				double dist = space.findShortestDistance(asteroid.getPosition(), ship.getPosition());
				if (dist < minDistance) {
					//System.out.println("Considering asteroid " + asteroid.getId() + " as a best one");
					bestAsteroid = asteroid;
				}
			}
		}
		//System.out.println("Best asteroid has " + bestMoney);
		return bestAsteroid;
	}
	/**
	 * Find the nearest ship on our team and aim for it
	 * Goal is to potentially retreat to ally ship
	 * @param space
	 * @param ship
	 * @return
	 */
	private Ship pickNearestFriendlyShip(Toroidal2DPhysics space, Ship ship)
	{
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		for (Ship otherShip : space.getShips()) {
			// don't aim for our own team (or ourself)
			if (otherShip.getTeamName().equals(ship.getTeamName())) {
				double distance = space.findShortestDistance(ship.getPosition(), otherShip.getPosition());
				if (distance < minDistance) {
					minDistance = distance;
					nearestShip = otherShip;
				}
			}
		}

		return nearestShip;
	}
	/**
	 * Find the nearest ship on another team and aim for it
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
	 * Returns the asteroid of highest value that isn't already being chased by this team
	 * 
	 * @return
	 */
	private Asteroid pickHighestValueNearestFreeAsteroid(Toroidal2DPhysics space, Ship ship) {
		Set<Asteroid> asteroids = space.getAsteroids();
		int bestMoney = Integer.MIN_VALUE;
		Asteroid bestAsteroid = null;
		double minDistance = Double.MAX_VALUE;

		for (Asteroid asteroid : asteroids) {
			if (!asteroidToShipMap.containsKey(asteroid.getId())) {
				if (asteroid.isMineable() && asteroid.getResources().getTotal() > bestMoney) {
					double dist = space.findShortestDistance(asteroid.getPosition(), ship.getPosition());
					if (dist < minDistance) {
						bestMoney = asteroid.getResources().getTotal();
						//System.out.println("Considering asteroid " + asteroid.getId() + " as a best one");
						bestAsteroid = asteroid;
						minDistance = dist;
					}
				}
			}
		}
		//System.out.println("Best asteroid has " + bestMoney);
		return bestAsteroid;
	}


	/**
	 * Find the nearest beacon to this ship
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
				//System.out.println("Removing asteroid from map");
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
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	/**
	 * If there is enough resourcesAvailable, buy a base.  Place it by finding a ship that is sufficiently
	 * far away from the existing bases
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
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
						//System.out.println("Buying a base!!");
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

					if (!ship.getId().equals(asteroidCollectorID) && !ship.isValidPowerup(PurchaseTypes.POWERUP_EMP_LAUNCHER.getPowerupMap())) {
						purchases.put(ship.getId(), PurchaseTypes.POWERUP_EMP_LAUNCHER);
					}
				}
			}		
		} 


		// can I buy a ship?
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable) && bought_base == false) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;

					purchases.put(base.getId(), PurchaseTypes.SHIP);
					break;
				}

			}

		}


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

		for (AbstractActionableObject actionableObject : actionableObjects){
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.values()[random.nextInt(SpaceSettlersPowerupEnum.values().length)];
			if (actionableObject.isValidPowerup(powerup) && random.nextDouble() < weaponsProbability && shouldShoot){
				powerUps.put(actionableObject.getId(), powerup);
			}
		}


		return powerUps;
	}

}
