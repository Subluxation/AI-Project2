package nguy0001;

import java.util.ArrayList;

public class TreeGrid<GridSquare> {
	private ArrayList<TreeGrid<GridSquare>> children = new ArrayList<TreeGrid<GridSquare>>();
	private TreeGrid<GridSquare> parent = null;
	private GridSquare data = null;
	
	// Root Node construction
	public TreeGrid(GridSquare data)
	{
		this.data = data;
	}
	
	// Child Node construction
	public TreeGrid(TreeGrid<GridSquare> parent, GridSquare data)
	{
		this.parent = parent;
		this.data = data;
		
		this.parent.getChildren().add(this);
	}
	
	
	// Returns this node's data
	public GridSquare getData()
	{
		return this.data;
	}
	
	// Returns this node's children
	public ArrayList<TreeGrid<GridSquare>> getChildren()
	{
		return children;
	}
	
	// Sets this node's children
	public void setChildren(ArrayList<TreeGrid<GridSquare>> children)
	{
		this.children = children;
	}
	
	// Go through the entire tree to see if the specified gridSquare is
	// in this tree (DFS)
	public boolean contains(GridSquare grid)
	{
		if (this.data == grid)
			return true;
		else
		{
			// FIFO loop
			for (int i = 0; i < children.size(); i--)
			{
				children.get(i).contains(grid);
			}
		}
		return false;
	}
	
	

}
