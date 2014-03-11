/*
 * Copyright (c) 2010 Ecole des Mines de Nantes.
 *
 *      This file is part of Entropy.
 *
 *      Entropy is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      Entropy is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package f4g.optimizer.entropy.plan.action;

import btrplace.plan.event.Action;

/**
 * Interface that define a driver to perform an action.
 * 
 * @author Fabien Hermenier
 * 
 */
public abstract class Driver {
	
	/**
	 * The action to execute.
	 */
	private Action action;
	
	/**
	 * Get the action to execute.
	 * @return an action
	 */
	public Action getAction() {
		return this.action;
	}
	
	/**
	 * Create and configure the driver to execute an action.
	 * @param a the action to execute	 
	 */
	public Driver(Action a) {
		this.action = a;
	}
	
	/**
	 * Execute the action.
	 * @throws DriverException if an error occurred during the execution
	 */
	public abstract void execute();
	
	/**
	 * Textual representation of the driver.
	 * @return a String!
	 */
	public abstract String toString();
}
