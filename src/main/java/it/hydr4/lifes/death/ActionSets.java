package it.hydr4.lifes.death;

import java.util.List;

/** The four configured action pipelines, built once per settings load. */
public record ActionSets(List<LifesAction> death, List<LifesAction> exhaustion, List<LifesAction> gain, List<LifesAction> resurrect) {
    public ActionSets {
        death = List.copyOf(death);
        exhaustion = List.copyOf(exhaustion);
        gain = List.copyOf(gain);
        resurrect = List.copyOf(resurrect);
    }

    public ActionSets(List<LifesAction> death, List<LifesAction> exhaustion) {
        this(death, exhaustion, List.of(), List.of());
    }

    public static ActionSets empty() {
        return new ActionSets(List.of(), List.of(), List.of(), List.of());
    }
}
