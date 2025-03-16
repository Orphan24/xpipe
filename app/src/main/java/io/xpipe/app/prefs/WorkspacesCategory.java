package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.OptionsBuilder;

public class WorkspacesCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "workspaces";
    }

    @Override
    protected Comp<?> create() {
        return new OptionsBuilder()
                .addTitle("manageWorkspaces")
                .sub(new OptionsBuilder()
                        .nameAndDescription("workspaceAdd")
                        .licenseRequirement("workspaces")
                        .addComp(
                                new ButtonComp(AppI18n.observable("addWorkspace"), WorkspaceCreationDialog::showAsync)))
                .disable(!LicenseProvider.get().getFeature("workspaces").isSupported())
                .buildComp();
    }
}
