package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.DerivedObservableList;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

@Getter
public class StoreSection {

    private final StoreEntryWrapper wrapper;
    private final DerivedObservableList<StoreSection> allChildren;
    private final DerivedObservableList<StoreSection> shownChildren;
    private final int depth;
    private final ObservableBooleanValue showDetails;

    public StoreSection(
            StoreEntryWrapper wrapper,
            DerivedObservableList<StoreSection> allChildren,
            DerivedObservableList<StoreSection> shownChildren,
            int depth) {
        this.wrapper = wrapper;
        this.allChildren = allChildren;
        this.shownChildren = shownChildren;
        this.depth = depth;
        if (wrapper != null) {
            this.showDetails = Bindings.createBooleanBinding(
                    () -> {
                        return wrapper.getExpanded().get()
                                || allChildren.getList().isEmpty();
                    },
                    wrapper.getExpanded(),
                    allChildren.getList());
        } else {
            this.showDetails = new SimpleBooleanProperty(true);
        }
    }

    public static Comp<?> customSection(StoreSection e) {
        var prov = e.getWrapper().getEntry().getProvider();
        if (prov != null) {
            return prov.customSectionComp(e);
        } else {
            return new StoreSectionComp(e);
        }
    }

    private static DerivedObservableList<StoreSection> sorted(
            DerivedObservableList<StoreSection> list,
            ObservableValue<StoreCategoryWrapper> category,
            ObservableIntegerValue updateObservable) {
        var explicitOrderComp = Comparator.<StoreSection>comparingInt(new ToIntFunction<>() {
            @Override
            public int applyAsInt(StoreSection value) {
                if (!value.getWrapper().getEntry().getValidity().isUsable()) {
                    return 1;
                }

                var explicit = value.getWrapper().getEntry().getExplicitOrder();
                if (explicit == null) {
                    return 0;
                }

                return switch (explicit) {
                    case TOP -> -1;
                    case BOTTOM -> 1;
                };
            }
        });
        var comp = explicitOrderComp;
        var mappedSortMode =
                BindingsHelper.flatMap(category, storeCategoryWrapper -> storeCategoryWrapper.getSortMode());
        return list.sorted(
                (o1, o2) -> {
                    var r = comp.compare(o1, o2);
                    if (r != 0) {
                        return r;
                    }

                    var current = mappedSortMode.getValue();
                    if (current != null) {
                        return current.comparator().compare(o1, o2);
                    } else {
                        return 0;
                    }
                },
                mappedSortMode,
                updateObservable);
    }

    public static StoreSection createTopLevel(
            DerivedObservableList<StoreEntryWrapper> all,
            Predicate<StoreEntryWrapper> entryFilter,
            ObservableValue<String> filterString,
            ObservableValue<StoreCategoryWrapper> category,
            ObservableIntegerValue updateObservable) {
        var topLevel = all.filtered(
                section -> {
                    return DataStorage.get()
                            .isRootEntry(section.getEntry(), category.getValue().getCategory());
                },
                category,
                updateObservable);
        var cached = topLevel.mapped(storeEntryWrapper ->
                create(List.of(), storeEntryWrapper, 1, all, entryFilter, filterString, category, updateObservable));
        var ordered = sorted(cached, category, updateObservable);
        var shown = ordered.filtered(
                section -> {
                    // matches filter
                    return (filterString == null || section.matchesFilter(filterString.getValue()))
                            &&
                            // matches selector
                            (section.anyMatches(entryFilter))
                            &&
                            // same category
                            (showInCategory(category.getValue(), section.getWrapper()));
                },
                category,
                filterString,
                updateObservable);
        return new StoreSection(null, ordered, shown, 0);
    }

    private static StoreSection create(
            List<StoreEntryWrapper> parents,
            StoreEntryWrapper e,
            int depth,
            DerivedObservableList<StoreEntryWrapper> all,
            Predicate<StoreEntryWrapper> entryFilter,
            ObservableValue<String> filterString,
            ObservableValue<StoreCategoryWrapper> category,
            ObservableIntegerValue updateObservable) {
        if (e.getEntry().getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return new StoreSection(
                    e,
                    new DerivedObservableList<>(FXCollections.observableArrayList(), true),
                    new DerivedObservableList<>(FXCollections.observableArrayList(), true),
                    depth);
        }

        var allChildren = all.filtered(
                other -> {
                    // Legacy implementation that does not use children caches. Use for testing
                    //                                if (true) return DataStorage.get()
                    //                                        .getDefaultDisplayParent(other.getEntry())
                    //                                        .map(found -> found.equals(e.getEntry()))
                    //                                        .orElse(false);

                    // is children. This check is fast as the children are cached in the storage
                    if (DataStorage.get() == null
                            || !DataStorage.get().getStoreChildren(e.getEntry()).contains(other.getEntry())) {
                        return false;
                    }

                    var showProvider = true;
                    try {
                        showProvider = other.getEntry().getProvider().shouldShow(other);
                    } catch (Exception ignored) {
                    }
                    return showProvider;
                },
                e.getPersistentState(),
                e.getCache(),
                updateObservable);
        var l = new ArrayList<>(parents);
        l.add(e);
        var cached = allChildren.mapped(
                c -> create(l, c, depth + 1, all, entryFilter, filterString, category, updateObservable));
        var ordered = sorted(cached, category, updateObservable);
        var filtered = ordered.filtered(
                section -> {
                    // matches filter
                    return (filterString == null
                                    || section.matchesFilter(filterString.getValue())
                                    || l.stream().anyMatch(p -> p.matchesFilter(filterString.getValue())))
                            &&
                            // matches selector
                            section.anyMatches(entryFilter)
                            &&
                            // matches category
                            // Prevent updates for children on category switching by checking depth
                            (showInCategory(category.getValue(), section.getWrapper()) || depth > 0)
                            &&
                            // not root
                            // If this entry is already shown as root due to a different category than parent, don't
                            // show it
                            // again here
                            !DataStorage.get()
                                    .isRootEntry(
                                            section.getWrapper().getEntry(),
                                            category.getValue().getCategory());
                },
                category,
                filterString,
                e.getPersistentState(),
                e.getCache(),
                updateObservable);
        return new StoreSection(e, cached, filtered, depth);
    }

    private static boolean showInCategory(StoreCategoryWrapper categoryWrapper, StoreEntryWrapper entryWrapper) {
        var current = entryWrapper.getCategory().getValue();
        while (current != null) {
            if (categoryWrapper
                    .getCategory()
                    .getUuid()
                    .equals(current.getCategory().getUuid())) {
                return true;
            }

            if (!AppPrefs.get().showChildCategoriesInParentCategory().get()) {
                break;
            }

            current = current.getParent();
        }
        return false;
    }

    public boolean matchesFilter(String filter) {
        return anyMatches(storeEntryWrapper -> storeEntryWrapper.matchesFilter(filter));
    }

    public boolean anyMatches(Predicate<StoreEntryWrapper> c) {
        return c == null
                || c.test(wrapper)
                || allChildren.getList().stream().anyMatch(storeEntrySection -> storeEntrySection.anyMatches(c));
    }
}
