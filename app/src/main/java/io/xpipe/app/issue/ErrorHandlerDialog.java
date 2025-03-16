package io.xpipe.app.issue;

import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.PlatformState;

import javafx.application.Platform;
import javafx.scene.paint.Color;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.concurrent.atomic.AtomicReference;

public class ErrorHandlerDialog {

    public static void showAndWait(ErrorEvent event) {
        if (PlatformState.getCurrent() == PlatformState.EXITED || event.isOmitted()) {
            ErrorAction.ignore().handle(event);
            return;
        }

        // There might be unfortunate freezes when there are errors on the platform
        // thread on startup
        if (Platform.isFxApplicationThread() && OperationMode.isInStartup()) {
            ErrorAction.ignore().handle(event);
        }

        try {
            PlatformInit.init(true);
            AppMainWindow.init(true);
        } catch (Throwable t) {
            var platformEvent = ErrorEvent.fromThrowable(t).build();
            ErrorAction.ignore().handle(platformEvent);
            ErrorAction.ignore().handle(event);
            return;
        }

        try {
            var modal = new AtomicReference<ModalOverlay>();
            var comp = new ErrorHandlerComp(event, () -> {
                AppDialog.closeDialog(modal.get());
            });
            comp.prefWidth(550);
            var headerId = event.isTerminal() ? "terminalErrorOccured" : "errorOccured";
            modal.set(ModalOverlay.of(headerId, comp, new LabelGraphic.NodeGraphic(() -> {
                var graphic = new FontIcon("mdomz-warning");
                graphic.setIconColor(Color.RED);
                return graphic;
            })));
            AppDialog.showAndWait(modal.get());
            if (comp.getTakenAction().getValue() == null) {
                ErrorAction.ignore().handle(event);
                comp.getTakenAction().setValue(ErrorAction.ignore());
            }
        } catch (Throwable t) {
            ErrorAction.ignore().handle(ErrorEvent.fromThrowable(t).build());
            ErrorAction.ignore().handle(event);
        }
    }
}
