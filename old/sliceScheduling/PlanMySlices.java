/*
 * Copyright (c) 2010 Ecole des Mines de Nantes and Fabien Hermenier.
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

package f4g.optimizer.entropy.plan.constraint.sliceScheduling;

import choco.cp.solver.variables.integer.IntVarEvent;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.util.tools.ArrayUtils;
import choco.kernel.memory.IEnvironment;
import choco.kernel.memory.IStateInt;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.constraints.integer.AbstractLargeIntSConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import gnu.trove.TIntIntHashMap;

import java.util.Arrays;
import java.util.BitSet;

/**
 * A constraint to maintains bounds for all
 * the incoming and the outgoing slices for a node.
 * <p/>
 * TODO: Adapt to have a single constraint for all the nodes, not one per node (unjustified)
 * TODO: Reduce the memory footprint by avoiding to create an array of key for each propagate?
 *
 * @author Fabien Hermenier
 */
public class PlanMySlices extends AbstractLargeIntSConstraint {

    /**
     * My CPU capacity.
     */
    private int capacityCPU;

    /**
     * My memory capacity.
     */
    private int capacityMem;

    /**
     * My identifier.
     */
    private int me;

    //consuming slice part

    /**
     * out[i] = true <=> the consuming slice i will leave me.
     */
    private BitSet out;

    /**
     * The hosting variables of the consuming slices.
     */
    private IntDomainVar[] cHosters;

    /**
     * The moment the consuming slices ends. Same order as the hosting variables.
     */
    private IntDomainVar[] cEnds;

    /**
     * The CPU height for each consuming slice. Same order as the hosting variables.
     */
    private int[] cCPUHeights;

    /**
     * The Memory height for each consuming slice. Same order as the hosting variables.
     */
    private int[] cMemHeights;

    //Demanding slice part
    /**
     * in[i] = true <=> the demanding slice i will come to me.
     */
    private BitSet in;

    /**
     * The hosting variable for each demanding slice.
     */
    private IntDomainVar[] dHosters;

    /*
     * The moment the demanding slices ends. Same order as the hosting variables.
     */
    private IntDomainVar[] dStarts;

    /**
     * The CPU height for each demanding slice. Same order as the hosting variable.
     */
    private int[] dCPUHeights;

    /**
     * The memory heighr for each demanding slice. Same order as the hosting variable.
     */
    private int[] dMemHeights;

    /**
     * The amount of free memory at startup.
     */
    private int startupFreeMem;

    /**
     * The amount of free CPU at startup.
     */
    private int startupFreeCPU;

    private static final int DEBUG = 201;

    private IStateInt toInstantiate;

    private IEnvironment env;

    private int[] associations;

    private int[] revAssociations;

    public static final int NO_ASSOCIATIONS = -1;

    private TIntIntHashMap profileMinCPU = new TIntIntHashMap();

    private TIntIntHashMap profileMinMem = new TIntIntHashMap();

    private int[] sortedMinProfile;

    private TIntIntHashMap profileMaxCPU = new TIntIntHashMap();

    private TIntIntHashMap profileMaxMem = new TIntIntHashMap();

    private int[] sortedMaxProfile;

    /**
     * Make a new constraint.
     *
     * @param env         the solving environment
     * @param me          the identifier of the node
     * @param capacityCPU the CPU capacity of the node
     * @param capacityMem the memory capacity of the node
     * @param cHosters    The hoster variable for all the consuming slices
     * @param cCPUHeights the CPU height for the consuming slices (same order as cHosters)
     * @param cMemHeights the memory height for the consuming slices (same order as cHosters)
     * @param cEnds       the moments the consuming slices will end (same order as cHosters)
     * @param dHosters    the hoster variable for all the demanding slices
     * @param dCPUHeights the CPU height for the demanding slices (same order as dHosters)
     * @param dMemHeights the memory height for the demanding slices (same order as dHosters)
     * @param dStarts     the moments the demanding slices will starts (same order as dHosters)
     */
    public PlanMySlices(IEnvironment env,
                        int me,
                        int capacityCPU,
                        int capacityMem,
                        IntDomainVar[] cHosters,
                        int[] cCPUHeights,
                        int[] cMemHeights,
                        IntDomainVar[] cEnds,
                        IntDomainVar[] dHosters,
                        int[] dCPUHeights,
                        int[] dMemHeights,
                        IntDomainVar[] dStarts,
                        int[] assocs
    ) {
        super(ArrayUtils.append(dHosters, cHosters, cEnds, dStarts));
        this.associations = assocs;
        this.me = me;
        this.env = env;
        this.capacityCPU = capacityCPU;
        this.capacityMem = capacityMem;
        this.cHosters = cHosters;
        this.cEnds = cEnds;
        this.cCPUHeights = cCPUHeights;
        this.cMemHeights = cMemHeights;
        this.out = new BitSet(this.cHosters.length);

        this.dHosters = dHosters;
        this.dStarts = dStarts;
        this.dCPUHeights = dCPUHeights;
        this.dMemHeights = dMemHeights;
        this.in = new BitSet(this.dHosters.length);

        revAssociations = new int[cCPUHeights.length];
        for (int i = 0; i < revAssociations.length; i++) {
            revAssociations[i] = NO_ASSOCIATIONS;
        }

        for (int i = 0; i < associations.length; i++) {
            if (associations[i] != NO_ASSOCIATIONS) {
                revAssociations[associations[i]] = i;
            }
        }
    }

