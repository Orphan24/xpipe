package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class DialogElement {

    protected final String id;

    public DialogElement() {
        this.id = UUID.randomUUID().toString();
    }

    public abstract String toDisplayString();

    public boolean requiresExplicitUserInput() {
        return false;
    }

    public boolean apply(String value) {
        throw new UnsupportedOperationException();
    }
}
