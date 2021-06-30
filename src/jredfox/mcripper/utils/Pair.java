package jredfox.mcripper.utils;

public class Pair<L, R> {
	
	public L left;
	public R right;
	
	public Pair(L l, R r)
	{
		this.left = l;
		this.right = r;
	}
	
	public L getLeft()
	{
		return this.left;
	}
	
	public R getRight()
	{
		return this.right;
	}
	
	public void setLeft(L l)
	{
		this.left = l;
	}
	
	public void setRight(R r)
	{
		this.right = r;
	}
	
	@Override
	public int hashCode()
	{
		return 31 * this.left.hashCode() + this.right.hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if(!(other instanceof Pair))
			return false;
		Pair<?, ?> o = (Pair<?, ?>) other;
		return this.left.equals(o.left) && this.right.equals(o.right);
	}
	
	@Override
	public String toString()
	{
		return "{" + this.left + ", " + this.right + "}";
	}

}
