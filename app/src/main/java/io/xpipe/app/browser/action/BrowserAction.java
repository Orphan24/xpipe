package io.xpipe.app.browser.action;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.util.ModuleLayerLoader;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public interface BrowserAction {

    List<BrowserAction> ALL = new ArrayList<>();

    static List<BrowserLeafAction> getFlattened(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return ALL.stream()
                .map(browserAction -> getFlattened(browserAction, model, entries))
                .flatMap(List::stream)
                .toList();
    }

    static List<BrowserLeafAction> getFlattened(
            BrowserAction browserAction, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return browserAction instanceof BrowserLeafAction
                ? List.of((BrowserLeafAction) browserAction)
                : ((BrowserBranchAction) browserAction)
                        .getBranchingActions(model, entries).stream()
                                .map(action -> getFlattened(action, model, entries))
                                .flatMap(List::stream)
                                .toList();
    }

    static BrowserLeafAction byId(String id, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return getFlattened(model, entries).stream()
                .filter(browserAction -> id.equals(browserAction.getId()))
                .findAny()
                .orElseThrow();
    }

    default List<BrowserEntry> resolveFilesIfNeeded(List<BrowserEntry> selected) {
        return automaticallyResolveLinks()
                ? selected.stream()
                        .map(browserEntry ->
                                new BrowserEntry(browserEntry.getRawFileEntry().resolved(), browserEntry.getModel()))
                        .toList()
                : selected;
    }

    MenuItem toMenuItem(BrowserFileSystemTabModel model, List<BrowserEntry> selected);

    default void init(BrowserFileSystemTabModel model) throws Exception {}

    default String getProFeatureId() {
        return null;
    }

    default Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return null;
    }

    default Category getCategory() {
        return null;
    }

    default KeyCombination getShortcut() {
        return null;
    }

    default boolean acceptsEmptySelection() {
        return false;
    }

    ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries);

    default boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return true;
    }

    default boolean automaticallyResolveLinks() {
        return true;
    }

    default boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return true;
    }

    enum Category {
        CUSTOM,
        OPEN,
        NATIVE,
        COPY_PASTE,
        MUTATION
    }

    class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL.addAll(ServiceLoader.load(layer, BrowserAction.class).stream()
                    .map(actionProviderProvider -> actionProviderProvider.get())
                    .filter(provider -> {
                        try {
                            return true;
                        } catch (Throwable e) {
                            ErrorEvent.fromThrowable(e).handle();
                            return false;
                        }
                    })
                    .toList());
        }
    }
}
