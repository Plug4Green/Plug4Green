/*
 * Copyright (c) Fabien Hermenier
 *
 * This file is part of Entropy.
 *
 * Entropy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Entropy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.f4g.entropy.plan.constraint.sliceScheduling;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ActionModel;
import entropy.plan.choco.actionModel.ShutdownableNodeActionModel;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.plan.choco.actionModel.slice.Slice;
import entropy.plan.choco.constraint.GlobalConstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A global constraint to help to plan all the slices in a reconfiguration problem.
 *
 * @author Fabien Hermenier
 */
public class SlicesPlanner implements GlobalConstraint {

    @Override
    public void add(ReconfigurationProblem rp) {
        List<DemandingSlice> dS = new LinkedList<DemandingSlice>();
        List<ConsumingSlice> cS = new LinkedList<ConsumingSlice>();

        List<int[]> linked = new ArrayList<int[]>();
        int dIdx = 0;
        int cIdx = 0;
        List<ActionModel> allActions = new ArrayList<ActionModel>();
        allActions.addAll(rp.getNodeMachineActions());
        allActions.addAll(rp.getVirtualMachineActions());
        if (allActions.isEmpty()) {
            return;
        }
        //The boolean variable to indicate if a d-slice will be exclusive or not
        IntDomainVar[] excls = new IntDomainVar[rp.getNodes().length];
        int[] exclSlice = new int[rp.getNodes().length];
        for (int sliceIdx = 0; sliceIdx < allActions.size(); sliceIdx++) {
            ActionModel na = allActions.get(sliceIdx);

            if (na.getDemandingSlice() != null && na.getConsumingSlice() != null) {
                linked.add(new int[]{dIdx, cIdx});
            }
            if (na.getDemandingSlice() != null) {
                dS.add(dIdx, na.getDemandingSlice());
                //Check for the exclusive flag for Demanding Slice
                if (na instanceof ShutdownableNodeActionModel) {
                    ShutdownableNodeActionModel a = (ShutdownableNodeActionModel) na;
                    DemandingSlice ds = a.getDemandingSlice();
                    excls[rp.getNode(a.getNode())] = ds.isExclusive();
                    exclSlice[rp.getNode(a.getNode())] = dIdx;//sliceIdx;
                    //System.err.println("excl slice for " + rp.getNode(a.getNode()) + " " + a.getNode().getName() + " is " + dIdx + " " + dS.get(dIdx));
                }
                dIdx++;
            }

            if (na.getConsumingSlice() != null) {
                cS.add(cIdx, na.getConsumingSlice());
                cIdx++;
            }
        }

        //System.err.println(Arrays.toString(exclSlice) + " " + Arrays.toString(excls));

        Slice[] dSlices = dS.toArray(new Slice[dS.size()]);
        Slice[] cSlices = cS.toArray(new Slice[cS.size()]);

        int[] cCPUH = new int[cSlices.length];
        int[] cMemH = new int[cSlices.length];
        IntDomainVar[] cHosters = new IntDomainVar[cSlices.length];
        IntDomainVar[] cEnds = new IntDomainVar[cSlices.length];
        for (int i = 0; i < cSlices.length; i++) {
            Slice c = cSlices[i];
            cCPUH[i] = c.getCPUheight();
            cMemH[i] = c.getMemoryheight();
            cHosters[i] = c.hoster();
            cEnds[i] = c.end();
        }

        int[] dCPUH = new int[dSlices.length];
        int[] dMemH = new int[dSlices.length];
        IntDomainVar[] dHosters = new IntDomainVar[dSlices.length];
        IntDomainVar[] dStart = new IntDomainVar[dSlices.length];
        for (int i = 0; i < dSlices.length; i++) {
            Slice d = dSlices[i];
            dCPUH[i] = d.getCPUheight();
            dMemH[i] = d.getMemoryheight();
            dHosters[i] = d.hoster();
            dStart[i] = d.start();
        }

        int[] associations = new int[dHosters.length];
        for (int i = 0; i < associations.length; i++) {
            associations[i] = PlanMySlices.NO_ASSOCIATIONS; //No associations task
        }
        for (int i = 0; i < linked.size(); i++) {
            int[] assoc = linked.get(i);
            associations[assoc[0]] = assoc[1];
        }
        int[] capaCPU = new int[rp.getNodes().length];
        int[] capaMem = new int[rp.getNodes().length];
        for (int idx = 0; idx < rp.getNodes().length; idx++) {
            Node n = rp.getNodes()[idx];
            capaMem[idx] = n.getMemoryCapacity();
            capaCPU[idx] = n.getCPUCapacity();
        }
        Plan.logger.debug("SlicesPlanner branched");

        rp.post(new SlicesScheduler(rp.getEnvironment(), capaCPU, capaMem, cHosters, cCPUH, cMemH, cEnds,
                dHosters, dCPUH, dMemH, dStart, associations, excls, exclSlice));
    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
        return true;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private static void printLinked(List<int[]> l) {
        for (int[] arr : l) {
            System.err.println(Arrays.toString(arr));
        }
    }
}
