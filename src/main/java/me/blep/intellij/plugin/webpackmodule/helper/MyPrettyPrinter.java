package me.blep.intellij.plugin.webpackmodule.helper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

import java.io.IOException;

public class MyPrettyPrinter extends DefaultPrettyPrinter {

    public MyPrettyPrinter(String indent) {
        super(indent);
        _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE.withIndent(indent);
        _objectIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE.withIndent(indent);
        _objectFieldValueSeparatorWithSpaces = ": ";
    }

    public MyPrettyPrinter(DefaultPrettyPrinter base) {
        super(base);
    }

    @Override
    public MyPrettyPrinter createInstance() {
        if (getClass() != MyPrettyPrinter.class) {
            throw new IllegalStateException("Failed `createInstance()`: " + getClass().getName()
                    + " does not override method; it has to");
        }
        return new MyPrettyPrinter(this);
    }

    @Override
    public MyPrettyPrinter withSeparators(Separators separators) {
        this._separators = separators;
        _objectFieldValueSeparatorWithSpaces = ": ";
        return this;
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
        if (!_arrayIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfValues > 0) {
            _arrayIndenter.writeIndentation(g, _nesting);
        }
        g.writeRaw(']');
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
        if (!_objectIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfEntries > 0) {
            _objectIndenter.writeIndentation(g, _nesting);
        }
        g.writeRaw('}');
    }
}