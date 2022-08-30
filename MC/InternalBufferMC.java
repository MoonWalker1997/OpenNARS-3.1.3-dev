package nars.MC;

import nars.entity.*;
import nars.inference.BudgetFunctions;
import nars.inference.TemporalRules;
import nars.inference.TruthFunctions;
import nars.io.Symbols;
import nars.language.CompoundTerm;
import nars.language.Conjunction;
import nars.language.Statement;
import nars.language.Term;
import nars.storage.Memory;

import java.util.ArrayList;

import static java.lang.Float.min;

public class InternalBufferMC extends EventBufferMC{

    public InternalBufferMC(int num_slot, int observation_capacity, int anticipation_capacity, int prediction_capacity, Memory memory) {
        super(num_slot, observation_capacity, anticipation_capacity, prediction_capacity, memory);
    }

    /**
     * As the compound generation method in the internal buffer, it is assumed to consider the mental operations carried out.
     * Though they are not here yet.
     */
    public void compound_generation() {
        ArrayList<Task> cpd = new ArrayList<Task>();

        ArrayList<Task> concurrentTasks = new ArrayList<>();
        ArrayList<PriorityPairMC> currentTasks = this.timeSlots.get(this.present).getConcurrent_observations().get_data();
        for (int i = 0; i < currentTasks.size(); i++) {
            Task singleTask = currentTasks.get(i).getContent();
            concurrentTasks.add(singleTask);
        }
        ArrayList<Task> compoundTasks = new ArrayList<>();
        int N = concurrentTasks.size();
        boolean[] flag = new boolean[N];
        for (int k = 1; k <= N; k++) {
            dfs(concurrentTasks, -1, k, flag, N);
        }
        if (this.timeSlots.get(this.present).getConcurrent_observations().get_highest(false) != null) {
            this.timeSlots.get(this.present).setHighest_concurrent_compound(this.timeSlots.get(this.present).getConcurrent_observations().get_highest(false).getContent());
        }
    }

}