    @Override
    public void propagate() throws ContradictionException {
        if (isFull2()) {
            if (me == DEBUG) {
                ChocoLogging.getBranchingLogger().finest("PlanMySlices activated");
            }

            this.updateResourceDistribution();

            computeProfiles();
            checkInvariant();
            updateDStartsSup();
            updateCEndsSup();
            updateDStartsInf();
        }
    }


    /**
     * Update the structure of the constraints.
     * Set the moments for the LB and the UB.
     *
     * @throws ContradictionException if an instantiation is not consistent
     */
    private void updateResourceDistribution() throws ContradictionException {
        in.clear();
        for (int i = 0; i < dHosters.length; i++) {
            if (dHosters[i].isInstantiated() && dHosters[i].getVal() == me) {
                in.set(i);
            }
        }
    }

    /**
     * Translation for a relatives resources changes to an absolute free resources.
     *
     * @param changes       the map that indicates the free CPU variation
     * @param sortedMoments the different moments sorted in ascending order
     */
    private static void toAbsoluteFreeResources(TIntIntHashMap changes, int[] sortedMoments) {
        for (int i = 1; i < sortedMoments.length; i++) {
            int t = sortedMoments[i];
            int lastT = sortedMoments[i - 1];
            int lastFree = changes.get(lastT);

            changes.put(t, changes.get(t) + lastFree);
        }
    }

    @Override
    public void awake() throws ContradictionException {
        out.clear();
        for (int i = 0; i < cHosters.length; i++) {
            if (cHosters[i].getVal() == me) {
                out.set(i);
            }
        }

        //The amount of free resources at startup
        startupFreeMem = capacityMem;
        startupFreeCPU = capacityCPU;

        for (int j = out.nextSetBit(0); j >= 0; j = out.nextSetBit(j + 1)) {
            startupFreeCPU -= cCPUHeights[j];
            startupFreeMem -= cMemHeights[j];
        }
        this.toInstantiate = env.makeInt(dHosters.length);
        //Check wether some hosting variable are already instantiated
        for (int i = 0; i < dHosters.length; i++) {
            if (dHosters[i].isInstantiated()) {
                if (me == DEBUG) {
                    ChocoLogging.getBranchingLogger().finest("Already instantiated:" + dHosters[i]);
                }
                toInstantiate.set(toInstantiate.get() - 1);
            }
        }
        if (me == DEBUG) {
            ChocoLogging.getBranchingLogger().finest("me= " + me + " cpuCapa=" + capacityCPU + ", memCapa=" + capacityMem);
            ChocoLogging.getBranchingLogger().finest(toInstantiate.get() + " placement variable to instantiate before activation");
        }
    }

    @Override
    public void awakeOnInst(int idx) throws ContradictionException {
        if (idx < dHosters.length) {
            toInstantiate.set(toInstantiate.get() - 1);
            if (me == DEBUG && dHosters[idx].getVal() == me) {
                ChocoLogging.getBranchingLogger().finest(me + "-- " + dHosters[idx].getName() + " on me. Still waiting for " + toInstantiate.get());
            }
        }
        this.constAwake(false);
    }

