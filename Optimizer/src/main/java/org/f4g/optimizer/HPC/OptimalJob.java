/**
* ============================== Header ============================== 
* file:          OptimalJob.java
* project:       FIT4Green/Optimizer
* created:       29 nov. 2010 by omammela
* last modified: $LastChangedDate: 2011-10-21 14:40:57 +0200 (vie, 21 oct 2011) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 923 $
* 
* short description:
*   Optimal job structure for the backfill best fit algorithm
* ============================= /Header ==============================
*/


package org.f4g.optimizer.HPC;

import java.util.Vector;

public class OptimalJob {
	
	private int freeNodes = -1;
	private int freeCores = -1;
	private double freeMemory = -1;
	private int nodes = 0;
	private int cores = 0;
	private double memory = 0;
	Vector<Integer> serverNumber = new Vector<Integer> ();
	private int jobIndex;
	
	long wallTime = 0;
	
	public void setFreeNodes(int freeNodes) {
		this.freeNodes= freeNodes;
	}
	
	public int getFreeNodes() {
		return freeNodes;
	}
	
	public void setFreeCores(int freeCores) {
		this.freeCores= freeCores;
	}
	
	public int getFreeCores() {
		return freeCores;
	}
	
	public void setNodes(int nodes) {
		this.nodes= nodes;
	}
	
	public int getNodes() {
		return nodes;
	}
	
	public void setCores(int cores) {
		this.cores= cores;
	}
	
	public int getCores() {
		return cores;
	}
	
	public void setMemory(double memory) {
		this.memory= memory;
	}
	
	public double getMemory() {
		return memory;
	}
	
	public void setWallTime(long wallTime) {
		this.wallTime= wallTime;
	}
	
	public long getWallTime() {
		return wallTime;
	}
	
	public void setFreeMemory(double freeMemory) {
		this.freeMemory= freeMemory;
	}
	
	public double getFreeMemory() {
		return freeMemory;
	}
	
	public void setJobIndex(int jobIndex)
	{
		this.jobIndex = jobIndex;
	}
	
	public int getJobIndex()
	{
		return jobIndex;
	}
	
	public void addServerNumber(int serverNumber)
	{
		this.serverNumber.add(serverNumber);
	}
	
	public void clearServerNumber()
	{
		this.serverNumber.clear();
	}
	
	public int getServerNumber(int index)
	{
		return this.serverNumber.get(index);
	}
	

}
