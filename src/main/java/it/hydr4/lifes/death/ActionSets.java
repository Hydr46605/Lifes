package it.hydr4.lifes.death;

import java.util.List;

/** The two configured action pipelines, built once per settings load. */
public record ActionSets(List<LifesAction> death, List<LifesAction> exhaustion) {
    public ActionSets {
        death = List.copyOf(death);
        exhaustion = List.copyOf(exhaustion);
    }

    public static ActionSets empty() {
        return new ActionSets(List.of(), List.of());
    }
}