    @Override
    public boolean isSatisfied() {
        int[] vals = new int[vars.length];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = vars[i].getVal();
        }
        return isSatisfied(vals);
    }

    @Override
    public boolean isSatisfied(int[] vals) {
        //Split this use tab to ease the analysis
        int[] dHostersVals = new int[dHosters.length];
        int[] dStartsVals = new int[dStarts.length];
        int[] cHostersVals = new int[cHosters.length];
        int[] cEndsVals = new int[cEnds.length];

        //dHosters, cHosters, cEnds, dStarts
        for (int i = 0; i < dHosters.length; i++) {
            dHostersVals[i] = vals[i];
            dStartsVals[i] = vals[i + dHosters.length + cHosters.length + cEnds.length];
        }

        for (int i = 0; i < cHosters.length; i++) {
            cHostersVals[i] = vals[i + dHosters.length];
            cEndsVals[i] = vals[i + dHosters.length + cHosters.length];
        }


        //A hashmap to save the changes (relatives to the previous moment) in the resources distribution
        TIntIntHashMap cpuChanges = new TIntIntHashMap();
        TIntIntHashMap memChanges = new TIntIntHashMap();
        for (int i = 0; i < dHostersVals.length; i++) {
            if (dHostersVals[i] == me) {
                cpuChanges.put(dStartsVals[i], cpuChanges.get(dStartsVals[i]) - dCPUHeights[i]);
                memChanges.put(dStartsVals[i], memChanges.get(dStartsVals[i]) - dMemHeights[i]);
            }
        }

        int currentFreeCPU = capacityCPU;
        int currentFreeMem = capacityMem;
        for (int i = 0; i < cHostersVals.length; i++) {
            if (cHostersVals[i] == me) {
                cpuChanges.put(cEndsVals[i], cpuChanges.get(cEndsVals[i]) + cCPUHeights[i]);
                memChanges.put(cEndsVals[i], memChanges.get(cEndsVals[i]) + cMemHeights[i]);
                currentFreeCPU -= cCPUHeights[i];
                currentFreeMem -= cMemHeights[i];
            }
        }
        //Now we check the evolution of the absolute free space.

        if (me == DEBUG) {
            ChocoLogging.getBranchingLogger().finest("--- " + me + " isSatisfied() ---");
            for (int i = 0; i < cHostersVals.length; i++) {
                ChocoLogging.getBranchingLogger().finest(me + " " + cEnds[i].pretty() + " ends at " + cEndsVals[i]);
            }
            for (int i = 0; i < dHostersVals.length; i++) {
                ChocoLogging.getBranchingLogger().finest(dStarts[i].pretty());
            }
            ChocoLogging.getBranchingLogger().finest(me + " currentFreeCPU=" + currentFreeCPU);
            ChocoLogging.getBranchingLogger().finest(me + " currentFreeMem=" + currentFreeMem);
            ChocoLogging.getBranchingLogger().finest(cpuChanges.toString());
            ChocoLogging.getBranchingLogger().finest(memChanges.toString());
        }
        for (int i = 0; i < cpuChanges.keys().length; i++) {
            currentFreeCPU += cpuChanges.get(i);
            currentFreeMem += memChanges.get(i);
            if (currentFreeCPU < 0 || currentFreeMem < 0) {
                ChocoLogging.getMainLogger().severe(me + " at moment " + i + ": freeCPU=" + currentFreeCPU + ", freeMem=" + currentFreeMem);
                return false;
            }
        }
        return true;
    }


    @Override
    public int getFilteredEventMask(int idx) {
        return IntVarEvent.INSTINT_MASK;
    }

    private boolean isFull2() {
        return toInstantiate.get() == 0;
    }

    private void computeProfiles() {
        //Sur de ce qui est utilise sur la ressource
        profileMinCPU.clear();
        profileMinMem.clear();

        //Maximum simultanee dans le pire des cas sur la ressource
        profileMaxCPU.clear();
        profileMaxMem.clear();


        profileMinCPU.put(0, capacityCPU - startupFreeCPU);
        profileMaxCPU.put(0, capacityCPU - startupFreeCPU);
        profileMinMem.put(0, capacityMem - startupFreeMem);
        profileMaxMem.put(0, capacityMem - startupFreeMem);
        for (int i = out.nextSetBit(0); i >= 0; i = out.nextSetBit(i + 1)) {
            int t = cEnds[i].getInf();
            if (associatedToDSliceOnCurrentNode(i) &&
                    dCPUHeights[revAssociations[i]] > cCPUHeights[i]) {
                if (me == DEBUG) {
                    ChocoLogging.getBranchingLogger().finest(me + " " + cEnds[i].pretty() + " increasing");
                }
                profileMaxCPU.put(t, profileMaxCPU.get(t) - cCPUHeights[i]);
                profileMaxMem.put(t, profileMaxMem.get(t) - cMemHeights[i]);
            } else {
                if (me == DEBUG) {
                    ChocoLogging.getBranchingLogger().finest(me + " " + cEnds[i].pretty() + " decreasing or non-associated (" + dStarts[revAssociations[i]].pretty() + "?)");
                }
                profileMinCPU.put(t, profileMinCPU.get(t) - cCPUHeights[i]);
                profileMinMem.put(t, profileMinMem.get(t) - cMemHeights[i]);
            }

            t = cEnds[i].getSup();
            if (associatedToDSliceOnCurrentNode(i) &&
                    dCPUHeights[revAssociations[i]] > cCPUHeights[i]) {
                profileMinCPU.put(t, profileMinCPU.get(t) - cCPUHeights[i]);
                profileMinMem.put(t, profileMinMem.get(t) - cMemHeights[i]);
            } else {
                profileMaxCPU.put(t, profileMaxCPU.get(t) - cCPUHeights[i]);
                profileMaxMem.put(t, profileMaxMem.get(t) - cMemHeights[i]);
            }
        }

        for (int i = in.nextSetBit(0); i >= 0; i = in.nextSetBit(i + 1)) {
            int t = dStarts[i].getSup();
            profileMinCPU.put(t, profileMinCPU.get(t) + dCPUHeights[i]);
            profileMinMem.put(t, profileMinMem.get(t) + dMemHeights[i]);

            t = dStarts[i].getInf();
            profileMaxCPU.put(t, profileMaxCPU.get(t) + dCPUHeights[i]);
            profileMaxMem.put(t, profileMaxMem.get(t) + dMemHeights[i]);
        }

        //Now transforms into an absolute profile
        sortedMinProfile = null;
        sortedMinProfile = profileMinCPU.keys();
        Arrays.sort(sortedMinProfile);

        sortedMaxProfile = null;
        sortedMaxProfile = profileMaxCPU.keys();
        profileMaxCPU.keys(sortedMaxProfile);
        Arrays.sort(sortedMaxProfile);

        toAbsoluteFreeResources(profileMinCPU, sortedMinProfile);
        toAbsoluteFreeResources(profileMinMem, sortedMinProfile);
        toAbsoluteFreeResources(profileMaxCPU, sortedMaxProfile);
        toAbsoluteFreeResources(profileMaxMem, sortedMaxProfile);

        if (me == DEBUG) {
            ChocoLogging.getBranchingLogger().finest("---" + me + "--- startup=(" + startupFreeCPU + "; " + startupFreeMem + ") init=(" + capacityCPU + "; " + capacityMem + ")");
            for (int i = in.nextSetBit(0); i >= 0; i = in.nextSetBit(i + 1)) {
                ChocoLogging.getBranchingLogger().finest((dStarts[i].isInstantiated() ? "!" : "?") + " " + dStarts[i].pretty() + " " + dCPUHeights[i] + " " + dMemHeights[i]);
            }

            for (int i = out.nextSetBit(0); i >= 0; i = out.nextSetBit(i + 1)) {
                ChocoLogging.getBranchingLogger().finest((cEnds[i].isInstantiated() ? "!" : "?") + " " + cEnds[i].pretty() + " " + cCPUHeights[i] + " " + cMemHeights[i]);
            }
            ChocoLogging.getBranchingLogger().finest("---");


            ChocoLogging.getBranchingLogger().finest("profileMin=" + prettyProfile(sortedMinProfile, profileMinCPU, profileMinMem));
            ChocoLogging.getBranchingLogger().finest("profileMax=" + prettyProfile(sortedMaxProfile, profileMaxCPU, profileMaxMem));
        }
    }

    private boolean associatedToDSliceOnCurrentNode(int cSlice) {
        if (revAssociations[cSlice] != NO_ASSOCIATIONS
                && in.get(revAssociations[cSlice])) {
            if (me == DEBUG) {
                ChocoLogging.getBranchingLogger().finest(me + " " + cEnds[cSlice].getName() + " with " + dStarts[revAssociations[cSlice]]);
            }
            return true;
        }
        return false;
    }

    private boolean associatedToCSliceOnCurrentNode(int dSlice) {
        if (associations[dSlice] != NO_ASSOCIATIONS
                && out.get(associations[dSlice])) {
            if (me == DEBUG) {
                ChocoLogging.getBranchingLogger().finest(me + " " + dStarts[dSlice].getName() + " with " + cEnds[associations[dSlice]]);
            }
            return true;
        }
        return false;
    }

    private static String prettyProfile(int[] ascMoments, TIntIntHashMap cpuProfile, TIntIntHashMap memProfile) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ascMoments.length; i++) {
            int t = ascMoments[i];
            b.append(t);
            b.append(":(");
            b.append(cpuProfile.get(t));
            b.append(",");
            b.append(memProfile.get(t));
            b.append(")");
            if (i != ascMoments.length - 1) {
                b.append(" ");
            }
        }
        return b.toString();
    }

    private void checkInvariant() throws ContradictionException {
        for (int i = 0; i < sortedMinProfile.length; i++) {
            int t = sortedMinProfile[i];
            if (profileMinCPU.get(t) > capacityCPU || profileMinMem.get(t) > capacityMem) {
                //if (me == DEBUG) {
                ChocoLogging.getBranchingLogger().finest(me + ": Invalid profile at moment " + t + " - " + prettyProfile(sortedMinProfile, profileMinCPU, profileMinMem));
                //}
                fail();
            }
        }
    }

    private void updateDStartsInf() throws ContradictionException {
        for (int i = in.nextSetBit(0); i >= 0; i = in.nextSetBit(i + 1)) {
            if (!dStarts[i].isInstantiated() && !associatedToCSliceOnCurrentNode(i)) {
                int dCpu = dCPUHeights[i];
                int dMem = dMemHeights[i];

                int lastT = -1;
                for (int x = sortedMinProfile.length - 1; x >= 0; x--) {
                    int t = sortedMinProfile[x];
                    if (t <= dStarts[i].getInf()) {
                        break;
                    }
                    int prevT = sortedMinProfile[x - 1];
                    if (t <= dStarts[i].getSup()
                            && (profileMinCPU.get(prevT) + dCpu > capacityCPU || profileMinMem.get(prevT) + dMem > capacityMem)) {
                        lastT = t;
                        break;
                    }
                }
                if (lastT != -1) {
                    if (me == DEBUG) {
                        ChocoLogging.getBranchingLogger().finest(me + ": " + dStarts[i].pretty() + " lb =" + lastT);
                    }
                    dStarts[i].setInf(lastT);
                }
            }
        }
    }

    private void updateDStartsSup() throws ContradictionException {

        int lastSup = -1;
        for (int i = sortedMaxProfile.length - 1; i >= 0; i--) {
            int t = sortedMaxProfile[i];
            if (profileMaxCPU.get(t) <= capacityCPU && profileMaxMem.get(t) <= capacityMem) {
                lastSup = t;
            } else {
                break;
            }
        }
        if (me == DEBUG) {
            ChocoLogging.getBranchingLogger().finest(me + ": lastSup=" + lastSup);
        }
        if (lastSup != -1) {
            for (int i = in.nextSetBit(0); i >= 0; i = in.nextSetBit(i + 1)) {
                if (!dStarts[i].isInstantiated() && !associatedToCSliceOnCurrentNode(i) && dStarts[i].getSup() > lastSup) {
                    int s = Math.max(dStarts[i].getInf(), lastSup);
                    if (me == DEBUG) {
                        ChocoLogging.getBranchingLogger().finest(me + ": " + dStarts[i].pretty() + " ub=" + s + ");");
                    }
                    dStarts[i].setSup(s);
                }
            }
        }
    }

    private void updateCEndsSup() throws ContradictionException {
        for (int i = out.nextSetBit(0); i >= 0; i = out.nextSetBit(i + 1)) {
            if (!cEnds[i].isInstantiated() && !associatedToDSliceOnCurrentNode(i)) {
                int cCpu = cCPUHeights[i];
                int cMem = cMemHeights[i];
                int lastT = -1;
                for (int x = 0; x < sortedMinProfile.length; x++) {
                    int t = sortedMinProfile[x];
                    if (t >= cEnds[i].getSup()) {
                        break;
                    } else if (t >= cEnds[i].getInf() && (profileMinCPU.get(t) + cCpu > capacityCPU
                            || profileMinMem.get(t) + cMem > capacityMem)) {
                        lastT = t;
                        break;
                    }
                }
                if (lastT != -1) {
                    if (me == DEBUG) {
                        ChocoLogging.getBranchingLogger().finest(me + ": " + cEnds[i].pretty() + " cEndsSup =" + lastT);
                    }
                    cEnds[i].setSup(lastT);
                }

            }
        }
    }
}
