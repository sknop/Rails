/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Track.java,v 1.3 2008/01/17 21:13:48 evos Exp $ */
package rails.game;

/**
 * Represents a piece of track on one tile.
 * <p>
 * Endpoints can be: <br>- a hex side (number >= 0), or <br>- a station
 * (number < 0)
 */
public class Track
{

	int startPoint;
	int endPoint;
	
	// TEMPORARY, because we can't yet handle OO cities correctly.
	int comparableStartPoint;
	int comparableEndPoint;

	public Track(int startPoint, int endPoint)
	{
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		
		// TEMPORARY
		this.comparableStartPoint = Math.max(startPoint, -1);
		this.comparableEndPoint = Math.max(endPoint, -1);
	}

	public boolean hasPoint(int point)
	{
		return startPoint == point || endPoint == point;
	}

	public int[] points()
	{
		return new int[] { startPoint, endPoint };
	}
	
	public int getComparableEndPoint (int comparableStartPoint) {
	    
	    if (comparableStartPoint == this.comparableStartPoint) {
	        return this.comparableEndPoint;
	    } else if (comparableStartPoint == this.comparableEndPoint) {
	        return this.comparableStartPoint;
	    } else {
	        return -99;
	    }
	}

}
